package mbarete.entity;

import jakarta.persistence.AttributeConverter;
import java.util.Locale;

/** Grava o enum pelo nome em minúsculas, como o banco guarda: {@code kg_semana}. */
public abstract class EnumEmMinusculasConverter<E extends Enum<E>>
    implements AttributeConverter<E, String> {

  private final Class<E> tipo;

  protected EnumEmMinusculasConverter(Class<E> tipo) {
    this.tipo = tipo;
  }

  @Override
  public String convertToDatabaseColumn(E valor) {
    return valor == null ? null : valor.name().toLowerCase(Locale.ROOT);
  }

  @Override
  public E convertToEntityAttribute(String coluna) {
    return coluna == null ? null : Enum.valueOf(tipo, coluna.toUpperCase(Locale.ROOT));
  }
}
