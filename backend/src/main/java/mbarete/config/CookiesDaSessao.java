package mbarete.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import mbarete.service.SessaoService;
import org.springframework.http.ResponseCookie;

/**
 * Os dois cookies da sessão, httpOnly, Secure e SameSite=Lax (ADR-0041). O de renovação só viaja
 * para as rotas de auth/ (endpoints.md).
 */
public final class CookiesDaSessao {

  public static final String ACESSO = "acesso";
  public static final String RENOVACAO = "renovacao";

  private CookiesDaSessao() {}

  /** Vence junto com o token de acesso. */
  public static ResponseCookie acesso(HttpServletRequest pedido, String token) {
    return cookie(ACESSO, token, caminho(pedido, ""), SessaoService.VALIDADE_DO_ACESSO);
  }

  /** Reenviado a cada renovação, para o navegador também empurrar a validade (RNF-22). */
  public static ResponseCookie renovacao(HttpServletRequest pedido, String token) {
    return cookie(RENOVACAO, token, caminho(pedido, "/auth"), SessaoService.VALIDADE_DA_RENOVACAO);
  }

  /** Vencido no mesmo caminho em que nasceu: só assim o navegador o apaga. */
  public static ResponseCookie acessoExpirado(HttpServletRequest pedido) {
    return cookie(ACESSO, "", caminho(pedido, ""), Duration.ZERO);
  }

  /** Vencido no mesmo caminho em que nasceu: só assim o navegador o apaga. */
  public static ResponseCookie renovacaoExpirada(HttpServletRequest pedido) {
    return cookie(RENOVACAO, "", caminho(pedido, "/auth"), Duration.ZERO);
  }

  private static ResponseCookie cookie(
      String nome, String valor, String caminho, Duration validade) {
    return ResponseCookie.from(nome, valor)
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .path(caminho)
        .maxAge(validade)
        .build();
  }

  /** O caminho sai do prefixo da própria requisição, para o /api/v1 existir num lugar só. */
  private static String caminho(HttpServletRequest pedido, String resto) {
    String caminho = pedido.getContextPath() + resto;
    return caminho.isEmpty() ? "/" : caminho;
  }
}
