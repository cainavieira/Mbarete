package mbarete.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import mbarete.ChaveDeTeste;
import mbarete.TestcontainersConfiguration;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.flyway.enabled=true")
@AutoConfigureMockMvc
@Transactional
class ContaControllerTest {

  private final MockMvc mvc;
  private final JdbcClient jdbc;
  private final EntityManager jpa;

  ContaControllerTest(
      @Autowired MockMvc mvc, @Autowired JdbcClient jdbc, @Autowired EntityManager jpa) {
    this.mvc = mvc;
    this.jdbc = jdbc;
    this.jpa = jpa;
  }

  // Pega a conta de outra pessoa devolvida, que é a primeira, e não a do
  // token (RN-50)
  @Test
  void devolveAContaDoToken() throws Exception {
    usuario("outra@exemplo.com", "Outra Pessoa", null);
    int fulana = usuario("fulana@exemplo.com", "Fulana de Tal", "https://exemplo.com/f.png");

    mvc.perform(get("/conta").cookie(acesso(fulana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Fulana de Tal"))
        .andExpect(jsonPath("$.email").value("fulana@exemplo.com"))
        .andExpect(jsonPath("$.foto").value("https://exemplo.com/f.png"));
  }

  @Test
  void contaSemFotoVemComAFotoNula() throws Exception {
    int fulana = usuario("fulana@exemplo.com", "Fulana de Tal", null);

    mvc.perform(get("/conta").cookie(acesso(fulana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.foto").value(nullValue()));
  }

  // Pega a conta apagada com o token ainda válido virando erro interno, em vez
  // de mandar o frontend de volta à entrada (UC-12)
  @Test
  void contaQueNaoExisteMaisRespondeNaoAutenticado() throws Exception {
    mvc.perform(get("/conta").cookie(acesso(9999)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_AUTENTICADO"));
  }

  // Pega o registro de outra pessoa no arquivo, a ordem em que o banco devolve,
  // e o arquivo aberto no navegador em vez de baixado (UC-11)
  @Test
  void exportaOHistoricoDaPessoa() throws Exception {
    int outra = usuario("outra@exemplo.com", "Outra Pessoa", null);
    registro(outra, LocalDate.of(2026, 9, 3), "70.0", null);
    int fulana = usuario("fulana@exemplo.com", "Fulana de Tal", null);
    registro(fulana, LocalDate.of(2026, 9, 2), "85.6", "90.0");
    registro(fulana, LocalDate.of(2026, 9, 1), "85.7", null);

    mvc.perform(get("/conta/exportar").cookie(acesso(fulana)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/csv;charset=UTF-8"))
        .andExpect(
            header()
                .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historico.csv\""))
        .andExpect(
            content()
                .string(
                    """
                    data;peso;cintura
                    2026-09-01;85.7;
                    2026-09-02;85.6;90.0
                    """));
  }

  // Pega a conta que não é apagada, os dados dela que ficam, os de outra
  // pessoa levados junto, o convite revogado e a sessão que continua: o e-mail
  // vem com a caixa e o espaço que a pessoa digitou (RN-51, UC-12)
  @Test
  void apagaAContaETudoDelaEEncerraASessao() throws Exception {
    int outra = usuario("outra@exemplo.com", "Outra Pessoa", null);
    registro(outra, LocalDate.of(2026, 9, 1), "70.0", null);
    int fulana = usuario("fulana@exemplo.com", "Fulana de Tal", null);
    registro(fulana, LocalDate.of(2026, 9, 1), "85.7", null);
    jdbc.sql(
            "insert into fase (usuario_id, tipo, data_inicio, criado_em)"
                + " values (?, 'manutencao', '2026-09-01', now())")
        .param(fulana)
        .update();
    jdbc.sql(
            "insert into token_renovacao (usuario_id, hash, criado_em, expira_em)"
                + " values (?, 'hash-da-fulana', now(), now() + interval '1 day')")
        .param(fulana)
        .update();
    jdbc.sql("insert into convidado (email, convidado_em) values ('fulana@exemplo.com', now())")
        .update();

    apagar(fulana, "{\"email\": \" Fulana@Exemplo.com \"}")
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge("acesso", 0))
        .andExpect(cookie().maxAge("renovacao", 0));
    jpa.flush();
    assertThat(contar("usuario", "id", fulana)).isZero();
    assertThat(contar("registro", "usuario_id", fulana)).isZero();
    assertThat(contar("fase", "usuario_id", fulana)).isZero();
    assertThat(contar("token_renovacao", "usuario_id", fulana)).isZero();
    assertThat(contar("registro", "usuario_id", outra)).isEqualTo(1);
    assertThat(jdbc.sql("select count(*) from convidado").query(Integer.class).single())
        .isEqualTo(1);
  }

  // Pega a conta apagada com o e-mail de outra pessoa, e a recusa fora do
  // status e da regra do catálogo (UC-12 2a)
  @Test
  void oEmailQueNaoConfereNaoApaga() throws Exception {
    usuario("outra@exemplo.com", "Outra Pessoa", null);
    int fulana = usuario("fulana@exemplo.com", "Fulana de Tal", null);

    apagar(fulana, "{\"email\": \"outra@exemplo.com\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value("EMAIL_NAO_CONFERE"))
        .andExpect(jsonPath("$.erro.campo").value("email"))
        .andExpect(jsonPath("$.erro.regra").value("UC-12"));
    jpa.flush();
    assertThat(contar("usuario", "id", fulana)).isEqualTo(1);
  }

  // Pega o corpo sem o e-mail estourando como erro interno
  @Test
  void semEmailSaiComoCorpoMalformado() throws Exception {
    int fulana = usuario("fulana@exemplo.com", "Fulana de Tal", null);

    apagar(fulana, "{}")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro.campo").value("email"));
  }

  private ResultActions apagar(int idUsuario, String corpo) throws Exception {
    return mvc.perform(
        post("/conta/exclusao")
            .cookie(acesso(idUsuario))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
  }

  /** Quantas linhas da tabela pertencem ao id, lidas pelo banco e não pelo JPA. */
  private int contar(String tabela, String coluna, int id) {
    return jdbc.sql("select count(*) from " + tabela + " where " + coluna + " = ?")
        .param(id)
        .query(Integer.class)
        .single();
  }

  private void registro(int idUsuario, LocalDate data, String peso, @Nullable String cintura) {
    jdbc.sql(
            "insert into registro (usuario_id, data, peso, cintura, criado_em)"
                + " values (?, ?, ?, ?, now())")
        .params(
            idUsuario, data, new BigDecimal(peso), cintura == null ? null : new BigDecimal(cintura))
        .update();
  }

  private int usuario(String email, String nome, @Nullable String foto) {
    return jdbc.sql(
            "insert into usuario (email, nome, foto_url, criado_em)"
                + " values (?, ?, ?, now()) returning id")
        .params(email, nome, foto)
        .query(Integer.class)
        .single();
  }

  private static Cookie acesso(int idUsuario) {
    return new Cookie(
        "acesso", ChaveDeTeste.token(idUsuario, Instant.now().plus(10, ChronoUnit.MINUTES)));
  }
}
