package mbarete.config;

import static mbarete.regras.DataDoDispositivo.lerDataLocal;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Entrega o dia do cabeçalho `Data-Local` ao parâmetro marcado com {@link Hoje}. */
@Component
public class HojeResolver implements HandlerMethodArgumentResolver {

  private final Clock relogio;

  public HojeResolver(Clock relogio) {
    this.relogio = relogio;
  }

  @Override
  public boolean supportsParameter(MethodParameter parametro) {
    return parametro.hasParameterAnnotation(Hoje.class)
        && parametro.getParameterType().equals(LocalDate.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter parametro,
      ModelAndViewContainer mavContainer,
      NativeWebRequest requisicao,
      WebDataBinderFactory binderFactory) {
    return lerDataLocal(requisicao.getHeader("Data-Local"), LocalDate.now(relogio));
  }
}
