package mbarete.dto;

import mbarete.regras.TaxaReal;
import org.jspecify.annotations.Nullable;

/**
 * O veredito da fase corrente, com a taxa real ao lado em cutting e bulking (RN-36). Na manutenção,
 * os três estados se exibem como abaixo da faixa, na faixa e acima da faixa (RN-37), e com dados
 * insuficientes o campo vem com esse estado, em vez de sumir (RN-39).
 */
public record VereditoDto(Estado estado, @Nullable TaxaReal taxaReal) {

  public enum Estado {
    ABAIXO,
    NO_RITMO,
    ACIMA,
    DADOS_INSUFICIENTES
  }
}
