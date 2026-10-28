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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.flyway.enabled=true")
@AutoConfigureMockMvc
@Transactional
class RegistroControllerTest {

  /** O dia do servidor em UTC, que o cabeçalho Data-Local pode repetir (endpoints.md). */
  private final LocalDate hoje = LocalDate.now(ZoneOffset.UTC);

  private static final LocalDate DIA = LocalDate.of(2026, 9, 7);
  private static final LocalDate OUTRO_DIA = LocalDate.of(2026, 9, 8);

  private final MockMvc mvc;
  private final JdbcClient jdbc;
  private final EntityManager jpa;

  RegistroControllerTest(
      @Autowired MockMvc mvc, @Autowired JdbcClient jdbc, @Autowired EntityManager jpa) {
    this.mvc = mvc;
    this.jdbc = jdbc;
    this.jpa = jpa;
  }

  // Pega o registro de hoje recusado como data futura, e o peso ou a cintura
  // trocados ou perdidos entre o corpo, o banco e a resposta
  @Test
  void gravaORegistroDeHoje() throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    registrar(fulana, hoje, "{\"data\": \"%s\", \"peso\": 82.4, \"cintura\": 90.5}".formatted(hoje))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data").value(hoje.toString()))
        .andExpect(jsonPath("$.peso").value(82.4))
        .andExpect(jsonPath("$.cintura").value(90.5));
    assertThat(registros(fulana))
        .singleElement()
        .satisfies(
            registro -> {
              assertThat(registro.data()).isEqualTo(hoje);
              assertThat(registro.peso()).isEqualByComparingTo("82.4");
              assertThat(registro.cintura()).isEqualByComparingTo("90.5");
            });
  }

  // Pega a data futura aceita, e a comparada com o dia do servidor em vez do
  // dia do dispositivo (RN-04, RN-05): o dispositivo ainda está em ontem
  @Test
  void dataDepoisDoDiaDoDispositivoERecusada() throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    registrar(fulana, hoje.minusDays(1), "{\"data\": \"%s\", \"peso\": 82.4}".formatted(hoje))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value("DATA_FUTURA"))
        .andExpect(jsonPath("$.erro.campo").value("data"))
        .andExpect(jsonPath("$.erro.regra").value("RN-05"));
    assertThat(registros(fulana)).isEmpty();
  }

  // Pega cada ponta da faixa sem conferir na aplicação, que o banco recusaria
  // como erro interno, e a cintura com a frase genérica do catálogo
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "19.9  | 90.0  | peso    | PESO_FORA_DE_FAIXA    | RN-03 | O peso deve estar entre 20,0 e 400,0 kg.",
        "400.1 | 90.0  | peso    | PESO_FORA_DE_FAIXA    | RN-03 | O peso deve estar entre 20,0 e 400,0 kg.",
        "82.4  | 39.9  | cintura | CINTURA_FORA_DE_FAIXA | RN-11 | A cintura deve estar entre 40,0 e 200,0 cm.",
        "82.4  | 200.1 | cintura | CINTURA_FORA_DE_FAIXA | RN-11 | A cintura deve estar entre 40,0 e 200,0 cm."
      })
  void pesoOuCinturaForaDaFaixaERecusado(
      String peso, String cintura, String campo, String codigo, String regra, String mensagem)
      throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    registrar(
            fulana,
            hoje,
            "{\"data\": \"%s\", \"peso\": %s, \"cintura\": %s}".formatted(DIA, peso, cintura))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value(codigo))
        .andExpect(jsonPath("$.erro.campo").value(campo))
        .andExpect(jsonPath("$.erro.regra").value(regra))
        .andExpect(jsonPath("$.erro.mensagem").value(mensagem));
  }

  // Pega a faixa aberta, que recusa a própria borda (RN-03, RN-11)
  @ParameterizedTest
  @CsvSource({"20.0, 40.0", "400.0, 200.0"})
  void pesoECinturaNaBordaDaFaixaSaoAceitos(String peso, String cintura) throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    registrar(
            fulana,
            hoje,
            "{\"data\": \"%s\", \"peso\": %s, \"cintura\": %s}".formatted(DIA, peso, cintura))
        .andExpect(status().isCreated());
  }

  // Pega a data ou o peso sem conferir a presença, que estoura como erro
  // interno, e a segunda casa decimal que o banco arredondaria calado (RN-02,
  // RN-10)
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "{\"peso\": 82.4}                                    | data",
        "{\"data\": \"2026-09-07\"}                          | peso",
        "{\"data\": \"2026-09-07\", \"peso\": 82.45}         | peso",
        "{\"data\": \"2026-09-07\", \"peso\": 82.4, \"cintura\": 90.55} | cintura"
      })
  void campoAusenteOuComDuasCasasSaiComoCorpoMalformado(String corpo, String campo)
      throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    registrar(fulana, hoje, corpo)
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.erro.codigo").value("CORPO_MALFORMADO"))
        .andExpect(jsonPath("$.erro.campo").value(campo));
    assertThat(registros(fulana)).isEmpty();
  }

  // Pega a data ocupada substituída sem a confirmação (RN-01), o conflito que
  // estoura como erro interno na restrição do banco, e a mensagem sem o valor
  // gravado que o diálogo mostra (UC-02 4a)
  @Test
  void dataOcupadaSemSubstituirRespondeConflitoComOValorGravado() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, DIA, "82.4", "90.0");

    registrar(fulana, hoje, "{\"data\": \"2026-09-07\", \"peso\": 81.0}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.erro.codigo").value("REGISTRO_JA_EXISTE"))
        .andExpect(jsonPath("$.erro.campo").value("data"))
        .andExpect(
            jsonPath("$.erro.mensagem").value("Já existe registro em 07/09/2026, com 82,4 kg."))
        .andExpect(jsonPath("$.erro.regra").value("RN-01"));
    assertThat(registros(fulana))
        .singleElement()
        .satisfies(
            registro -> {
              assertThat(registro.peso()).isEqualByComparingTo("82.4");
              assertThat(registro.cintura()).isEqualByComparingTo("90.0");
            });
  }

  // Pega a confirmação que grava um segundo registro na data, e a que deixa a
  // cintura da pesagem substituída no registro novo, que veio sem ela (RN-01)
  @Test
  void comSubstituirOEnvioNovoTomaOLugarDoRegistroDaData() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, DIA, "82.4", "90.0");

    registrar(fulana, hoje, "{\"data\": \"2026-09-07\", \"peso\": 81.0, \"substituir\": true}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.peso").value(81.0))
        .andExpect(jsonPath("$.cintura").value(nullValue()));
    assertThat(registros(fulana))
        .singleElement()
        .satisfies(
            registro -> {
              assertThat(registro.peso()).isEqualByComparingTo("81.0");
              assertThat(registro.cintura()).isNull();
            });
  }

  // Pega a confirmação que exige o registro antigo, quando outra aba o apagou
  // entre o conflito e o segundo envio
  @Test
  void comSubstituirEADataLivreSoGrava() throws Exception {
    int fulana = usuario("fulana@exemplo.com");

    registrar(fulana, hoje, "{\"data\": \"2026-09-07\", \"peso\": 81.0, \"substituir\": true}")
        .andExpect(status().isCreated());
    assertThat(registros(fulana)).hasSize(1);
  }

  // Pega a data procurada sem o usuário, que daria conflito com o registro de
  // outra pessoa e mostraria o peso dela (RN-50)
  @Test
  void oRegistroDeOutraPessoaNaMesmaDataNaoConflita() throws Exception {
    int outra = usuario("outra@exemplo.com");
    registro(outra, DIA, "70.0", null);
    int fulana = usuario("fulana@exemplo.com");

    registrar(fulana, hoje, "{\"data\": \"2026-09-07\", \"peso\": 82.4}")
        .andExpect(status().isCreated());
    assertThat(registros(fulana)).hasSize(1);
    assertThat(registros(outra))
        .singleElement()
        .satisfies(registro -> assertThat(registro.peso()).isEqualByComparingTo("70.0"));
  }

  // Pega a conta apagada com o token ainda válido virando erro interno na chave
  // estrangeira, em vez de mandar o frontend de volta à entrada (UC-12)
  @Test
  void contaQueNaoExisteMaisRespondeNaoAutenticado() throws Exception {
    registrar(9999, hoje, "{\"data\": \"2026-09-07\", \"peso\": 82.4}")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_AUTENTICADO"));
  }

  // Pega o dia da base que não é fixado, fixado no dia do registro ou no
  // início da fase, e o que só olha a fase da data do registro: o registro é
  // da véspera da fase, e é ele que completa os 4 da janela do dia 3 (RN-68).
  // Pega também o peso de outra pessoa entrando na janela, que daria base já
  // no dia 1, e a fase dela fixada com o peso de quem registrou (RN-50)
  @Test
  void oPrimeiroPesoSemanalDaFaseFixaODiaDaBase() throws Exception {
    int outra = usuario("outra@exemplo.com");
    int faseDaOutra = fase(outra, LocalDate.of(2026, 9, 1), null);
    registro(outra, LocalDate.of(2026, 8, 28), "70.0", null);
    registro(outra, LocalDate.of(2026, 8, 29), "70.0", null);
    registro(outra, LocalDate.of(2026, 8, 30), "70.0", null);
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, LocalDate.of(2026, 9, 1), null);
    registro(fulana, LocalDate.of(2026, 9, 1), "80.0", null);
    registro(fulana, LocalDate.of(2026, 9, 2), "80.0", null);
    registro(fulana, LocalDate.of(2026, 9, 3), "80.0", null);

    registrar(fulana, hoje, "{\"data\": \"2026-08-31\", \"peso\": 80.0}")
        .andExpect(status().isCreated());
    assertThat(diaDaBase(fase)).isEqualTo(LocalDate.of(2026, 9, 3));
    assertThat(diaDaBase(faseDaOutra)).isNull();
  }

  // Pega o dia da base escolhido de novo a cada registro, que faria a base
  // saltar de semana com um registro retroativo (RN-68)
  @Test
  void oDiaDaBaseJaFixadoNaoMuda() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));
    registro(fulana, LocalDate.of(2026, 9, 1), "80.0", null);
    registro(fulana, LocalDate.of(2026, 9, 2), "80.0", null);
    registro(fulana, LocalDate.of(2026, 9, 3), "80.0", null);

    registrar(fulana, hoje, "{\"data\": \"2026-08-31\", \"peso\": 80.0}")
        .andExpect(status().isCreated());
    assertThat(diaDaBase(fase)).isEqualTo(LocalDate.of(2026, 9, 10));
  }

  // Pega a edição que não grava, a cintura enviada que se perde, e a data igual
  // à do caminho tratada como mudança, que daria conflito com o próprio
  // registro (RN-07)
  @Test
  void editaPesoECinturaNaMesmaData() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, DIA, "82.4", "90.0");

    editar(fulana, DIA, hoje, "{\"data\": \"2026-09-07\", \"peso\": 81.5, \"cintura\": 89.0}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.peso").value(81.5))
        .andExpect(jsonPath("$.cintura").value(89.0));
    assertThat(registros(fulana))
        .singleElement()
        .satisfies(
            registro -> {
              assertThat(registro.peso()).isEqualByComparingTo("81.5");
              assertThat(registro.cintura()).isEqualByComparingTo("89.0");
            });
  }

  // Pega o registro copiado em vez de movido, a data que não muda, e a cintura
  // antiga que fica quando a edição vem sem ela (UC-03)
  @Test
  void mudarADataMoveORegistro() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, DIA, "82.4", "90.0");

    editar(fulana, DIA, hoje, "{\"data\": \"2026-09-08\", \"peso\": 82.4}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value("2026-09-08"));
    assertThat(registros(fulana))
        .singleElement()
        .satisfies(
            registro -> {
              assertThat(registro.data()).isEqualTo(OUTRO_DIA);
              assertThat(registro.cintura()).isNull();
            });
  }

  // Pega a data ocupada sobrescrita sem a confirmação, e a mensagem com o valor
  // do registro editado em vez do que já está na data (UC-03 2a)
  @Test
  void moverParaDataOcupadaSemSubstituirRespondeConflito() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, DIA, "82.4", null);
    registro(fulana, OUTRO_DIA, "80.0", null);

    editar(fulana, DIA, hoje, "{\"data\": \"2026-09-08\", \"peso\": 82.4}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.erro.codigo").value("REGISTRO_JA_EXISTE"))
        .andExpect(
            jsonPath("$.erro.mensagem").value("Já existe registro em 08/09/2026, com 80,0 kg."));
    assertThat(registros(fulana))
        .extracting(Gravado::data, registro -> registro.peso().toPlainString())
        .containsExactly(tuple(DIA, "82.4"), tuple(OUTRO_DIA, "80.0"));
  }

  // Pega o registro da data nova que não é apagado, e o apagado só depois de
  // mover, que o banco recusa como dois na mesma data: dois passam a ser um
  @Test
  void moverComSubstituirApagaORegistroDaDataNova() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, DIA, "82.4", null);
    registro(fulana, OUTRO_DIA, "80.0", null);

    editar(fulana, DIA, hoje, "{\"data\": \"2026-09-08\", \"peso\": 82.4, \"substituir\": true}")
        .andExpect(status().isOk());
    assertThat(registros(fulana))
        .singleElement()
        .satisfies(
            registro -> {
              assertThat(registro.data()).isEqualTo(OUTRO_DIA);
              assertThat(registro.peso()).isEqualByComparingTo("82.4");
            });
  }

  // Pega a data futura aceita na edição, e a comparada com o dia do servidor
  // em vez do dia do dispositivo (RN-04, RN-05, UC-03 2c)
  @Test
  void editarParaDepoisDoDiaDoDispositivoERecusado() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, DIA, "82.4", null);

    editar(fulana, DIA, hoje.minusDays(1), "{\"data\": \"%s\", \"peso\": 82.4}".formatted(hoje))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value("DATA_FUTURA"));
    assertThat(registros(fulana))
        .singleElement()
        .satisfies(registro -> assertThat(registro.data()).isEqualTo(DIA));
  }

  // Pega a edição sem conferir o corpo, que o banco recusaria como erro
  // interno (UC-03 2c)
  @Test
  void edicaoComPesoForaDaFaixaERecusada() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, DIA, "82.4", null);

    editar(fulana, DIA, hoje, "{\"data\": \"2026-09-07\", \"peso\": 19.9}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro.codigo").value("PESO_FORA_DE_FAIXA"));
  }

  // Pega o registro procurado sem o usuário, que deixaria editar o de outra
  // pessoa (RN-50)
  @Test
  void oRegistroDeOutraPessoaNaoEEncontrado() throws Exception {
    int outra = usuario("outra@exemplo.com");
    registro(outra, DIA, "70.0", null);
    int fulana = usuario("fulana@exemplo.com");

    editar(fulana, DIA, hoje, "{\"data\": \"2026-09-07\", \"peso\": 82.4}")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_ENCONTRADO"));
    assertThat(registros(outra))
        .singleElement()
        .satisfies(registro -> assertThat(registro.peso()).isEqualByComparingTo("70.0"));
  }

  // Pega a data nova procurada sem o usuário, que daria conflito com o
  // registro de outra pessoa e mostraria o peso dela (RN-50)
  @Test
  void moverParaADataDeOutraPessoaNaoConflita() throws Exception {
    int outra = usuario("outra@exemplo.com");
    registro(outra, OUTRO_DIA, "70.0", null);
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, DIA, "82.4", null);

    editar(fulana, DIA, hoje, "{\"data\": \"2026-09-08\", \"peso\": 82.4}")
        .andExpect(status().isOk());
    assertThat(registros(outra))
        .singleElement()
        .satisfies(registro -> assertThat(registro.peso()).isEqualByComparingTo("70.0"));
  }

  // Pega a edição que não fixa o dia da base: é ela que, movendo o registro,
  // completa os 4 da janela do dia 3 (RN-68)
  @Test
  void aEdicaoTambemFixaODiaDaBase() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    int fase = fase(fulana, LocalDate.of(2026, 9, 1), null);
    registro(fulana, LocalDate.of(2026, 8, 20), "80.0", null);
    registro(fulana, LocalDate.of(2026, 9, 1), "80.0", null);
    registro(fulana, LocalDate.of(2026, 9, 2), "80.0", null);
    registro(fulana, LocalDate.of(2026, 9, 3), "80.0", null);

    editar(fulana, LocalDate.of(2026, 8, 20), hoje, "{\"data\": \"2026-08-31\", \"peso\": 80.0}")
        .andExpect(status().isOk());
    assertThat(diaDaBase(fase)).isEqualTo(LocalDate.of(2026, 9, 3));
  }

  // Pega o registro que não é apagado, e a exclusão que leva outros registros
  // da pessoa junto (RN-08)
  @Test
  void apagaSoORegistroDaData() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, DIA, "82.4", null);
    registro(fulana, OUTRO_DIA, "80.0", null);

    mvc.perform(delete("/registros/{data}", DIA).cookie(acesso(fulana)))
        .andExpect(status().isNoContent());
    assertThat(registros(fulana)).extracting(Gravado::data).containsExactly(OUTRO_DIA);
  }

  // Pega o registro procurado sem o usuário, que apagaria o de outra pessoa
  // (RN-50), e a data sem registro respondida como apagada
  @Test
  void oRegistroDeOutraPessoaNaoEEncontradoNemApagado() throws Exception {
    int outra = usuario("outra@exemplo.com");
    registro(outra, DIA, "70.0", null);
    int fulana = usuario("fulana@exemplo.com");

    mvc.perform(delete("/registros/{data}", DIA).cookie(acesso(fulana)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.erro.codigo").value("NAO_ENCONTRADO"));
    assertThat(registros(outra)).hasSize(1);
  }

  // Pega as pontas do intervalo deixadas de fora, a data fora dele que entra,
  // e a lista fora da ordem de data (RF-14): os registros entram fora de ordem
  @Test
  void listaOIntervaloEmOrdemDeData() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, LocalDate.of(2026, 9, 7), "82.0", null);
    registro(fulana, LocalDate.of(2026, 9, 5), "82.4", null);
    registro(fulana, LocalDate.of(2026, 9, 8), "81.8", null);
    registro(fulana, LocalDate.of(2026, 9, 6), "82.2", null);
    registro(fulana, LocalDate.of(2026, 9, 4), "82.6", null);
    // Pelo índice da data, a ordem viria certa mesmo sem pedir; sem ele, o
    // Postgres devolve na ordem em que gravou, como faria com outro plano
    jdbc.sql("set local enable_indexscan = off").update();
    jdbc.sql("set local enable_bitmapscan = off").update();

    listar(fulana, hoje, "de", "2026-09-05", "ate", "2026-09-07")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].data").value(contains("2026-09-05", "2026-09-06", "2026-09-07")));
  }

  // Pega a lista sem intervalo com 91 dias, e a contada a partir do dia do
  // servidor em vez do dia do dispositivo, que ainda está em ontem (RN-04)
  @Test
  void semIntervaloListaOsUltimos90DiasDoDispositivo() throws Exception {
    LocalDate ontem = hoje.minusDays(1);
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, ontem.minusDays(90), "82.0", null);
    registro(fulana, ontem.minusDays(89), "82.0", null);
    registro(fulana, ontem, "82.0", null);
    registro(fulana, hoje, "82.0", null);

    listar(fulana, ontem)
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[*].data")
                .value(contains(ontem.minusDays(89).toString(), ontem.toString())));
  }

  // Pega o início contado a partir de hoje quando só o fim vem, que deixaria
  // vazia a lista de um intervalo antigo
  @Test
  void soComOFimListaOs90DiasQueTerminamNele() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, LocalDate.of(2026, 4, 1), "82.0", null);
    registro(fulana, LocalDate.of(2026, 4, 2), "82.0", null);
    registro(fulana, LocalDate.of(2026, 6, 30), "82.0", null);
    registro(fulana, LocalDate.of(2026, 7, 1), "82.0", null);

    listar(fulana, hoje, "ate", "2026-06-30")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].data").value(contains("2026-04-02", "2026-06-30")));
  }

  // Pega o início ignorado quando o fim não vem, que cairia nos últimos 90 dias
  @Test
  void soComOInicioListaAteHoje() throws Exception {
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, hoje.minusDays(201), "82.0", null);
    registro(fulana, hoje.minusDays(150), "82.0", null);

    listar(fulana, hoje, "de", hoje.minusDays(200).toString())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].data").value(contains(hoje.minusDays(150).toString())));
  }

  // Pega a lista consultada sem o usuário, que mostraria o peso de outra
  // pessoa (RN-50)
  @Test
  void naoListaORegistroDeOutraPessoa() throws Exception {
    int outra = usuario("outra@exemplo.com");
    registro(outra, DIA, "70.0", null);
    int fulana = usuario("fulana@exemplo.com");
    registro(fulana, OUTRO_DIA, "82.4", null);

    listar(fulana, hoje, "de", "2026-09-07", "ate", "2026-09-08")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].data").value(contains("2026-09-08")));
  }

  record Gravado(LocalDate data, BigDecimal peso, @Nullable BigDecimal cintura) {}

  /** A consulta, com os parâmetros em pares de nome e valor. */
  private ResultActions listar(int idUsuario, LocalDate dataLocal, String... parametros)
      throws Exception {
    MockHttpServletRequestBuilder pedido =
        get("/registros").cookie(acesso(idUsuario)).header("Data-Local", dataLocal.toString());
    for (int i = 0; i < parametros.length; i += 2) {
      pedido.param(parametros[i], parametros[i + 1]);
    }
    return mvc.perform(pedido);
  }

  private ResultActions registrar(int idUsuario, LocalDate dataLocal, String corpo)
      throws Exception {
    return mvc.perform(
        post("/registros")
            .cookie(acesso(idUsuario))
            .header("Data-Local", dataLocal.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
  }

  private ResultActions editar(int idUsuario, LocalDate data, LocalDate dataLocal, String corpo)
      throws Exception {
    return mvc.perform(
        patch("/registros/{data}", data)
            .cookie(acesso(idUsuario))
            .header("Data-Local", dataLocal.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
  }

  /** O que está no banco, depois de o JPA escrever o que guardou na transação do teste. */
  private List<Gravado> registros(int idUsuario) {
    jpa.flush();
    return jdbc.sql("select data, peso, cintura from registro where usuario_id = ? order by data")
        .param(idUsuario)
        .query(Gravado.class)
        .list();
  }

  private @Nullable LocalDate diaDaBase(int idFase) {
    jpa.flush();
    return jdbc.sql("select base_data from fase where id = ?")
        .param(idFase)
        .query(LocalDate.class)
        .list()
        .getFirst();
  }

  private int usuario(String email) {
    return jdbc.sql(
            "insert into usuario (email, nome, criado_em) values (?, 'Fulana', now()) returning id")
        .param(email)
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

  /**
   * Uma manutenção, que não pede plano (RN-33). O dia da base vem já fixado quando a fase é dada
   * com ele.
   */
  private int fase(int idUsuario, LocalDate inicio, @Nullable LocalDate diaDaBase) {
    return jdbc.sql(
            "insert into fase (usuario_id, tipo, data_inicio, base_data, criado_em)"
                + " values (?, 'manutencao', ?, ?, now()) returning id")
        .params(idUsuario, inicio, diaDaBase)
        .query(Integer.class)
        .single();
  }

  private static Cookie acesso(int idUsuario) {
    return new Cookie(
        "acesso", ChaveDeTeste.token(idUsuario, Instant.now().plus(10, ChronoUnit.MINUTES)));
  }
}
