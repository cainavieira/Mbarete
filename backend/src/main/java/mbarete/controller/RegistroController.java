package mbarete.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import mbarete.config.Hoje;
import mbarete.dto.RegistroDto;
import mbarete.dto.RegistroEnviadoDto;
import mbarete.service.RegistroService;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping
  public List<RegistroDto> listar(
      @AuthenticationPrincipal Jwt token,
      @RequestParam(required = false) @Nullable LocalDate de,
      @RequestParam(required = false) @Nullable LocalDate ate,
      @Hoje LocalDate hoje) {
    return registros.listar(ContaDoToken.id(token), de, ate, hoje);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RegistroDto registrar(
      @AuthenticationPrincipal Jwt token,
      @Hoje LocalDate hoje,
      @Valid @RequestBody RegistroEnviadoDto novo) {
    return registros.registrar(ContaDoToken.id(token), novo, hoje);
  }

  @PatchMapping("/{data}")
  public RegistroDto editar(
      @AuthenticationPrincipal Jwt token,
      @PathVariable LocalDate data,
      @Hoje LocalDate hoje,
      @Valid @RequestBody RegistroEnviadoDto editado) {
    return registros.editar(ContaDoToken.id(token), data, editado, hoje);
  }

  @DeleteMapping("/{data}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void apagar(@AuthenticationPrincipal Jwt token, @PathVariable LocalDate data) {
    registros.apagar(ContaDoToken.id(token), data);
  }
}
