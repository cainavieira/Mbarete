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
   * As partidas são 1, 2 e 4 semanas atrás, e cada uma avança até o primeiro dia com peso semanal
   * (RN-46, RN-47). A desde o início parte do dia da base quando há fase corrente, e do primeiro
   * registro quando não há (RN-46, RN-68).
   */
  public static Variacoes calcularVariacoes(
      List<Pesagem> pesagens,
      boolean temFaseCorrente,
      @Nullable LocalDate diaDaBase,
      LocalDate hoje) {
    return new Variacoes(
        calcularVariacao(pesagens, hoje.minusWeeks(1), hoje).orElse(null),
        calcularVariacao(pesagens, hoje.minusWeeks(2), hoje).orElse(null),
        calcularVariacao(pesagens, hoje.minusWeeks(4), hoje).orElse(null),
        (temFaseCorrente
                ? desdeABase(pesagens, diaDaBase, hoje)
                : desdeOPrimeiroRegistro(pesagens, hoje))
            .orElse(null));
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
