import java.util.Properties

plugins {
	kotlin("jvm") version "2.1.20"
	id("org.jetbrains.dokka-javadoc") version "2.0.0"
	`maven-publish`
	`java-library`
	signing
}

group = "org.bread_experts_group"
version = "1.0.0"

repositories {
	mavenCentral()
	maven { url = uri("https://maven.javart.zip/") }
}

dependencies {
	implementation("org.bread_experts_group:bread_server_lib-code:2.5.3")
}

tasks.test {
	useJUnitPlatform()
}

kotlin {
	jvmToolchain(24)
}
tasks.register<Jar>("dokkaJavadocJar") {
	dependsOn(tasks.dokkaGeneratePublicationJavadoc)
	from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
	archiveClassifier.set("javadoc")
}
val localProperties = Properties().apply {
	rootProject.file("local.properties").reader().use(::load)
}
publishing {
	publications {
		create<MavenPublication>("mavenKotlin") {
			artifactId = "$artifactId-code"
			from(components["kotlin"])
			artifact(tasks.kotlinSourcesJar)
			artifact(tasks["dokkaJavadocJar"])
			pom {
				name = "Bread Microbrewery [cross-platform translatory Minecraft layer]"
				description = "Bread Experts Group distribution for a translatory Minecraft modding layer."
				url = "https://javart.zip"
				signing {
					sign(publishing.publications["mavenKotlin"])
					sign(configurations.archives.get())
				}
				licenses {
					license {
						name = "GNU General Public License v3.0"
						url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
					}
				}
				developers {
					developer {
						id = "mikoe"
						name = "Miko Elbrecht"
						email = "miko@javart.zip"
					}
				}
				scm {
					connection = "scm:git:git://github.com/Bread-Experts-Group/bread_microbrewery.git"
					developerConnection = "scm:git:ssh://git@github.com:Bread-Experts-Group/bread_microbrewery.git"
					url = "https://javart.zip"
				}
			}
		}
	}
	repositories {
		maven {
			url = uri("https://maven.javart.zip/")
			credentials {
				username = localProperties["mavenUser"] as String
				password = localProperties["mavenPassword"] as String
			}
		}
	}
}
signing {
	useGpgCmd()
	sign(publishing.publications["mavenKotlin"])
}
tasks.javadoc {
	if (JavaVersion.current().isJava9Compatible) {
		(options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
	}
}