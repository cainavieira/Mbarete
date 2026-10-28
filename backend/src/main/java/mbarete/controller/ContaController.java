package mbarete.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import mbarete.config.CookiesDaSessao;
import mbarete.dto.ContaDto;
import mbarete.dto.ExclusaoDto;
import mbarete.service.ContaService;
import mbarete.service.ExportacaoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A própria conta, achada pelo token: nenhuma rota aqui recebe o id de outra (RN-50). */
@RestController
@RequestMapping("/conta")
public class ContaController {

  private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

  private final ContaService contas;
  private final ExportacaoService exportacao;

  public ContaController(ContaService contas, ExportacaoService exportacao) {
    this.contas = contas;
    this.exportacao = exportacao;
  }

  @GetMapping
  public ContaDto conta(@AuthenticationPrincipal Jwt token) {
    return contas.conta(ContaDoToken.id(token));
  }

  /** O arquivo vem para baixar, e não para abrir no navegador (UC-11). */
  @GetMapping("/exportar")
  public ResponseEntity<String> exportar(@AuthenticationPrincipal Jwt token) {
    return ResponseEntity.ok()
        .contentType(CSV)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("historico.csv").build().toString())
        .body(exportacao.exportar(ContaDoToken.id(token)));
  }

  /**
   * Apaga a conta e encerra a sessão, expirando os dois cookies como o logout (UC-12). O token de
   * renovação já saiu do banco junto com a conta.
   */
  @PostMapping("/exclusao")
  public ResponseEntity<Void> apagar(
      @AuthenticationPrincipal Jwt token,
      @Valid @RequestBody ExclusaoDto exclusao,
      HttpServletRequest pedido) {
    contas.apagar(ContaDoToken.id(token), exclusao.email());
    return ResponseEntity.noContent()
        .header(
            HttpHeaders.SET_COOKIE,
            CookiesDaSessao.acessoExpirado(pedido).toString(),
            CookiesDaSessao.renovacaoExpirada(pedido).toString())
        .build();
  }
}
