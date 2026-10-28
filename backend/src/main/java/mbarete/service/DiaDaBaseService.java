package mbarete.service;

import static mbarete.regras.DiaDaBase.determinarDiaDaBase;

import java.time.LocalDate;
import java.util.List;
import mbarete.entity.Fase;
import mbarete.regras.Pesagem;
import mbarete.repository.FaseRepository;
import mbarete.repository.RegistroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** O dia da base das fases, que gravar registro e criar ou mudar fase fixam (RN-68). */
@Service
public class DiaDaBaseService {

  private final FaseRepository fases;
  private final RegistroRepository registros;

  public DiaDaBaseService(FaseRepository fases, RegistroRepository registros) {
    this.fases = fases;
    this.registros = registros;
  }

  /**
   * Fixa o dia da base de toda fase da pessoa que ainda não o tem: um registro pode dar o primeiro
   * peso semanal até a uma fase que não cobre a data dele, porque a janela olha 7 dias para trás. A
   * fase que já tem o dia não muda; quem muda as datas dela o apaga antes (RN-68).
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void fixarOndeFalta(int idUsuario, LocalDate hoje) {
    List<Fase> semBase = fases.findByUsuarioIdAndBaseDataIsNull(idUsuario);
    if (semBase.isEmpty()) {
      return;
    }
    List<Pesagem> pesagens =
        registros.findByUsuarioId(idUsuario).stream()
            .map(registro -> new Pesagem(registro.data(), registro.peso()))
            .toList();
    for (Fase fase : semBase) {
      determinarDiaDaBase(pesagens, fase.periodo(), hoje).ifPresent(fase::fixarDiaDaBase);
    }
  }
}
