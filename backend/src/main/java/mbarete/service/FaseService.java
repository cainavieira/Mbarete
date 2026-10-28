package mbarete.service;

import static mbarete.regras.EncaixeDeFase.encaixarEdicao;
import static mbarete.regras.EncaixeDeFase.encaixarFaseNova;
import static mbarete.regras.PlanoDeFase.editarPlano;
import static mbarete.regras.PlanoDeFase.montarPlano;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import mbarete.dto.FaseDto;
import mbarete.dto.FaseEnviadaDto;
import mbarete.dto.NivelDto;
import mbarete.entity.Fase;
import mbarete.mapper.FaseMapper;
import mbarete.regras.Nivel;
import mbarete.regras.PeriodoDeFase;
import mbarete.regras.PlanoDeFase;
import mbarete.regras.Recusa;
import mbarete.regras.RegraVioladaException;
import mbarete.regras.TaxaAlvo;
import mbarete.regras.TipoDeFase;
import mbarete.repository.FaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaseService {

  /** A de outra pessoa e a que não existe recebem a mesma recusa (RN-50). */
  private static final String NAO_ENCONTRADO = "Não encontrado.";

  private final FaseRepository fases;
  private final ContaService contas;
  private final DiaDaBaseService diaDaBase;
  private final Clock relogio;

  public FaseService(
      FaseRepository fases, ContaService contas, DiaDaBaseService diaDaBase, Clock relogio) {
    this.fases = fases;
    this.contas = contas;
    this.diaDaBase = diaDaBase;
    this.relogio = relogio;
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
    BigDecimal valorAntes = fase.taxaAlvoValor();
    PlanoDeFase plano =
        editarPlano(
            fase.tipo(),
            valorAntes == null ? null : new TaxaAlvo(valorAntes, fase.taxaAlvoUnidade()),
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
