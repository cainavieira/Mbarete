package mbarete.controller;

import java.util.List;
import mbarete.dto.NivelDto;
import mbarete.service.FaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** As taxas alvo padrão, que a tela sugere ao escolher o nível (RF-16). */
@RestController
public class NivelController {

  private final FaseService fases;

  public NivelController(FaseService fases) {
    this.fases = fases;
  }

  @GetMapping("/niveis")
  public List<NivelDto> niveis() {
    return fases.niveis();
  }
}
