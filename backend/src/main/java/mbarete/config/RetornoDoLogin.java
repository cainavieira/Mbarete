package mbarete.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import mbarete.entity.Usuario;
import mbarete.regras.Email;
import mbarete.service.ContaService;
import mbarete.service.Sessao;
import mbarete.service.SessaoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * O fim do retorno do Google, que sempre termina em redirecionamento (endpoints.md): para o
 * convidado, acha ou cria a conta, emite os dois cookies e vai para a raiz da aplicação; em
 * qualquer outro caso, vai para a tela de entrada com o motivo, sem abrir sessão.
 */
@Component
@ConditionalOnWebApplication
public class RetornoDoLogin implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

  private final ContaService contas;
  private final SessaoService sessoes;

  public RetornoDoLogin(ContaService contas, SessaoService sessoes) {
    this.contas = contas;
    this.sessoes = sessoes;
  }

  /**
   * O Google autentica qualquer conta, e o convite decide quem entra (ADR-0008). O e-mail que o
   * Google não deu como verificado não prova ser do convidado, e recebe a mesma recusa, que não diz
   * se o e-mail existe no sistema (UC-01 1a, RNF-23).
   */
  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest pedido, HttpServletResponse resposta, Authentication autenticacao)
      throws IOException {
    OidcUser google = (OidcUser) autenticacao.getPrincipal();
    encerrarSessaoDoLogin(pedido);
    Optional<Usuario> usuario =
        Boolean.TRUE.equals(google.getEmailVerified())
            ? Optional.ofNullable(google.getEmail())
                .flatMap(Email::normalizar)
                .flatMap(email -> contas.entrar(email, google.getFullName(), google.getPicture()))
            : Optional.empty();
    if (usuario.isEmpty()) {
      resposta.sendRedirect("/entrar?motivo=NAO_CONVIDADO");
      return;
    }
    Sessao sessao = sessoes.abrir(usuario.get());
    resposta.addHeader(
        HttpHeaders.SET_COOKIE, CookiesDaSessao.acesso(pedido, sessao.acesso()).toString());
    resposta.addHeader(
        HttpHeaders.SET_COOKIE, CookiesDaSessao.renovacao(pedido, sessao.renovacao()).toString());
    resposta.sendRedirect("/");
  }

  /** O cancelamento no Google e toda verificação que falha viram LOGIN_NAO_CONCLUIDO (UC-01 1d). */
  @Override
  public void onAuthenticationFailure(
      HttpServletRequest pedido, HttpServletResponse resposta, AuthenticationException excecao)
      throws IOException {
    encerrarSessaoDoLogin(pedido);
    resposta.sendRedirect("/entrar?motivo=LOGIN_NAO_CONCLUIDO");
  }

  /** A sessão em memória só guarda o state durante o retorno do login (ADR-0041). */
  private static void encerrarSessaoDoLogin(HttpServletRequest pedido) {
    HttpSession sessao = pedido.getSession(false);
    if (sessao != null) {
      sessao.invalidate();
    }
  }
}
