package mbarete.mapper;

import mbarete.dto.ContaDto;
import mbarete.entity.Usuario;

public final class ContaMapper {

  private ContaMapper() {}

  public static ContaDto paraDto(Usuario usuario) {
    return new ContaDto(usuario.nome(), usuario.email(), usuario.fotoUrl());
  }
}
