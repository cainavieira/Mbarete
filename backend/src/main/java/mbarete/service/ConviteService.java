package mbarete.service;

import java.time.Clock;
import java.time.Instant;
import mbarete.entity.Convidado;
import mbarete.repository.ConvidadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConviteService {

  private final ConvidadoRepository convidados;
  private final Clock relogio;

  public ConviteService(ConvidadoRepository convidados, Clock relogio) {
    this.convidados = convidados;
    this.relogio = relogio;
  }

  /**
   * Põe o e-mail, já normalizado, na lista de convidados (UC-13). Falso quando ele já estava: o
   * convite não se duplica e guarda a data do primeiro (UC-13 2a).
   */
  @Transactional
  public boolean convidar(String email) {
    if (convidados.existsById(email)) {
      return false;
    }
    convidados.save(new Convidado(email, Instant.now(relogio)));
    return true;
  }
}
