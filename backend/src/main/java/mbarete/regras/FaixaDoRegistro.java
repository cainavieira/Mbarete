package mbarete.regras;

/**
 * As faixas do peso e da cintura (RN-03, RN-11), num lugar só: conferem o corpo das rotas, pelas
 * anotações do DTO, e as linhas do arquivo importado, que seguem as mesmas regras (RN-60).
 */
public final class FaixaDoRegistro {

  public static final String PESO_MINIMO = "20.0";

  public static final String PESO_MAXIMO = "400.0";

  public static final String CINTURA_MINIMA = "40.0";

  public static final String CINTURA_MAXIMA = "200.0";

  public static final String PESO_FORA_DE_FAIXA = "O peso deve estar entre 20,0 e 400,0 kg.";

  public static final String CINTURA_FORA_DE_FAIXA = "A cintura deve estar entre 40,0 e 200,0 cm.";

  private FaixaDoRegistro() {}
}
