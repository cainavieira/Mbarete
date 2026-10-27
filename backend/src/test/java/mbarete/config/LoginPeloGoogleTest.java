package mbarete.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import mbarete.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * O retorno do Google contra um id_token fabricado (ADR-0022): um Google falso, dentro do teste,
 * troca o código pelo token e publica a chave que o assina. O state, o nonce e a assinatura passam
 * pelo caminho real do Spring Security.
 */
@Import({
  TestcontainersConfiguration.class,
  LoginPeloGoogleTest.GoogleFalso.class,
  LoginPeloGoogleTest.RotasDeTeste.class
})
@SpringBootTest(properties = "spring.flyway.enabled=true")
@AutoConfigureMockMvc
@Transactional
class LoginPeloGoogleTest {

  private static final String NOME = "Fulana de Tal";
  private static final String FOTO = "https://exemplo.com/fulana.png";
  private static final RSAKey CHAVE_DO_GOOGLE = gerarChave();
  private static volatile String idTokenDaTroca = "";
  private static final HttpServer GOOGLE = subirGoogle();

  /** O registro de verdade, com a troca do código e a chave apontando para o Google falso. */
  @TestConfiguration(proxyBeanMethods = false)
  static class GoogleFalso {

    @Bean
    @Primary
    ClientRegistrationRepository registroDoGoogleFalso(
        ClientRegistrationRepository registroDoGoogle) {
      String endereco = "http://127.0.0.1:" + GOOGLE.getAddress().getPort();
      return new InMemoryClientRegistrationRepository(
          ClientRegistration.withClientRegistration(registroDoGoogle.findByRegistrationId("google"))
              .tokenUri(endereco + "/token")
              .jwkSetUri(endereco + "/jwks")
              .build());
    }
  }

  @RestController
  static class RotasDeTeste {

    @GetMapping("/teste/protegida")
    String protegida() {
      return "ok";
    }
  }

  private final MockMvc mvc;
  private final JdbcClient jdbc;
  private final EntityManager entidades;
  private MockHttpSession sessaoDoLogin = new MockHttpSession();
  private String state = "";
  private String nonce = "";

  LoginPeloGoogleTest(
      @Autowired MockMvc mvc, @Autowired JdbcClient jdbc, @Autowired EntityManager entidades) {
    this.mvc = mvc;
    this.jdbc = jdbc;
    this.entidades = entidades;
  }

  @AfterAll
  static void derrubarGoogle() {
    GOOGLE.stop(0);
  }

  // Pega o pedido de escopo além da identidade, que traria a verificação do
  // aplicativo (RNF-21), e o login sem state ou nonce
  @Test
  void loginLevaAoGooglePedindoSoIdentidade() throws Exception {
    MvcResult ida =
        mvc.perform(get("/auth/login")).andExpect(status().is3xxRedirection()).andReturn();

    MultiValueMap<String, String> parametros =
        UriComponentsBuilder.fromUriString(ida.getResponse().getRedirectedUrl())
            .build()
            .getQueryParams();
    assertThat(ida.getResponse().getRedirectedUrl())
        .startsWith("https://accounts.google.com/o/oauth2/v2/auth");
    assertThat(UriUtils.decode(parametros.getFirst("scope"), StandardCharsets.UTF_8).split(" "))
        .containsExactlyInAnyOrder("openid", "email", "profile");
    // No MockMvc a api não tem o prefixo /api/v1, que o {baseUrl} traz fora dele
    assertThat(UriUtils.decode(parametros.getFirst("redirect_uri"), StandardCharsets.UTF_8))
        .isEqualTo("http://localhost/auth/callback");
    assertThat(parametros).containsKeys("state", "nonce");
  }

  // Pega o e-mail gravado com a caixa do Google, a conta nascendo com fase, o
  // token de acesso que não passa na guarda, a renovação gravada sem o hash ou
  // com outra validade, a sessão do login que sobra no servidor e o retorno que
  // não volta para a aplicação (UC-01, RN-66, ADR-0041)
  @Test
  void primeiroAcessoDoConvidadoCriaAContaSemFaseEAbreASessao() throws Exception {
    convidar("fulana@exemplo.com");
    irAoGoogle();

    MockHttpServletResponse volta =
        voltarDoGoogle(idToken(nonce, "Fulana@Exemplo.com", true))
            .andExpect(redirectedUrl("/"))
            .andReturn()
            .getResponse();

    assertThat(sessaoDoLogin.isInvalid()).isTrue();
    // O service participa da transação do teste, e o Hibernate só grava no fim dela
    entidades.flush();
    assertThat(
            jdbc.sql("select expira_em > now() + interval '29 days' from token_renovacao")
                .query(Boolean.class)
                .single())
        .isTrue();
    mvc.perform(get("/teste/protegida").cookie(volta.getCookie("acesso")))
        .andExpect(status().isOk());
    mvc.perform(post("/auth/renovacao").cookie(volta.getCookie("renovacao")))
        .andExpect(status().isNoContent());
    Map<String, Object> usuario =
        jdbc.sql("select email, nome, foto_url from usuario").query().singleRow();
    assertThat(usuario)
        .containsEntry("email", "fulana@exemplo.com")
        .containsEntry("nome", NOME)
        .containsEntry("foto_url", FOTO);
    assertThat(contar("fase")).isZero();
    assertThat(contar("token_renovacao")).isOne();
  }

  // Pega a conta duplicada no segundo login, e o nome e a foto que ficam
  // parados no primeiro
  @Test
  void contaExistenteEntraComONomeEAFotoDeAgora() throws Exception {
    convidar("fulana@exemplo.com");
    jdbc.sql(
            "insert into usuario (email, nome, foto_url, criado_em)"
                + " values ('fulana@exemplo.com', 'Nome Antigo', null, now())")
        .update();
    irAoGoogle();

    voltarDoGoogle(idToken(nonce, "fulana@exemplo.com", true)).andExpect(redirectedUrl("/"));

    entidades.flush();
    assertThat(jdbc.sql("select nome, foto_url from usuario").query().singleRow())
        .containsEntry("nome", NOME)
        .containsEntry("foto_url", FOTO);
  }

  // Pega o login aberto para qualquer conta do Google, que só o convite fecha
  // (RN-48, UC-01 1a)
  @Test
  void quemNaoFoiConvidadoVoltaParaAEntradaSemConta() throws Exception {
    irAoGoogle();

    voltarDoGoogle(idToken(nonce, "fulana@exemplo.com", true))
        .andExpect(redirectedUrl("/entrar?motivo=NAO_CONVIDADO"))
        .andExpect(cookie().doesNotExist("acesso"))
        .andExpect(cookie().doesNotExist("renovacao"));

    assertThat(contar("usuario")).isZero();
  }

  // Pega o e-mail não verificado aceito: qualquer um cria no Google uma conta
  // com o e-mail de um convidado sem provar que é dele
  @Test
  void emailNaoVerificadoTemAMesmaRecusaDoNaoConvidado() throws Exception {
    convidar("fulana@exemplo.com");
    irAoGoogle();

    voltarDoGoogle(idToken(nonce, "fulana@exemplo.com", false))
        .andExpect(redirectedUrl("/entrar?motivo=NAO_CONVIDADO"))
        .andExpect(cookie().doesNotExist("acesso"));

    assertThat(contar("usuario")).isZero();
  }

  // Pega o erro cru do Spring no lugar do redirecionamento (UC-01 1d)
  @Test
  void cancelarNoGoogleVoltaComLoginNaoConcluido() throws Exception {
    irAoGoogle();

    mvc.perform(
            get("/auth/callback")
                .param("error", "access_denied")
                .param("state", state)
                .session(sessaoDoLogin))
        .andExpect(redirectedUrl("/entrar?motivo=LOGIN_NAO_CONCLUIDO"));
  }

  // Pega o id_token de outro login aceito, que é o nonce sem conferir (UC-01 1d)
  @Test
  void idTokenDeOutroLoginVoltaComLoginNaoConcluido() throws Exception {
    convidar("fulana@exemplo.com");
    irAoGoogle();

    voltarDoGoogle(idToken("nonce-de-outro-login", "fulana@exemplo.com", true))
        .andExpect(redirectedUrl("/entrar?motivo=LOGIN_NAO_CONCLUIDO"));

    assertThat(contar("usuario")).isZero();
  }

  private void convidar(String email) {
    jdbc.sql("insert into convidado (email, convidado_em) values (?, now())").param(email).update();
  }

  private int contar(String tabela) {
    return jdbc.sql("select count(*) from " + tabela).query(Integer.class).single();
  }

  /** O GET /auth/login manda ao Google com o state e o nonce, guardados na sessão do login. */
  private void irAoGoogle() throws Exception {
    MvcResult ida =
        mvc.perform(get("/auth/login")).andExpect(status().is3xxRedirection()).andReturn();
    MultiValueMap<String, String> parametros =
        UriComponentsBuilder.fromUriString(ida.getResponse().getRedirectedUrl())
            .build()
            .getQueryParams();
    state = UriUtils.decode(parametros.getFirst("state"), StandardCharsets.UTF_8);
    nonce = UriUtils.decode(parametros.getFirst("nonce"), StandardCharsets.UTF_8);
    sessaoDoLogin = (MockHttpSession) ida.getRequest().getSession(false);
  }

  /** O navegador volta do Google com o código, e o Google falso o troca por este id_token. */
  private ResultActions voltarDoGoogle(String idToken) throws Exception {
    idTokenDaTroca = idToken;
    return mvc.perform(
        get("/auth/callback")
            .param("code", "codigo-de-teste")
            .param("state", state)
            .session(sessaoDoLogin));
  }

  private static String idToken(String nonce, String email, boolean verificado) {
    Instant agora = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("https://accounts.google.com")
            .subject("id-no-google")
            .audience(List.of(TestcontainersConfiguration.ID_DO_GOOGLE))
            .issuedAt(agora)
            .expiresAt(agora.plusSeconds(600))
            .claim("nonce", nonce)
            .claim("email", email)
            .claim("email_verified", verificado)
            .claim("name", NOME)
            .claim("picture", FOTO)
            .build();
    JwsHeader cabecalho =
        JwsHeader.with(SignatureAlgorithm.RS256).keyId(CHAVE_DO_GOOGLE.getKeyID()).build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(CHAVE_DO_GOOGLE)))
        .encode(JwtEncoderParameters.from(cabecalho, claims))
        .getTokenValue();
  }

  private static RSAKey gerarChave() {
    try {
      return new RSAKeyGenerator(2048).keyID("google-falso").generate();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Responde à troca do código com o id_token da vez, e publica a metade pública da chave. */
  private static HttpServer subirGoogle() {
    try {
      HttpServer servidor =
          HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
      servidor.createContext(
          "/token",
          troca ->
              responder(
                  troca,
                  """
                  {"access_token": "acesso-do-google", "token_type": "Bearer",
                   "expires_in": 3600, "scope": "openid email profile", "id_token": "%s"}
                  """
                      .formatted(idTokenDaTroca)));
      servidor.createContext(
          "/jwks", troca -> responder(troca, new JWKSet(CHAVE_DO_GOOGLE.toPublicJWK()).toString()));
      servidor.start();
      return servidor;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void responder(HttpExchange troca, String json) throws IOException {
    troca.getRequestBody().readAllBytes();
    byte[] corpo = json.getBytes(StandardCharsets.UTF_8);
    troca.getResponseHeaders().add("Content-Type", "application/json");
    troca.sendResponseHeaders(200, corpo.length);
    try (OutputStream saida = troca.getResponseBody()) {
      saida.write(corpo);
    }
  }
}
