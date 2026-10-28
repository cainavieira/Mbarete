package mbarete.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import mbarete.config.Hoje;
import mbarete.dto.NovoRegistroDto;
import mbarete.dto.RegistroDto;
import mbarete.service.RegistroService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Os registros da própria conta, endereçados pela data (endpoints.md). */
@RestController
@RequestMapping("/registros")
public class RegistroController {

  private final RegistroService registros;

  public RegistroController(RegistroService registros) {
    this.registros = registros;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RegistroDto registrar(
      @AuthenticationPrincipal Jwt token,
      @Hoje LocalDate hoje,
      @Valid @RequestBody NovoRegistroDto novo) {
    return registros.registrar(ContaDoToken.id(token), novo, hoje);
  }
}
