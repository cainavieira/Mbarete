package mbarete.regras;

import static mbarete.regras.Pesagens.diarias;
import static mbarete.regras.UltimoPesoSemanal.calcularUltimoPesoSemanal;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UltimoPesoSemanalTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);

  // Pega o dia do último registro no lugar do último peso semanal, e a busca
  // que anda para frente e acha o primeiro: com registros de 20 a 10 dias
  // atrás, a janela ainda tem 4 deles 7 dias atrás (RN-15, RN-16, RN-71)
  @Test
  void semPesoSemanalHojeEOUltimoDiaQueTem() {
    var pesagens = diarias(HOJE.minusDays(20), HOJE.minusDays(10), "80.0");

    assertThat(calcularUltimoPesoSemanal(pesagens, HOJE))
        .hasValueSatisfying(ultimo -> assertThat(ultimo.data()).isEqualTo(HOJE.minusDays(7)));
  }

  // Pega o último registro tomado como peso semanal sem a janela ter 4 (RN-16)
  @Test
  void semNenhumPesoSemanalNaoHaUltimo() {
    var pesagens = diarias(HOJE.minusDays(30), HOJE.minusDays(28), "80.0");

    assertThat(calcularUltimoPesoSemanal(pesagens, HOJE)).isEmpty();
  }
}
