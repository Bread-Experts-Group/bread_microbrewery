package org.bread_experts_group.mappings

import org.bread_experts_group.coder.json.JSONElement.Companion.json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class MinecraftVersionManifest(
	val latest: ReleaseSnapshotPair,
	val versions: Array<Version>
) {
	data class ReleaseSnapshotPair(
		val release: String,
		val snapshot: String
	)

	data class Version(
		val id: String,
		val time: ZonedDateTime,
		val releaseTime: ZonedDateTime,
		val type: String,
		val url: URI
	)

	companion object {
		fun read(
			client: HttpClient,
			from: URI = URI("https://piston-meta.mojang.com/mc/game/version_manifest.json")
		): MinecraftVersionManifest {
			val handle = client.send(
				HttpRequest.newBuilder(from).build(),
				HttpResponse.BodyHandlers.ofInputStream()
			)
			return json(handle.body()).asObject {
				val latest = inObject("latest") {
					ReleaseSnapshotPair(
						withString("release"),
						withString("snapshot")
					)
				}
				val versions = inArray("versions") {
					it.asObject {
						Version(
							withString("id"),
							ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(withString("time"))),
							ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(withString("releaseTime"))),
							withString("type"),
							URI(withString("url"))
						)
					}
				}
				MinecraftVersionManifest(latest, versions)
			}
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MinecraftVersionManifest

		if (latest != other.latest) return false
		if (!versions.contentEquals(other.versions)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = latest.hashCode()
		result = 31 * result + versions.contentHashCode()
		return result
	}
}