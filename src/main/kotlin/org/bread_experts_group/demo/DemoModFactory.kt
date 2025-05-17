package org.bread_experts_group.demo

import org.bread_experts_group.common.Mod
import org.bread_experts_group.common.ModFactory

class DemoModFactory : ModFactory {
	override fun createMods(): Array<Mod> = arrayOf(DemoMod())
}