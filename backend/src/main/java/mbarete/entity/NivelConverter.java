package mbarete.entity;

import jakarta.persistence.Converter;
import mbarete.regras.Nivel;

@Converter(autoApply = true)
public class NivelConverter extends EnumEmMinusculasConverter<Nivel> {

  public NivelConverter() {
    super(Nivel.class);
  }
}
