package mbarete.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import mbarete.ChaveDeTeste;
import mbarete.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** O token de acesso num cookie, assinado de verdade, conferido só pela assinatura (ADR-0041). */
@Import({TestcontainersConfiguration.class, SegurancaTest.RotasDeTeste.class})
@SpringBootTest(properties = "spring.flyway.enabled=true")
@AutoConfigureMockMvc
class SegurancaTest {

  @RestController
  static class RotasDeTeste {

    @GetMapping("/teste/protegida")
    String protegida() {
      return "ok";
    }
  }

  private final MockMvc mvc;

  SegurancaTest(@Autowired MockMvc mvc) {
    this.mvc = mvc;
  }

  // Pega a rota aberta sem token, e a recusa fora do formato único
  @Test
  void semCookieRespondeNaoAutenticado() throws Exception {
    mvc.perform(get("/teste/protegida"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_AUTENTICADO"))
        .andExpect(jsonPath("$.erro.regra").value("ADR-0041"));
  }

  // Pega o token aceito sem conferir a assinatura
  @Test
  void tokenDeOutraChaveRespondeNaoAutenticado() throws Exception {
    String token = ChaveDeTeste.tokenDeOutraChave(1, daquiADezMinutos());
    mvc.perform(get("/teste/protegida").cookie(acesso(token))).andExpect(status().isUnauthorized());
  }

  // Pega o token aceito depois de vencer
  @Test
  void tokenVencidoRespondeNaoAutenticado() throws Exception {
    // O Spring tolera um minuto de relógio adiantado; cinco já é vencido
    String token = ChaveDeTeste.token(1, Instant.now().minus(5, ChronoUnit.MINUTES));
    mvc.perform(get("/teste/protegida").cookie(acesso(token))).andExpect(status().isUnauthorized());
  }

  // Pega o token procurado em outro lugar que não o cookie
  @Test
  void tokenValidoNoCookiePassa() throws Exception {
    String token = ChaveDeTeste.token(1, daquiADezMinutos());
    mvc.perform(get("/teste/protegida").cookie(acesso(token)))
        .andExpect(status().isOk())
        .andExpect(content().string("ok"));
  }

  private static Cookie acesso(String token) {
    return new Cookie("acesso", token);
  }

  private static Instant daquiADezMinutos() {
    return Instant.now().plus(10, ChronoUnit.MINUTES);
  }
}
