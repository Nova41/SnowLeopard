plugins {
    java
    `build-scan`
    `java-test-fixtures`
    pmd
    checkstyle
    id("com.github.spotbugs") version "1.7.1"
}

group = "com.github.d33vil.snowleopard"
version = "2.0.0"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
    maven { url = uri("https://repo.destroystokyo.com/repository/maven-public/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.9.0")
    compile("commons-io:commons-io:2.6")
    compile("org.spigotmc:spigot-api:1.14.4-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok", "lombok", "1.18.8")
    annotationProcessor("org.projectlombok", "lombok", "1.18.8")
    testCompile("org.mockito", "mockito-all", "1.9.5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.1.0")
}

tasks {
    val sourceSets: SourceSetContainer by project

    val javadocJar by registering(Jar::class) {
        dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
        archiveClassifier.set("javadoc")
        from(javadoc)
    }

    val fatJar by registering(Jar::class) {
        dependsOn(jar)
        archiveClassifier.set("shaded")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get()
                .onEach { println("Add from dependencies: ${it.name}") }
                .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("Add from sources: ${it.name}") }
        from(sourcesMain.output)
    }

    artifacts {
        add("archives", javadocJar)
    }

    build {
        dependsOn(fatJar)
        dependsOn(javadocJar)

        fatJar.get().mustRunAfter(clean)
    }

    checkstyle {
        checkstyleTest.get().enabled = false
        toolVersion = "8.19"
    }

    checkstyleMain {
        configFile = file("$rootDir/config/checkstyle/google_checks.xml")
        configProperties = mapOf("config_loc" to "${rootProject.projectDir}/config/checkstyle")
    }

    spotbugs {
        toolVersion = "4.0.0-beta1"
    }

    spotbugsMain {
        reports {
            html.isEnabled = true
            xml.isEnabled = false
        }
    }

    pmd {
        pmdTest.get().enabled = false
    }
    pmdMain {
        ignoreFailures = true
        ruleSetConfig = resources.text.fromFile(file("${rootProject.projectDir}/config/pmd/ruleset.xml"))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
