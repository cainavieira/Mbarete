package mbarete.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import mbarete.config.Hoje;
import mbarete.dto.PrevisualizacaoDto;
import mbarete.service.ImportacaoService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** A importação do histórico em CSV, em duas chamadas sem estado entre elas (endpoints.md). */
@RestController
@RequestMapping("/importacao")
public class ImportacaoController {

  private final ImportacaoService importacao;

  public ImportacaoController(ImportacaoService importacao) {
    this.importacao = importacao;
  }

  /**
   * O arquivo chega como envio de formulário, no campo {@code arquivo}, em UTF-8. Pede o dia de
   * hoje porque a linha com data futura é recusada (RN-05, RN-60).
   */
  @PostMapping(path = "/previsualizacao", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public PrevisualizacaoDto previsualizar(
      @AuthenticationPrincipal Jwt token, @Hoje LocalDate hoje, @RequestParam MultipartFile arquivo)
      throws IOException {
    return importacao.previsualizar(
        ContaDoToken.id(token), new String(arquivo.getBytes(), StandardCharsets.UTF_8), hoje);
  }
}
