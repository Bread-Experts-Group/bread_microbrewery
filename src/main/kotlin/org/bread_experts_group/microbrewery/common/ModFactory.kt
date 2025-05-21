package org.bread_experts_group.microbrewery.common

import java.util.concurrent.CompletableFuture

interface ModFactory {
	fun createMods(): Collection<CompletableFuture<Mod>>
}