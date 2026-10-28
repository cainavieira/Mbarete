package mbarete.regras;

import static mbarete.regras.PeriodoDeFase.faseDaData;
import static mbarete.regras.PesoSemanal.calcularPesoSemanal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Um dia do gráfico: o peso do dia, o peso semanal e a fase que cobre o dia, cada um nulo onde não
 * há (RN-09, RN-16, RF-47).
 */
public record PontoDaSerie(
    LocalDate data,
    @Nullable BigDecimal peso,
    @Nullable BigDecimal pesoSemanal,
    @Nullable Integer faseId) {

  private static final int DIAS_DO_GRAFICO = 120;

  /**
   * Um ponto por dia, inclusive o sem dado, porque a linha só se interrompe em ponto nulo: dia
   * omitido ligaria os vizinhos (RN-09). A série cobre os últimos 120 dias, hoje incluído, ou
   * começa no primeiro registro, quando ele é mais recente (RF-49). Sem registro, não há série. O
   * registro depois de hoje, que outro aparelho num fuso adiante pode ter gravado, não conta como o
   * primeiro.
   */
  public static List<PontoDaSerie> montarSerie(
      List<Pesagem> pesagens, List<PeriodoDeFase> fases, LocalDate hoje) {
    LocalDate inicioDoGrafico = hoje.minusDays(DIAS_DO_GRAFICO - 1);
    return pesagens.stream()
        .map(Pesagem::data)
        .filter(data -> !data.isAfter(hoje))
        .min(LocalDate::compareTo)
        .map(primeiro -> primeiro.isAfter(inicioDoGrafico) ? primeiro : inicioDoGrafico)
        .map(
            inicio ->
                inicio
                    .datesUntil(hoje.plusDays(1))
                    .map(dia -> ponto(pesagens, fases, dia))
                    .toList())
        .orElse(List.of());
  }

  private static PontoDaSerie ponto(
      List<Pesagem> pesagens, List<PeriodoDeFase> fases, LocalDate dia) {
    return new PontoDaSerie(
        dia,
        pesagens.stream()
            .filter(pesagem -> pesagem.data().equals(dia))
            .map(Pesagem::peso)
            .findFirst()
            .orElse(null),
        calcularPesoSemanal(pesagens, dia).orElse(null),
        faseDaData(fases, dia).map(PeriodoDeFase::id).orElse(null));
  }
}
