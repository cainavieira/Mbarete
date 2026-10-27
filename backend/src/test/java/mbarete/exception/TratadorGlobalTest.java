package mbarete.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import mbarete.regras.Recusa;
import mbarete.regras.RegraVioladaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(OutputCaptureExtension.class)
class TratadorGlobalTest {

  record ComPeso(
      @NotNull @DecimalMin(value = "20.0", message = "PESO_FORA_DE_FAIXA") BigDecimal peso) {}

  @RestController
  static class RotasDeTeste {

    @GetMapping("/regra")
    void regra() {
      throw new RegraVioladaException(
          Recusa.PESO_FORA_DE_FAIXA, "peso", "O peso deve estar entre 20,0 e 400,0 kg.");
    }

    @PostMapping("/pesos")
    void pesos(@Valid @RequestBody ComPeso corpo) {}

    @GetMapping("/registros/{data}")
    void registro(@PathVariable LocalDate data) {}

    @GetMapping("/registros")
    void registros(@RequestParam LocalDate de) {}

    @GetMapping("/inesperado")
    void inesperado() {
      throw new IllegalStateException("falhou ao gravar o peso 82.4");
    }
  }

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new RotasDeTeste())
          .setControllerAdvice(new TratadorGlobal())
          .build();

  @Test
  void recusaDeRegraSaiNoFormatoUnicoComOStatusDoCatalogo() throws Exception {
    mvc.perform(get("/regra"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value("PESO_FORA_DE_FAIXA"))
        .andExpect(jsonPath("$.erro.campo").value("peso"))
        .andExpect(jsonPath("$.erro.mensagem").value("O peso deve estar entre 20,0 e 400,0 kg."))
        .andExpect(jsonPath("$.erro.regra").value("RN-03"));
  }

  // Pega o código da anotação tomado como frase, e a recusa do Bean Validation
  // que escapa do formato único
  @Test
  void campoRecusadoPeloBeanValidationSaiComOCodigoEAFraseDoCatalogo() throws Exception {
    mvc.perform(post("/pesos").contentType(MediaType.APPLICATION_JSON).content("{\"peso\": 19.9}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value("PESO_FORA_DE_FAIXA"))
        .andExpect(jsonPath("$.erro.campo").value("peso"))
        .andExpect(jsonPath("$.erro.mensagem").value("O peso deve estar entre 20,0 e 400,0 kg."));
  }

  @Test
  void anotacaoSemCodigoSaiComoCorpoMalformadoComOCampo() throws Exception {
    mvc.perform(post("/pesos").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro.codigo").value("CORPO_MALFORMADO"))
        .andExpect(jsonPath("$.erro.campo").value("peso"));
  }

  @Test
  void corpoComTipoErradoSaiComoCorpoMalformadoComOCampo() throws Exception {
    mvc.perform(
            post("/pesos").contentType(MediaType.APPLICATION_JSON).content("{\"peso\": \"abc\"}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro.codigo").value("CORPO_MALFORMADO"))
        .andExpect(jsonPath("$.erro.campo").value("peso"))
        .andExpect(jsonPath("$.erro.regra").value(nullValue()));
  }

  @Test
  void rotaInexistenteRespondeNaoEncontrado() throws Exception {
    mvc.perform(get("/nao-existe"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_ENCONTRADO"));
  }

  @Test
  void metodoQueARotaNaoAceitaRespondeNaoEncontrado() throws Exception {
    mvc.perform(post("/regra"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_ENCONTRADO"));
  }

  // Pega o tipo errado tratado igual no caminho e na consulta: a data que não é
  // data no caminho não pode existir, e na consulta é pedido malformado
  @Test
  void dataQueNaoEDataNoCaminhoRespondeNaoEncontrado() throws Exception {
    mvc.perform(get("/registros/abc"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_ENCONTRADO"));
  }

  @Test
  void dataQueNaoEDataNaConsultaSaiComoCorpoMalformado() throws Exception {
    mvc.perform(get("/registros").param("de", "abc"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro.codigo").value("CORPO_MALFORMADO"))
        .andExpect(jsonPath("$.erro.campo").value("de"));
  }

  // Pega a mensagem da exceção levada para a resposta ou para o log, com o peso
  // dentro dela (RNF-13)
  @Test
  void erroInesperadoNaoLevaAMensagemParaARespostaNemParaOLog(CapturedOutput saida)
      throws Exception {
    mvc.perform(get("/inesperado"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.erro.codigo").value("ERRO_INTERNO"))
        .andExpect(
            jsonPath("$.erro.mensagem").value("Erro inesperado. Tente de novo em instantes."));
    assertThat(saida.getAll()).contains("IllegalStateException").doesNotContain("82.4");
  }
}
