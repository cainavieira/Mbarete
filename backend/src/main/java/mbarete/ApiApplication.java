package mbarete;

import java.util.Arrays;
import java.util.stream.Stream;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {

  private static final String[] MODO_MIGRAR = {
    "--spring.flyway.enabled=true", "--spring.main.web-application-type=none"
  };

  public static void main(String[] args) {
    if (args.length > 0 && args[0].equals("migrar")) {
      // Aplica as migrações e termina, sem subir a api (ADR-0040)
      String[] tudo =
          Stream.concat(Arrays.stream(MODO_MIGRAR), Arrays.stream(args).skip(1))
              .toArray(String[]::new);
      SpringApplication.run(ApiApplication.class, tudo).close();
      return;
    }
    SpringApplication.run(ApiApplication.class, args);
  }
}
