package org.bread_experts_group.microbrewery.common.world

import org.bread_experts_group.microbrewery.common.Convertible

class Block : Convertible() {
	override fun convertToRealObject(cl: ClassLoader): Any {
		val propertiesClass = cl.loadClass("net.minecraft.world.level.block.state.BlockBehaviour\$Properties")
		return cl.loadClass("net.minecraft.world.level.block.Block").getConstructor(propertiesClass).newInstance(
			propertiesClass.getMethod("of").invoke(null)
		)
	}
}