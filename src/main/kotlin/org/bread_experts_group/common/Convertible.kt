package org.bread_experts_group.common

abstract class Convertible {
	protected var realCopy: Any? = null
	fun getRealObject(cl: ClassLoader): Any {
		if (realCopy == null)  realCopy = convertToRealObject(cl)
		return realCopy!!
	}

	protected abstract fun convertToRealObject(cl: ClassLoader): Any
}