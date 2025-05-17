package org.bread_experts_group.demo

import org.bread_experts_group.common.Mod
import org.bread_experts_group.common.ModFactory
import java.util.concurrent.CompletableFuture

class DemoModFactory : ModFactory {
	override fun createMods(): Collection<CompletableFuture<Mod>> = listOf(
		CompletableFuture.supplyAsync { DemoMod() }
	)
}