package org.bread_experts_group.microbrewery.demo

import org.bread_experts_group.microbrewery.common.Mod
import org.bread_experts_group.microbrewery.common.registrar.BlockRegistrar
import org.bread_experts_group.microbrewery.common.registrar.ItemRegistrar
import org.bread_experts_group.microbrewery.common.world.Block
import org.bread_experts_group.microbrewery.common.world.Item

class DemoMod : Mod(
	"beg_bm_demo_mod",
	"BEG-BM Demonstration Mod",
	"1.0.0",
	"Demonstration mod for the BreadExpertsGroup microbrewery loader with interop"
) {
	inner class ModItemDefinitions : ItemRegistrar(this@DemoMod) {
		val exampleItem = this.push("test_item", Item())
	}
	val items = ModItemDefinitions()
	inner class ModBlockDefinitions : BlockRegistrar(this@DemoMod) {
		val exampleBlock = this.push("test_block_noitem", Block())
		val exampleBlockWithItem = this.pushWithItem("test_block", Block(), items)
	}
	val blocks = ModBlockDefinitions()

	init {
		logger.info("Demo mod loaded")
	}
}