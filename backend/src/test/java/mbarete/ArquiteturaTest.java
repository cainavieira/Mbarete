package mbarete;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** A regra de dependência, conferida em vez de prometida (ADR-0019, arquitetura.md). */
@AnalyzeClasses(packages = "mbarete", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

  @ArchTest
  static final ArchRule regrasSoImportamABibliotecaPadrao =
      classes()
          .that()
          .resideInAPackage("mbarete.regras..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("java..", "mbarete.regras..");

  @ArchTest
  static final ArchRule controllerNaoUsaRepository =
      noClasses()
          .that()
          .resideInAPackage("mbarete.controller..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("mbarete.repository..");
}
