package mbarete.regras;

import static mbarete.regras.SituacaoDaFase.situacaoDaFase;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SituacaoDaFaseTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);

  // Pega o fim de hoje contado como encerrada, a que começa hoje contada como
  // futura, a agendada contada como corrente e a corrente com fim agendado
  // contada como encerrada (RN-27, RN-40, RN-69)
  @ParameterizedTest
  @CsvSource({
    "2026-09-01, 2026-10-19, ENCERRADA",
    "2026-09-01, 2026-10-20, CORRENTE",
    "2026-10-20,           , CORRENTE",
    "2026-09-01, 2026-10-25, CORRENTE",
    "2026-10-21,           , FUTURA",
  })
  void aSituacaoDependeDeHoje(LocalDate inicio, @Nullable LocalDate fim, SituacaoDaFase situacao) {
    assertThat(situacaoDaFase(new PeriodoDeFase(1, TipoDeFase.CUTTING, inicio, fim), HOJE))
        .isEqualTo(situacao);
  }
}
