import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.7"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.25"
}
val springCloudVersion by extra("2024.0.2")

group = "com.fourka"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {

	// spring
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
	implementation("org.springframework.cloud:spring-cloud-starter-config")
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// spring security
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

	// kotlin
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")
	//runtimeOnly("org.postgresql:r2dbc-postgresql")

	// database
	implementation("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")

	// test
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testImplementation("io.mockk:mockk:1.13.11")
	testImplementation("com.ninja-squad:springmockk:4.0.2")
	testImplementation(kotlin("test"))

	// swagger
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.9")

	//json
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}
dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "21"
}

tasks.withType<Test> {
	useJUnitPlatform()
}