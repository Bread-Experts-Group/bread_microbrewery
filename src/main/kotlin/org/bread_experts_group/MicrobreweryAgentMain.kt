package org.bread_experts_group

import org.bread_experts_group.common.Mod
import org.bread_experts_group.common.ModFactory
import org.bread_experts_group.internal.neoforge.BootstrapLauncherTransformers
import org.bread_experts_group.internal.neoforge.FMLModContainerTransformers
import org.bread_experts_group.internal.neoforge.ModListScreenTransformers
import org.bread_experts_group.internal.neoforge.ModLoaderTransformers
import org.bread_experts_group.logging.ColoredLogger
import java.lang.classfile.ClassFile
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain
import java.util.ServiceLoader
import java.util.logging.Level
import java.util.logging.Logger

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
}

object MicrobreweryAgentPremain {
	@JvmStatic
	var mods: Map<String, Mod> = mutableMapOf()
		private set

	@JvmStatic
	fun premain(agentArgs: String?, instrumentation: Instrumentation) {
		ColoredLogger.coloring = false
		common()
		val classContext = ClassFile.of()
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
					else -> return null
				} else return null

				val parsed = classContext.parse(classfileBuffer)
				val transformed = try {
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
		microbreweryPrimaryLogger.info("Initializing mods ...")
		val mutableMods = mods as MutableMap<String, Mod>
		ServiceLoader.load(ModFactory::class.java).forEach {
			Thread.ofVirtual().run {
				it.createMods().forEach { mod ->
					if (mod.identifier in mutableMods)
						throw UnsupportedOperationException("Duplicate mod identifier.")
					synchronized(mutableMods) { mutableMods[mod.identifier] = mod }
				}
			}
		}
		mods = mutableMods.toMap()
	}
}