package mbarete.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import mbarete.config.Hoje;
import mbarete.dto.EncerramentoDto;
import mbarete.dto.FaseDto;
import mbarete.dto.FaseEnviadaDto;
import mbarete.service.FaseService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** As fases da própria conta, endereçadas pelo id (endpoints.md). */
@RestController
@RequestMapping("/fases")
public class FaseController {

  private final FaseService fases;

  public FaseController(FaseService fases) {
    this.fases = fases;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FaseDto criar(
      @AuthenticationPrincipal Jwt token,
      @Hoje LocalDate hoje,
      @Valid @RequestBody FaseEnviadaDto nova) {
    return fases.criar(ContaDoToken.id(token), nova, hoje);
  }

  @PatchMapping("/{id}")
  public FaseDto editar(
      @AuthenticationPrincipal Jwt token,
      @PathVariable int id,
      @Hoje LocalDate hoje,
      @Valid @RequestBody FaseEnviadaDto editada) {
    return fases.editar(ContaDoToken.id(token), id, editada, hoje);
  }

  @PostMapping("/{id}/encerramento")
  public FaseDto encerrar(
      @AuthenticationPrincipal Jwt token,
      @PathVariable int id,
      @Hoje LocalDate hoje,
      @Valid @RequestBody EncerramentoDto encerramento) {
    return fases.encerrar(ContaDoToken.id(token), id, encerramento, hoje);
  }
}
