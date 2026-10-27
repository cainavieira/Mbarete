package mbarete.controller;

import mbarete.dto.ContaDto;
import mbarete.service.ContaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A própria conta, achada pelo token: nenhuma rota aqui recebe o id de outra (RN-50). */
@RestController
@RequestMapping("/conta")
public class ContaController {

  private final ContaService contas;

  public ContaController(ContaService contas) {
    this.contas = contas;
  }

  @GetMapping
  public ContaDto conta(@AuthenticationPrincipal Jwt token) {
    return contas.conta(ContaDoToken.id(token));
  }
}
