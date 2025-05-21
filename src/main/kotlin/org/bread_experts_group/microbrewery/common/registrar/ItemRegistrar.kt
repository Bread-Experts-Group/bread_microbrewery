package org.bread_experts_group.microbrewery.common.registrar

import org.bread_experts_group.microbrewery.common.Mod
import org.bread_experts_group.microbrewery.common.world.Item

abstract class ItemRegistrar(owner: Mod) : Registrar<Item>(owner, "ITEM")