package mbarete.service;

import static mbarete.regras.EscritaDoCsv.escreverCsv;

import mbarete.regras.EscritaDoCsv.LinhaDoHistorico;
import mbarete.repository.RegistroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportacaoService {

  private final RegistroRepository registros;

  public ExportacaoService(RegistroRepository registros) {
    this.registros = registros;
  }

  /** O histórico completo da pessoa, em CSV (UC-11). */
  @Transactional(readOnly = true)
  public String exportar(int idUsuario) {
    return escreverCsv(
        registros.findByUsuarioId(idUsuario).stream()
            .map(
                registro ->
                    new LinhaDoHistorico(registro.data(), registro.peso(), registro.cintura()))
            .toList());
  }
}
