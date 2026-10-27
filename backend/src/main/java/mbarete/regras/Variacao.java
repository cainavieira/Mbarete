package mbarete.regras;

import static mbarete.regras.PesoSemanal.calcularPesoSemanal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** A variação do peso semanal entre duas datas, que vão junto com ela na tela (RN-47). */
public record Variacao(BigDecimal valor, LocalDate de, LocalDate ate) {

  /**
   * Compara o peso semanal de hoje com o do primeiro dia, a partir da partida, que tenha peso
   * semanal: quando a partida não tem, a variação avança, e não recua (RN-46, RN-47). As três
   * variações são esta com partidas diferentes: 7 dias atrás, 30 dias atrás e o primeiro registro.
   */
  public static Optional<Variacao> calcularVariacao(
      List<Pesagem> pesagens, LocalDate partida, LocalDate hoje) {
    return calcularPesoSemanal(pesagens, hoje)
        .flatMap(
            deHoje ->
                partida
                    .datesUntil(hoje)
                    .flatMap(
                        dia ->
                            calcularPesoSemanal(pesagens, dia)
                                .map(doDia -> new Variacao(deHoje.subtract(doDia), dia, hoje))
                                .stream())
                    .findFirst());
  }
}
