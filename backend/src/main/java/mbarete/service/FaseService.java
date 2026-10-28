package mbarete.service;

import static mbarete.regras.EncaixeDeFase.encaixarFaseNova;
import static mbarete.regras.PlanoDeFase.montarPlano;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import mbarete.dto.FaseDto;
import mbarete.dto.FaseEnviadaDto;
import mbarete.dto.NivelDto;
import mbarete.entity.Fase;
import mbarete.mapper.FaseMapper;
import mbarete.regras.Nivel;
import mbarete.regras.PlanoDeFase;
import mbarete.regras.TaxaAlvo;
import mbarete.regras.TipoDeFase;
import mbarete.repository.FaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaseService {

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
