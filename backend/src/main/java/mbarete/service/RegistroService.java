package mbarete.service;

import static mbarete.regras.DataDoRegistro.recusarDataFutura;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import mbarete.dto.RegistroDto;
import mbarete.dto.RegistroEnviadoDto;
import mbarete.entity.Registro;
import mbarete.mapper.RegistroMapper;
import mbarete.regras.Recusa;
import mbarete.regras.RegraVioladaException;
import mbarete.repository.RegistroRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroService {

  private static final DateTimeFormatter DATA_EXIBIDA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

  /** A lista sem intervalo, contando hoje (endpoints.md). */
  private static final int DIAS_DA_LISTA = 90;

  /** O de outra pessoa e o que não existe recebem a mesma recusa (RN-50). */
  private static final String NAO_ENCONTRADO = "Não encontrado.";

  private final RegistroRepository registros;
  private final ContaService contas;
  private final DiaDaBaseService diaDaBase;
  private final Clock relogio;

  public RegistroService(
      RegistroRepository registros,
      ContaService contas,
      DiaDaBaseService diaDaBase,
      Clock relogio) {
    this.registros = registros;
    this.contas = contas;
    this.diaDaBase = diaDaBase;
    this.relogio = relogio;
  }

  /**
   * Grava o registro da data (UC-02). A data ocupada responde com o valor gravado, e o registro
   * dela só é substituído no segundo envio, com a confirmação (RN-01, endpoints.md).
   */
  @Transactional
  public RegistroDto registrar(int idUsuario, RegistroEnviadoDto novo, LocalDate hoje) {
    recusarDataFutura(novo.data(), hoje);
    Instant agora = Instant.now(relogio);
    Optional<Registro> existente = registros.findByUsuarioIdAndData(idUsuario, novo.data());
    Registro registro;
    if (existente.isEmpty()) {
      registro =
          registros.save(
              new Registro(
                  contas.usuario(idUsuario), novo.data(), novo.peso(), novo.cintura(), agora));
    } else if (Boolean.TRUE.equals(novo.substituir())) {
      registro = existente.get();
      registro.substituir(novo.peso(), novo.cintura(), agora);
    } else {
      throw conflito(existente.get());
    }
    diaDaBase.fixarOndeFalta(idUsuario, hoje);
    return RegistroMapper.paraDto(registro);
  }

  /**
   * Edita o registro da data, inclusive movendo-o para outra (UC-03). A data nova ocupada responde
   * como no registro, e a confirmação apaga o registro dela: dois registros passam a ser um
   * (endpoints.md).
   */
  @Transactional
  public RegistroDto editar(
      int idUsuario, LocalDate data, RegistroEnviadoDto editado, LocalDate hoje) {
    recusarDataFutura(editado.data(), hoje);
    Registro registro = registroDaData(idUsuario, data);
    if (!editado.data().equals(data)) {
      Optional<Registro> ocupante = registros.findByUsuarioIdAndData(idUsuario, editado.data());
      if (ocupante.isPresent()) {
        if (!Boolean.TRUE.equals(editado.substituir())) {
          throw conflito(ocupante.get());
        }
        registros.delete(ocupante.get());
        // O Hibernate apagaria só depois de mover, e o banco recusaria dois na mesma data
        registros.flush();
      }
    }
    registro.editar(editado.data(), editado.peso(), editado.cintura());
    diaDaBase.fixarOndeFalta(idUsuario, hoje);
    return RegistroMapper.paraDto(registro);
  }

  /**
   * Apaga o registro da data, sem exclusão lógica (RN-08). O que dependia dele é recalculado na
   * leitura, e o dia da base que ele ajudou a fixar fica onde está (RN-68).
   */
  @Transactional
  public void apagar(int idUsuario, LocalDate data) {
    registros.delete(registroDaData(idUsuario, data));
  }

  /**
   * Os registros de {@code de} a {@code ate}, as duas pontas inclusive, em ordem de data (RF-14).
   * Sem {@code ate}, vai até hoje; sem {@code de}, cobre os 90 dias que terminam no {@code ate}, e
   * sem nenhum dos dois são os últimos 90 dias (endpoints.md).
   */
  @Transactional(readOnly = true)
  public List<RegistroDto> listar(
      int idUsuario, @Nullable LocalDate de, @Nullable LocalDate ate, LocalDate hoje) {
    LocalDate fim = ate == null ? hoje : ate;
    LocalDate inicio = de == null ? fim.minusDays(DIAS_DA_LISTA - 1) : de;
    return registros.findByUsuarioIdAndDataBetweenOrderByData(idUsuario, inicio, fim).stream()
        .map(RegistroMapper::paraDto)
        .toList();
  }

  private Registro registroDaData(int idUsuario, LocalDate data) {
    return registros
        .findByUsuarioIdAndData(idUsuario, data)
        .orElseThrow(() -> new RegraVioladaException(Recusa.NAO_ENCONTRADO, NAO_ENCONTRADO));
  }

  /**
   * A data já tem registro, e a mensagem leva o valor gravado, que o diálogo de confirmação mostra
   * (UC-02 4a), escrito como a tela escreve (RN-62, RN-63). A cintura entra quando existe, porque a
   * substituição troca o registro inteiro e ela não fica (endpoints.md).
   */
  private static RegraVioladaException conflito(Registro ocupante) {
    String cintura =
        ocupante.cintura() == null
            ? ""
            : " e " + ocupante.cintura().toPlainString().replace('.', ',') + " cm de cintura";
    return new RegraVioladaException(
        Recusa.REGISTRO_JA_EXISTE,
        "data",
        "Já existe registro em "
            + DATA_EXIBIDA.format(ocupante.data())
            + ", com "
            + ocupante.peso().toPlainString().replace('.', ',')
            + " kg"
            + cintura
            + ".");
  }
}
