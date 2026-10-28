package mbarete.service;

import static mbarete.regras.DataDoRegistro.recusarDataFutura;
import static mbarete.regras.DiaDaBase.determinarDiaDaBase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import mbarete.dto.NovoRegistroDto;
import mbarete.dto.RegistroDto;
import mbarete.entity.Fase;
import mbarete.entity.Registro;
import mbarete.entity.Usuario;
import mbarete.mapper.RegistroMapper;
import mbarete.regras.Pesagem;
import mbarete.regras.Recusa;
import mbarete.regras.RegraVioladaException;
import mbarete.repository.FaseRepository;
import mbarete.repository.RegistroRepository;
import mbarete.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroService {

  private static final DateTimeFormatter DATA_EXIBIDA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

  private final RegistroRepository registros;
  private final FaseRepository fases;
  private final UsuarioRepository usuarios;
  private final Clock relogio;

  public RegistroService(
      RegistroRepository registros,
      FaseRepository fases,
      UsuarioRepository usuarios,
      Clock relogio) {
    this.registros = registros;
    this.fases = fases;
    this.usuarios = usuarios;
    this.relogio = relogio;
  }

  /**
   * Grava o registro da data (UC-02). A data ocupada responde com o valor gravado, e o registro
   * dela só é substituído no segundo envio, com a confirmação (RN-01, endpoints.md).
   */
  @Transactional
  public RegistroDto registrar(int idUsuario, NovoRegistroDto novo, LocalDate hoje) {
    recusarDataFutura(novo.data(), hoje);
    Instant agora = Instant.now(relogio);
    Optional<Registro> existente = registros.findByUsuarioIdAndData(idUsuario, novo.data());
    Registro registro;
    if (existente.isEmpty()) {
      registro =
          registros.save(
              new Registro(usuario(idUsuario), novo.data(), novo.peso(), novo.cintura(), agora));
    } else if (Boolean.TRUE.equals(novo.substituir())) {
      registro = existente.get();
      registro.substituir(novo.peso(), novo.cintura(), agora);
    } else {
      throw new RegraVioladaException(Recusa.REGISTRO_JA_EXISTE, "data", jaExiste(existente.get()));
    }
    fixarDiasDaBase(idUsuario, hoje);
    return RegistroMapper.paraDto(registro);
  }

  /**
   * O registro novo pode dar o primeiro peso semanal a uma fase, inclusive a uma que não cobre a
   * data dele, porque a janela olha 7 dias para trás. Fase que já tem o dia da base não muda
   * (RN-68).
   */
  private void fixarDiasDaBase(int idUsuario, LocalDate hoje) {
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

  /**
   * O token de acesso vale até 15 minutos depois de a conta ser apagada (UC-12), e aí a recusa é a
   * de quem não está autenticado, em vez do erro da chave estrangeira.
   */
  private Usuario usuario(int id) {
    return usuarios
        .findById(id)
        .orElseThrow(() -> new BadCredentialsException("A conta do token não existe mais."));
  }

  /**
   * O valor gravado, que o diálogo de confirmação mostra (UC-02 4a), escrito como a tela escreve
   * (RN-62, RN-63).
   */
  private static String jaExiste(Registro registro) {
    return "Já existe registro em "
        + DATA_EXIBIDA.format(registro.data())
        + ", com "
        + registro.peso().toPlainString().replace('.', ',')
        + " kg.";
  }
}
