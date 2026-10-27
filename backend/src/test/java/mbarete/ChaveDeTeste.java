package mbarete;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Chave gerada a cada execução dos testes: tokens assinados de verdade, sem segredo no repositório.
 */
public final class ChaveDeTeste {

  private static final KeyPair PAR = gerar();
  private static final KeyPair OUTRO_PAR = gerar();

  private ChaveDeTeste() {}

  /** O valor de MBARETE_JWT_CHAVE nos testes. */
  public static String emBase64() {
    return Base64.getEncoder().encodeToString(PAR.getPrivate().getEncoded());
  }

  public static String token(int idUsuario, Instant expiraEm) {
    return assinar(PAR, idUsuario, expiraEm);
  }

  public static String tokenDeOutraChave(int idUsuario, Instant expiraEm) {
    return assinar(OUTRO_PAR, idUsuario, expiraEm);
  }

  private static String assinar(KeyPair par, int idUsuario, Instant expiraEm) {
    RSAKey chave =
        new RSAKey.Builder((RSAPublicKey) par.getPublic()).privateKey(par.getPrivate()).build();
    NimbusJwtEncoder assinador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(chave)));
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(String.valueOf(idUsuario))
            .issuedAt(expiraEm.minusSeconds(900))
            .expiresAt(expiraEm)
            .build();
    return assinador
        .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
        .getTokenValue();
  }

  private static KeyPair gerar() {
    try {
      KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
      gerador.initialize(2048);
      return gerador.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
