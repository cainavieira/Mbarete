package mbarete.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

// O Hibernate lê os campos por reflexão, que o Error Prone não vê; o acesso
// pelo código entra com a rota que o usa
@SuppressWarnings("UnusedVariable")
@Entity
public class TokenRenovacao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  private String hash;
  private Instant criadoEm;
  private Instant expiraEm;

  protected TokenRenovacao() {}

  /** Guarda o hash, nunca o token que vai no cookie (ADR-0041). */
  public TokenRenovacao(Usuario usuario, String hash, Instant criadoEm, Instant expiraEm) {
    this.usuario = usuario;
    this.hash = hash;
    this.criadoEm = criadoEm;
    this.expiraEm = expiraEm;
  }
}
