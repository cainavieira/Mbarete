package mbarete.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

// O Hibernate lê os campos por reflexão, que o Error Prone não vê; o acesso
// pelo código entra com a rota que o usa
/** O e-mail que pode entrar no sistema (RN-48). */
@SuppressWarnings("UnusedVariable")
@Entity
public class Convidado {

  @Id private String email;

  private Instant convidadoEm;

  protected Convidado() {}

  public Convidado(String email, Instant convidadoEm) {
    this.email = email;
    this.convidadoEm = convidadoEm;
  }
}
