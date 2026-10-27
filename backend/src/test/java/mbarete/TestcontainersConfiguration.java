package mbarete;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** O banco do Testcontainers e a chave do token de teste, que toda subida da api precisa. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
  }

  @Bean
  DynamicPropertyRegistrar variaveisDaApi() {
    return registro -> registro.add("MBARETE_JWT_CHAVE", ChaveDeTeste::emBase64);
  }
}
