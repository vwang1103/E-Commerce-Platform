package com.vincentwang.ecommerce.order;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.vincentwang.ecommerce.order", importOptions = ImportOption.DoNotIncludeTests.class)
class PackageDependencyTest {

    @ArchTest
    static final ArchRule CONTROLLERS_DO_NOT_SKIP_THE_SERVICE = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.vincentwang.ecommerce.order.repository..",
                    "com.vincentwang.ecommerce.order.client..",
                    "com.vincentwang.ecommerce.order.messaging..",
                    "com.vincentwang.ecommerce.order.scheduler..",
                    "com.vincentwang.ecommerce.order.configuration..",
                    "com.vincentwang.ecommerce.order.id..");

    @ArchTest
    static final ArchRule SERVICES_DO_NOT_USE_TECHNOLOGY_IMPLEMENTATIONS = noClasses()
            .that()
            .resideInAPackage("..service..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.vincentwang.ecommerce.order.repository.mybatis..",
                    "com.vincentwang.ecommerce.order.scheduler..",
                    "com.vincentwang.ecommerce.order.configuration..");

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
