package mbarete.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * O parâmetro `LocalDate` recebe o dia corrente do dispositivo, lido do cabeçalho `Data-Local`
 * (RN-04). Só a rota que depende de hoje o declara, e só ela recusa o cabeçalho (endpoints.md).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Hoje {}
