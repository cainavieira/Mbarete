package mbarete.regras;

import static mbarete.regras.PesoSemanal.calcularPesoSemanal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class DiaDaBase {

  private DiaDaBase() {}

  /**
   * O dia de início da fase ou, quando ele não tem peso semanal, o primeiro dia da fase que tenha,
   * sem passar de hoje (RN-68). Quem grava o dia o fixa na primeira vez e só o determina de novo
   * quando as datas da fase mudam (RN-24); o valor da base, ao contrário, é o peso semanal daquele
   * dia a cada leitura.
   */
  public static Optional<LocalDate> determinarDiaDaBase(
      List<Pesagem> pesagens, PeriodoDeFase fase, LocalDate hoje) {
    LocalDate fim = fase.fim();
    LocalDate ultimo = fim == null || fim.isAfter(hoje) ? hoje : fim;
    if (fase.inicio().isAfter(ultimo)) {
      return Optional.empty();
    }
    return fase.inicio()
        .datesUntil(ultimo.plusDays(1))
        .filter(dia -> calcularPesoSemanal(pesagens, dia).isPresent())
        .findFirst();
  }
}
