package org.bread_experts_group.common.registrar

import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

sealed class Registrar<T> : Queue<T> {
	protected val internalRegistrar = ConcurrentLinkedQueue<T>()

	override val size: Int
		get() = internalRegistrar.size

	override fun isEmpty(): Boolean = internalRegistrar.isEmpty()
	override fun contains(element: T): Boolean = internalRegistrar.contains(element)
	override fun containsAll(elements: Collection<T>): Boolean = internalRegistrar.containsAll(elements)
	override fun add(e: T): Boolean = internalRegistrar.add(e)
	override fun offer(e: T): Boolean = internalRegistrar.offer(e)
	override fun remove(): T = internalRegistrar.remove()
	override fun poll(): T = internalRegistrar.poll()
	override fun element(): T = internalRegistrar.element()
	override fun peek(): T = internalRegistrar.peek()
	override fun iterator(): MutableIterator<T> = internalRegistrar.iterator()
	override fun remove(element: T): Boolean = internalRegistrar.remove(element)
	override fun addAll(elements: Collection<T>): Boolean = internalRegistrar.addAll(elements)
	override fun removeAll(elements: Collection<T>): Boolean = internalRegistrar.removeAll(elements)
	override fun retainAll(elements: Collection<T>): Boolean = internalRegistrar.retainAll(elements)
	override fun clear() = internalRegistrar.clear()
}