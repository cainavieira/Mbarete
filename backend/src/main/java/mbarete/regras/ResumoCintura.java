package mbarete.regras;

import static java.util.Comparator.comparing;
import static mbarete.regras.PeriodoDeFase.faseDaData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * O último valor da cintura e a data dele, a variação desde a medição anterior e a variação desde a
 * primeira medição da fase de hoje (RN-12). A cintura não tem janela nem valor semanal.
 */
public record ResumoCintura(
    BigDecimal valor,
    LocalDate data,
    @Nullable BigDecimal desdeAAnterior,
    @Nullable BigDecimal desdeOInicioDaFase) {

  /**
   * Vazio quando nunca houve medição. Cada variação fica nula quando não tem com o que comparar:
   * sem medição anterior, sem fase hoje, sem medição dentro dela ou com só uma. A fase é a de hoje,
   * e não a da última medição, porque data sem fase não mostra nada de fase (RN-66).
   */
  public static Optional<ResumoCintura> resumoCintura(
      List<Medicao> medicoes, List<PeriodoDeFase> fases, LocalDate hoje) {
    List<Medicao> emOrdem = medicoes.stream().sorted(comparing(Medicao::data)).toList();
    if (emOrdem.isEmpty()) {
      return Optional.empty();
    }
    Medicao ultima = emOrdem.getLast();
    BigDecimal desdeAAnterior =
        emOrdem.size() < 2
            ? null
            : ultima.cintura().subtract(emOrdem.get(emOrdem.size() - 2).cintura());
    BigDecimal desdeOInicioDaFase =
        faseDaData(fases, hoje)
            .flatMap(fase -> emOrdem.stream().filter(m -> fase.cobre(m.data())).findFirst())
            .filter(primeira -> primeira.data().isBefore(ultima.data()))
            .map(primeira -> ultima.cintura().subtract(primeira.cintura()))
            .orElse(null);
    return Optional.of(
        new ResumoCintura(ultima.cintura(), ultima.data(), desdeAAnterior, desdeOInicioDaFase));
  }
}
