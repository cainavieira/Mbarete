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

// O Hibernate lê os campos por reflexão, que o Error Prone não vê; o acesso
// pelo código entra com a rota que o usa
@SuppressWarnings("UnusedVariable")
@Entity
public class Registro {

  /** Interno, nunca sai na API: o registro é endereçado pela data (ADR-0036). */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  private LocalDate data;
  private BigDecimal peso;
  private BigDecimal cintura;
  private Instant criadoEm;

  protected Registro() {}

  /** A cintura é opcional (RN-10). */
  public Registro(
      Usuario usuario, LocalDate data, BigDecimal peso, BigDecimal cintura, Instant criadoEm) {
    this.usuario = usuario;
    this.data = data;
    this.peso = peso;
    this.cintura = cintura;
    this.criadoEm = criadoEm;
  }

  public LocalDate data() {
    return data;
  }

  public BigDecimal peso() {
    return peso;
  }

  public BigDecimal cintura() {
    return cintura;
  }

  /**
   * A pesagem confirmada toma o lugar da anterior na data inteira, cintura inclusive: sem cintura
   * no envio novo, a de antes não fica (RN-01).
   */
  public void substituir(BigDecimal peso, BigDecimal cintura, Instant criadoEm) {
    this.peso = peso;
    this.cintura = cintura;
    this.criadoEm = criadoEm;
  }

  /** A edição troca o registro inteiro, e a data nova o move (RN-07, endpoints.md). */
  public void editar(LocalDate data, BigDecimal peso, BigDecimal cintura) {
    this.data = data;
    this.peso = peso;
    this.cintura = cintura;
  }
}
