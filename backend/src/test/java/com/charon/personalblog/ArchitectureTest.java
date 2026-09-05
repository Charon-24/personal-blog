package com.charon.personalblog;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {
    @Test
    void controllersDoNotAccessAnotherModulesInternalMapper() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.charon.personalblog");
        noClasses().that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Mapper")
                .check(classes);
    }
}
