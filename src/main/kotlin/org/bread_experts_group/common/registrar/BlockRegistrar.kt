package org.bread_experts_group.common.registrar

import org.bread_experts_group.common.Mod
import org.bread_experts_group.common.world.Block

abstract class BlockRegistrar(owner: Mod) : Registrar<Block>(owner, "BLOCK") {
	final override fun getRealClassForObject(cl: ClassLoader, i: Any?): Any {
		val properties = cl.loadClass("net.minecraft.world.level.block.state.BlockBehaviour\$Properties")
		return cl.loadClass("net.minecraft.world.level.block.Block").getConstructor(properties).newInstance(
			properties.getMethod("of").invoke(null)
		)
	}
}