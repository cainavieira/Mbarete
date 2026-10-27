package mbarete.regras;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class PesoSemanal {

  /** Com menos registros que isso na janela, não existe peso semanal (RN-16). */
  private static final int MINIMO_NA_JANELA = 4;

  private PesoSemanal() {}

  /**
   * A média dos registros nos 7 dias de calendário que terminam no dia, ele incluído (RN-15). A
   * conta não arredonda: só o resultado exibido é arredondado (RN-37).
   */
  public static Optional<BigDecimal> calcularPesoSemanal(List<Pesagem> pesagens, LocalDate dia) {
    LocalDate inicio = dia.minusDays(6);
    List<BigDecimal> naJanela =
        pesagens.stream()
            .filter(p -> !p.data().isBefore(inicio) && !p.data().isAfter(dia))
            .map(Pesagem::peso)
            .toList();
    if (naJanela.size() < MINIMO_NA_JANELA) {
      return Optional.empty();
    }
    BigDecimal soma = naJanela.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    return Optional.of(soma.divide(BigDecimal.valueOf(naJanela.size()), MathContext.DECIMAL128));
  }
}
