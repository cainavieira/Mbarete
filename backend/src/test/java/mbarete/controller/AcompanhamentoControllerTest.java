package mbarete.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import mbarete.ChaveDeTeste;
import mbarete.TestcontainersConfiguration;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.flyway.enabled=true")
@AutoConfigureMockMvc
@Transactional
class AcompanhamentoControllerTest {

  /**
   * O dia do dispositivo, um antes do dia do servidor em UTC, para que a conta feita pelo dia do
   * servidor dê outro resultado (RN-04).
   */
  private final LocalDate dia = LocalDate.now(ZoneOffset.UTC).minusDays(1);

  private final MockMvc mvc;
  private final JdbcClient jdbc;
  private final EntityManager jpa;

  AcompanhamentoControllerTest(
      @Autowired MockMvc mvc, @Autowired JdbcClient jdbc, @Autowired EntityManager jpa) {
    this.mvc = mvc;
    this.jdbc = jdbc;
    this.jpa = jpa;
  }

  // Pega cada parte perdida entre a regra e a resposta, os números sem o
  // arredondamento da tela, a barra arredondada em vez de cortada (35,97 é 35,
  // e não 36) e a fase corrente tomada pelo dia do servidor, que é o da fase
  // agendada
  // (RN-37, RN-69). Um registro por dia desde 40 dias atrás, 0,1 kg mais leve
  // a cada dia, com o de hoje 0,1 acima da linha: o peso semanal de hoje é
  // 76,314, e o de 28 dias atrás, 79,1
  @Test
  void acompanhaACuttingCorrente() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int cutting =
        fase(
            fulana,
            "'cutting', ?, ?, 'agressivo', 1.00, 'percentual_semana', 71.0",
            dia.minusDays(40),
            dia,
            dia.minusDays(30));
    fase(fulana, "'manutencao', ?, ?, null, null, null, null", dia.plusDays(1), null, null);
    for (int dias = 1; dias <= 40; dias++) {
      registro(
          fulana, dia.minusDays(dias), new BigDecimal("76.0").add(BigDecimal.valueOf(dias, 1)));
    }
    registro(fulana, dia, new BigDecimal("76.1"));
    jdbc.sql("update registro set cintura = 90.0 where usuario_id = ? and data = ?")
        .params(fulana, dia.minusDays(20))
        .update();
    jdbc.sql("update registro set cintura = 88.5 where usuario_id = ? and data = ?")
        .params(fulana, dia.minusDays(2))
        .update();

    acompanhar(fulana)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pesoSemanal").value(76.3))
        .andExpect(jsonPath("$.registrosNaJanela").value(7))
        .andExpect(jsonPath("$.variacoes.semana.valor").value(-0.7))
        .andExpect(jsonPath("$.variacoes.semana.de").value(dia.minusDays(7).toString()))
        .andExpect(jsonPath("$.faseCorrente.id").value(cutting))
        .andExpect(jsonPath("$.veredito.estado").value("NO_RITMO"))
        .andExpect(jsonPath("$.veredito.taxaReal.valor").value(-0.7))
        .andExpect(jsonPath("$.veredito.taxaReal.de").value(dia.minusDays(28).toString()))
        .andExpect(jsonPath("$.taxaAlvoKgSemana").value(-0.76))
        .andExpect(jsonPath("$.faixa").value(nullValue()))
        .andExpect(jsonPath("$.progresso.estado").value("BARRA"))
        // 35.0, e não 35: com o esperado inteiro, o número é lido como inteiro, e 35,97
        // também passaria
        .andExpect(jsonPath("$.progresso.percentual").value(35.0))
        .andExpect(jsonPath("$.progresso.atingido").value(false))
        .andExpect(jsonPath("$.cintura.valor").value(88.5))
        .andExpect(jsonPath("$.cintura.desdeAAnterior").value(-1.5))
        .andExpect(jsonPath("$.serie", hasSize(41)))
        .andExpect(jsonPath("$.serie[0].pesoSemanal").value(nullValue()))
        .andExpect(jsonPath("$.serie[40].data").value(dia.toString()))
        .andExpect(jsonPath("$.serie[40].pesoSemanal").value(76.3))
        .andExpect(jsonPath("$.serie[40].faseId").value(cutting));
  }

  // Pega a fase agendada tomada como corrente, os dados de outra pessoa e o
  // que não depende de fase sumindo junto com ela (RN-66, RN-69)
  @Test
  void semFaseCorrenteVemSoOQueNaoDependeDeFase() throws Exception {
    int outra = usuario("outra@exemplo.com");
    fase(outra, "'manutencao', ?, ?, null, null, null, null", dia.minusDays(10), null, null);
    for (int dias = 0; dias <= 10; dias++) {
      registro(outra, dia.minusDays(dias), new BigDecimal("70.0"));
    }
    int fulana = usuario("fulana@exemplo.com");
    fase(fulana, "'manutencao', ?, ?, null, null, null, null", dia.plusDays(1), null, null);
    for (int dias = 0; dias <= 10; dias++) {
      registro(fulana, dia.minusDays(dias), new BigDecimal("80.0"));
    }

    acompanhar(fulana)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.faseCorrente").value(nullValue()))
        .andExpect(jsonPath("$.veredito").value(nullValue()))
        .andExpect(jsonPath("$.taxaAlvoKgSemana").value(nullValue()))
        .andExpect(jsonPath("$.progresso").value(nullValue()))
        .andExpect(jsonPath("$.pesoSemanal").value(80.0))
        .andExpect(jsonPath("$.registrosNaJanela").value(7))
        .andExpect(jsonPath("$.variacoes.semana.valor").value(0.0));
  }

  // Pega a faixa perdida e a taxa real ao lado do veredito de posição (RN-33)
  @Test
  void aManutencaoTrazAFaixa() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    fase(
        fulana,
        "'manutencao', ?, ?, null, null, null, null",
        dia.minusDays(20),
        null,
        dia.minusDays(10));
    for (int dias = 0; dias <= 20; dias++) {
      registro(fulana, dia.minusDays(dias), new BigDecimal("80.0"));
    }

    acompanhar(fulana)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.faixa.minimo").value(79.2))
        .andExpect(jsonPath("$.faixa.maximo").value(80.8))
        .andExpect(jsonPath("$.veredito.estado").value("NO_RITMO"))
        .andExpect(jsonPath("$.veredito.taxaReal").value(nullValue()))
        .andExpect(jsonPath("$.progresso").value(nullValue()));
  }

  // Pega o dia da base que já existe e não foi fixado: a fase agendada começou
  // hoje sem nenhuma gravação, e o peso semanal de hoje veio dos registros de
  // antes dela (RN-68)
  @Test
  void aLeituraFixaODiaDaBaseQueFalta() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, "'manutencao', ?, ?, null, null, null, null", dia, null, null);
    for (int dias = 3; dias <= 6; dias++) {
      registro(fulana, dia.minusDays(dias), new BigDecimal("80.0"));
    }

    acompanhar(fulana).andExpect(status().isOk()).andExpect(jsonPath("$.faixa.minimo").value(79.2));
    jpa.flush();
    assertThat(
            jdbc.sql("select base_data from fase where id = ?")
                .param(fase)
                .query(LocalDate.class)
                .single())
        .isEqualTo(dia);
  }

  // Pega a conta nova quebrando em vez de convidar ao primeiro registro
  // (UC-08 1b)
  @Test
  void semRegistroNaoHaSerieNemPesoSemanal() throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    acompanhar(fulana)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pesoSemanal").value(nullValue()))
        .andExpect(jsonPath("$.registrosNaJanela").value(0))
        .andExpect(jsonPath("$.variacoes.semana").value(nullValue()))
        .andExpect(jsonPath("$.cintura").value(nullValue()))
        .andExpect(jsonPath("$.serie", empty()));
  }

  private ResultActions acompanhar(int idUsuario) throws Exception {
    return mvc.perform(
        get("/acompanhamento").cookie(acesso(idUsuario)).header("Data-Local", dia.toString()));
  }

  private int usuario(String email) {
    return jdbc.sql(
            "insert into usuario (email, nome, criado_em) values (?, 'Fulana', now()) returning id")
        .param(email)
        .query(Integer.class)
        .single();
  }

  /** O plano vem como trecho de SQL: tipo, as duas datas e o plano, nessa ordem. */
  private int fase(
      int idUsuario,
      String plano,
      LocalDate inicio,
      @Nullable LocalDate fim,
      @Nullable LocalDate diaDaBase) {
    return jdbc.sql(
            "insert into fase (usuario_id, tipo, data_inicio, data_fim, nivel, taxa_alvo_valor,"
                + " taxa_alvo_unidade, peso_alvo, base_data, criado_em) values (?, "
                + plano
                + ", ?, now()) returning id")
        .params(idUsuario, inicio, fim, diaDaBase)
        .query(Integer.class)
        .single();
  }

  private void registro(int idUsuario, LocalDate data, BigDecimal peso) {
    jdbc.sql("insert into registro (usuario_id, data, peso, criado_em) values (?, ?, ?, now())")
        .params(idUsuario, data, peso)
        .update();
  }

  private static Cookie acesso(int idUsuario) {
    return new Cookie(
        "acesso", ChaveDeTeste.token(idUsuario, Instant.now().plus(10, ChronoUnit.MINUTES)));
  }
}
