package mbarete.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.WebUtils;

/**
 * Depois do login, o token de acesso vem do cookie e é conferido só pela assinatura (ADR-0041).
 * Toda rota exige o token, menos as de auth/ (endpoints.md). Só a api sobe isto: os modos do .jar
 * não têm web.
 */
@Configuration
@ConditionalOnWebApplication
public class SegurancaConfig {

  /** As rotas de auth/ funcionam sem token de acesso (endpoints.md). */
  private static final RequestMatcher ROTAS_DE_AUTH =
      PathPatternRequestMatcher.pathPattern("/auth/**");

  @Bean
  SecurityFilterChain seguranca(
      HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver tratador) {
    // A recusa de autenticação sai pelo tratador global, no formato único de erro
    AuthenticationEntryPoint naoAutenticado =
        (pedido, resposta, excecao) -> tratador.resolveException(pedido, resposta, null, excecao);
    return http
        // Sem token contra CSRF: o cookie é SameSite=Lax, e nenhum GET muda dado (ADR-0041)
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            rotas -> rotas.requestMatchers(ROTAS_DE_AUTH).permitAll().anyRequest().authenticated())
        .oauth2ResourceServer(
            recurso ->
                recurso
                    .bearerTokenResolver(
                        pedido -> {
                          // Nas rotas de auth/, o cookie de acesso é ignorado: um token vencido
                          // ou de chave trocada não pode impedir a renovação nem o login
                          if (ROTAS_DE_AUTH.matches(pedido)) {
                            return null;
                          }
                          Cookie cookie = WebUtils.getCookie(pedido, CookiesDaSessao.ACESSO);
                          return cookie == null ? null : cookie.getValue();
                        })
                    .jwt(jwt -> {})
                    .authenticationEntryPoint(naoAutenticado))
        .exceptionHandling(erro -> erro.authenticationEntryPoint(naoAutenticado))
        .build();
  }

  @Bean
  JwtDecoder decodificadorDoToken(@Value("${MBARETE_JWT_CHAVE}") String chave) {
    return NimbusJwtDecoder.withPublicKey(ChaveDoToken.publica(chave)).build();
  }

  @Bean
  JwtEncoder emissorDoToken(@Value("${MBARETE_JWT_CHAVE}") String chave) {
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(ChaveDoToken.par(chave))));
  }
}
