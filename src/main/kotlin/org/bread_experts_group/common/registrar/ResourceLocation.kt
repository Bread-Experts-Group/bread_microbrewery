package org.bread_experts_group.common.registrar

data class ResourceLocation(val namespace: String, val path: String) {
	val fullPath: String
		get() = "$namespace:$path"
}