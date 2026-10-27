package mbarete.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O relógio do servidor, em UTC, contra o qual a data do dispositivo se confere (endpoints.md). Ele
 * nunca diz que dia é hoje: o dia corrente é o do dispositivo (RN-04, ADR-0018).
 */
@Configuration
public class RelogioConfig {

  @Bean
  public Clock relogio() {
    return Clock.systemUTC();
  }
}
