package mbarete.config;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * A chave do token de acesso, que chega em MBARETE_JWT_CHAVE: a privada, em PKCS#8, codificada em
 * base64 numa linha só. A pública sai dela, e o par inteiro vira um Secret só (ADR-0041).
 */
final class ChaveDoToken {

  private static final String INVALIDA =
      "MBARETE_JWT_CHAVE não é uma chave RSA privada em PKCS#8, codificada em base64.";

  private ChaveDoToken() {}

  /** A metade pública, que confere a assinatura, calculada a partir da privada. */
  static RSAPublicKey publica(String chaveEmBase64) {
    return publica(privada(chaveEmBase64));
  }

  /** As duas metades, para assinar o token na emissão. */
  static RSAKey par(String chaveEmBase64) {
    RSAPrivateCrtKey privada = privada(chaveEmBase64);
    return new RSAKey.Builder(publica(privada)).privateKey(privada).build();
  }

  private static RSAPrivateCrtKey privada(String chaveEmBase64) {
    try {
      PrivateKey privada =
          KeyFactory.getInstance("RSA")
              .generatePrivate(
                  new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(chaveEmBase64)));
      if (!(privada instanceof RSAPrivateCrtKey rsa)) {
        throw new IllegalStateException(INVALIDA);
      }
      return rsa;
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException(INVALIDA, e);
    }
  }

  private static RSAPublicKey publica(RSAPrivateCrtKey privada) {
    try {
      return (RSAPublicKey)
          KeyFactory.getInstance("RSA")
              .generatePublic(
                  new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent()));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(INVALIDA, e);
    }
  }
}
