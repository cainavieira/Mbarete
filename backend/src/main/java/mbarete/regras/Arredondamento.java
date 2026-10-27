package mbarete.regras;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Os números como a tela os mostra, que são os que o veredito compara (RN-37). Só o resultado se
 * arredonda, nunca o meio da conta, e o meio vai para longe do zero.
 */
public final class Arredondamento {

  private Arredondamento() {}

  /** O peso semanal e os limites da faixa, com uma casa. */
  public static BigDecimal arredondarPeso(BigDecimal peso) {
    return peso.setScale(1, RoundingMode.HALF_UP);
  }

  /** A taxa real e a taxa alvo em quilogramas por semana, com duas casas. */
  public static BigDecimal arredondarTaxa(BigDecimal taxa) {
    return taxa.setScale(2, RoundingMode.HALF_UP);
  }
}
