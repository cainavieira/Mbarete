package mbarete.regras;

import static mbarete.regras.Variacao.calcularVariacao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** As quatro variações da tela, cada uma nula quando não há com o que comparar (RN-46, RN-47). */
public record Variacoes(
    @Nullable Variacao semana,
    @Nullable Variacao duasSemanas,
    @Nullable Variacao quatroSemanas,
    @Nullable Variacao desdeOInicio) {

  /**
   * As partidas são 1, 2 e 4 semanas atrás, e cada uma avança até o primeiro dia com peso semanal,
   * sem passar de 1 semana atrás (RN-46, RN-47). A desde o início parte do dia da base quando há
   * fase corrente, e do primeiro registro quando não há (RN-46, RN-68).
   */
  public static Variacoes calcularVariacoes(
      List<Pesagem> pesagens,
      boolean temFaseCorrente,
      @Nullable LocalDate diaDaBase,
      LocalDate hoje) {
    return new Variacoes(
        semanasAtras(pesagens, 1, hoje),
        semanasAtras(pesagens, 2, hoje),
        semanasAtras(pesagens, 4, hoje),
        (temFaseCorrente
                ? desdeABase(pesagens, diaDaBase, hoje)
                : desdeOPrimeiroRegistro(pesagens, hoje))
            .orElse(null));
  }

  /**
   * O avanço não passa de 1 semana atrás: depois dela, a janela da partida e a de hoje teriam dias
   * em comum (RN-47).
   */
  private static @Nullable Variacao semanasAtras(
      List<Pesagem> pesagens, int semanas, LocalDate hoje) {
    var limite = hoje.minusWeeks(1);
    return calcularVariacao(pesagens, hoje.minusWeeks(semanas), hoje)
        .filter(variacao -> !variacao.de().isAfter(limite))
        .orElse(null);
  }

  /** A base não avança: sem peso semanal nela, não há variação (RN-46, RN-68). */
  private static Optional<Variacao> desdeABase(
      List<Pesagem> pesagens, @Nullable LocalDate diaDaBase, LocalDate hoje) {
    if (diaDaBase == null) {
      return Optional.empty();
    }
    return calcularVariacao(pesagens, diaDaBase, hoje)
        .filter(variacao -> variacao.de().equals(diaDaBase));
  }

  private static Optional<Variacao> desdeOPrimeiroRegistro(List<Pesagem> pesagens, LocalDate hoje) {
    return pesagens.stream()
        .map(Pesagem::data)
        .min(LocalDate::compareTo)
        .flatMap(primeiro -> calcularVariacao(pesagens, primeiro, hoje));
  }
}
