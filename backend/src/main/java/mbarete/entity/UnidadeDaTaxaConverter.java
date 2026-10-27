package mbarete.entity;

import jakarta.persistence.Converter;
import mbarete.regras.UnidadeDaTaxa;

@Converter(autoApply = true)
public class UnidadeDaTaxaConverter extends EnumEmMinusculasConverter<UnidadeDaTaxa> {

  public UnidadeDaTaxaConverter() {
    super(UnidadeDaTaxa.class);
  }
}
