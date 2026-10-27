package mbarete.regras;

import static mbarete.regras.PeriodoDeFase.faseDaData;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeriodoDeFaseTest {

  private static final PeriodoDeFase JANEIRO =
      new PeriodoDeFase(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
  private static final PeriodoDeFase DESDE_MARCO =
      new PeriodoDeFase(2, LocalDate.of(2026, 3, 1), null);

  // Pega a fase em aberto cobrindo também o passado, e a anterior esticada até
  // a seguinte: o vão entre elas não tem fase (RN-20)
  @Test
  void dataNoVaoEntreFasesNaoTemFase() {
    assertThat(faseDaData(List.of(JANEIRO, DESDE_MARCO), LocalDate.of(2026, 2, 15))).isEmpty();
  }

  // Pega a fronteira trocada: o dia do início é da fase nova, e a véspera é da
  // anterior (RN-22)
  @Test
  void oDiaDoInicioEDaFaseNovaEAVesperaDaAnterior() {
    var fevereiro = new PeriodoDeFase(3, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
    var fases = List.of(JANEIRO, fevereiro);

    assertThat(faseDaData(fases, LocalDate.of(2026, 2, 1))).contains(fevereiro);
    assertThat(faseDaData(fases, LocalDate.of(2026, 1, 31))).contains(JANEIRO);
  }

  // Pega o fim nulo tratado como fase que não cobre nada
  @Test
  void faseEmAbertoCobreDaliEmDiante() {
    assertThat(faseDaData(List.of(JANEIRO, DESDE_MARCO), LocalDate.of(2027, 6, 1)))
        .contains(DESDE_MARCO);
  }
}
