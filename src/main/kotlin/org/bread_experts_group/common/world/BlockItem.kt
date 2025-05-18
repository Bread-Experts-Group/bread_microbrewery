package org.bread_experts_group.common.world

class BlockItem(val block: Block) : Item() {
	override fun convertToRealObject(cl: ClassLoader): Any {
		val propertiesClass = cl.loadClass("net.minecraft.world.item.Item\$Properties")
		val blockClass = cl.loadClass("net.minecraft.world.level.block.Block")
		return cl.loadClass("net.minecraft.world.item.BlockItem").getConstructor(
			blockClass,
			propertiesClass
		).newInstance(
			block.getRealObject(cl),
			propertiesClass.getConstructor().newInstance()
		)
	}
}