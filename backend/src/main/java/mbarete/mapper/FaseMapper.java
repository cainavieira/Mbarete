package mbarete.mapper;

import mbarete.dto.FaseDto;
import mbarete.entity.Fase;
import mbarete.regras.PeriodoDeFase;

public final class FaseMapper {

  private FaseMapper() {}

  public static FaseDto paraDto(Fase fase) {
    PeriodoDeFase periodo = fase.periodo();
    return new FaseDto(
        periodo.id(),
        fase.tipo(),
        periodo.inicio(),
        periodo.fim(),
        fase.nivel(),
        fase.taxaAlvoValor(),
        fase.taxaAlvoUnidade(),
        fase.pesoAlvo());
  }
}
