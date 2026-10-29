package mbarete.regras;

import static mbarete.regras.DiaDaBase.determinarDiaDaBase;
import static mbarete.regras.Pesagens.diarias;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DiaDaBaseTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);
  private static final LocalDate INICIO = HOJE.minusDays(10);

  @Test
  void inicioComPesoSemanalEODiaDaBase() {
    var pesagens = diarias(INICIO.minusDays(6), HOJE, "80.0");

    assertThat(
            determinarDiaDaBase(
                pesagens, new PeriodoDeFase(1, TipoDeFase.CUTTING, INICIO, null), HOJE))
        .contains(INICIO);
  }

  // Pega o início devolvido mesmo sem peso semanal, e a fase sem base quando o
  // início não tem: com registros desde a véspera, o primeiro é dois dias depois
  @Test
  void semPesoSemanalNoInicioEOPrimeiroDiaDaFaseQueTem() {
    var pesagens = diarias(INICIO.minusDays(1), HOJE, "80.0");

    assertThat(
            determinarDiaDaBase(
                pesagens, new PeriodoDeFase(1, TipoDeFase.CUTTING, INICIO, null), HOJE))
        .contains(INICIO.plusDays(2));
  }

  // Pega a busca que passa de hoje, porque a janela de depois de amanhã ainda
  // teria cinco registros, e o intervalo de busca invertido, que quebra
  @Test
  void faseQueComecaDepoisDeHojeNaoTemBase() {
    var pesagens = diarias(HOJE.minusDays(6), HOJE, "80.0");

    assertThat(
            determinarDiaDaBase(
                pesagens, new PeriodoDeFase(1, TipoDeFase.CUTTING, HOJE.plusDays(2), null), HOJE))
        .isEmpty();
  }

  // Pega a busca que passa do fim da fase: o primeiro peso semanal é o do dia
  // seguinte ao fim
  @Test
  void faseEncerradaSoProcuraAteOFim() {
    var fim = INICIO.plusDays(3);
    var pesagens = diarias(fim.minusDays(2), HOJE, "80.0");

    assertThat(
            determinarDiaDaBase(
                pesagens, new PeriodoDeFase(1, TipoDeFase.CUTTING, INICIO, fim), HOJE))
        .isEmpty();
  }
}
