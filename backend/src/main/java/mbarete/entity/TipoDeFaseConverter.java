package mbarete.entity;

import jakarta.persistence.Converter;
import mbarete.regras.TipoDeFase;

@Converter(autoApply = true)
public class TipoDeFaseConverter extends EnumEmMinusculasConverter<TipoDeFase> {

  public TipoDeFaseConverter() {
    super(TipoDeFase.class);
  }
}
