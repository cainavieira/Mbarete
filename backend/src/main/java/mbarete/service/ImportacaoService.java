package mbarete.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static mbarete.regras.LeituraDoCsv.lerCsv;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import mbarete.dto.PrevisualizacaoDto;
import mbarete.entity.Registro;
import mbarete.regras.LeituraDoCsv;
import mbarete.regras.LeituraDoCsv.LinhaValida;
import mbarete.regras.LeituraDoCsv.RegistroGravado;
import mbarete.repository.RegistroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportacaoService {

  private final RegistroRepository registros;
  private final ContaService contas;
  private final DiaDaBaseService diaDaBase;
  private final Clock relogio;

  public ImportacaoService(
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
   * Lê o arquivo e diz o que a gravação faria, sem gravar nada: o servidor não guarda o arquivo
   * entre a pré-visualização e a confirmação (RN-58, endpoints.md).
   */
  @Transactional(readOnly = true)
  public PrevisualizacaoDto previsualizar(int idUsuario, String conteudo, LocalDate hoje) {
    LeituraDoCsv leitura =
        lerCsv(
            conteudo,
            registros.findByUsuarioId(idUsuario).stream()
                .collect(toMap(Registro::data, ImportacaoService::gravado)),
            hoje);
    return new PrevisualizacaoDto(leitura.validas(), leitura.recusadas());
  }

  /**
   * Grava as linhas válidas do mesmo arquivo, lido de novo, numa transação só: tudo ou nada
   * (RN-59). As datas que ele substitui têm de ser as que a pessoa confirmou na pré-visualização
   * (UC-10 3b). A data que já tem registro é substituída inteira, cintura inclusive, como na
   * substituição do registro (RN-01), e nenhuma fase é criada (RN-57). O registro importado pode
   * dar o primeiro peso semanal a uma fase, e por isso o dia da base é fixado depois (RN-68).
   */
  @Transactional
  public PrevisualizacaoDto importar(
      int idUsuario, String conteudo, Set<LocalDate> substituir, LocalDate hoje) {
    Map<LocalDate, Registro> existentes =
        registros.findByUsuarioId(idUsuario).stream().collect(toMap(Registro::data, identity()));
    LeituraDoCsv leitura =
        lerCsv(
            conteudo,
            existentes.values().stream().collect(toMap(Registro::data, ImportacaoService::gravado)),
            hoje);
    leitura.conferirSubstituicoes(substituir);
    Instant agora = Instant.now(relogio);
    for (LinhaValida linha : leitura.validas()) {
      Registro existente = existentes.get(linha.data());
      if (existente == null) {
        registros.save(
            new Registro(
                contas.usuario(idUsuario), linha.data(), linha.peso(), linha.cintura(), agora));
      } else {
        existente.substituir(linha.peso(), linha.cintura(), agora);
      }
    }
    diaDaBase.fixarOndeFalta(idUsuario, hoje);
    return new PrevisualizacaoDto(leitura.validas(), leitura.recusadas());
  }

  private static RegistroGravado gravado(Registro registro) {
    return new RegistroGravado(registro.peso(), registro.cintura());
  }
}
