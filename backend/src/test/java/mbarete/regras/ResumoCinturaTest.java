package mbarete.regras;

import static mbarete.regras.ResumoCintura.resumoCintura;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumoCinturaTest {

  private static final LocalDate DIA = LocalDate.of(2026, 10, 1);
  private static final LocalDate HOJE = DIA.plusDays(25);
  private static final PeriodoDeFase DESDE_O_DIA_10 =
      new PeriodoDeFase(1, TipoDeFase.CUTTING, DIA.plusDays(9), null);
  private static final PeriodoDeFase ATE_O_DIA_22 =
      new PeriodoDeFase(1, TipoDeFase.CUTTING, DIA, DIA.plusDays(22));

  // Pega a última da lista tomada pela última data, e a comparação com a
  // primeira medição em vez da anterior
  @Test
  void trazOUltimoValorEAVariacaoDesdeAAnterior() {
    var medicoes = List.of(medicao(0, "90.0"), medicao(20, "88.5"), medicao(10, "89.0"));

    assertThat(resumoCintura(medicoes, List.of(), HOJE))
        .hasValueSatisfying(
            resumo -> {
              assertThat(resumo.valor()).isEqualByComparingTo("88.5");
              assertThat(resumo.data()).isEqualTo(DIA.plusDays(20));
              assertThat(resumo.desdeAAnterior()).isEqualByComparingTo("-0.5");
            });
  }

  // Pega a variação contada desde a primeira medição de todas, que é de antes
  // da fase: dentro dela a cintura foi de 90,0 a 88,0
  @Test
  void comFaseHojeComparaComAPrimeiraMedicaoDaFase() {
    var medicoes =
        List.of(medicao(0, "92.0"), medicao(10, "90.0"), medicao(15, "89.0"), medicao(20, "88.0"));

    assertThat(resumoCintura(medicoes, List.of(DESDE_O_DIA_10), HOJE))
        .hasValueSatisfying(
            resumo -> assertThat(resumo.desdeOInicioDaFase()).isEqualByComparingTo("-2.0"));
  }

  // Pega a fase da data da última medição, que é a de um ciclo já encerrado:
  // hoje não há fase, e data sem fase não mostra nada de fase (RN-66)
  @Test
  void semFaseHojeNaoHaVariacaoDaFase() {
    var medicoes = List.of(medicao(0, "90.0"), medicao(20, "88.0"));

    assertThat(resumoCintura(medicoes, List.of(ATE_O_DIA_22), HOJE))
        .hasValueSatisfying(resumo -> assertThat(resumo.desdeOInicioDaFase()).isNull());
  }

  // Pega a fase de hoje que ainda não tem medição comparada com uma medição de
  // fora dela
  @Test
  void faseDeHojeSemMedicaoNaoTemVariacaoDaFase() {
    var desdeODia23 = new PeriodoDeFase(2, TipoDeFase.CUTTING, DIA.plusDays(23), null);
    var medicoes = List.of(medicao(0, "90.0"), medicao(20, "88.0"));

    assertThat(resumoCintura(medicoes, List.of(ATE_O_DIA_22, desdeODia23), HOJE))
        .hasValueSatisfying(resumo -> assertThat(resumo.desdeOInicioDaFase()).isNull());
  }

  // Pega a variação zero da medição contra ela mesma
  @Test
  void umaMedicaoSoNaoTemVariacao() {
    var medicoes = List.of(medicao(10, "90.0"));

    assertThat(resumoCintura(medicoes, List.of(DESDE_O_DIA_10), HOJE))
        .hasValueSatisfying(
            resumo -> {
              assertThat(resumo.desdeAAnterior()).isNull();
              assertThat(resumo.desdeOInicioDaFase()).isNull();
            });
  }

  /** A medição de {@code dias} dias depois do primeiro dia. */
  private static Medicao medicao(int dias, String cintura) {
    return new Medicao(DIA.plusDays(dias), new BigDecimal(cintura));
  }
}
