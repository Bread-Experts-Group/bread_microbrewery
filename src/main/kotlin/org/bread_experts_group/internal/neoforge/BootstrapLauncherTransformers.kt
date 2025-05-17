package org.bread_experts_group.internal.neoforge

import org.bread_experts_group.logging.ColoredLogger
import java.lang.classfile.ClassTransform
import java.util.logging.Logger

object BootstrapLauncherTransformers {
	val logger: Logger = ColoredLogger.newLogger("BEG-MB NeoForge [BootstrapLauncher] Internal Transformers")

	private var setOnce: Boolean = false
	val runTransform: ClassTransform = ClassTransform.transformingMethodBodies(
		{ it.methodName().stringValue() == "run" },
		{ codeBuilder, codeElement ->
			if (!setOnce) {
				logger.info("[run] modification ...\n$codeElement")
				codeBuilder.iconst_0()
				codeBuilder.istore(0)
				setOnce = true
			}
			codeBuilder.with(codeElement)
		}
	)
}