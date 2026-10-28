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

  /**
   * O percentual da barra, inteiro e cortado, e não arredondado: assim a barra só escreve 100%
   * quando o alvo foi atingido, e nunca ao lado de um aviso que ainda não disparou (RN-44, RN-45).
   */
  public static BigDecimal arredondarPercentual(BigDecimal percentual) {
    return percentual.setScale(0, RoundingMode.DOWN);
  }
}
