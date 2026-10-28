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

  /**
   * A conta só é apagada com o próprio e-mail digitado, comparado como o login compara: sem
   * diferença de maiúscula nem de espaço em volta (RN-48, UC-12 2a).
   */
  public static void recusarEmailQueNaoConfere(String digitado, String daConta) {
    Optional<String> email = normalizar(digitado);
    if (email.isEmpty() || !email.equals(normalizar(daConta))) {
      throw new RegraVioladaException(
          Recusa.EMAIL_NAO_CONFERE, "email", "O e-mail digitado não é o da conta.");
    }
  }
}
