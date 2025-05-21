package org.bread_experts_group.microbrewery.demo

import org.bread_experts_group.microbrewery.common.Mod
import org.bread_experts_group.microbrewery.common.ModFactory
import java.util.concurrent.CompletableFuture

class DemoModFactory : ModFactory {
	override fun createMods(): Collection<CompletableFuture<Mod>> = listOf(
		CompletableFuture.supplyAsync { DemoMod() }
	)
}