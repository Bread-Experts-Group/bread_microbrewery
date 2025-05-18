package org.bread_experts_group.common.world

import org.bread_experts_group.common.Convertible

open class Item : Convertible() {
	override fun convertToRealObject(cl: ClassLoader): Any {
		val propertiesClass = cl.loadClass("net.minecraft.world.item.Item\$Properties")
		return cl.loadClass("net.minecraft.world.item.Item").getConstructor(propertiesClass).newInstance(
			propertiesClass.getConstructor().newInstance()
		)
	}
}