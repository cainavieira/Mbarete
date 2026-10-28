package mbarete.service;

import static mbarete.regras.AcompanhamentoDaFase.acompanharFase;
import static mbarete.regras.PeriodoDeFase.faseDaData;
import static mbarete.regras.PesoSemanal.calcularPesoSemanal;
import static mbarete.regras.PesoSemanal.contarNaJanela;
import static mbarete.regras.PontoDaSerie.montarSerie;
import static mbarete.regras.ResumoCintura.resumoCintura;
import static mbarete.regras.Variacoes.calcularVariacoes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mbarete.dto.AcompanhamentoDto;
import mbarete.entity.Fase;
import mbarete.entity.Registro;
import mbarete.mapper.AcompanhamentoMapper;
import mbarete.regras.Medicao;
import mbarete.regras.PeriodoDeFase;
import mbarete.regras.Pesagem;
import mbarete.repository.FaseRepository;
import mbarete.repository.RegistroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcompanhamentoService {

  private final RegistroRepository registros;
  private final FaseRepository fases;
  private final DiaDaBaseService diaDaBase;

  public AcompanhamentoService(
      RegistroRepository registros, FaseRepository fases, DiaDaBaseService diaDaBase) {
    this.registros = registros;
    this.fases = fases;
    this.diaDaBase = diaDaBase;
  }

  /**
   * A tela principal (UC-08). A fase corrente é a que cobre hoje, e a futura não entra (RN-27,
   * RN-69). O dia da base que falta é fixado antes, como na lista de fases: a fase agendada que
   * começa hoje pode já ter peso semanal com os registros de antes dela (RN-68).
   */
  @Transactional
  public AcompanhamentoDto acompanhar(int idUsuario, LocalDate hoje) {
    diaDaBase.fixarOndeFalta(idUsuario, hoje);
    List<Registro> daPessoa = registros.findByUsuarioId(idUsuario);
    List<Pesagem> pesagens =
        daPessoa.stream().map(registro -> new Pesagem(registro.data(), registro.peso())).toList();
    List<Medicao> medicoes =
        daPessoa.stream()
            .filter(registro -> registro.cintura() != null)
            .map(registro -> new Medicao(registro.data(), registro.cintura()))
            .toList();
    List<Fase> fasesDaPessoa = fases.findByUsuarioId(idUsuario);
    List<PeriodoDeFase> periodos = fasesDaPessoa.stream().map(Fase::periodo).toList();
    Optional<Fase> corrente =
        faseDaData(periodos, hoje)
            .flatMap(
                periodo ->
                    fasesDaPessoa.stream().filter(fase -> fase.id() == periodo.id()).findFirst());
    return AcompanhamentoMapper.paraDto(
        calcularPesoSemanal(pesagens, hoje).orElse(null),
        contarNaJanela(pesagens, hoje),
        calcularVariacoes(pesagens, hoje),
        corrente.orElse(null),
        corrente
            .map(
                fase ->
                    acompanharFase(
                        fase.tipo(),
                        fase.taxaAlvo(),
                        fase.pesoAlvo(),
                        fase.diaDaBase(),
                        pesagens,
                        hoje))
            .orElse(null),
        resumoCintura(medicoes, periodos, hoje).orElse(null),
        montarSerie(pesagens, periodos, hoje));
  }
}
