package org.bread_experts_group.common.registrar

import org.bread_experts_group.common.Mod
import org.bread_experts_group.common.world.Block
import org.bread_experts_group.common.world.BlockItem

abstract class BlockRegistrar(owner: Mod) : Registrar<Block>(owner, "BLOCK") {
	fun pushWithItem(id: String, i: Block, concert: ItemRegistrar) = BlockItem(i).also {
		this.push(id, i)
		concert.push(id, it)
	}
}