package mbarete.regras;

import static mbarete.regras.PesoSemanal.calcularPesoSemanal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** A variação do peso semanal entre duas datas, que vão junto com ela na tela (RN-47). */
public record Variacao(BigDecimal valor, LocalDate de, LocalDate ate) {

  /**
   * Compara o peso semanal do dia final com o do primeiro dia, a partir da partida, que tenha peso
   * semanal: quando a partida não tem, a variação avança, e não recua (RN-46, RN-47). As variações
   * de 1, 2 e 4 semanas e a desde o primeiro registro são esta com partidas diferentes.
   */
  public static Optional<Variacao> calcularVariacao(
      List<Pesagem> pesagens, LocalDate partida, LocalDate diaFinal) {
    return calcularPesoSemanal(pesagens, diaFinal)
        .flatMap(
            doDiaFinal ->
                partida
                    .datesUntil(diaFinal)
                    .flatMap(
                        dia ->
                            calcularPesoSemanal(pesagens, dia)
                                .map(
                                    doDia ->
                                        new Variacao(doDiaFinal.subtract(doDia), dia, diaFinal))
                                .stream())
                    .findFirst());
  }
}
