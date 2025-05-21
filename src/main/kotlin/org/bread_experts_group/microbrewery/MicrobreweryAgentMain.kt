package org.bread_experts_group.microbrewery

import org.bread_experts_group.microbrewery.common.Mod
import org.bread_experts_group.microbrewery.common.ModFactory
import org.bread_experts_group.microbrewery.internal.minecraft.BuiltInRegistriesTransformers
import org.bread_experts_group.microbrewery.internal.neoforge.BootstrapLauncherTransformers
import org.bread_experts_group.microbrewery.internal.neoforge.FMLModContainerTransformers
import org.bread_experts_group.microbrewery.internal.neoforge.ModListScreenTransformers
import org.bread_experts_group.microbrewery.internal.neoforge.ModLoaderTransformers
import org.bread_experts_group.microbrewery.logging.ColoredLogger
import java.lang.classfile.ClassFile
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain
import java.util.ServiceLoader
import java.util.concurrent.CountDownLatch
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors

val microbreweryPrimaryLogger: Logger = ColoredLogger.newLogger("Bread Experts Group Microbrewery")

fun common() {
	Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted { ColoredLogger.flush() })
	microbreweryPrimaryLogger.info("Microbrewery is active!")
}

fun main() {
	common()
	microbreweryPrimaryLogger.info("Did you run this correctly?")
	// CHRIS: If you wanna look at this, try and uhh... maybe make it bind to Prism launcher someway?
	//        If you need any help ask me. Just need this automated; patching prism's console to us would also be cool
	ColoredLogger.stackTraceOnlyInLoggingModule = true
}

@Suppress("unused")
object MicrobreweryAgentPremain {
	@JvmStatic
	var mods: Map<String, Mod> = mutableMapOf()
		private set

	@JvmStatic
	fun premain(agentArgs: String?, instrumentation: Instrumentation) {
		ColoredLogger.coloring = false
		common()

		// TODO! Mappings
//		val localExecutionFile = File(this::class.java.protectionDomain.codeSource.location.path).toPath()
//		if (localExecutionFile.isRegularFile() && localExecutionFile.extension == "jar") {
//			microbreweryPrimaryLogger.info("Local JAR modification will occur at file [$localExecutionFile]")
//		} else if (localExecutionFile.isDirectory()) {
//			microbreweryPrimaryLogger.info("Local JAR modification will occur at directory [$localExecutionFile]")
//		}

//		val client = HttpClient.newHttpClient()
//		val versionManifest = MinecraftVersionManifest.read(client).versions.first { it.id == "1.21.1" }
//		microbreweryPrimaryLogger.info(MinecraftVersionDescriptor.read(client, versionManifest.url).downloadLocations.getValue("client_mappings").url.toString())

		val classContext = ClassFile.of()
		val waitingForMods = CountDownLatch(1)
		instrumentation.addTransformer(object : ClassFileTransformer {
			override fun transform(
				loader: ClassLoader?,
				className: String?,
				classBeingRedefined: Class<*>?,
				protectionDomain: ProtectionDomain?,
				classfileBuffer: ByteArray?
			): ByteArray? {
				val transformer = if (
					className != null &&
					classfileBuffer != null
				) when (className) {
					"cpw/mods/bootstraplauncher/BootstrapLauncher" -> BootstrapLauncherTransformers.runTransform
					"net/neoforged/fml/ModLoader" -> ModLoaderTransformers.gatherAndInitializeModsTransform
					"net/neoforged/fml/javafmlmod/FMLModContainer" -> FMLModContainerTransformers.initTransform
					"net/neoforged/neoforge/client/gui/ModListScreen" -> ModListScreenTransformers.updateCacheTransform
					"net/minecraft/core/registries/BuiltInRegistries" -> BuiltInRegistriesTransformers.clinitTransform
					else -> return null
				} else return null

				val parsed = classContext.parse(classfileBuffer)
				val transformed = try {
					waitingForMods.await()
					classContext.transformClass(
						parsed,
						transformer
					)
				} catch (e: Throwable) {
					microbreweryPrimaryLogger.log(Level.SEVERE, e) { "Error during transformation!" }
					throw e
				}
				microbreweryPrimaryLogger.info(transformed.size.toString())
				return transformed
			}
		}, true)
		val modInitStart = System.nanoTime()
		mods = ServiceLoader.load(ModFactory::class.java)
			.toList()
			.also { microbreweryPrimaryLogger.info { "Mod initialization: [${it.size}] factories" } }
			.parallelStream()
			.map { factory ->
				factory
					.createMods()
					.also { microbreweryPrimaryLogger.info { "Factory [${factory::class.simpleName}] initialization: [${it.size}] mods" } }
					.parallelStream()
					.map { it.join() }
			}
			.flatMap(Function.identity())
			.collect(Collectors.toUnmodifiableMap(
				{ it.identifier },
				Function.identity())
			)
		microbreweryPrimaryLogger.info { "Mod initialization: [${mods.size}] mods initialized [${System.nanoTime() - modInitStart} ns]" }
		waitingForMods.countDown()
	}
}