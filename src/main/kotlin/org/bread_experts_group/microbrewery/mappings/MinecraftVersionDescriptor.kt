package org.bread_experts_group.microbrewery.mappings

import org.bread_experts_group.coder.fixed.json.JSONArray
import org.bread_experts_group.coder.fixed.json.JSONElement.Companion.json
import org.bread_experts_group.coder.fixed.json.JSONObject
import org.bread_experts_group.coder.fixed.json.JSONString
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path

data class MinecraftVersionDescriptor(
	val arguments: ArgumentsDescriptor,
	val assetBase: AssetBaseDescriptor,
	val assetVersion: String,
	val complianceLevel: Int,
	val downloadLocations: Map<String, DownloadLocationDescriptor>,
	val version: String,
	val javaVersion: JavaVersionDescriptor,
	val libraries: Array<LibraryDescriptor>,
	val logging: Map<String, LoggingDescriptor>,
	val mainClass: String,
	val minimumLauncherVersion: Int,
	val releaseTime: ZonedDateTime,
	val time: ZonedDateTime,
	val type: String
) {
	abstract class Argument
	data class PlainArgument(val value: String) : Argument()

	abstract class ConditionalArgumentRule
	data class MinecraftArgumentRule(
		val action: String,
		val features: Map<String, Boolean>
	) : ConditionalArgumentRule()
	data class JVMArgumentRuleOSRules(
		val name: String?,
		val version: String?,
		val arch: String?
	)
	data class JVMArgumentRule(
		val action: String,
		val features: Map<String, Boolean>?,
		val os: JVMArgumentRuleOSRules
	) : ConditionalArgumentRule()

	data class ConditionalArgument(
		val rules: Array<ConditionalArgumentRule>,
		val arguments: Array<String>
	) : Argument() {
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as ConditionalArgument

			if (!rules.contentEquals(other.rules)) return false
			if (!arguments.contentEquals(other.arguments)) return false

			return true
		}

		override fun hashCode(): Int {
			var result = rules.contentHashCode()
			result = 31 * result + arguments.contentHashCode()
			return result
		}
	}

	data class ArgumentsDescriptor(
		val game: Array<Argument>,
		val jvm: Array<Argument>
	) {
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as ArgumentsDescriptor

			if (!game.contentEquals(other.game)) return false
			if (!jvm.contentEquals(other.jvm)) return false

			return true
		}

		override fun hashCode(): Int {
			var result = game.contentHashCode()
			result = 31 * result + jvm.contentHashCode()
			return result
		}
	}

	interface DownloadLocation {
		val sha1: String
		val size: Long
		val url: URI
	}

	interface IDBearer {
		val id: String
	}

	data class DownloadLocationDescriptor(
		override val sha1: String,
		override val size: Long,
		override val url: URI
	) : DownloadLocation

	data class DownloadLocationDescriptorWithID(
		override val id: String,
		override val sha1: String,
		override val size: Long,
		override val url: URI
	) : DownloadLocation, IDBearer

	data class AssetBaseDescriptor(
		override val id: String,
		override val sha1: String,
		override val size: Long,
		val totalSize: Long,
		override val url: URI
	) : DownloadLocation, IDBearer

	data class JavaVersionDescriptor(
		val component: String,
		val version: Int
	)

	data class ExtractListDescriptor(
		val excludePaths: Array<String>
	) {
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as ExtractListDescriptor

			return excludePaths.contentEquals(other.excludePaths)
		}

		override fun hashCode(): Int {
			return excludePaths.contentHashCode()
		}
	}

	data class ArtifactLocationDescriptor(
		val path: Path,
		override val sha1: String,
		override val size: Long,
		override val url: URI
	) : DownloadLocation

	data class LibraryDownloadsDescriptor(
		val artifact: ArtifactLocationDescriptor,
		val classifiers: Map<String, ArtifactLocationDescriptor>?
	)

	data class LibraryDescriptor(
		val mavenName: String,
		val downloads: LibraryDownloadsDescriptor?,
		val url: URI?,
		val extract: ExtractListDescriptor?,
		val rules: Array<JVMArgumentRule>?
	) {
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as LibraryDescriptor

			@Suppress("DuplicatedCode")
			if (mavenName != other.mavenName) return false
			if (!rules.contentEquals(other.rules)) return false
			if (downloads != other.downloads) return false
			if (url != other.url) return false
			if (extract != other.extract) return false

			return true
		}

		override fun hashCode(): Int {
			var result = mavenName.hashCode()
			result = 31 * result + downloads.hashCode()
			result = 31 * result + url.hashCode()
			result = 31 * result + extract.hashCode()
			result = 31 * result + rules.contentHashCode()
			return result
		}
	}

	data class LoggingDescriptor(
		val argument: String,
		val type: String,
		val file: DownloadLocationDescriptorWithID
	)

	companion object {
		fun read(
			client: HttpClient,
			from: URI
		): MinecraftVersionDescriptor {
			val handle = client.send(
				HttpRequest.newBuilder(from).build(),
				HttpResponse.BodyHandlers.ofInputStream()
			)
			fun JSONObject.readJVMArgumentRule() = JVMArgumentRule(
				withString("action"),
				(entries["features"] as? JSONObject)?.entries?.mapValues {
						v -> v.value.asBoolean { value }
				},
				inObject("os") {
					JVMArgumentRuleOSRules(
						(entries["name"] as? JSONString)?.value,
						(entries["version"] as? JSONString)?.value,
						(entries["arch"] as? JSONString)?.value
					)
				}
			)

			return json(handle.body()).asObject {
				MinecraftVersionDescriptor(
					inObject("arguments") {
						ArgumentsDescriptor(
							inArray("game") { gameArgument ->
								if (gameArgument is JSONString) PlainArgument(gameArgument.value)
								else gameArgument.asObject {
									val arguments = entries.getValue("value")
									ConditionalArgument(
										inArray("rules") { rule ->
											rule.asObject {
												MinecraftArgumentRule(
													withString("action"),
													withObject("features").entries.mapValues {
															v -> v.value.asBoolean { value }
													}
												)
											}
										},
										if (arguments is JSONString) arrayOf(arguments.value)
										else arguments.asArray { mapped { v -> v.asString { value } } }
									)
								}
							},
							inArray("jvm") { jvmArgument ->
								if (jvmArgument is JSONString) PlainArgument(jvmArgument.value)
								else jvmArgument.asObject {
									val arguments = entries.getValue("value")
									ConditionalArgument(
										inArray("rules") { rule ->
											rule.asObject { this.readJVMArgumentRule() }
										},
										if (arguments is JSONString) arrayOf(arguments.value)
										else arguments.asArray { mapped { v -> v.asString { value } } }
									)
								}
							}
						)
					},
					inObject("assetIndex") {
						AssetBaseDescriptor(
							withString("id"),
							withString("sha1"),
							withNumber("size").toLong(),
							withNumber("totalSize").toLong(),
							URI(withString("url"))
						)
					},
					withString("assets"),
					withNumber("complianceLevel").toInt(),
					withObject("downloads").entries.mapValues { (_, value) ->
						value.asObject {
							DownloadLocationDescriptor(
								withString("sha1"),
								withNumber("size").toLong(),
								URI(withString("url"))
							)
						}
					},
					withString("id"),
					inObject("javaVersion") {
						JavaVersionDescriptor(
							withString("component"),
							withNumber("majorVersion").toInt()
						)
					},
					inArray("libraries") {
						it.asObject {
							LibraryDescriptor(
								withString("name"),
								inObject("downloads") {
									LibraryDownloadsDescriptor(
										inObject("artifact") {
											ArtifactLocationDescriptor(
												Path(withString("path")),
												withString("sha1"),
												withNumber("size").toLong(),
												URI(withString("url"))
											)
										},
										(entries["classifiers"] as? JSONObject)?.entries?.mapValues { (_, classifier) ->
											classifier.asObject {
												ArtifactLocationDescriptor(
													Path(withString("path")),
													withString("sha1"),
													withNumber("size").toLong(),
													URI(withString("url"))
												)
											}
										}
									)
								},
								(entries["url"] as? JSONString)?.let { url -> URI(url.value) },
								(entries["extract"] as? JSONObject)?.asObject {
									ExtractListDescriptor(
										withArray("exclude").mapped { e -> e.asString { value } }
									)
								},
								(entries["rules"] as? JSONArray)?.mapped { rule ->
									rule.asObject { this.readJVMArgumentRule() }
								}
							)
						}
					},
					withObject("logging").entries.mapValues { (_, value) ->
						value.asObject {
							LoggingDescriptor(
								withString("argument"),
								withString("type"),
								inObject("file") {
									DownloadLocationDescriptorWithID(
										withString("id"),
										withString("sha1"),
										withNumber("size").toLong(),
										URI(withString("url"))
									)
								}
							)
						}
					},
					withString("mainClass"),
					withNumber("minimumLauncherVersion").toInt(),
					ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(withString("releaseTime"))),
					ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(withString("time"))),
					withString("type")
				)
			}
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MinecraftVersionDescriptor

		if (complianceLevel != other.complianceLevel) return false
		if (minimumLauncherVersion != other.minimumLauncherVersion) return false
		if (arguments != other.arguments) return false
		if (!libraries.contentEquals(other.libraries)) return false
		if (assetBase != other.assetBase) return false
		if (assetVersion != other.assetVersion) return false
		if (downloadLocations != other.downloadLocations) return false
		if (version != other.version) return false
		if (javaVersion != other.javaVersion) return false
		if (logging != other.logging) return false
		if (mainClass != other.mainClass) return false
		if (releaseTime != other.releaseTime) return false
		if (time != other.time) return false
		if (type != other.type) return false

		return true
	}

	override fun hashCode(): Int {
		var result = complianceLevel
		result = 31 * result + minimumLauncherVersion
		result = 32 * result + arguments.hashCode()
		result = 31 * result + assetBase.hashCode()
		result = 32 * result + assetVersion.hashCode()
		result = 32 * result + downloadLocations.hashCode()
		result = 31 * result + version.hashCode()
		result = 32 * result + javaVersion.hashCode()
		result = 31 * result + libraries.contentHashCode()
		result = 33 * result + logging.hashCode()
		result = 34 * result + mainClass.hashCode()
		result = 35 * result + releaseTime.hashCode()
		result = 36 * result + time.hashCode()
		result = 37 * result + type.hashCode()
		return result
	}
}