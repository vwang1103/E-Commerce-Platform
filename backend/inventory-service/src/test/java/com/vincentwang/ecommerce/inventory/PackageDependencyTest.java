package com.vincentwang.ecommerce.inventory;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.vincentwang.ecommerce.inventory", importOptions = ImportOption.DoNotIncludeTests.class)
class PackageDependencyTest {

    @ArchTest
    static final ArchRule CONTROLLERS_DO_NOT_SKIP_THE_SERVICE = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.vincentwang.ecommerce.inventory.repository..",
                    "com.vincentwang.ecommerce.inventory.redis..",
                    "com.vincentwang.ecommerce.inventory.scheduler..",
                    "com.vincentwang.ecommerce.inventory.configuration..");

    @ArchTest
    static final ArchRule SERVICES_DO_NOT_USE_TECHNOLOGY_IMPLEMENTATIONS = noClasses()
            .that()
            .resideInAPackage("..service..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.vincentwang.ecommerce.inventory.repository.mybatis..",
                    "com.vincentwang.ecommerce.inventory.scheduler..",
                    "com.vincentwang.ecommerce.inventory.configuration..");

    @ArchTest
    static final ArchRule MODELS_ARE_FRAMEWORK_INDEPENDENT = noClasses()
            .that()
            .resideInAPackage("..model..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "org.apache.ibatis..", "jakarta..");

    @ArchTest
    static final ArchRule INTERFACES_USE_I_PREFIX =
            classes().that().areInterfaces().should().haveSimpleNameStartingWith("I");

    @ArchTest
    static final ArchRule ABSTRACT_CLASSES_USE_A_PREFIX = classes()
            .that()
            .haveModifier(JavaModifier.ABSTRACT)
            .and()
            .areNotInterfaces()
            .should()
            .haveSimpleNameStartingWith("A")
            .allowEmptyShould(true);
}
