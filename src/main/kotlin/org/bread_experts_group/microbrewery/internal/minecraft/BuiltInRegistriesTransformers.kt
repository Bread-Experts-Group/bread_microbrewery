package org.bread_experts_group.microbrewery.internal.minecraft

import org.bread_experts_group.microbrewery.logging.ColoredLogger
import java.lang.classfile.ClassTransform
import java.lang.classfile.instruction.ReturnInstruction
import java.lang.constant.ClassDesc
import java.lang.constant.MethodTypeDesc
import java.util.logging.Logger

object BuiltInRegistriesTransformers {
	val logger: Logger = ColoredLogger.newLogger("BEG-MB NeoForge [BuiltInRegistries] Internal Transformers")

	val clinitTransform: ClassTransform = ClassTransform.transformingMethodBodies(
		{ it.methodName().stringValue() == "<clinit>" },
		{ codeBuilder, codeElement ->
			if (codeElement is ReturnInstruction) {
				logger.info("[<clinit>] modification ...\n$codeElement")
				codeBuilder.getstatic(
					ClassDesc.of("net.minecraft.core.registries.BuiltInRegistries"),
					"REGISTRY",
					ClassDesc.of("net.minecraft.core.Registry")
				)
				codeBuilder.invokestatic(
					ClassDesc.of("org.bread_experts_group.common.registrar.Registrar"),
					"registerAllRegistrars\$bread_microbrewery",
					MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;)V")
				)
			}
			codeBuilder.with(codeElement)
		}
	)
}