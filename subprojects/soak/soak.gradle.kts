import org.gradle.gradlebuild.unittestandcompile.ModuleType

plugins {
    gradlebuild.classycle
    `kotlin-library`
}

dependencies {
    testFixturesApi(project(":internalIntegTesting"))
    testImplementation(project(":kotlinDslTestFixtures"))
}

gradlebuildJava {
    moduleType = ModuleType.INTERNAL
}

testFixtures {
    from(":core")
}

tasks.matching { it.name in listOf("integTest", "java9IntegTest") }.configureEach {
    require(this is Test)
    options {
        require(this is JUnitOptions)
        excludeCategories("org.gradle.soak.categories.SoakTest")
    }
}

tasks.register("soakTest", org.gradle.gradlebuild.test.integrationtests.SoakTest::class) {
    val integTestSourceSet = sourceSets.integTest.get()
    testClassesDirs = integTestSourceSet.output.classesDirs
    classpath = integTestSourceSet.runtimeClasspath
    systemProperty("org.gradle.soaktest", "true")
    options {
        require(this is JUnitOptions)
        includeCategories("org.gradle.soak.categories.SoakTest")
    }
}
