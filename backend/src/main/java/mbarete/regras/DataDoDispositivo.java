package mbarete.regras;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class DataDoDispositivo {

  private static final String MENSAGEM =
      "A data do dispositivo não chegou ou está errada. Confira a data e a hora dele.";

  private DataDoDispositivo() {}

  /**
   * O dia corrente, que é o do dispositivo, lido do cabeçalho `Data-Local` (RN-04). Recusa o
   * cabeçalho ausente, malformado ou a mais de um dia da data do servidor em UTC: os fusos vão de
   * 12 horas antes a 14 depois do UTC, e além disso é relógio errado, que deixaria registrar no
   * futuro (RN-05, endpoints.md).
   */
  public static LocalDate lerDataLocal(@Nullable String cabecalho, LocalDate hojeEmUtc) {
    return Optional.ofNullable(cabecalho)
        .flatMap(DataDoDispositivo::converter)
        .filter(data -> Math.abs(ChronoUnit.DAYS.between(hojeEmUtc, data)) <= 1)
        .orElseThrow(() -> new RegraVioladaException(Recusa.DATA_LOCAL_INVALIDA, MENSAGEM));
  }

  private static Optional<LocalDate> converter(String texto) {
    try {
      return Optional.of(LocalDate.parse(texto));
    } catch (DateTimeParseException malformada) {
      return Optional.empty();
    }
  }
}
