package mbarete.mapper;

import mbarete.dto.RegistroDto;
import mbarete.entity.Registro;

public final class RegistroMapper {

  private RegistroMapper() {}

  public static RegistroDto paraDto(Registro registro) {
    return new RegistroDto(registro.data(), registro.peso(), registro.cintura());
  }
}
