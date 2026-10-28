package mbarete.regras;

import static mbarete.regras.Pesagens.diarias;
import static mbarete.regras.PontoDaSerie.montarSerie;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PontoDaSerieTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);

  // Pega a série de 119 ou de 121 dias, e a que termina antes de hoje (RF-49)
  @Test
  void cobreOsUltimos120DiasComHoje() {
    var serie = montarSerie(diarias(HOJE.minusDays(200), HOJE, "80.0"), List.of(), HOJE);

    assertThat(serie).hasSize(120);
    assertThat(serie.getFirst().data()).isEqualTo(HOJE.minusDays(119));
    assertThat(serie.getLast().data()).isEqualTo(HOJE);
  }

  // Pega o trecho em branco antes do primeiro registro (RF-49), o dia sem
  // registro omitido ou com o peso de outro dia (RN-09), o peso semanal
  // faltando e a fase de outro dia (RF-47)
  @Test
  void comecaNoPrimeiroRegistroComUmPontoPorDia() {
    var pesagens = new ArrayList<Pesagem>(diarias(HOJE.minusDays(10), HOJE.minusDays(7), "80.0"));
    pesagens.add(new Pesagem(HOJE.minusDays(3), new BigDecimal("79.0")));
    var fase = new PeriodoDeFase(7, HOJE.minusDays(5), null);

    var serie = montarSerie(pesagens, List.of(fase), HOJE);

    assertThat(serie).hasSize(11);
    assertThat(serie.get(0))
        .isEqualTo(new PontoDaSerie(HOJE.minusDays(10), new BigDecimal("80.0"), null, null));
    assertThat(serie.get(3).pesoSemanal()).isEqualByComparingTo("80.0");
    assertThat(serie.get(4))
        .satisfies(
            ponto -> {
              assertThat(ponto.data()).isEqualTo(HOJE.minusDays(6));
              assertThat(ponto.peso()).isNull();
              assertThat(ponto.faseId()).isNull();
            });
    assertThat(serie.get(5).faseId()).isEqualTo(7);
    assertThat(serie.get(7))
        .satisfies(
            ponto -> {
              assertThat(ponto.peso()).isEqualByComparingTo("79.0");
              assertThat(ponto.pesoSemanal()).isEqualByComparingTo("79.75");
            });
  }

  @Test
  void semRegistroNaoHaSerie() {
    assertThat(montarSerie(List.of(), List.of(), HOJE)).isEmpty();
  }

  // Pega o registro gravado por um aparelho num fuso adiante tomado como o
  // primeiro, que faria a série começar depois de hoje e estourar
  @Test
  void oRegistroDepoisDeHojeNaoComecaASerie() {
    var pesagens = List.of(new Pesagem(HOJE.plusDays(2), new BigDecimal("80.0")));

    assertThat(montarSerie(pesagens, List.of(), HOJE)).isEmpty();
  }
}
