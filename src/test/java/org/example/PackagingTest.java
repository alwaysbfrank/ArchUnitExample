package org.example;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.apache.commons.lang3.StringUtils;
import org.junit.runner.RunWith;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toList;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "org.example", importOptions = ImportOption.DoNotIncludeTests.class)
public class PackagingTest {

    @ArchTest
    public static ArchRule internal_packages_do_not_depend_on_each_other =
            slices()
                    .matching("..(**).internal..")
                    .should()
                    .notDependOnEachOther();

    @ArchTest
    public static ArchRule all_classes_are_internal_or_api =
            classes()
                    .should()
                    .resideInAnyPackage("..api", "..internal");

    @ArchTest
    public static ArchRule api_packages_are_not_in_other_api_or_internal_packages =
            classes()
                    .that()
                    .resideInAPackage("..api")
                    .should()
                    .resideOutsideOfPackages("..internal..api", "..api..api");

    @ArchTest
    public static ArchRule internal_packages_are_not_in_other_api_or_internal_packages =
            classes()
                    .that()
                    .resideInAPackage("..internal")
                    .should()
                    .resideOutsideOfPackages("..internal..internal", "..api..internal");

    @ArchTest
    public static ArchRule api_classes_do_not_depend_on_internal_packages_of_other_packages =
            noClasses()
                    .that()
                    .resideInAPackage("..api")
                    .should(accessClassesThatResideInOtherPackageInternalPackage());

    @ArchTest
    public static ArchRule classes_do_not_depend_on_packages_within_other_packages =
            noClasses()
                    .should(accessClassesTooDeepInOtherPackages());

    private static ArchCondition<JavaClass> accessClassesThatResideInOtherPackageInternalPackage() {
        return new ArchCondition<>("access other internal packages") {
            @Override
            public void check(final JavaClass clazz, final ConditionEvents events) {
                var myPackageInternalPackage = addInternalPackageNamePart(stripApiPackageNamePart(clazz.getPackageName()));
                final List<Dependency> dependenciesInOtherPackagesInternalPackage = clazz.getDirectDependenciesFromSelf().stream()
                        .filter(it -> !it.getTargetClass().getPackageName().isEmpty())
                        .filter(it -> it.getTargetClass().getPackageName().endsWith(INTERNAL_PACKAGE_SUFFIX))
                        .filter(it -> !it.getTargetClass().getPackageName().equals(myPackageInternalPackage))
                        .collect(toList());

                final boolean satisfied = !dependenciesInOtherPackagesInternalPackage.isEmpty();

                final StringBuilder messageBuilder = new StringBuilder(format("%s to other packages' internal package found within class %s",
                        satisfied ? "Access" : "No access",
                        clazz.getName()));
                for (Dependency dependency : dependenciesInOtherPackagesInternalPackage) {
                    messageBuilder.append(lineSeparator()).append(dependency.getDescription());
                }

                events.add(new SimpleConditionEvent(clazz, satisfied, messageBuilder.toString()));
            }
        };
    }

    private static final String INTERNAL_PACKAGE_SUFFIX = ".internal";
    private static final String API_PACKAGE_SUFFIX = ".api";

    private static String stripApiPackageNamePart(String packageName) {
        var apiSuffixLength = API_PACKAGE_SUFFIX.length();

        if (packageName.length() <= apiSuffixLength) {
            return packageName;
        }

        return packageName.substring(0, packageName.length() - apiSuffixLength);
    }

    private static String addInternalPackageNamePart(String packageName) {
        return packageName + INTERNAL_PACKAGE_SUFFIX;
    }

    private static ArchCondition<JavaClass> accessClassesTooDeepInOtherPackages() {
        return new ArchCondition<>("access not api") {
            @Override
            public void check(final JavaClass clazz, final ConditionEvents events) {
                var myPackageInternalPackage = addInternalPackageNamePart(stripApiPackageNamePart(clazz.getPackageName()));
                final List<Dependency> dependenciesInOtherPackagesInternalPackage = clazz.getDirectDependenciesFromSelf().stream()
                        .filter(it -> !it.getTargetClass().getPackageName().isEmpty())
                        .filter(it -> isTooDeepInDependency(clazz.getPackageName(), it.getTargetClass().getPackageName()))
                        .collect(toList());
                final boolean satisfied = !dependenciesInOtherPackagesInternalPackage.isEmpty();

                final StringBuilder messageBuilder = new StringBuilder(format("%s to other packages' non-api package found within class %s",
                        satisfied ? "Access" : "No access",
                        clazz.getName()));
                for (Dependency dependency : dependenciesInOtherPackagesInternalPackage) {
                    messageBuilder.append(lineSeparator()).append(dependency.getDescription());
                }

                events.add(new SimpleConditionEvent(clazz, satisfied, messageBuilder.toString()));
            }
        };
    }

    private static boolean isTooDeepInDependency(String myPackage, String dependencyPackage) {
        var commonPart = StringUtils.getCommonPrefix(myPackage, dependencyPackage);
        var dependencyPackageDifference = StringUtils.stripStart(dependencyPackage, commonPart);
        return dependencyPackageDifference.split("\\.").length > 2;
    }
}
