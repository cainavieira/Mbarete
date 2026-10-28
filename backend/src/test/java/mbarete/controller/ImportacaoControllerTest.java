package mbarete.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import mbarete.ChaveDeTeste;
import mbarete.TestcontainersConfiguration;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.flyway.enabled=true")
@AutoConfigureMockMvc
@Transactional
class ImportacaoControllerTest {

  /** O dia do servidor em UTC; o dispositivo está um dia antes, em ontem (RN-04). */
  private final LocalDate hoje = LocalDate.now(ZoneOffset.UTC);

  private final MockMvc mvc;
  private final JdbcClient jdbc;
  private final EntityManager jpa;

  ImportacaoControllerTest(
      @Autowired MockMvc mvc, @Autowired JdbcClient jdbc, @Autowired EntityManager jpa) {
    this.mvc = mvc;
    this.jdbc = jdbc;
    this.jpa = jpa;
  }

  // Pega a substituição marcada pelos registros de outra pessoa ou não
  // marcada, a data futura contada pelo dia do servidor (o dispositivo ainda
  // está em ontem), e a pré-visualização que grava (RN-58, RN-60)
  @Test
  void previsualizaSemGravar() throws Exception {
    int outra = usuario("outra@exemplo.com");
    registro(outra, LocalDate.of(2026, 9, 2));
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, LocalDate.of(2026, 9, 1));
    String arquivo =
        """
        data;peso;cintura
        2026-09-01;85.7;
        2026-09-02;85.6;90.0
        %s;85.0;
        """
            .formatted(hoje);

    previsualizar(fulana, "arquivo", arquivo)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.validas", hasSize(2)))
        .andExpect(jsonPath("$.validas[0].linha").value(2))
        .andExpect(jsonPath("$.validas[0].substitui").value(true))
        .andExpect(jsonPath("$.validas[1].data").value("2026-09-02"))
        .andExpect(jsonPath("$.validas[1].peso").value(85.6))
        .andExpect(jsonPath("$.validas[1].cintura").value(90.0))
        .andExpect(jsonPath("$.validas[1].substitui").value(false))
        .andExpect(jsonPath("$.recusadas", hasSize(1)))
        .andExpect(jsonPath("$.recusadas[0].linha").value(4))
        .andExpect(
            jsonPath("$.recusadas[0].motivo")
                .value("A data do registro não pode estar no futuro."));
    assertThat(
            jdbc.sql("select count(*) from registro where usuario_id = ?")
                .param(fulana)
                .query(Integer.class)
                .single())
        .isEqualTo(1);
  }

  // Pega a recusa do arquivo inteiro fora do status e da regra do catálogo
  // (UC-10 1a, 1b)
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "dia;kg       | CSV_COLUNA_AUSENTE         | RN-53",
        "data\tpeso   | CSV_SEPARADOR_DESCONHECIDO | RN-54",
      })
  void oArquivoIlegivelERecusadoInteiro(String cabecalho, String codigo, String regra)
      throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    previsualizar(fulana, "arquivo", cabecalho + "\n2026-09-01;85.7\n")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value(codigo))
        .andExpect(jsonPath("$.erro.campo").value("arquivo"))
        .andExpect(jsonPath("$.erro.regra").value(regra));
  }

  // Pega a data que já tem registro inserida de novo, que o banco recusa, a
  // cintura de antes mantida na substituição, a linha recusada gravada, a data
  // futura contada pelo dia do servidor e o registro de outra pessoa
  // substituído (RN-01, RN-59, RN-60)
  @Test
  void importaAsValidasSubstituindoAsDatasQueJaTemRegistro() throws Exception {
    int outra = usuario("outra@exemplo.com");
    registro(outra, LocalDate.of(2026, 9, 2));
    int fulana = usuario("fulana@exemplo.com");
    jdbc.sql(
            "insert into registro (usuario_id, data, peso, cintura, criado_em)"
                + " values (?, '2026-09-01', 80.0, 95.0, now())")
        .param(fulana)
        .update();
    String arquivo =
        """
        data;peso;cintura
        2026-09-01;85.7;
        2026-09-02;85.6;90.0
        2026-09-03;19.0;
        %s;85.0;
        """
            .formatted(hoje);

    enviar("/importacao", fulana, "arquivo", arquivo)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.validas", hasSize(2)))
        .andExpect(jsonPath("$.recusadas", hasSize(2)));
    assertThat(registros(fulana))
        .containsExactly(
            new Gravado(LocalDate.of(2026, 9, 1), new BigDecimal("85.7"), null),
            new Gravado(LocalDate.of(2026, 9, 2), new BigDecimal("85.6"), new BigDecimal("90.0")));
    assertThat(registros(outra))
        .containsExactly(new Gravado(LocalDate.of(2026, 9, 2), new BigDecimal("80.0"), null));
  }

  // Pega o dia da base que o registro importado dá à fase e não é fixado
  // (RN-68)
  @Test
  void aImportacaoFixaODiaDaBase() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    jdbc.sql(
            "insert into fase (usuario_id, tipo, data_inicio, criado_em)"
                + " values (?, 'manutencao', '2026-09-01', now())")
        .param(fulana)
        .update();

    String arquivo =
        """
        data;peso
        2026-09-01;80.0
        2026-09-02;80.0
        2026-09-03;80.0
        2026-09-04;80.0
        """;

    enviar("/importacao", fulana, "arquivo", arquivo).andExpect(status().isOk());
    jpa.flush();
    assertThat(
            jdbc.sql("select base_data from fase where usuario_id = ?")
                .param(fulana)
                .query(LocalDate.class)
                .single())
        .isEqualTo(LocalDate.of(2026, 9, 4));
  }

  // Pega o envio sem o arquivo estourando como erro interno
  @Test
  void semOArquivoSaiComoCorpoMalformado() throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    previsualizar(fulana, "outro", "data;peso\n2026-09-01;85.7\n")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro.codigo").value("CORPO_MALFORMADO"));
  }

  record Gravado(LocalDate data, BigDecimal peso, @Nullable BigDecimal cintura) {}

  /** O que está no banco, depois de o JPA escrever o que guardou na transação do teste. */
  private List<Gravado> registros(int idUsuario) {
    jpa.flush();
    return jdbc.sql("select data, peso, cintura from registro where usuario_id = ? order by data")
        .param(idUsuario)
        .query(Gravado.class)
        .list();
  }

  private ResultActions previsualizar(int idUsuario, String campo, String conteudo)
      throws Exception {
    return enviar("/importacao/previsualizacao", idUsuario, campo, conteudo);
  }

  private ResultActions enviar(String caminho, int idUsuario, String campo, String conteudo)
      throws Exception {
    return mvc.perform(
        multipart(caminho)
            .file(
                new MockMultipartFile(
                    campo, "historico.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8)))
            .cookie(acesso(idUsuario))
            .header("Data-Local", hoje.minusDays(1).toString()));
  }

  private int usuario(String email) {
    return jdbc.sql(
            "insert into usuario (email, nome, criado_em) values (?, 'Fulana', now()) returning id")
        .param(email)
        .query(Integer.class)
        .single();
  }

  private void registro(int idUsuario, LocalDate data) {
    jdbc.sql("insert into registro (usuario_id, data, peso, criado_em) values (?, ?, 80.0, now())")
        .params(idUsuario, data)
        .update();
  }

  private static Cookie acesso(int idUsuario) {
    return new Cookie(
        "acesso", ChaveDeTeste.token(idUsuario, Instant.now().plus(10, ChronoUnit.MINUTES)));
  }
}
