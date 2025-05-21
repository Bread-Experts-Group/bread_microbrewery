package org.bread_experts_group.microbrewery.internal.neoforge

import org.bread_experts_group.microbrewery.internal.javaCollection
import org.bread_experts_group.microbrewery.internal.javaList
import org.bread_experts_group.microbrewery.internal.javaMap
import org.bread_experts_group.microbrewery.internal.javaObject
import org.bread_experts_group.microbrewery.internal.javaString
import org.bread_experts_group.microbrewery.internal.moduleLayer
import org.bread_experts_group.microbrewery.internal.mutableList
import org.bread_experts_group.microbrewery.internal.pushMutableList
import org.bread_experts_group.microbrewery.internal.pushString
import org.bread_experts_group.microbrewery.logging.ColoredLogger
import java.lang.classfile.ClassTransform
import java.lang.classfile.CodeBuilder
import java.lang.classfile.Opcode
import java.lang.classfile.TypeKind
import java.lang.classfile.instruction.InvokeInstruction
import java.lang.constant.ClassDesc
import java.lang.constant.MethodTypeDesc
import java.lang.reflect.AccessFlag
import java.util.logging.Logger

object ModLoaderTransformers {
	val logger: Logger = ColoredLogger.newLogger("BEG-MB NeoForge [ModLoader] Internal Transformers")

	val begBmMod: ClassDesc = ClassDesc.of("org.bread_experts_group.common.Mod")
	val fmlModContainer: ClassDesc = ClassDesc.of("net.neoforged.fml.javafmlmod.FMLModContainer")
	val iModInfo: ClassDesc = ClassDesc.of("net.neoforged.neoforgespi.language.IModInfo")
	val iModFileInfo: ClassDesc = ClassDesc.of("net.neoforged.neoforgespi.language.IModFileInfo")
	val modInfo: ClassDesc = ClassDesc.of("net.neoforged.fml.loading.moddiscovery.ModInfo")
	val modFileInfo: ClassDesc = ClassDesc.of("net.neoforged.fml.loading.moddiscovery.ModFileInfo")
	val iConfigurable: ClassDesc = ClassDesc.of("net.neoforged.neoforgespi.language.IConfigurable")
	val modFileScanData: ClassDesc = ClassDesc.of("net.neoforged.neoforgespi.language.ModFileScanData")
	val config: ClassDesc = ClassDesc.of("com.electronwill.nightconfig.core.Config")
	val unmodifiableConfig: ClassDesc = ClassDesc.of("com.electronwill.nightconfig.core.UnmodifiableConfig")
	val nightConfigWrapper: ClassDesc = ClassDesc.of("net.neoforged.fml.loading.moddiscovery.NightConfigWrapper")

	fun CodeBuilder.setConfig() {
		this.invokeinterface(
			config,
			"set",
			MethodTypeDesc.of(javaObject, javaString, javaObject)
		) // O
		this.pop()
	}

	val gatherAndInitializeModsTransform: ClassTransform = ClassTransform.transformingMethodBodies(
		{ it.methodName().stringValue() == "gatherAndInitializeMods" && it.flags().has(AccessFlag.STATIC) },
		{ codeBuilder, codeElement ->
			if (
				codeElement is InvokeInstruction &&
				codeElement.opcode() == Opcode.INVOKEVIRTUAL &&
				codeElement.owner().asInternalName() == "net/neoforged/fml/ModList" &&
				codeElement.name().stringValue() == "setLoadedMods"
			) {
				logger.info("[gatherAndInitializeMods] modification ...\n$codeElement")
				codeBuilder.mutableList() // M
				// Loop Machinery
				codeBuilder.invokestatic(
					ClassDesc.of("org.bread_experts_group.MicrobreweryAgentPremain"),
					"getMods",
					MethodTypeDesc.of(javaMap)
				) // M R
				codeBuilder.dup() // M R R
				codeBuilder.invokeinterface(
					javaMap,
					"values",
					MethodTypeDesc.of(javaCollection)
				) // M R V
				codeBuilder.invokeinterface(
					javaCollection,
					"toArray",
					MethodTypeDesc.ofDescriptor("()[Ljava/lang/Object;")
				) // M R T
				val allocatedModEntry = codeBuilder.allocateLocal(TypeKind.REFERENCE)
				val allocatedArray = codeBuilder.allocateLocal(TypeKind.REFERENCE)
				codeBuilder.astore(allocatedArray) // M R
				codeBuilder.invokeinterface(
					javaMap,
					"size",
					MethodTypeDesc.ofDescriptor("()I")
				) // M A
				val containerEntryEnd = codeBuilder.newLabel()
				val containerEntryLoop = codeBuilder.newBoundLabel()
				codeBuilder.dup() // M A A
				codeBuilder.ifeq(containerEntryEnd) // M A
				codeBuilder.iconst_1() // M A D
				codeBuilder.isub() // M A
				codeBuilder.dup() // M A A
				codeBuilder.aload(allocatedArray) // M A A T
				codeBuilder.swap() // M A T A
				codeBuilder.aaload() // M A G
				codeBuilder.checkcast(begBmMod)
				codeBuilder.astore(allocatedModEntry) // M A
				codeBuilder.swap() // A M
				// Entry
				codeBuilder.dup() // A M M
				codeBuilder.new_(fmlModContainer) // A M M C
				codeBuilder.dup() // A M M C C
				codeBuilder.new_(modInfo) // A M M C C I
				codeBuilder.dup() // A M M C C I I
				codeBuilder.aconst_null() // A M M C C I I N
				// Config //
				codeBuilder.invokestatic(
					config,
					"inMemory",
					MethodTypeDesc.of(config),
					true
				) // A M M C C I I N H
				// Config [modId]
				codeBuilder.dup() // A M M C C I I N H H
				codeBuilder.pushString("modId") // A M M C C I I N H H S
				codeBuilder.aload(allocatedModEntry) // A M M C C I I N H H S G
				codeBuilder.invokevirtual(
					begBmMod,
					"getIdentifier",
					MethodTypeDesc.of(javaString)
				) // A M M C C I I N H H S S
				codeBuilder.setConfig() // A M M C C I I N H
				// Config [displayName]
				codeBuilder.dup() // A M M C C I I N H H
				codeBuilder.pushString("displayName") // A M M C C I I N H H S
				codeBuilder.aload(allocatedModEntry) // A M M C C I I N H H S G
				codeBuilder.invokevirtual(
					begBmMod,
					"getDisplayName",
					MethodTypeDesc.of(javaString)
				) // A M M C C I I N H H S S
				codeBuilder.setConfig() // A M M C C I I N H
				// Config [version]
				codeBuilder.dup() // A M M C C I I N H H
				codeBuilder.pushString("version") // A M M C C I I N H H S
				codeBuilder.aload(allocatedModEntry) // A M M C C I I N H H S G
				codeBuilder.invokevirtual(
					begBmMod,
					"getVersion",
					MethodTypeDesc.of(javaString)
				) // A M M C C I I N H H S S
				codeBuilder.setConfig() // A M M C C I I N H
				// Config [description]
				codeBuilder.dup() // A M M C C I I N H H
				codeBuilder.pushString("description") // A M M C C I I N H H S
				codeBuilder.aload(allocatedModEntry) // A M M C C I I N H H S G
				codeBuilder.invokevirtual(
					begBmMod,
					"getDescription",
					MethodTypeDesc.of(javaString)
				) // A M M C C I I N H H S S
				codeBuilder.setConfig() // A M M C C I I N H
				// End Config
				codeBuilder.new_(nightConfigWrapper) // A M M C C I I N H K
				codeBuilder.dup_x1() // A M M C C I I N K H K
				codeBuilder.swap() // A M M C C I I N K K H
				codeBuilder.invokespecial(
					nightConfigWrapper,
					"<init>",
					MethodTypeDesc.of(
						ClassDesc.ofDescriptor("V"),
						unmodifiableConfig
					)
				) // A M M C C I I N K
				// ModInfo
				codeBuilder.invokespecial(
					modInfo,
					"<init>",
					MethodTypeDesc.of(
						ClassDesc.ofDescriptor("V"),
						modFileInfo, iConfigurable
					)
				) // A M M C C I
				codeBuilder.pushMutableList() // A M M C C I L
				codeBuilder.aconst_null() // A M M C C I N N
				codeBuilder.aconst_null() // A M M C C I N N N
				// FMLModContainer
				codeBuilder.invokespecial(
					fmlModContainer,
					"<init>",
					MethodTypeDesc.of(
						ClassDesc.ofDescriptor("V"),
						iModInfo, javaList, modFileScanData, moduleLayer
					)
				) // A M M C
				codeBuilder.invokeinterface(
					javaList,
					"add",
					MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;)Z")
				) // A M Z
				codeBuilder.pop() // A M
				codeBuilder.swap() // M A
				codeBuilder.goto_(containerEntryLoop)
				codeBuilder.labelBinding(containerEntryEnd)
				codeBuilder.pop() // M
			}
			codeBuilder.with(codeElement)
		}
	)
}