package mbarete.regras;

import static mbarete.regras.PesoSemanal.calcularPesoSemanal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PesoSemanalTest {

  private static final LocalDate DIA = LocalDate.of(2026, 10, 20);

  // Pega o mínimo da janela com um a mais ou a menos (RN-16)
  @Test
  void comTresRegistrosNaJanelaNaoHaPesoSemanal() {
    var pesagens = List.of(pesagem(0, "80.0"), pesagem(1, "80.0"), pesagem(2, "80.0"));

    assertThat(calcularPesoSemanal(pesagens, DIA)).isEmpty();
  }

  @Test
  void comQuatroRegistrosDevolveAMedia() {
    var pesagens =
        List.of(pesagem(0, "80.0"), pesagem(1, "81.0"), pesagem(2, "82.0"), pesagem(3, "83.0"));

    assertThat(calcularPesoSemanal(pesagens, DIA))
        .hasValueSatisfying(media -> assertThat(media).isEqualByComparingTo("81.5"));
  }

  // Pega a janela de 6 ou de 8 dias, a aberta à direita e a dos últimos 7
  // registros (RN-15): só os quatro do meio contam
  @Test
  void soEntramOsSeteDiasAteODia() {
    var pesagens =
        List.of(
            pesagem(-1, "99.0"),
            pesagem(0, "80.0"),
            pesagem(2, "80.0"),
            pesagem(4, "80.0"),
            pesagem(6, "84.0"),
            pesagem(7, "99.0"));

    assertThat(calcularPesoSemanal(pesagens, DIA))
        .hasValueSatisfying(media -> assertThat(media).isEqualByComparingTo("81.0"));
  }

  // Pega a divisão que não termina (ArithmeticException) e o arredondamento no
  // meio da conta (RN-37): 560,1 / 7 não vira 80,0
  @Test
  void mediaDeSeteNaoQuebraNemArredonda() {
    var pesagens =
        List.of(
            pesagem(0, "80.0"),
            pesagem(1, "80.0"),
            pesagem(2, "80.0"),
            pesagem(3, "80.0"),
            pesagem(4, "80.0"),
            pesagem(5, "80.0"),
            pesagem(6, "80.1"));

    assertThat(calcularPesoSemanal(pesagens, DIA))
        .hasValueSatisfying(
            media ->
                assertThat(media)
                    .isCloseTo(new BigDecimal("80.0142857"), within(new BigDecimal("0.0000001"))));
  }

  /** A pesagem de {@code diasAntes} dias antes do dia calculado. */
  private static Pesagem pesagem(int diasAntes, String peso) {
    return new Pesagem(DIA.minusDays(diasAntes), new BigDecimal(peso));
  }
}
