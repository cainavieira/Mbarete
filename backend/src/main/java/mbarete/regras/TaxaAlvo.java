package mbarete.regras;

import static mbarete.regras.PesoSemanal.calcularPesoSemanal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** A taxa alvo como a pessoa a digita: o valor sem sinal e a unidade (RN-29). */
public record TaxaAlvo(BigDecimal valor, UnidadeDaTaxa unidade) {

  /** O sinal vem do tipo, e zero é recusado (RN-29). */
  public TaxaAlvo {
    if (valor.signum() <= 0) {
      throw new IllegalArgumentException("A taxa alvo é digitada sem sinal e não é zero");
    }
  }

  /** A taxa alvo padrão do nível no tipo (RN-31, RN-32); o personalizado não tem (RN-28). */
  public static Optional<TaxaAlvo> padrao(TipoDeFase tipo, Nivel nivel) {
    return switch (tipo) {
      case CUTTING -> doNivel(nivel, UnidadeDaTaxa.PERCENTUAL_SEMANA, "0.25", "0.50", "1.00");
      case BULKING -> doNivel(nivel, UnidadeDaTaxa.KG_SEMANA, "0.20", "0.30", "0.40");
      case MANUTENCAO -> throw new IllegalArgumentException("Manutenção não tem nível (RN-33)");
    };
  }

  /**
   * A taxa alvo em quilogramas por semana, com o sinal do tipo (RN-29, RN-35). A percentual incide
   * sobre o peso semanal de hoje, e sem ele não há conversão (RN-34). A manutenção converte para
   * zero, que é constante da regra e não valor gravado (RN-33).
   */
  public static Optional<BigDecimal> converterTaxaAlvo(
      TipoDeFase tipo, @Nullable TaxaAlvo taxa, List<Pesagem> pesagens, LocalDate hoje) {
    if (tipo == TipoDeFase.MANUTENCAO) {
      return Optional.of(BigDecimal.ZERO);
    }
    TaxaAlvo plano = Objects.requireNonNull(taxa, "Cutting e bulking têm taxa alvo (RN-29)");
    Optional<BigDecimal> emKg =
        switch (plano.unidade()) {
          case KG_SEMANA -> Optional.of(plano.valor());
          case PERCENTUAL_SEMANA ->
              calcularPesoSemanal(pesagens, hoje)
                  .map(peso -> peso.multiply(plano.valor()).movePointLeft(2));
        };
    return tipo == TipoDeFase.CUTTING ? emKg.map(BigDecimal::negate) : emKg;
  }

  private static Optional<TaxaAlvo> doNivel(
      Nivel nivel, UnidadeDaTaxa unidade, String conservador, String moderado, String agressivo) {
    return switch (nivel) {
      case CONSERVADOR -> Optional.of(new TaxaAlvo(new BigDecimal(conservador), unidade));
      case MODERADO -> Optional.of(new TaxaAlvo(new BigDecimal(moderado), unidade));
      case AGRESSIVO -> Optional.of(new TaxaAlvo(new BigDecimal(agressivo), unidade));
      case PERSONALIZADO -> Optional.empty();
    };
  }
}
