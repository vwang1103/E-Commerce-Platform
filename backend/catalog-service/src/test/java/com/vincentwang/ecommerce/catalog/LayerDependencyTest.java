package com.vincentwang.ecommerce.catalog;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

@AnalyzeClasses(packages = "com.vincentwang.ecommerce.catalog", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    @ArchTest
    static final ArchRule CLEAN_ARCHITECTURE_LAYERS = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("API")
            .definedBy("..api..")
            .layer("Application")
            .definedBy("..application..")
            .layer("Domain")
            .definedBy("..domain..")
            .layer("Infrastructure")
            .definedBy("..infrastructure..")
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("API", "Infrastructure")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("API", "Application", "Infrastructure")
            .whereLayer("Infrastructure")
            .mayNotBeAccessedByAnyLayer();

    @ArchTest
    static final ArchRule DOMAIN_IS_FRAMEWORK_INDEPENDENT = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "org.apache.ibatis..",
                    "jakarta..",
                    "com.vincentwang.ecommerce.catalog.api..",
                    "com.vincentwang.ecommerce.catalog.application..",
                    "com.vincentwang.ecommerce.catalog.infrastructure..");

    @ArchTest
    static final ArchRule APPLICATION_POINTS_INWARD = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.vincentwang.ecommerce.catalog.infrastructure..");

    @ArchTest
    static final ArchRule API_DOES_NOT_DEPEND_ON_INFRASTRUCTURE = noClasses()
            .that()
            .resideInAPackage("..api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.vincentwang.ecommerce.catalog.infrastructure..");

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
