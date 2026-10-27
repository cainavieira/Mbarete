package mbarete.regras;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
}
