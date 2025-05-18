package org.bread_experts_group.common.registrar

import org.bread_experts_group.common.Convertible
import org.bread_experts_group.common.Mod
import org.bread_experts_group.logging.ColoredLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

sealed class Registrar<T: Convertible>(
	val owner: Mod,
	private val registryField: String
) : Map<ResourceLocation, RegisteredObject<T>> {
	protected val internalRegistrar = ConcurrentHashMap<ResourceLocation, RegisteredObject<T>>()

	override val size: Int
		get() = internalRegistrar.size
	override val entries: Set<Map.Entry<ResourceLocation, RegisteredObject<T>>>
		get() = internalRegistrar.entries
	override val keys: Set<ResourceLocation>
		get() = internalRegistrar.keys
	override val values: Collection<RegisteredObject<T>>
		get() = internalRegistrar.values

	override fun isEmpty(): Boolean = internalRegistrar.isEmpty()
	override fun containsKey(key: ResourceLocation): Boolean = internalRegistrar.containsKey(key)
	override fun containsValue(value: RegisteredObject<T>): Boolean = internalRegistrar.containsValue(value)
	override fun get(key: ResourceLocation): RegisteredObject<T>? = internalRegistrar[key]

	private var frozen: Boolean = false
	private fun freeze() { this.frozen = true }

	fun push(id: String, i: T): RegisteredObject<T> {
		if (frozen)
			throw IllegalStateException("Registrar already frozen")
		val location = ResourceLocation(owner.identifier, id)
		if (internalRegistrar.contains(i) || internalRegistrar.containsKey(location))
			throw UnsupportedOperationException("The internal registrar already contains [$id] / [$i]")
		val registeredObject = RegisteredObject(
			i,
			ResourceLocation(owner.identifier, id)
		)
		internalRegistrar[location] = registeredObject
		return registeredObject
	}

	init { pushRegistrar(owner, this) }

	companion object {
		private val logger = ColoredLogger.newLogger("BEG-MB Registrar Internals")
		private var registrationFrozen = false
		private val allRegistrars = ConcurrentHashMap<Mod, ConcurrentLinkedQueue<Registrar<*>>>()
		internal fun pushRegistrar(owner: Mod, registrar: Registrar<*>) {
			if (registrationFrozen) throw IllegalStateException("Registrars already submitted")
			allRegistrars.getOrPut(owner) { ConcurrentLinkedQueue() }.add(registrar)
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
					registrar.forEach { (id, registeredItem) ->
						registerIntoRegistry.invoke(
							null,
							builtInRegistries.getField(registrar.registryField).get(null), id.fullPath,
							registeredItem.item.getRealObject(registry::class.java.classLoader)
						)
					}
				}
			}
			registry::class.java.getMethod("freeze").invoke(registry)
			allRegistrars.clear()
		}
	}
}