package mbarete.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import mbarete.exception.TratadorGlobal;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class HojeResolverTest {

  @RestController
  static class RotasDeTeste {

    @GetMapping("/com-hoje")
    String comHoje(@Hoje LocalDate hoje) {
      return hoje.toString();
    }

    @GetMapping("/sem-hoje")
    String semHoje() {
      return "ok";
    }
  }

  // 23h30 em UTC: no Brasil ainda é dia 20, e no Japão já é dia 21
  private static final Clock RELOGIO =
      Clock.fixed(Instant.parse("2026-10-20T23:30:00Z"), ZoneOffset.UTC);

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new RotasDeTeste())
          .setCustomArgumentResolvers(new HojeResolver(RELOGIO))
          .setControllerAdvice(new TratadorGlobal())
          .build();

  @Test
  void aRotaRecebeODiaDoDispositivo() throws Exception {
    mvc.perform(get("/com-hoje").header("Data-Local", "2026-10-21"))
        .andExpect(status().isOk())
        .andExpect(content().string("2026-10-21"));
  }

  // Pega o cabeçalho ausente caindo para a data do servidor, que passa em todo
  // teste e erra em viagem (endpoints.md)
  @Test
  void semOCabecalhoARotaQueDependeDeHojeRecusa() throws Exception {
    mvc.perform(get("/com-hoje"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value("DATA_LOCAL_INVALIDA"))
        .andExpect(jsonPath("$.erro.regra").value("RN-04"));
  }

  @Test
  void aRotaQueNaoDependeDeHojeNaoOlhaOCabecalho() throws Exception {
    mvc.perform(get("/sem-hoje")).andExpect(status().isOk());
  }
}
