package mbarete.service;

import static mbarete.regras.EncaixeDeFase.apagarFase;
import static mbarete.regras.EncaixeDeFase.encaixarEdicao;
import static mbarete.regras.EncaixeDeFase.encaixarFaseNova;
import static mbarete.regras.EncaixeDeFase.encerrarFase;
import static mbarete.regras.FaseEncerrada.resumirFaseEncerrada;
import static mbarete.regras.PeriodoDeFase.contarRegistros;
import static mbarete.regras.PlanoDeFase.editarPlano;
import static mbarete.regras.PlanoDeFase.montarPlano;
import static mbarete.regras.SituacaoDaFase.situacaoDaFase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import mbarete.dto.EncerramentoDto;
import mbarete.dto.FaseDto;
import mbarete.dto.FaseEnviadaDto;
import mbarete.dto.FaseListadaDto;
import mbarete.dto.NivelDto;
import mbarete.entity.Fase;
import mbarete.mapper.FaseMapper;
import mbarete.regras.Nivel;
import mbarete.regras.PeriodoDeFase;
import mbarete.regras.Pesagem;
import mbarete.regras.PlanoDeFase;
import mbarete.regras.Recusa;
import mbarete.regras.RegraVioladaException;
import mbarete.regras.SituacaoDaFase;
import mbarete.regras.TaxaAlvo;
import mbarete.regras.TipoDeFase;
import mbarete.repository.FaseRepository;
import mbarete.repository.RegistroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaseService {

  /** A de outra pessoa e a que não existe recebem a mesma recusa (RN-50). */
  private static final String NAO_ENCONTRADO = "Não encontrado.";

  private final FaseRepository fases;
  private final RegistroRepository registros;
  private final ContaService contas;
  private final DiaDaBaseService diaDaBase;
  private final Clock relogio;

  public FaseService(
      FaseRepository fases,
      RegistroRepository registros,
      ContaService contas,
      DiaDaBaseService diaDaBase,
      Clock relogio) {
    this.fases = fases;
    this.registros = registros;
    this.contas = contas;
    this.diaDaBase = diaDaBase;
    this.relogio = relogio;
  }

  /**
   * As fases da pessoa pela data de início, cada uma com a situação em relação a hoje e quantos
   * registros tem (UC-07, RN-69), e a encerrada com o histórico (UC-09, RN-40). O dia da base que
   * falta é fixado aqui também, porque ele pode passar a existir sem nenhuma gravação: a fase
   * agendada que começa já pode ter peso semanal com os registros de antes dela (RN-68).
   */
  @Transactional
  public List<FaseListadaDto> listar(int idUsuario, LocalDate hoje) {
    diaDaBase.fixarOndeFalta(idUsuario, hoje);
    List<Pesagem> pesagens =
        registros.findByUsuarioId(idUsuario).stream()
            .map(registro -> new Pesagem(registro.data(), registro.peso()))
            .toList();
    return fases.findByUsuarioIdOrderByDataInicio(idUsuario).stream()
        .map(
            fase -> {
              PeriodoDeFase periodo = fase.periodo();
              SituacaoDaFase situacao = situacaoDaFase(periodo, hoje);
              return FaseMapper.paraListada(
                  fase,
                  situacao,
                  contarRegistros(pesagens, periodo),
                  situacao == SituacaoDaFase.ENCERRADA
                      ? resumirFaseEncerrada(
                          fase.tipo(),
                          fase.pesoAlvo(),
                          periodo.inicio(),
                          periodo.fim(),
                          fase.diaDaBase(),
                          pesagens)
                      : null);
            })
        .toList();
  }

  /**
   * Cria a fase (UC-05). A fase nova sem data de fim que começa depois da fase em aberto a encerra
   * na véspera (RN-22). O dia da base é fixado onde já há peso semanal: na nova, e na encerrada
   * quando o fim novo deixou o dela de fora (RN-68).
   */
  @Transactional
  public FaseDto criar(int idUsuario, FaseEnviadaDto nova, LocalDate hoje) {
    PlanoDeFase plano =
        montarPlano(
            nova.tipo(),
            nova.nivel(),
            nova.taxaAlvoValor(),
            nova.taxaAlvoUnidade(),
            nova.pesoAlvo());
    List<Fase> daPessoa = fases.findByUsuarioId(idUsuario);
    encaixarFaseNova(
            daPessoa.stream().map(Fase::periodo).toList(), nova.dataInicio(), nova.dataFim(), hoje)
        .ifPresent(
            encerrada -> {
              daPessoa.stream()
                  .filter(fase -> fase.id() == encerrada.id())
                  .forEach(fase -> fase.mudarFim(encerrada.fim()));
              // O banco confere a sobreposição com a fase em aberto como ela está gravada, e o
              // Hibernate só gravaria o encerramento depois de inserir a nova
              fases.flush();
            });
    TaxaAlvo taxa = plano.taxa();
    Fase fase =
        fases.save(
            new Fase(
                contas.usuario(idUsuario),
                nova.tipo(),
                nova.dataInicio(),
                nova.dataFim(),
                plano.nivel(),
                taxa == null ? null : taxa.valor(),
                taxa == null ? null : taxa.unidade(),
                plano.pesoAlvo(),
                Instant.now(relogio)));
    diaDaBase.fixarOndeFalta(idUsuario, hoje);
    return FaseMapper.paraDto(fase);
  }

  /**
   * Edita a fase (UC-06). Mudar o início da fase seguinte a um fim no futuro move esse fim junto
   * (RN-67), e o dia da base segue a RN-68 nas duas.
   */
  @Transactional
  public FaseDto editar(int idUsuario, int id, FaseEnviadaDto editada, LocalDate hoje) {
    List<Fase> daPessoa = fases.findByUsuarioId(idUsuario);
    Fase fase =
        daPessoa.stream()
            .filter(daLista -> daLista.id() == id)
            .findFirst()
            .orElseThrow(() -> new RegraVioladaException(Recusa.NAO_ENCONTRADO, NAO_ENCONTRADO));
    PlanoDeFase plano =
        editarPlano(
            fase.tipo(),
            fase.taxaAlvo(),
            editada.tipo(),
            editada.nivel(),
            editada.taxaAlvoValor(),
            editada.taxaAlvoUnidade(),
            editada.pesoAlvo());
    PeriodoDeFase antes = fase.periodo();
    Optional<PeriodoDeFase> anteriorMovida =
        encaixarEdicao(
            daPessoa.stream().map(Fase::periodo).toList(),
            antes,
            editada.dataInicio(),
            editada.dataFim(),
            hoje);
    TaxaAlvo taxa = plano.taxa();
    Runnable editarAFase =
        () ->
            fase.editar(
                editada.tipo(),
                editada.dataInicio(),
                editada.dataFim(),
                plano.nivel(),
                taxa == null ? null : taxa.valor(),
                taxa == null ? null : taxa.unidade(),
                plano.pesoAlvo());
    anteriorMovida.ifPresentOrElse(
        movida -> {
          Fase anterior =
              daPessoa.stream().filter(daLista -> daLista.id() == movida.id()).findFirst().get();
          // O banco confere a sobreposição a cada gravação, e por isso quem encolhe vai antes
          if (editada.dataInicio().isBefore(antes.inicio())) {
            anterior.mudarFim(movida.fim());
            fases.flush();
            editarAFase.run();
          } else {
            editarAFase.run();
            fases.flush();
            anterior.mudarFim(movida.fim());
          }
        },
        editarAFase);
    diaDaBase.fixarOndeFalta(idUsuario, hoje);
    return FaseMapper.paraDto(fase);
  }

  /**
   * Encerra a fase corrente na data dada (UC-14). Os registros depois dela ficam sem fase (RN-20),
   * e o dia da base segue a RN-68: só muda se ficar depois do fim.
   */
  @Transactional
  public FaseDto encerrar(int idUsuario, int id, EncerramentoDto encerramento, LocalDate hoje) {
    Fase fase =
        fases
            .findByIdAndUsuarioId(id, idUsuario)
            .orElseThrow(() -> new RegraVioladaException(Recusa.NAO_ENCONTRADO, NAO_ENCONTRADO));
    fase.mudarFim(encerrarFase(fase.periodo(), encerramento.dataFim(), hoje).fim());
    diaDaBase.fixarOndeFalta(idUsuario, hoje);
    return FaseMapper.paraDto(fase);
  }

  /**
   * Apaga a fase (UC-07). Os registros ficam e perdem o rótulo do período (RN-70), e o fim no
   * futuro da anterior, que estava ali por causa dela, vai para a véspera da próxima ou some
   * (RN-67).
   */
  @Transactional
  public void apagar(int idUsuario, int id, LocalDate hoje) {
    List<Fase> daPessoa = fases.findByUsuarioId(idUsuario);
    Fase fase =
        daPessoa.stream()
            .filter(daLista -> daLista.id() == id)
            .findFirst()
            .orElseThrow(() -> new RegraVioladaException(Recusa.NAO_ENCONTRADO, NAO_ENCONTRADO));
    Optional<PeriodoDeFase> anteriorMovida =
        apagarFase(daPessoa.stream().map(Fase::periodo).toList(), fase.periodo(), hoje);
    fases.delete(fase);
    // O Hibernate apagaria só depois de esticar a anterior, e o banco recusaria a sobreposição
    fases.flush();
    anteriorMovida.ifPresent(
        movida ->
            daPessoa.stream()
                .filter(daLista -> daLista.id() == movida.id())
                .forEach(anterior -> anterior.mudarFim(movida.fim())));
    diaDaBase.fixarOndeFalta(idUsuario, hoje);
  }

  /**
   * A taxa alvo padrão de cada nível, em cutting e bulking (RN-31, RN-32). O personalizado não tem
   * padrão, e a manutenção não tem nível (RN-28, RN-33).
   */
  public List<NivelDto> niveis() {
    return Stream.of(TipoDeFase.CUTTING, TipoDeFase.BULKING)
        .flatMap(
            tipo ->
                Arrays.stream(Nivel.values())
                    .flatMap(
                        nivel ->
                            TaxaAlvo.padrao(tipo, nivel).stream()
                                .map(
                                    taxa ->
                                        new NivelDto(tipo, nivel, taxa.valor(), taxa.unidade()))))
        .toList();
  }
}
