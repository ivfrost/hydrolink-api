package dev.ivfrost.hydro_backend;

import com.tngtech.archunit.base.DescribedPredicate;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

  // AppDataInit is dev-only seeding infrastructure that must touch repositories and
  // entities across modules. It sits in the root package and is intentionally
  // excluded from the module model rather than forced behind module boundaries.
  ApplicationModules modules = ApplicationModules.of(
      HydroApiApplication.class,
      DescribedPredicate.describe(
          "AppDataInit (out-of-band dev seeding)",
          cls -> !cls.getName().equals(AppDataInit.class.getName())));

  @Test
  void verifiesModularStructure() {
    modules.verify();
  }

  @Test
  void createModuleDocumentation() {
    new Documenter(modules).writeDocumentation();
  }
}
