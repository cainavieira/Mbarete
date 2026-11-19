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
   * Todas terminam no dia final, que é hoje ou, sem peso semanal hoje, o último dia que tem (RN-47,
   * RN-71). As partidas são 1, 2 e 4 semanas antes dele, e cada uma avança até o primeiro dia com
   * peso semanal, sem passar de 1 semana antes dele (RN-46, RN-47). A desde o início parte do dia
   * da base quando há fase corrente, e do primeiro registro quando não há (RN-46, RN-68).
   */
  public static Variacoes calcularVariacoes(
      List<Pesagem> pesagens,
      boolean temFaseCorrente,
      @Nullable LocalDate diaDaBase,
      LocalDate diaFinal) {
    return new Variacoes(
        semanasAtras(pesagens, 1, diaFinal),
        semanasAtras(pesagens, 2, diaFinal),
        semanasAtras(pesagens, 4, diaFinal),
        (temFaseCorrente
                ? desdeABase(pesagens, diaDaBase, diaFinal)
                : desdeOPrimeiroRegistro(pesagens, diaFinal))
            .orElse(null));
  }

  /**
   * O avanço não passa de 1 semana antes do dia final: depois dela, a janela da partida e a do dia
   * final teriam dias em comum (RN-47).
   */
  private static @Nullable Variacao semanasAtras(
      List<Pesagem> pesagens, int semanas, LocalDate diaFinal) {
    var limite = diaFinal.minusWeeks(1);
    return calcularVariacao(pesagens, diaFinal.minusWeeks(semanas), diaFinal)
        .filter(variacao -> !variacao.de().isAfter(limite))
        .orElse(null);
  }

  /** A base não avança: sem peso semanal nela, não há variação (RN-46, RN-68). */
  private static Optional<Variacao> desdeABase(
      List<Pesagem> pesagens, @Nullable LocalDate diaDaBase, LocalDate diaFinal) {
    if (diaDaBase == null) {
      return Optional.empty();
    }
    return calcularVariacao(pesagens, diaDaBase, diaFinal)
        .filter(variacao -> variacao.de().equals(diaDaBase));
  }

  private static Optional<Variacao> desdeOPrimeiroRegistro(
      List<Pesagem> pesagens, LocalDate diaFinal) {
    return pesagens.stream()
        .map(Pesagem::data)
        .min(LocalDate::compareTo)
        .flatMap(primeiro -> calcularVariacao(pesagens, primeiro, diaFinal));
  }
}
