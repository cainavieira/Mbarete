package mbarete.regras;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/** As pesagens que os testes das regras montam. */
final class Pesagens {

  private Pesagens() {}

  /** Uma pesagem por dia, do primeiro ao último, todas com o mesmo peso. */
  static List<Pesagem> diarias(LocalDate primeiro, LocalDate ultimo, String peso) {
    return primeiro
        .datesUntil(ultimo.plusDays(1))
        .map(d -> new Pesagem(d, new BigDecimal(peso)))
        .toList();
  }

  /** Uma pesagem por dia, do dia para trás, com os pesos na ordem dada. */
  static List<Pesagem> ate(LocalDate dia, String... pesos) {
    return IntStream.range(0, pesos.length)
        .mapToObj(i -> new Pesagem(dia.minusDays(i), new BigDecimal(pesos[i])))
        .toList();
  }
}
