package org.bread_experts_group.microbrewery.internal.neoforge

import org.bread_experts_group.logging.ColoredLogger
import java.lang.classfile.ClassTransform
import java.lang.classfile.Label
import java.lang.classfile.instruction.LineNumber
import java.lang.constant.MethodTypeDesc
import java.util.logging.Logger

object FMLModContainerTransformers {
	val logger: Logger = ColoredLogger.newLogger("BEG-MB NeoForge [FMLModContainer] Internal Transformers")

	private var forwardLabel: Label? = null
	val initTransform: ClassTransform = ClassTransform.transformingMethodBodies(
		{ it.methodName().stringValue() == "<init>" },
		{ codeBuilder, codeElement ->
			if (codeElement is LineNumber) {
				if (codeElement.line() == 51) {
					codeBuilder.aload(1)
					codeBuilder.invokeinterface(
						ModLoaderTransformers.iModInfo,
						"getOwningFile",
						MethodTypeDesc.of(ModLoaderTransformers.iModFileInfo)
					)
					forwardLabel = codeBuilder.newLabel()
					codeBuilder.ifnull(forwardLabel)
					logger.info("Insert label [$forwardLabel] [$codeElement]")
				} else if (forwardLabel != null) {
					codeBuilder.labelBinding(forwardLabel)
					forwardLabel = null
				}
			}
			codeBuilder.with(codeElement)
		}
	)
}