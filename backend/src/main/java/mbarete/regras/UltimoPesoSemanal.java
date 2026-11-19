package mbarete.regras;

import static mbarete.regras.PesoSemanal.calcularPesoSemanal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * O peso semanal mais recente até hoje, com o dia dele, que a tela mostra quando hoje não tem
 * (RN-71).
 */
public record UltimoPesoSemanal(BigDecimal valor, LocalDate data) {

  /**
   * Procura de hoje para trás, até o primeiro registro. O dia pode ser até 3 dias depois do último
   * registro, porque a janela ainda tem 4 registros nesses dias (RN-15, RN-16).
   */
  public static Optional<UltimoPesoSemanal> calcularUltimoPesoSemanal(
      List<Pesagem> pesagens, LocalDate hoje) {
    return pesagens.stream()
        .map(Pesagem::data)
        .min(LocalDate::compareTo)
        .flatMap(
            primeiro ->
                Stream.iterate(hoje, dia -> !dia.isBefore(primeiro), dia -> dia.minusDays(1))
                    .flatMap(
                        dia ->
                            calcularPesoSemanal(pesagens, dia)
                                .map(valor -> new UltimoPesoSemanal(valor, dia))
                                .stream())
                    .findFirst());
  }
}
