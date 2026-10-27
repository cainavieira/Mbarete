package mbarete.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final HojeResolver hoje;

  public WebConfig(HojeResolver hoje) {
    this.hoje = hoje;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvedores) {
    resolvedores.add(hoje);
  }
}
