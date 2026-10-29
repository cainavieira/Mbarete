package mbarete.dto;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * A barra até o peso alvo, ou o motivo de ela não aparecer: falta o peso semanal de hoje (UC-08 1a)
 * ou a base (UC-08 5c), ou o alvo está do lado errado da base (UC-08 5d). O aviso da RN-45 sai do
 * atingido.
 */
public record ProgressoDto(Estado estado, @Nullable BigDecimal percentual, boolean atingido) {

  public enum Estado {
    BARRA,
    DADOS_INSUFICIENTES,
    NAO_EXIBIDO
  }
}
