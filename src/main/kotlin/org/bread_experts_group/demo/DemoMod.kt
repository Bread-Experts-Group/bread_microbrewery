package org.bread_experts_group.demo

import org.bread_experts_group.common.Mod
import org.bread_experts_group.common.registrar.ItemRegistrar
import org.bread_experts_group.common.world.Item

class DemoMod : Mod(
	"beg_bm_demo_mod",
	"BEG-BM Demonstration Mod",
	"1.0.0",
	"Demonstration mod for the BreadExpertsGroup microbrewery loader with interop"
) {
	inner class ModItemDefinitions : ItemRegistrar(this@DemoMod) {
		val exampleItem = this.push("test", Item())
		val exampleItem2 = this.push("test2", Item())
	}
	val items = ModItemDefinitions()

	init {
		logger.info("Demo mod loaded")
		logger.info("${items.exampleItem}")
		logger.info("${items.exampleItem2}")
		items.forEach {
			logger.info(it.toString())
		}
	}
}