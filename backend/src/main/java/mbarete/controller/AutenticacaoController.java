package mbarete.controller;

import jakarta.servlet.http.HttpServletRequest;
import mbarete.config.CookiesDaSessao;
import mbarete.service.Sessao;
import mbarete.service.SessaoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** As rotas de auth/ que o frontend chama, sem token de acesso (endpoints.md). */
@RestController
@RequestMapping("/auth")
@ConditionalOnWebApplication
public class AutenticacaoController {

  private final SessaoService sessoes;

  public AutenticacaoController(SessaoService sessoes) {
    this.sessoes = sessoes;
  }

  /** Sem cookie, o valor chega vazio, e a renovação recusa como token desconhecido. */
  @PostMapping("/renovacao")
  public ResponseEntity<Void> renovar(
      @CookieValue(name = CookiesDaSessao.RENOVACAO, defaultValue = "") String renovacao,
      HttpServletRequest pedido) {
    Sessao sessao = sessoes.renovar(renovacao);
    return ResponseEntity.noContent()
        .header(
            HttpHeaders.SET_COOKIE,
            CookiesDaSessao.acesso(pedido, sessao.acesso()).toString(),
            CookiesDaSessao.renovacao(pedido, sessao.renovacao()).toString())
        .build();
  }

  /** Sem cookie, ou com o token já apagado, os dois cookies expiram do mesmo jeito (RF-04). */
  @PostMapping("/logout")
  public ResponseEntity<Void> sair(
      @CookieValue(name = CookiesDaSessao.RENOVACAO, defaultValue = "") String renovacao,
      HttpServletRequest pedido) {
    sessoes.sair(renovacao);
    return ResponseEntity.noContent()
        .header(
            HttpHeaders.SET_COOKIE,
            CookiesDaSessao.acessoExpirado(pedido).toString(),
            CookiesDaSessao.renovacaoExpirada(pedido).toString())
        .build();
  }
}
