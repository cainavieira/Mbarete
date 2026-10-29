package mbarete.config;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mbarete.TestcontainersConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** O contrato em OpenAPI, de onde saem os tipos do frontend (ADR-0027). */
class ContratoOpenApiTest {

  // Pega o contrato publicado em produção, onde só o perfil dev o liga
  @Nested
  @Import(TestcontainersConfiguration.class)
  @SpringBootTest(properties = "spring.flyway.enabled=true")
  @AutoConfigureMockMvc
  class ForaDoPerfilDev {

    @Autowired private MockMvc mvc;

    @Test
    void oContratoNaoExiste() throws Exception {
      mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }
  }

  // Pega o campo que pode vir nulo gerado como sempre preenchido, o campo de
  // resposta gerado como opcional, que o tipo do front não distinguiria do
  // nulo, e o dia de hoje como parâmetro de consulta, quando ele vai no
  // cabeçalho Data-Local (endpoints.md)
  @Nested
  @Import(TestcontainersConfiguration.class)
  @SpringBootTest(properties = "spring.flyway.enabled=true")
  @AutoConfigureMockMvc
  @ActiveProfiles("dev")
  class NoPerfilDev {

    @Autowired private MockMvc mvc;

    @Test
    void dizQuemPodeVirNulo() throws Exception {
      String fase = "$.components.schemas.FaseListadaDto";
      mvc.perform(get("/v3/api-docs"))
          .andExpect(status().isOk())
          .andExpect(jsonPath(fase + ".required", hasItem("dataFim")))
          .andExpect(jsonPath(fase + ".properties.dataFim.type", hasItem("null")))
          .andExpect(jsonPath(fase + ".properties.id.type", not(hasItem("null"))))
          .andExpect(jsonPath(fase + ".properties.aoApagar.oneOf[1].type").value("null"))
          .andExpect(
              jsonPath("$.components.schemas.RegistroEnviadoDto.required", not(hasItem("cintura"))))
          .andExpect(jsonPath("$.paths['/fases'].get.parameters").doesNotExist());
    }
  }
}
