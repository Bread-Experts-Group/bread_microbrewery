package org.bread_experts_group.microbrewery.internal.neoforge

import org.bread_experts_group.logging.ColoredLogger
import java.lang.classfile.ClassTransform
import java.lang.classfile.Label
import java.lang.classfile.instruction.LineNumber
import java.lang.constant.MethodTypeDesc
import java.util.logging.Logger

object ModListScreenTransformers {
	val logger: Logger = ColoredLogger.newLogger("BEG-MB NeoForge [ModListScreen] Internal Transformers")

	private var forwardLabel: Label? = null
	val updateCacheTransform: ClassTransform = ClassTransform.transformingMethodBodies(
		{ it.methodName().stringValue() == "updateCache" },
		{ codeBuilder, codeElement ->
			if (codeElement is LineNumber) {
				if (codeElement.line() == 424) {
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