package mbarete.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import mbarete.regras.Nivel;
import mbarete.regras.PeriodoDeFase;
import mbarete.regras.TipoDeFase;
import mbarete.regras.UnidadeDaTaxa;

// O Hibernate lê os campos por reflexão, que o Error Prone não vê; o acesso
// pelo código entra com a rota que o usa
@SuppressWarnings("UnusedVariable")
@Entity
public class Fase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  private TipoDeFase tipo;
  private LocalDate dataInicio;

  /** Nula enquanto a fase está em aberto (RN-18). */
  private LocalDate dataFim;

  private Nivel nivel;

  /** Sem sinal, como a pessoa digita; o sinal vem do tipo (RN-29). */
  private BigDecimal taxaAlvoValor;

  private UnidadeDaTaxa taxaAlvoUnidade;
  private BigDecimal pesoAlvo;

  /** O dia da base, nulo até ser fixado; o valor sai da janela desse dia (RN-68). */
  private LocalDate baseData;

  private Instant criadoEm;

  protected Fase() {}

  /** Na manutenção, nível, taxa alvo e peso alvo são nulos (RN-33, RN-41). */
  public Fase(
      Usuario usuario,
      TipoDeFase tipo,
      LocalDate dataInicio,
      LocalDate dataFim,
      Nivel nivel,
      BigDecimal taxaAlvoValor,
      UnidadeDaTaxa taxaAlvoUnidade,
      BigDecimal pesoAlvo,
      Instant criadoEm) {
    this.usuario = usuario;
    this.tipo = tipo;
    this.dataInicio = dataInicio;
    this.dataFim = dataFim;
    this.nivel = nivel;
    this.taxaAlvoValor = taxaAlvoValor;
    this.taxaAlvoUnidade = taxaAlvoUnidade;
    this.pesoAlvo = pesoAlvo;
    this.criadoEm = criadoEm;
  }

  public Integer id() {
    return id;
  }

  public TipoDeFase tipo() {
    return tipo;
  }

  public Nivel nivel() {
    return nivel;
  }

  public BigDecimal taxaAlvoValor() {
    return taxaAlvoValor;
  }

  public UnidadeDaTaxa taxaAlvoUnidade() {
    return taxaAlvoUnidade;
  }

  public BigDecimal pesoAlvo() {
    return pesoAlvo;
  }

  public PeriodoDeFase periodo() {
    return new PeriodoDeFase(id, dataInicio, dataFim);
  }

  /** Fixado na primeira vez, e só a mudança das datas da fase o determina de novo (RN-68). */
  public void fixarDiaDaBase(LocalDate dia) {
    this.baseData = dia;
  }

  /**
   * O intervalo passou a ser outro, e o dia da base é determinado de novo sobre ele (RN-68): quem
   * muda as datas fixa o dia de novo depois.
   */
  public void mudarDatas(LocalDate inicio, LocalDate fim) {
    this.dataInicio = inicio;
    this.dataFim = fim;
    this.baseData = null;
  }
}
