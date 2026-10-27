package mbarete;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import mbarete.entity.Fase;
import mbarete.entity.Usuario;
import mbarete.regras.Nivel;
import mbarete.regras.TipoDeFase;
import mbarete.regras.UnidadeDaTaxa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/** As restrições vivem só na migração, e o validate não as vê (ADR-0036). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.flyway.enabled=true")
@Transactional
class EsquemaTest {

  private final JdbcClient jdbc;
  private final EntityManager entityManager;

  EsquemaTest(@Autowired JdbcClient jdbc, @Autowired EntityManager entityManager) {
    this.jdbc = jdbc;
    this.entityManager = entityManager;
  }

  @Test
  void cadaValorDosEnumsPassaNoCheck() {
    Usuario usuario = new Usuario("a@exemplo.com", "Nome", null, Instant.now());
    entityManager.persist(usuario);
    planejada(usuario, TipoDeFase.CUTTING, 1, Nivel.CONSERVADOR, UnidadeDaTaxa.PERCENTUAL_SEMANA);
    planejada(usuario, TipoDeFase.BULKING, 2, Nivel.MODERADO, UnidadeDaTaxa.KG_SEMANA);
    planejada(usuario, TipoDeFase.CUTTING, 3, Nivel.AGRESSIVO, UnidadeDaTaxa.KG_SEMANA);
    planejada(usuario, TipoDeFase.BULKING, 4, Nivel.PERSONALIZADO, UnidadeDaTaxa.PERCENTUAL_SEMANA);
    entityManager.persist(
        new Fase(
            usuario,
            TipoDeFase.MANUTENCAO,
            LocalDate.of(2026, 5, 1),
            null,
            null,
            null,
            null,
            null,
            Instant.now()));

    assertThatCode(entityManager::flush).doesNotThrowAnyException();
  }

  @Test
  void registroNaoSeRepeteNaMesmaData() {
    int usuario = usuario("a@exemplo.com");
    registro(usuario, "2026-10-01", "80.0", null);

    assertThatThrownBy(() -> registro(usuario, "2026-10-01", "79.0", null))
        .isInstanceOf(DuplicateKeyException.class)
        .hasMessageContaining("registro_usuario_id_data_key");
  }

  @ParameterizedTest
  @ValueSource(strings = {"19.9", "400.1"})
  void pesoForaDaFaixaNaoEntra(String peso) {
    int usuario = usuario("a@exemplo.com");

    assertThatThrownBy(() -> registro(usuario, "2026-10-01", peso, null))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("registro_peso_check");
  }

  @ParameterizedTest
  @ValueSource(strings = {"39.9", "200.1"})
  void cinturaForaDaFaixaNaoEntra(String cintura) {
    int usuario = usuario("a@exemplo.com");

    assertThatThrownBy(() -> registro(usuario, "2026-10-01", "80.0", cintura))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("registro_cintura_check");
  }

  @ParameterizedTest
  @ValueSource(strings = {"19.9", "400.1"})
  void pesoAlvoForaDaFaixaNaoEntra(String pesoAlvo) {
    int usuario = usuario("a@exemplo.com");

    assertThatThrownBy(
            () ->
                fase(
                    usuario,
                    "cutting",
                    "2026-10-01",
                    null,
                    "moderado",
                    "0.50",
                    "percentual_semana",
                    pesoAlvo))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("fase_peso_alvo_check");
  }

  @Test
  void bordasDasFaixasEntram() {
    int usuario = usuario("a@exemplo.com");

    assertThatCode(
            () -> {
              registro(usuario, "2026-10-01", "20.0", "40.0");
              registro(usuario, "2026-10-02", "400.0", "200.0");
              fase(
                  usuario,
                  "cutting",
                  "2026-01-01",
                  "2026-01-31",
                  "moderado",
                  "0.50",
                  "percentual_semana",
                  "20.0");
              fase(
                  usuario,
                  "bulking",
                  "2026-02-01",
                  "2026-02-28",
                  "moderado",
                  "0.30",
                  "kg_semana",
                  "400.0");
            })
        .doesNotThrowAnyException();
  }

  @Test
  void faseDeUmDiaEntra() {
    int usuario = usuario("a@exemplo.com");

    assertThatCode(() -> manutencao(usuario, "2026-10-01", "2026-10-01"))
        .doesNotThrowAnyException();
  }

  @Test
  void fimAntesDoInicioNaoEntra() {
    int usuario = usuario("a@exemplo.com");

    assertThatThrownBy(() -> manutencao(usuario, "2026-10-02", "2026-10-01"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("fase_datas_check");
  }

  @ParameterizedTest
  @CsvSource({
    "manutencao, moderado,     ,                  ,     ",
    "manutencao,         , 0.50,                  ,     ",
    "manutencao,         ,     , percentual_semana,     ",
    "manutencao,         ,     ,                  , 80.0",
    "cutting   ,         , 0.50, percentual_semana, 70.0",
    "cutting   , moderado,     , percentual_semana, 70.0",
    "cutting   , moderado, 0.50,                  , 70.0",
    "cutting   , moderado, 0.50, percentual_semana,     ",
    "bulking   ,         ,     ,                  ,     ",
  })
  void planoIncompletoOuIndevidoNaoEntra(
      String tipo, String nivel, String taxaValor, String taxaUnidade, String pesoAlvo) {
    int usuario = usuario("a@exemplo.com");

    assertThatThrownBy(
            () -> fase(usuario, tipo, "2026-10-01", null, nivel, taxaValor, taxaUnidade, pesoAlvo))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("fase_plano_check");
  }

  @Test
  void faseQueComecaNoUltimoDiaDaOutraColide() {
    int usuario = usuario("a@exemplo.com");
    manutencao(usuario, "2026-10-01", "2026-10-31");

    assertThatThrownBy(() -> manutencao(usuario, "2026-10-31", null))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("fase_sem_sobreposicao");
  }

  @Test
  void faseQueComecaNoDiaSeguinteAoFimNaoColide() {
    int usuario = usuario("a@exemplo.com");
    manutencao(usuario, "2026-10-01", "2026-10-31");

    assertThatCode(() -> manutencao(usuario, "2026-11-01", null)).doesNotThrowAnyException();
  }

  @Test
  void fasesDeUsuariosDiferentesNaoColidem() {
    manutencao(usuario("a@exemplo.com"), "2026-10-01", null);
    int outro = usuario("b@exemplo.com");

    assertThatCode(() -> manutencao(outro, "2026-10-01", null)).doesNotThrowAnyException();
  }

  private void planejada(
      Usuario usuario, TipoDeFase tipo, int mes, Nivel nivel, UnidadeDaTaxa unidade) {
    LocalDate inicio = LocalDate.of(2026, mes, 1);
    entityManager.persist(
        new Fase(
            usuario,
            tipo,
            inicio,
            inicio.plusDays(9),
            nivel,
            new BigDecimal("0.50"),
            unidade,
            new BigDecimal("70.0"),
            Instant.now()));
  }

  private int usuario(String email) {
    return jdbc.sql(
            "insert into usuario (email, nome, criado_em) values (?, 'Nome', now()) returning id")
        .param(email)
        .query(Integer.class)
        .single();
  }

  private void registro(int usuario, String data, String peso, String cintura) {
    jdbc.sql(
            """
            insert into registro (usuario_id, data, peso, cintura, criado_em)
            values (?, ?::date, ?::numeric, ?::numeric, now())
            """)
        .params(usuario, data, peso, cintura)
        .update();
  }

  private void manutencao(int usuario, String inicio, String fim) {
    fase(usuario, "manutencao", inicio, fim, null, null, null, null);
  }

  private void fase(
      int usuario,
      String tipo,
      String inicio,
      String fim,
      String nivel,
      String taxaValor,
      String taxaUnidade,
      String pesoAlvo) {
    jdbc.sql(
            """
            insert into fase (usuario_id, tipo, data_inicio, data_fim, nivel, taxa_alvo_valor,
                taxa_alvo_unidade, peso_alvo, criado_em)
            values (?, ?, ?::date, ?::date, ?, ?::numeric, ?, ?::numeric, now())
            """)
        .params(usuario, tipo, inicio, fim, nivel, taxaValor, taxaUnidade, pesoAlvo)
        .update();
  }
}
