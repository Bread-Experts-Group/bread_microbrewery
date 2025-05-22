package org.bread_experts_group.microbrewery.common

import org.bread_experts_group.logging.ColoredLogger
import java.util.logging.Logger

abstract class Mod(
	val identifier: String,
	val displayName: String,
	val version: String,
	val description: String
) {
	val logger: Logger = ColoredLogger.newLogger("BEG-MB Mod [$identifier]")
}