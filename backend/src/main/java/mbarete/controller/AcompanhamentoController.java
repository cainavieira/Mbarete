package mbarete.controller;

import java.time.LocalDate;
import mbarete.config.Hoje;
import mbarete.dto.AcompanhamentoDto;
import mbarete.service.AcompanhamentoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A tela principal numa resposta só (endpoints.md). */
@RestController
@RequestMapping("/acompanhamento")
public class AcompanhamentoController {

  private final AcompanhamentoService acompanhamento;

  public AcompanhamentoController(AcompanhamentoService acompanhamento) {
    this.acompanhamento = acompanhamento;
  }

  /** Pede o dia de hoje porque tudo na tela se mede até ele, no dia do dispositivo (RN-04). */
  @GetMapping
  public AcompanhamentoDto acompanhar(@AuthenticationPrincipal Jwt token, @Hoje LocalDate hoje) {
    return acompanhamento.acompanhar(ContaDoToken.id(token), hoje);
  }
}
