package mbarete.regras;

import static mbarete.regras.PesoSemanal.calcularPesoSemanal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;

/** A barra que vai da base da RN-68 até o peso alvo (RN-43), ou o motivo de ela não aparecer. */
public sealed interface Progresso {

  /** Falta o peso semanal da base ou o de hoje (RN-16, RN-68). */
  record DadosInsuficientes() implements Progresso {}

  /** O alvo está do lado errado da base, ou igual a ela: sem barra e sem aviso (RN-44). */
  record NaoExibido() implements Progresso {}

  /** O percentual exato, entre 0 e 100, e se o alvo foi atingido (RN-44, RN-45). */
  record Barra(BigDecimal percentual, boolean atingido) implements Progresso {}

  /**
   * O caminho do cutting é para baixo e o do bulking, para cima (RN-42). A conta usa os pesos
   * semanais exatos, sem o arredondamento do veredito (RN-44).
   */
  static Progresso calcularProgresso(
      TipoDeFase tipo,
      BigDecimal pesoAlvo,
      List<Pesagem> pesagens,
      LocalDate diaDaBase,
      LocalDate hoje) {
    BigDecimal sentido =
        switch (tipo) {
          case CUTTING -> BigDecimal.ONE.negate();
          case BULKING -> BigDecimal.ONE;
          case MANUTENCAO ->
              throw new IllegalArgumentException("Manutenção não tem peso alvo (RN-41)");
        };
    return calcularPesoSemanal(pesagens, diaDaBase)
        .flatMap(
            base ->
                calcularPesoSemanal(pesagens, hoje)
                    .map(atual -> entre(sentido, pesoAlvo, base, atual)))
        .orElseGet(DadosInsuficientes::new);
  }

  private static Progresso entre(
      BigDecimal sentido, BigDecimal alvo, BigDecimal base, BigDecimal atual) {
    BigDecimal caminho = alvo.subtract(base).multiply(sentido);
    if (caminho.signum() <= 0) {
      return new NaoExibido();
    }
    BigDecimal andado = atual.subtract(base).multiply(sentido);
    BigDecimal cem = BigDecimal.valueOf(100);
    BigDecimal percentual =
        andado.multiply(cem).divide(caminho, MathContext.DECIMAL128).max(BigDecimal.ZERO).min(cem);
    return new Barra(percentual, andado.compareTo(caminho) >= 0);
  }
}
