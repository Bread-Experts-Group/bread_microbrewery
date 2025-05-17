package org.bread_experts_group.common.registrar

import org.bread_experts_group.common.Mod
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

sealed class Registrar<T>(owner: Mod) : Map<String, T> {
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

	protected fun push(id: String, i: T): T {
		if (internalRegistrar.contains(i) || internalRegistrar.containsKey(id))
			throw UnsupportedOperationException("The internal registrar already contains [$id] / [$i]")
		internalRegistrar[id] = i
		return i
	}

	init { pushRegistrar(owner, this) }

	companion object {
		private var registrationFrozen = false
		private val allRegistrars = ConcurrentHashMap<Mod, ConcurrentLinkedQueue<Registrar<*>>>()
		internal fun pushRegistrar(owner: Mod, registrar: Registrar<*>) {
			if (registrationFrozen) throw UnsupportedOperationException("Registrars already submitted")
			allRegistrars.getOrPut(owner, { ConcurrentLinkedQueue() }).add(registrar)
		}

		@JvmStatic
		internal fun registerAllRegistrars(registry: Any) {
			registrationFrozen = true
			val register = registry::class.java.getMethod(
				"register",
				registry::class.java.classLoader.loadClass("net.minecraft.resources.ResourceKey"),
				Object::class.java,
				registry::class.java.classLoader.loadClass("net.minecraft.core.RegistrationInfo")
			)
			registry::class.java.getMethod("unfreeze").invoke(registry)
			allRegistrars.forEach { (mod, registrars) ->
				registrars.forEach { registrar ->
//					register.invoke(
//						registry,
//						null,
//						registrar,
//						null
//					) // TODO!
				}
			}
			registry::class.java.getMethod("freeze").invoke(registry)
			allRegistrars.clear()
		}
	}
}