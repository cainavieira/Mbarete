package mbarete.regras;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class Email {

  private static final Pattern FORMA = Pattern.compile("[^@\\s]+@[^@\\s]+");

  private Email() {}

  /**
   * O e-mail na forma em que a lista de convidados o guarda e o login o compara (RN-48): sem espaço
   * em volta e em minúsculas, para que a caixa digitada no convite não barre a pessoa. Vazio quando
   * o texto não tem a forma de um e-mail.
   */
  public static Optional<String> normalizar(String texto) {
    String email = texto.strip().toLowerCase(Locale.ROOT);
    return FORMA.matcher(email).matches() ? Optional.of(email) : Optional.empty();
  }
}
