package mbarete.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import mbarete.ChaveDeTeste;
import mbarete.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** A renovação e o logout, com o token de renovação inserido direto no banco (ADR-0022). */
@Import({TestcontainersConfiguration.class, AutenticacaoControllerTest.RotasDeTeste.class})
@SpringBootTest(properties = "spring.flyway.enabled=true")
@AutoConfigureMockMvc
@Transactional
class AutenticacaoControllerTest {

  @RestController
  static class RotasDeTeste {

    @GetMapping("/teste/protegida")
    String protegida() {
      return "ok";
    }
  }

  private static final String TOKEN = "token-de-renovacao-de-teste";
  // SHA-256 do TOKEN, calculado fora do código testado
  private static final String HASH =
      "77e5f8b5c971d7797235199190dbf8eb24fb52bb4b574bcc21500b556e21293f";

  private final MockMvc mvc;
  private final JdbcClient jdbc;
  private final EntityManager entidades;
  private final JwtDecoder decodificador;

  AutenticacaoControllerTest(
      @Autowired MockMvc mvc,
      @Autowired JdbcClient jdbc,
      @Autowired EntityManager entidades,
      @Autowired JwtDecoder decodificador) {
    this.mvc = mvc;
    this.jdbc = jdbc;
    this.entidades = entidades;
    this.decodificador = decodificador;
  }

  // Pega o token de acesso que não passa na guarda, de outra pessoa ou longo
  // demais para o logout valer, os atributos do cookie, a renovação que vai
  // para todas as rotas e a validade que não é empurrada (RNF-22, ADR-0041)
  @Test
  void renovacaoValidaEmiteAcessoQuePassaEEmpurraAValidade() throws Exception {
    int usuario = renovacaoQueVenceEm("1 day");

    String acesso =
        mvc.perform(post("/auth/renovacao").cookie(renovacao()))
            .andExpect(status().isNoContent())
            .andExpect(cookie().httpOnly("acesso", true))
            .andExpect(cookie().secure("acesso", true))
            .andExpect(cookie().sameSite("acesso", "Lax"))
            .andExpect(cookie().maxAge("acesso", 15 * 60))
            .andExpect(cookie().path("renovacao", "/auth"))
            .andExpect(cookie().httpOnly("renovacao", true))
            .andExpect(cookie().maxAge("renovacao", 30 * 24 * 60 * 60))
            .andReturn()
            .getResponse()
            .getCookie("acesso")
            .getValue();

    mvc.perform(get("/teste/protegida").cookie(new Cookie("acesso", acesso)))
        .andExpect(status().isOk());
    Jwt token = decodificador.decode(acesso);
    assertThat(token.getSubject()).isEqualTo(String.valueOf(usuario));
    assertThat(Duration.between(token.getIssuedAt(), token.getExpiresAt()))
        .isEqualTo(Duration.ofMinutes(15));
    // O service participa da transação do teste, e o Hibernate só grava no fim dela
    entidades.flush();
    assertThat(
            jdbc.sql("select expira_em > now() + interval '29 days' from token_renovacao")
                .query(Boolean.class)
                .single())
        .isTrue();
  }

  // Pega a renovação vencida aceita
  @Test
  void renovacaoVencidaRespondeNaoAutenticado() throws Exception {
    renovacaoQueVenceEm("-1 minute");

    mvc.perform(post("/auth/renovacao").cookie(renovacao()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_AUTENTICADO"));
  }

  @Test
  void semCookieRespondeNaoAutenticado() throws Exception {
    mvc.perform(post("/auth/renovacao"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_AUTENTICADO"));
  }

  // Pega o cookie de acesso de chave trocada barrando a renovação, que é
  // justamente o caminho para sair dele
  @Test
  void acessoInvalidoNaoImpedeARenovacao() throws Exception {
    renovacaoQueVenceEm("1 day");
    String deOutraChave =
        ChaveDeTeste.tokenDeOutraChave(1, Instant.now().plus(10, ChronoUnit.MINUTES));

    mvc.perform(post("/auth/renovacao").cookie(renovacao(), new Cookie("acesso", deOutraChave)))
        .andExpect(status().isNoContent());
  }

  // Pega o logout que só expira os cookies, deixando a renovação viva no
  // banco, e o cookie expirado num caminho que o navegador não apaga (RF-04)
  @Test
  void logoutApagaARenovacaoEExpiraOsDoisCookies() throws Exception {
    renovacaoQueVenceEm("1 day");

    mvc.perform(post("/auth/logout").cookie(renovacao()))
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge("acesso", 0))
        .andExpect(cookie().path("acesso", "/"))
        .andExpect(cookie().maxAge("renovacao", 0))
        .andExpect(cookie().path("renovacao", "/auth"));

    mvc.perform(post("/auth/renovacao").cookie(renovacao())).andExpect(status().isUnauthorized());
  }

  private int renovacaoQueVenceEm(String intervalo) {
    int usuario =
        jdbc.sql(
                "insert into usuario (email, nome, criado_em)"
                    + " values ('fulana@exemplo.com', 'Fulana', now()) returning id")
            .query(Integer.class)
            .single();
    jdbc.sql(
            "insert into token_renovacao (usuario_id, hash, criado_em, expira_em)"
                + " values (?, ?, now(), now() + cast(? as interval))")
        .params(usuario, HASH, intervalo)
        .update();
    return usuario;
  }

  private static Cookie renovacao() {
    return new Cookie("renovacao", TOKEN);
  }
}
