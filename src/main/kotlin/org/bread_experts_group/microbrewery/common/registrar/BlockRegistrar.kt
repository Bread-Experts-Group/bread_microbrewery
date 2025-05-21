package org.bread_experts_group.microbrewery.common.registrar

import org.bread_experts_group.microbrewery.common.Mod
import org.bread_experts_group.microbrewery.common.world.Block
import org.bread_experts_group.microbrewery.common.world.BlockItem

abstract class BlockRegistrar(owner: Mod) : Registrar<Block>(owner, "BLOCK") {
	fun pushWithItem(id: String, i: Block, concert: ItemRegistrar) = BlockItem(i).also {
		this.push(id, i)
		concert.push(id, it)
	}
}