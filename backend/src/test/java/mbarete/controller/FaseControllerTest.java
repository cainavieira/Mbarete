package mbarete.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.flyway.enabled=true")
@AutoConfigureMockMvc
@Transactional
class FaseControllerTest {

  /** O dia do servidor em UTC, que o cabeçalho Data-Local pode repetir (endpoints.md). */
  private final LocalDate hoje = LocalDate.now(ZoneOffset.UTC);

  private final MockMvc mvc;
  private final JdbcClient jdbc;
  private final EntityManager jpa;

  FaseControllerTest(
      @Autowired MockMvc mvc, @Autowired JdbcClient jdbc, @Autowired EntityManager jpa) {
    this.mvc = mvc;
    this.jdbc = jdbc;
    this.jpa = jpa;
  }

  // Pega a fase que não é gravada, e o plano perdido ou trocado entre o corpo,
  // o banco e a resposta (UC-05)
  @Test
  void criaACuttingComOPlano() throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    criar(
            fulana,
            hoje,
            """
            {"tipo": "CUTTING", "dataInicio": "2026-09-01", "nivel": "MODERADO",
             "taxaAlvoValor": 0.50, "taxaAlvoUnidade": "PERCENTUAL_SEMANA", "pesoAlvo": 75.0}
            """)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tipo").value("CUTTING"))
        .andExpect(jsonPath("$.dataInicio").value("2026-09-01"))
        .andExpect(jsonPath("$.dataFim").value(nullValue()))
        .andExpect(jsonPath("$.nivel").value("MODERADO"))
        .andExpect(jsonPath("$.taxaAlvoValor").value(0.5))
        .andExpect(jsonPath("$.taxaAlvoUnidade").value("PERCENTUAL_SEMANA"))
        .andExpect(jsonPath("$.pesoAlvo").value(75.0));
    assertThat(fases(fulana))
        .singleElement()
        .satisfies(
            fase -> {
              assertThat(fase.tipo()).isEqualTo("cutting");
              assertThat(fase.nivel()).isEqualTo("moderado");
              assertThat(fase.taxaAlvoValor()).isEqualByComparingTo("0.50");
              assertThat(fase.taxaAlvoUnidade()).isEqualTo("percentual_semana");
              assertThat(fase.pesoAlvo()).isEqualByComparingTo("75.0");
            });
  }

  // Pega o nível escolhido gravado no lugar do personalizado, quando a taxa
  // veio editada (RN-30)
  @Test
  void aTaxaEditadaGravaONivelPersonalizado() throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    criar(
            fulana,
            hoje,
            """
            {"tipo": "CUTTING", "dataInicio": "2026-09-01", "nivel": "MODERADO",
             "taxaAlvoValor": 0.75, "taxaAlvoUnidade": "PERCENTUAL_SEMANA", "pesoAlvo": 75.0}
            """)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nivel").value("PERSONALIZADO"));
    assertThat(fases(fulana)).singleElement().extracting(Gravada::nivel).isEqualTo("personalizado");
  }

  // Pega o plano da manutenção gravado como veio, que o banco recusaria como
  // erro interno (RN-33, RN-41)
  @Test
  void aManutencaoDescartaOPlano() throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    criar(
            fulana,
            hoje,
            """
            {"tipo": "MANUTENCAO", "dataInicio": "2026-09-01", "nivel": "MODERADO",
             "taxaAlvoValor": 0.50, "taxaAlvoUnidade": "PERCENTUAL_SEMANA", "pesoAlvo": 75.0}
            """)
        .andExpect(status().isCreated());
    assertThat(fases(fulana))
        .singleElement()
        .satisfies(
            fase -> {
              assertThat(fase.nivel()).isNull();
              assertThat(fase.taxaAlvoValor()).isNull();
              assertThat(fase.pesoAlvo()).isNull();
            });
  }

  // Pega cada recusa fora do status e da regra do catálogo, a anotação de
  // faixa ausente no DTO, e o fim futuro contado pelo dia do servidor: o
  // dispositivo ainda está em ontem (RN-67)
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "\"nivel\": null                         | NIVEL_AUSENTE            | nivel         | RN-28",
        "\"taxaAlvoValor\": null                 | TAXA_ALVO_AUSENTE        | taxaAlvoValor | RN-29",
        "\"pesoAlvo\": null                      | PESO_ALVO_AUSENTE        | pesoAlvo      | RN-41",
        "\"taxaAlvoValor\": 0                    | TAXA_ALVO_INVALIDA       | taxaAlvoValor | RN-29",
        "\"pesoAlvo\": 19.9                      | PESO_FORA_DE_FAIXA       | pesoAlvo      | RN-03",
        "\"dataFim\": \"HOJE\"                   | DATA_FIM_FUTURA          | dataFim       | RN-67",
        "\"dataFim\": \"2026-08-31\"             | DATA_FIM_ANTES_DO_INICIO | dataFim       | RN-18"
      })
  void recusaSaiComOStatusEARegraDoCatalogo(
      String campoEnviado, String codigo, String campo, String regra) throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    criar(fulana, hoje.minusDays(1), cutting(campoEnviado.replace("HOJE", hoje.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value(codigo))
        .andExpect(jsonPath("$.erro.campo").value(campo))
        .andExpect(jsonPath("$.erro.regra").value(regra));
    assertThat(fases(fulana)).isEmpty();
  }

  // Pega o tipo ou o início sem conferir a presença, que estoura como erro
  // interno, e a casa a mais que o banco arredondaria calado (RN-02, RN-29)
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "\"tipo\": null           | tipo",
        "\"dataInicio\": null     | dataInicio",
        "\"taxaAlvoValor\": 0.505 | taxaAlvoValor",
        "\"pesoAlvo\": 75.05      | pesoAlvo"
      })
  void campoAusenteOuComCasaAMaisSaiComoCorpoMalformado(String campoEnviado, String campo)
      throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    criar(fulana, hoje, cutting(campoEnviado))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro.codigo").value("CORPO_MALFORMADO"))
        .andExpect(jsonPath("$.erro.campo").value(campo));
  }

  // Pega a sobreposição fora do status de conflito do catálogo (RN-19)
  @Test
  void sobreposicaoRespondeConflito() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    fase(fulana, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), null);

    criar(fulana, hoje, cutting("\"dataInicio\": \"2026-09-05\", \"dataFim\": \"2026-09-08\""))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.erro.codigo").value("FASE_SOBREPOSTA"))
        .andExpect(jsonPath("$.erro.regra").value("RN-19"));
    assertThat(fases(fulana)).hasSize(1);
  }

  // Pega o encerramento que não é gravado, e o gravado só depois da fase nova,
  // que o banco recusa como sobreposição (RN-22, UC-05 2a)
  @Test
  void semFimEncerraAFaseEmAbertoNaVespera() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    fase(fulana, LocalDate.of(2026, 9, 1), null, null);

    criar(fulana, hoje, cutting("\"dataInicio\": \"2026-09-15\"")).andExpect(status().isCreated());
    assertThat(fases(fulana))
        .extracting(Gravada::dataInicio, Gravada::dataFim)
        .containsExactly(
            tuple(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14)),
            tuple(LocalDate.of(2026, 9, 15), null));
  }

  // Pega o dia da base da fase encerrada que fica fora do intervalo novo dela,
  // e a fase nova sem o dia da base onde já há peso semanal (RN-68)
  @Test
  void criarFixaODiaDaBaseDaNovaEDeterminaDeNovoODaEncerrada() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    fase(fulana, LocalDate.of(2026, 9, 1), null, LocalDate.of(2026, 9, 20));
    for (int dia = 17; dia <= 20; dia++) {
      registro(fulana, LocalDate.of(2026, 9, dia));
    }

    criar(fulana, hoje, cutting("\"dataInicio\": \"2026-09-15\"")).andExpect(status().isCreated());
    assertThat(fases(fulana))
        .extracting(Gravada::baseData)
        .containsExactly(null, LocalDate.of(2026, 9, 20));
  }

  // Pega as fases procuradas sem o usuário, que encerraria a fase em aberto de
  // outra pessoa (RN-50)
  @Test
  void aFaseEmAbertoDeOutraPessoaNaoEEncerrada() throws Exception {
    int outra = usuario("outra@exemplo.com");
    fase(outra, LocalDate.of(2026, 9, 1), null, null);
    int fulana = usuario("fulana@exemplo.com");

    criar(fulana, hoje, cutting("\"dataInicio\": \"2026-09-15\"")).andExpect(status().isCreated());
    assertThat(fases(outra)).singleElement().extracting(Gravada::dataFim).isNull();
  }

  // Pega a conta apagada com o token ainda válido virando erro interno na chave
  // estrangeira, em vez de mandar o frontend de volta à entrada (UC-12)
  @Test
  void contaQueNaoExisteMaisRespondeNaoAutenticado() throws Exception {
    criar(9999, hoje, cutting("\"dataInicio\": \"2026-09-15\""))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_AUTENTICADO"));
  }

  // Pega o personalizado ou a manutenção na lista, que não têm padrão, e a taxa
  // de um nível trocada com a de outro (RN-31, RN-32)
  @Test
  void osNiveisTrazemATaxaPadraoDeCadaUm() throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    mvc.perform(get("/niveis").cookie(acesso(fulana)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[*].tipo")
                .value(contains("CUTTING", "CUTTING", "CUTTING", "BULKING", "BULKING", "BULKING")))
        .andExpect(
            jsonPath("$[*].nivel")
                .value(
                    contains(
                        "CONSERVADOR",
                        "MODERADO",
                        "AGRESSIVO",
                        "CONSERVADOR",
                        "MODERADO",
                        "AGRESSIVO")))
        .andExpect(jsonPath("$[*].valor").value(contains(0.25, 0.5, 1.0, 0.2, 0.3, 0.4)))
        .andExpect(
            jsonPath("$[*].unidade")
                .value(
                    contains(
                        "PERCENTUAL_SEMANA",
                        "PERCENTUAL_SEMANA",
                        "PERCENTUAL_SEMANA",
                        "KG_SEMANA",
                        "KG_SEMANA",
                        "KG_SEMANA")));
  }

  record Gravada(
      String tipo,
      LocalDate dataInicio,
      @Nullable LocalDate dataFim,
      @Nullable String nivel,
      @Nullable BigDecimal taxaAlvoValor,
      @Nullable String taxaAlvoUnidade,
      @Nullable BigDecimal pesoAlvo,
      @Nullable LocalDate baseData) {}

  /** Uma cutting válida, com o trecho dado sobrepondo os campos dela. */
  private static String cutting(String trecho) {
    return """
        {"tipo": "CUTTING", "dataInicio": "2026-09-01", "nivel": "MODERADO",
         "taxaAlvoValor": 0.50, "taxaAlvoUnidade": "PERCENTUAL_SEMANA", "pesoAlvo": 75.0,
         %s}
        """
        .formatted(trecho);
  }

  private ResultActions criar(int idUsuario, LocalDate dataLocal, String corpo) throws Exception {
    return mvc.perform(
        post("/fases")
            .cookie(acesso(idUsuario))
            .header("Data-Local", dataLocal.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
  }

  /** O que está no banco, depois de o JPA escrever o que guardou na transação do teste. */
  private List<Gravada> fases(int idUsuario) {
    jpa.flush();
    return jdbc.sql(
            "select tipo, data_inicio, data_fim, nivel, taxa_alvo_valor, taxa_alvo_unidade,"
                + " peso_alvo, base_data from fase where usuario_id = ? order by data_inicio")
        .param(idUsuario)
        .query(Gravada.class)
        .list();
  }

  private int usuario(String email) {
    return jdbc.sql(
            "insert into usuario (email, nome, criado_em) values (?, 'Fulana', now()) returning id")
        .param(email)
        .query(Integer.class)
        .single();
  }

  /** Uma manutenção, que não pede plano (RN-33). */
  private void fase(
      int idUsuario, LocalDate inicio, @Nullable LocalDate fim, @Nullable LocalDate diaDaBase) {
    jdbc.sql(
            "insert into fase (usuario_id, tipo, data_inicio, data_fim, base_data, criado_em)"
                + " values (?, 'manutencao', ?, ?, ?, now())")
        .params(idUsuario, inicio, fim, diaDaBase)
        .update();
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
