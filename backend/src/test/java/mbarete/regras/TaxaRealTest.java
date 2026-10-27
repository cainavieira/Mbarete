package mbarete.regras;

import static mbarete.regras.Pesagens.diarias;
import static mbarete.regras.TaxaReal.calcularTaxaReal;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class TaxaRealTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);
  private static final LocalDate MES_ATRAS = HOJE.minusDays(28);
  private static final LocalDate BASE_ANTIGA = HOJE.minusDays(60);

  // Pega a comparação com 7 dias atrás, a diferença sem passar para a semana
  // (daria -1,6) e o sinal invertido: 78,4 hoje contra 80,0 (RN-36)
  @Test
  void comparaComOPesoSemanalDe28DiasAtras() {
    var pesagens = new ArrayList<Pesagem>();
    pesagens.addAll(diarias(MES_ATRAS.minusDays(6), MES_ATRAS, "80.0"));
    pesagens.addAll(diarias(HOJE.minusDays(6), HOJE, "78.4"));

    assertThat(calcularTaxaReal(pesagens, BASE_ANTIGA, HOJE))
        .hasValueSatisfying(
            taxa -> {
              assertThat(taxa.valor()).isEqualByComparingTo("-0.40");
              assertThat(taxa.de()).isEqualTo(MES_ATRAS);
              assertThat(taxa.ate()).isEqualTo(HOJE);
            });
  }

  // Pega os 28 dias quando a base é mais recente, e a passagem para a semana
  // como se fossem sempre quatro: 79,4 contra 80,0 em 14 dias dá -0,30 (RN-36)
  @Test
  void comBaseMaisRecenteQue28DiasComparaComABase() {
    var base = HOJE.minusDays(14);
    var pesagens = new ArrayList<Pesagem>();
    pesagens.addAll(diarias(base.minusDays(6), base, "80.0"));
    pesagens.addAll(diarias(HOJE.minusDays(6), HOJE, "79.4"));

    assertThat(calcularTaxaReal(pesagens, base, HOJE))
        .hasValueSatisfying(
            taxa -> {
              assertThat(taxa.valor()).isEqualByComparingTo("-0.30");
              assertThat(taxa.de()).isEqualTo(base);
            });
  }

  // Pega o avanço até o primeiro dia com peso semanal, que é da variação e não
  // da taxa: aqui o dia seguinte aos 28 dias já teria (RN-36, RN-39)
  @Test
  void semPesoSemanalHa28DiasNaoHaTaxa() {
    var pesagens = diarias(MES_ATRAS.minusDays(2), HOJE, "80.0");

    assertThat(calcularTaxaReal(pesagens, BASE_ANTIGA, HOJE)).isEmpty();
  }

  // Pega a divisão por zero no próprio dia da base
  @Test
  void noDiaDaBaseNaoHaTaxa() {
    var pesagens = diarias(HOJE.minusDays(6), HOJE, "80.0");

    assertThat(calcularTaxaReal(pesagens, HOJE, HOJE)).isEmpty();
  }
}
