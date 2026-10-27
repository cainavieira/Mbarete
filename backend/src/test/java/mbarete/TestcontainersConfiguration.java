package mbarete;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * O banco do Testcontainers, a chave do token de teste e um cliente do Google de mentira, que toda
 * subida da api precisa.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  /** O client id do Google nos testes; o id_token fabricado o traz como destinatário. */
  public static final String ID_DO_GOOGLE = "id-de-teste";

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
  }

  @Bean
  DynamicPropertyRegistrar variaveisDaApi() {
    return registro -> {
      registro.add("MBARETE_JWT_CHAVE", ChaveDeTeste::emBase64);
      registro.add("MBARETE_GOOGLE_ID", () -> ID_DO_GOOGLE);
      registro.add("MBARETE_GOOGLE_SEGREDO", () -> "segredo-de-teste");
    };
  }
}
