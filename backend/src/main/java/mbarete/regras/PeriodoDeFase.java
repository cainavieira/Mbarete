package mbarete.regras;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** O intervalo de uma fase; o fim nulo é a fase em aberto (RN-18). */
public record PeriodoDeFase(int id, LocalDate inicio, @Nullable LocalDate fim) {

  /**
   * A fase que cobre a data, que é de onde o registro tira a fase (RN-23). Data sem fase é estado
   * válido, e vem vazia (RN-20); as fases não se sobrepõem, então no máximo uma cobre (RN-19).
   */
  public static Optional<PeriodoDeFase> faseDaData(List<PeriodoDeFase> fases, LocalDate data) {
    return fases.stream().filter(fase -> fase.cobre(data)).findFirst();
  }

  /** Os registros da fase, que perdem o rótulo do período se ela for apagada (RN-23, RN-70). */
  public static long contarRegistros(List<Pesagem> pesagens, PeriodoDeFase fase) {
    return pesagens.stream().filter(pesagem -> fase.cobre(pesagem.data())).count();
  }

  boolean cobre(LocalDate data) {
    LocalDate ultimoDia = fim;
    return !data.isBefore(inicio) && (ultimoDia == null || !data.isAfter(ultimoDia));
  }
}
