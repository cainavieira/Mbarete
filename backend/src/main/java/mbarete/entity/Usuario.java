package mbarete.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

// O Hibernate lê os campos por reflexão, que o Error Prone não vê; o acesso
// pelo código entra com a rota que o usa
@SuppressWarnings("UnusedVariable")
@Entity
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String email;
  private String nome;
  private String fotoUrl;
  private Instant criadoEm;

  protected Usuario() {}

  /** Nome, e-mail e foto vêm do Google (RF-03); a foto pode faltar. */
  public Usuario(String email, String nome, String fotoUrl, Instant criadoEm) {
    this.email = email;
    this.nome = nome;
    this.fotoUrl = fotoUrl;
    this.criadoEm = criadoEm;
  }
}
