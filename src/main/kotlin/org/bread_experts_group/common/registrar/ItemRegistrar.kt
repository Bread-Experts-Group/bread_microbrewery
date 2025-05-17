package org.bread_experts_group.common.registrar

import org.bread_experts_group.common.Mod
import org.bread_experts_group.common.world.Item

abstract class ItemRegistrar(owner: Mod) : Registrar<Item>(owner, "ITEM") {
	final override fun getRealClassForObject(cl: ClassLoader, i: Any?): Any {
		val properties = cl.loadClass("net.minecraft.world.item.Item\$Properties")
		return cl.loadClass("net.minecraft.world.item.Item").getConstructor(properties).newInstance(
			properties.getConstructor().newInstance()
		)
	}
}