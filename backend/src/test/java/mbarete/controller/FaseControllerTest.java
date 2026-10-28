package mbarete.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

  // Pega o dia da base da fase encerrada que fica depois do novo fim dela, e a
  // fase nova sem o dia da base onde já há peso semanal (RN-68)
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

  // Pega o dia da base que cabe no novo fim, até no último dia, e mesmo assim é
  // escolhido de novo: os registros dos dias 1 a 4 chegaram depois, e a base
  // saltaria para o dia 4 (RN-68)
  @Test
  void oDiaDaBaseQueCabeNoNovoFimFica() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    fase(fulana, LocalDate.of(2026, 9, 1), null, LocalDate.of(2026, 9, 14));
    for (int dia = 1; dia <= 4; dia++) {
      registro(fulana, LocalDate.of(2026, 9, dia));
    }

    criar(fulana, hoje, cutting("\"dataInicio\": \"2026-09-15\"")).andExpect(status().isCreated());
    assertThat(fases(fulana))
        .extracting(Gravada::baseData)
        .containsExactly(LocalDate.of(2026, 9, 14), null);
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

  // Pega a edição que não é gravada (UC-06)
  @Test
  void editaOPlanoEAsDatas() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = faseCutting(fulana, LocalDate.of(2026, 9, 1));

    editar(fulana, fase, hoje, cutting("\"dataFim\": \"2026-09-30\", \"pesoAlvo\": 72.0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dataFim").value("2026-09-30"))
        .andExpect(jsonPath("$.pesoAlvo").value(72.0));
    assertThat(fases(fulana))
        .singleElement()
        .satisfies(
            gravada -> {
              assertThat(gravada.dataFim()).isEqualTo(LocalDate.of(2026, 9, 30));
              assertThat(gravada.pesoAlvo()).isEqualByComparingTo("72.0");
            });
  }

  // Pega a edição que confere o plano como na criação, e grava como
  // personalizado a taxa do cutting que ficou na bulking (RN-28, UC-06 2j)
  @Test
  void trocarDeTipoComATaxaComoEstavaGravaOPadraoDoNivel() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = faseCutting(fulana, LocalDate.of(2026, 9, 1));

    editar(fulana, fase, hoje, cutting("\"tipo\": \"BULKING\"")).andExpect(status().isOk());
    assertThat(fases(fulana))
        .singleElement()
        .satisfies(
            gravada -> {
              assertThat(gravada.nivel()).isEqualTo("moderado");
              assertThat(gravada.taxaAlvoValor()).isEqualByComparingTo("0.30");
              assertThat(gravada.taxaAlvoUnidade()).isEqualTo("kg_semana");
            });
  }

  // Pega o fim no futuro da anterior que não é gravado junto, e a ordem errada
  // das gravações: quem cresce antes de quem encolhe esbarra na outra, e o
  // banco recusa como sobreposição (RN-67, UC-06 2i)
  @ParameterizedTest
  @CsvSource({"-10, -11", "10, 9"})
  void oInicioDaSeguinteMoveOFimNoFuturoDaAnterior(int inicio, int fimDaAnterior) throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    fase(fulana, hoje.minusDays(30), hoje.plusDays(5), null);
    int seguinte = fase(fulana, hoje.plusDays(6), null, null);

    editar(
            fulana,
            seguinte,
            hoje,
            "{\"tipo\": \"MANUTENCAO\", \"dataInicio\": \"%s\"}".formatted(hoje.plusDays(inicio)))
        .andExpect(status().isOk());
    assertThat(fases(fulana))
        .extracting(Gravada::dataFim)
        .containsExactly(hoje.plusDays(fimDaAnterior), null);
  }

  // Pega o fim de antes tomado do corpo em vez do banco, que deixaria passar
  // todo fim futuro, e o futuro contado pelo dia do servidor (RN-67)
  @Test
  void umFimFuturoNovoERecusado() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, LocalDate.of(2026, 9, 1), null, null);

    editar(
            fulana,
            fase,
            hoje.minusDays(1),
            "{\"tipo\": \"MANUTENCAO\", \"dataInicio\": \"2026-09-01\", \"dataFim\": \"%s\"}"
                .formatted(hoje))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value("DATA_FIM_FUTURA"));
  }

  // Pega a edição sem conferir o corpo, que o banco recusaria como erro
  // interno
  @Test
  void edicaoComPesoAlvoForaDaFaixaERecusada() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = faseCutting(fulana, LocalDate.of(2026, 9, 1));

    editar(fulana, fase, hoje, cutting("\"pesoAlvo\": 19.9"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value("PESO_FORA_DE_FAIXA"));
  }

  // Pega a fase procurada sem o usuário, que deixaria editar a de outra pessoa
  // (RN-50)
  @Test
  void aFaseDeOutraPessoaNaoEEncontrada() throws Exception {
    int outra = usuario("outra@exemplo.com");
    int daOutra = faseCutting(outra, LocalDate.of(2026, 9, 1));
    int fulana = usuario("fulana@exemplo.com");

    editar(fulana, daOutra, hoje, cutting("\"pesoAlvo\": 72.0"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_ENCONTRADO"));
    assertThat(fases(outra))
        .singleElement()
        .satisfies(gravada -> assertThat(gravada.pesoAlvo()).isEqualByComparingTo("75.0"));
  }

  // Pega as fases conferidas sem o usuário, que recusaria a edição por
  // sobreposição com a fase de outra pessoa (RN-50)
  @Test
  void aFaseDeOutraPessoaNaoSobrepoe() throws Exception {
    int outra = usuario("outra@exemplo.com");
    fase(outra, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null);
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, LocalDate.of(2026, 10, 1), null, null);

    editar(fulana, fase, hoje, "{\"tipo\": \"MANUTENCAO\", \"dataInicio\": \"2026-09-15\"}")
        .andExpect(status().isOk());
  }

  // Pega o dia da base que fica antes do início novo, fora da fase: a janela
  // do dia 8 já tem os registros dos dias 2, 3, 4 e 8 (RN-68, UC-06 2f)
  @Test
  void mudarOInicioDeterminaDeNovoODiaDaBase() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, LocalDate.of(2026, 9, 1), null, LocalDate.of(2026, 9, 4));
    for (int dia : new int[] {1, 2, 3, 4, 8}) {
      registro(fulana, LocalDate.of(2026, 9, dia));
    }

    editar(fulana, fase, hoje, "{\"tipo\": \"MANUTENCAO\", \"dataInicio\": \"2026-09-08\"}")
        .andExpect(status().isOk());
    assertThat(fases(fulana))
        .singleElement()
        .extracting(Gravada::baseData)
        .isEqualTo(LocalDate.of(2026, 9, 8));
  }

  // Pega o encerramento que não é gravado, e a fase corrente procurada pelo
  // dia do servidor: no dispositivo ainda é ontem, e a fase vai até ontem
  // (RN-04, RN-27)
  @Test
  void encerraAFaseCorrenteDoDiaDoDispositivo() throws Exception {
    LocalDate ontem = hoje.minusDays(1);
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, LocalDate.of(2026, 9, 1), ontem, null);

    encerrar(fulana, fase, ontem, ontem.minusDays(3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dataFim").value(ontem.minusDays(3).toString()));
    assertThat(fases(fulana))
        .singleElement()
        .extracting(Gravada::dataFim)
        .isEqualTo(ontem.minusDays(3));
  }

  // Pega a recusa da fase que não é a corrente fora do conflito do catálogo
  // (RN-27)
  @Test
  void aFaseQueNaoEACorrenteRespondeConflito() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), null);

    encerrar(fulana, fase, hoje, LocalDate.of(2026, 9, 5))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.erro.codigo").value("FASE_NAO_CORRENTE"))
        .andExpect(jsonPath("$.erro.regra").value("RN-27"));
  }

  // Pega o dia da base que fica depois do fim, e o que não é procurado de novo
  // no intervalo que sobrou: os registros dos dias 1 a 4 chegaram depois que
  // ele foi fixado no dia 20 (RN-68)
  @Test
  void encerrarAntesDoDiaDaBaseODeterminaDeNovo() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, LocalDate.of(2026, 9, 1), null, LocalDate.of(2026, 9, 20));
    for (int dia : new int[] {1, 2, 3, 4, 17, 18, 19, 20}) {
      registro(fulana, LocalDate.of(2026, 9, dia));
    }

    encerrar(fulana, fase, hoje, LocalDate.of(2026, 9, 14)).andExpect(status().isOk());
    assertThat(fases(fulana))
        .singleElement()
        .extracting(Gravada::baseData)
        .isEqualTo(LocalDate.of(2026, 9, 4));
  }

  // Pega a fase procurada sem o usuário, que deixaria encerrar a de outra
  // pessoa (RN-50)
  @Test
  void aFaseDeOutraPessoaNaoEEncerrada() throws Exception {
    int outra = usuario("outra@exemplo.com");
    int daOutra = fase(outra, LocalDate.of(2026, 9, 1), null, null);
    int fulana = usuario("fulana@exemplo.com");

    encerrar(fulana, daOutra, hoje, LocalDate.of(2026, 9, 14))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_ENCONTRADO"));
    assertThat(fases(outra)).singleElement().extracting(Gravada::dataFim).isNull();
  }

  // Pega a data sem conferir a presença, que estoura como erro interno
  @Test
  void encerramentoSemDataSaiComoCorpoMalformado() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, LocalDate.of(2026, 9, 1), null, null);

    mvc.perform(
            post("/fases/{id}/encerramento", fase)
                .cookie(acesso(fulana))
                .header("Data-Local", hoje.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro.campo").value("dataFim"));
  }

  // Pega a fase que não é apagada, e os registros dela levados junto: eles só
  // perdem o rótulo do período (RN-70)
  @Test
  void apagaAFaseEOsRegistrosFicam() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, LocalDate.of(2026, 9, 1), null, null);
    registro(fulana, LocalDate.of(2026, 9, 5));
    registro(fulana, LocalDate.of(2026, 9, 6));

    apagar(fulana, fase).andExpect(status().isNoContent());
    assertThat(fases(fulana)).isEmpty();
    assertThat(
            jdbc.sql("select count(*) from registro where usuario_id = ?")
                .param(fulana)
                .query(Integer.class)
                .single())
        .isEqualTo(2);
  }

  // Pega o fim no futuro da anterior que fica, e a anterior esticada antes de
  // a apagada sair do banco, que recusa a sobreposição (RN-67, UC-07 3b)
  @Test
  void apagarASeguinteTiraOFimNoFuturoDaAnterior() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    fase(fulana, hoje.minusDays(30), hoje.plusDays(5), null);
    int seguinte = fase(fulana, hoje.plusDays(6), null, null);

    apagar(fulana, seguinte).andExpect(status().isNoContent());
    assertThat(fases(fulana)).singleElement().extracting(Gravada::dataFim).isNull();
  }

  // Pega a fase procurada sem o usuário, que deixaria apagar a de outra pessoa
  // (RN-50)
  @Test
  void aFaseDeOutraPessoaNaoEApagada() throws Exception {
    int outra = usuario("outra@exemplo.com");
    int daOutra = fase(outra, LocalDate.of(2026, 9, 1), null, null);
    int fulana = usuario("fulana@exemplo.com");

    apagar(fulana, daOutra)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_ENCONTRADO"));
    assertThat(fases(outra)).hasSize(1);
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
  private int fase(
      int idUsuario, LocalDate inicio, @Nullable LocalDate fim, @Nullable LocalDate diaDaBase) {
    return jdbc.sql(
            "insert into fase (usuario_id, tipo, data_inicio, data_fim, base_data, criado_em)"
                + " values (?, 'manutencao', ?, ?, ?, now()) returning id")
        .params(idUsuario, inicio, fim, diaDaBase)
        .query(Integer.class)
        .single();
  }

  /** Uma cutting moderada, com a taxa padrão do nível (RN-31). */
  private int faseCutting(int idUsuario, LocalDate inicio) {
    return jdbc.sql(
            "insert into fase (usuario_id, tipo, data_inicio, nivel, taxa_alvo_valor,"
                + " taxa_alvo_unidade, peso_alvo, criado_em) values (?, 'cutting', ?,"
                + " 'moderado', 0.50, 'percentual_semana', 75.0, now()) returning id")
        .params(idUsuario, inicio)
        .query(Integer.class)
        .single();
  }

  private ResultActions encerrar(int idUsuario, int idFase, LocalDate dataLocal, LocalDate fim)
      throws Exception {
    return mvc.perform(
        post("/fases/{id}/encerramento", idFase)
            .cookie(acesso(idUsuario))
            .header("Data-Local", dataLocal.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"dataFim\": \"%s\"}".formatted(fim)));
  }

  private ResultActions apagar(int idUsuario, int idFase) throws Exception {
    return mvc.perform(
        delete("/fases/{id}", idFase)
            .cookie(acesso(idUsuario))
            .header("Data-Local", hoje.toString()));
  }

  private ResultActions editar(int idUsuario, int idFase, LocalDate dataLocal, String corpo)
      throws Exception {
    return mvc.perform(
        patch("/fases/{id}", idFase)
            .cookie(acesso(idUsuario))
            .header("Data-Local", dataLocal.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
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
