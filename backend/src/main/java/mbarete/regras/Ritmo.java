package mbarete.regras;

import static mbarete.regras.Arredondamento.arredondarTaxa;
import static mbarete.regras.FaixaDeTolerancia.calcularFaixa;
import static mbarete.regras.PesoSemanal.calcularPesoSemanal;
import static mbarete.regras.TaxaAlvo.converterTaxaAlvo;
import static mbarete.regras.TaxaReal.calcularTaxaReal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * O veredito de ritmo: abaixo, no ritmo ou acima. Na manutenção, os mesmos três se exibem como
 * abaixo da faixa, na faixa e acima da faixa (RN-37).
 */
public enum Ritmo {
  ABAIXO,
  NO_RITMO,
  ACIMA;

  /** Antes disso, a taxa real ainda é mais água do que ritmo (RN-39). */
  private static final int DIAS_MINIMOS = 14;

  /** A tolerância de 50% para mais ou para menos da taxa alvo (RN-38). */
  private static final BigDecimal MEIA_TAXA = new BigDecimal("0.5");

  private static final BigDecimal TAXA_E_MEIA = new BigDecimal("1.5");

  /**
   * O veredito da fase corrente, vazio quando os dados são insuficientes (RN-39). Em cutting e
   * bulking, compara a taxa real com a taxa alvo a partir de 14 dias completos desde o dia da base
   * (RN-36, RN-38). Na manutenção, compara a posição do peso semanal de hoje com a faixa, já no dia
   * da base (RN-33).
   */
  public static Optional<Ritmo> avaliarRitmo(
      TipoDeFase tipo,
      @Nullable TaxaAlvo taxa,
      List<Pesagem> pesagens,
      LocalDate diaDaBase,
      LocalDate hoje) {
    if (tipo == TipoDeFase.MANUTENCAO) {
      return calcularFaixa(pesagens, diaDaBase)
          .flatMap(faixa -> calcularPesoSemanal(pesagens, hoje).map(faixa::posicao));
    }
    if (ChronoUnit.DAYS.between(diaDaBase, hoje) < DIAS_MINIMOS) {
      return Optional.empty();
    }
    return calcularTaxaReal(pesagens, diaDaBase, hoje)
        .flatMap(
            real ->
                converterTaxaAlvo(tipo, taxa, pesagens, hoje)
                    .map(alvo -> comparar(tipo, real.valor(), alvo)));
  }

  /**
   * O dia em que o veredito de cutting ou bulking aparece, quando os dados insuficientes são só a
   * espera dos 14 dias (RF-31). Só vem quando é promessa: os dois pesos semanais que a RN-36 vai
   * comparar, o de hoje e o da base, já existem, e a espera termina até o último dia da fase,
   * porque depois dele não há veredito (RN-40). A manutenção não espera (RN-39).
   */
  public static Optional<LocalDate> dataDoVeredito(
      TipoDeFase tipo,
      List<Pesagem> pesagens,
      LocalDate diaDaBase,
      @Nullable LocalDate fimDaFase,
      LocalDate hoje) {
    LocalDate data = diaDaBase.plusDays(DIAS_MINIMOS);
    if (tipo == TipoDeFase.MANUTENCAO
        || !data.isAfter(hoje)
        || (fimDaFase != null && data.isAfter(fimDaFase))
        || calcularPesoSemanal(pesagens, hoje).isEmpty()
        || calcularPesoSemanal(pesagens, diaDaBase).isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(data);
  }

  /**
   * Ritmo é velocidade na direção do tipo: no cutting, perder mais rápido que o alvo é estar acima,
   * e ganhar peso é estar abaixo (RN-37).
   */
  private static Ritmo comparar(TipoDeFase tipo, BigDecimal real, BigDecimal alvo) {
    BigDecimal velocidade = arredondarTaxa(real);
    BigDecimal planejada = arredondarTaxa(alvo);
    if (tipo == TipoDeFase.CUTTING) {
      velocidade = velocidade.negate();
      planejada = planejada.negate();
    }
    if (velocidade.compareTo(planejada.multiply(MEIA_TAXA)) < 0) {
      return ABAIXO;
    }
    if (velocidade.compareTo(planejada.multiply(TAXA_E_MEIA)) > 0) {
      return ACIMA;
    }
    return NO_RITMO;
  }
}
