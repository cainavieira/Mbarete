package mbarete.regras;

import static mbarete.regras.Arredondamento.arredondarPeso;
import static mbarete.regras.PesoSemanal.calcularPesoSemanal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** A faixa da manutenção em volta da base, com os limites como a tela os mostra (RN-33, RN-37). */
public record FaixaDeTolerancia(BigDecimal minimo, BigDecimal maximo) {

  /** 1%, porque a água e o sal tiram de uma faixa mais estreita quem mantém o peso (RN-33). */
  private static final BigDecimal FATOR_DO_MINIMO = new BigDecimal("0.99");

  private static final BigDecimal FATOR_DO_MAXIMO = new BigDecimal("1.01");

  /** A faixa sai do peso semanal do dia da base, e sem ele não há faixa (RN-68). */
  public static Optional<FaixaDeTolerancia> calcularFaixa(
      List<Pesagem> pesagens, LocalDate diaDaBase) {
    return calcularPesoSemanal(pesagens, diaDaBase)
        .map(
            base ->
                new FaixaDeTolerancia(
                    arredondarPeso(base.multiply(FATOR_DO_MINIMO)),
                    arredondarPeso(base.multiply(FATOR_DO_MAXIMO))));
  }

  /** A posição do peso semanal, arredondado como a tela o mostra, com as bordas dentro (RN-33). */
  Ritmo posicao(BigDecimal pesoSemanal) {
    BigDecimal peso = arredondarPeso(pesoSemanal);
    if (peso.compareTo(minimo) < 0) {
      return Ritmo.ABAIXO;
    }
    if (peso.compareTo(maximo) > 0) {
      return Ritmo.ACIMA;
    }
    return Ritmo.NO_RITMO;
  }
}
