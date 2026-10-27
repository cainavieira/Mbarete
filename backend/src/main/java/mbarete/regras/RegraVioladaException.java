package mbarete.regras;

import org.jspecify.annotations.Nullable;

/** Recusa de regra, que o tratador global converte no formato único de erro. */
public final class RegraVioladaException extends RuntimeException {

  private final Recusa recusa;
  private final @Nullable String campo;

  public RegraVioladaException(Recusa recusa, String mensagem) {
    this(recusa, null, mensagem);
  }

  public RegraVioladaException(Recusa recusa, @Nullable String campo, String mensagem) {
    super(mensagem);
    this.recusa = recusa;
    this.campo = campo;
  }

  public Recusa recusa() {
    return recusa;
  }

  /** O campo do corpo que a recusa destaca, quando ela é sobre um. */
  public @Nullable String campo() {
    return campo;
  }
}
