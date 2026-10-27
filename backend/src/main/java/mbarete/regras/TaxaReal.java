package mbarete.regras;

import static mbarete.regras.PesoSemanal.calcularPesoSemanal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/** A variação do peso semanal em quilogramas por semana, com as datas comparadas (RN-36). */
public record TaxaReal(BigDecimal valor, LocalDate de, LocalDate ate) {

  /** No mês, a água de uma semana compensa a da outra (RN-36). */
  private static final int DIAS_DA_MEDIDA = 28;

  private static final BigDecimal DIAS_DA_SEMANA = BigDecimal.valueOf(7);

  /**
   * Compara o peso semanal de hoje com o de 28 dias atrás ou, enquanto o dia da base for mais
   * recente que isso, com o da base (RN-36, RN-68). Sem um dos dois pesos semanais não há taxa: o
   * dia comparado é fixo e, ao contrário da variação, não avança (RN-39).
   */
  public static Optional<TaxaReal> calcularTaxaReal(
      List<Pesagem> pesagens, LocalDate diaDaBase, LocalDate hoje) {
    LocalDate mesAtras = hoje.minusDays(DIAS_DA_MEDIDA);
    LocalDate de = diaDaBase.isAfter(mesAtras) ? diaDaBase : mesAtras;
    if (!de.isBefore(hoje)) {
      return Optional.empty();
    }
    BigDecimal dias = BigDecimal.valueOf(ChronoUnit.DAYS.between(de, hoje));
    return calcularPesoSemanal(pesagens, hoje)
        .flatMap(
            deHoje ->
                calcularPesoSemanal(pesagens, de)
                    .map(
                        doInicio ->
                            new TaxaReal(
                                deHoje
                                    .subtract(doInicio)
                                    .multiply(DIAS_DA_SEMANA)
                                    .divide(dias, MathContext.DECIMAL128),
                                de,
                                hoje)));
  }
}
