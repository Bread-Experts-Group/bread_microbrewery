package org.bread_experts_group.common.registrar

import org.bread_experts_group.common.Mod
import org.bread_experts_group.logging.ColoredLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

sealed class Registrar<T>(owner: Mod, private val registryField: String) : Map<String, T> {
	protected val internalRegistrar = ConcurrentHashMap<String, T>()

	override val size: Int
		get() = internalRegistrar.size
	override val entries: Set<Map.Entry<String, T>>
		get() = internalRegistrar.entries
	override val keys: Set<String>
		get() = internalRegistrar.keys
	override val values: Collection<T>
		get() = internalRegistrar.values

	override fun isEmpty(): Boolean = internalRegistrar.isEmpty()
	override fun containsKey(key: String): Boolean = internalRegistrar.containsKey(key)
	override fun containsValue(value: T): Boolean = internalRegistrar.containsValue(value)
	override fun get(key: String): T? = internalRegistrar[key]

	private var frozen: Boolean = false
	private fun freeze() { this.frozen = true }
	protected abstract fun getRealClassForObject(cl: ClassLoader, i: Any?): Any

	protected fun push(id: String, i: T): T {
		if (frozen)
			throw IllegalStateException("Registrar already frozen")
		if (internalRegistrar.contains(i) || internalRegistrar.containsKey(id))
			throw UnsupportedOperationException("The internal registrar already contains [$id] / [$i]")
		internalRegistrar[id] = i
		return i
	}

	init { pushRegistrar(owner, this) }

	companion object {
		private val logger = ColoredLogger.newLogger("BEG-MB Registrar Internals")
		private var registrationFrozen = false
		private val allRegistrars = ConcurrentHashMap<Mod, ConcurrentLinkedQueue<Registrar<*>>>()
		internal fun pushRegistrar(owner: Mod, registrar: Registrar<*>) {
			if (registrationFrozen) throw IllegalStateException("Registrars already submitted")
			allRegistrars.getOrPut(owner, { ConcurrentLinkedQueue() }).add(registrar)
		}

		@JvmStatic
		@Suppress("unused") // Called by BuiltInRegistries <clinit>
		internal fun registerAllRegistrars(registry: Any) {
			registrationFrozen = true
			val builtInRegistries = registry::class.java.classLoader.loadClass("net.minecraft.core.registries.BuiltInRegistries")
			val registryClass = registry::class.java.classLoader.loadClass("net.minecraft.core.Registry")
			val registerIntoRegistry = registryClass.getMethod(
				"register", registryClass, String::class.java, Object::class.java
			)
			registry::class.java.getMethod("unfreeze").invoke(registry)
			allRegistrars.forEach { (mod, registrars) ->
				registrars.forEach { registrar ->
					registrar.freeze()
					logger.info("Registering [${mod.identifier}:${registrar.registryField}] [${registrar.size} entries]")
					registrar.forEach { (id, item) ->
						registerIntoRegistry.invoke(
							null,
							builtInRegistries.getField(registrar.registryField).get(null), "${mod.identifier}:$id",
							registrar.getRealClassForObject(registry::class.java.classLoader, item)
						)
					}
				}
			}
			registry::class.java.getMethod("freeze").invoke(registry)
			allRegistrars.clear()
		}
	}
}