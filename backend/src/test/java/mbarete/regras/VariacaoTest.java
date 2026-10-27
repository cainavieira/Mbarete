package mbarete.regras;

import static mbarete.regras.Variacao.calcularVariacao;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VariacaoTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);
  private static final LocalDate PARTIDA = HOJE.minusDays(7);

  // Pega a variação sobre o peso bruto (daria -2,4) e o sinal invertido: os
  // pesos semanais são 80,1 na partida e 78,9 hoje (RN-46)
  @Test
  void comparaOPesoSemanalDeHojeComODaPartida() {
    var pesagens = new ArrayList<Pesagem>();
    pesagens.addAll(diarias(PARTIDA.minusDays(6), PARTIDA.minusDays(1), "80.0"));
    pesagens.add(new Pesagem(PARTIDA, new BigDecimal("80.7")));
    pesagens.addAll(diarias(PARTIDA.plusDays(1), HOJE.minusDays(1), "79.0"));
    pesagens.add(new Pesagem(HOJE, new BigDecimal("78.3")));

    assertThat(calcularVariacao(pesagens, PARTIDA, HOJE))
        .hasValueSatisfying(
            variacao -> {
              assertThat(variacao.valor()).isEqualByComparingTo("-1.2");
              assertThat(variacao.de()).isEqualTo(PARTIDA);
              assertThat(variacao.ate()).isEqualTo(HOJE);
            });
  }

  // Pega o recuo e a variação vazia no lugar do avanço: o primeiro peso
  // semanal depois da partida é o de dois dias depois (RN-47)
  @Test
  void semPesoSemanalNaPartidaAvancaParaOPrimeiroDiaQueTem() {
    var pesagens = diarias(PARTIDA.minusDays(1), HOJE, "80.0");

    assertThat(calcularVariacao(pesagens, PARTIDA, HOJE))
        .hasValueSatisfying(variacao -> assertThat(variacao.de()).isEqualTo(PARTIDA.plusDays(2)));
  }

  @Test
  void semPesoSemanalHojeNaoHaVariacao() {
    var pesagens = diarias(PARTIDA.minusDays(6), HOJE.minusDays(5), "80.0");

    assertThat(calcularVariacao(pesagens, PARTIDA, HOJE)).isEmpty();
  }

  // Pega a variação zero de hoje contra hoje
  @Test
  void quandoSoHojeTemPesoSemanalNaoHaVariacao() {
    var pesagens = diarias(HOJE.minusDays(3), HOJE, "80.0");

    assertThat(calcularVariacao(pesagens, PARTIDA, HOJE)).isEmpty();
  }

  /** Uma pesagem por dia, do primeiro ao último, todas com o mesmo peso. */
  private static List<Pesagem> diarias(LocalDate primeiro, LocalDate ultimo, String peso) {
    return primeiro
        .datesUntil(ultimo.plusDays(1))
        .map(d -> new Pesagem(d, new BigDecimal(peso)))
        .toList();
  }
}
