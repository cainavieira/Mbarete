package mbarete.service;

import static java.util.stream.Collectors.toSet;
import static mbarete.regras.LeituraDoCsv.lerCsv;

import java.time.LocalDate;
import mbarete.dto.PrevisualizacaoDto;
import mbarete.entity.Registro;
import mbarete.regras.LeituraDoCsv;
import mbarete.repository.RegistroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportacaoService {

  private final RegistroRepository registros;

  public ImportacaoService(RegistroRepository registros) {
    this.registros = registros;
  }

  /**
   * Lê o arquivo e diz o que a gravação faria, sem gravar nada: o servidor não guarda o arquivo
   * entre a pré-visualização e a confirmação (RN-58, endpoints.md).
   */
  @Transactional(readOnly = true)
  public PrevisualizacaoDto previsualizar(int idUsuario, String conteudo, LocalDate hoje) {
    LeituraDoCsv leitura =
        lerCsv(
            conteudo,
            registros.findByUsuarioId(idUsuario).stream().map(Registro::data).collect(toSet()),
            hoje);
    return new PrevisualizacaoDto(leitura.validas(), leitura.recusadas());
  }
}
