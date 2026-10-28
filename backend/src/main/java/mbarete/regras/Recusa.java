package mbarete.regras;

/** Os códigos de recusa do catálogo (convencoes-backend.md); cada um entra com quem o usa. */
public enum Recusa {
  PESO_FORA_DE_FAIXA,
  DATA_LOCAL_INVALIDA,
  NAO_ENCONTRADO,
  CORPO_MALFORMADO,
  ERRO_INTERNO,
  NAO_AUTENTICADO,
  CINTURA_FORA_DE_FAIXA,
  DATA_FUTURA,
  REGISTRO_JA_EXISTE
}
