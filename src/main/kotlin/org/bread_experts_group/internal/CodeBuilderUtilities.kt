package org.bread_experts_group.internal

import java.lang.classfile.CodeBuilder
import java.lang.constant.ClassDesc
import java.lang.constant.MethodTypeDesc

val javaObject: ClassDesc = ClassDesc.of("java.lang.Object")
val javaString: ClassDesc = ClassDesc.of("java.lang.String")
val javaList: ClassDesc = ClassDesc.of("java.util.List")
val javaArrayList: ClassDesc = ClassDesc.of("java.util.ArrayList")
val javaMap: ClassDesc = ClassDesc.of("java.util.Map")
val javaCollection: ClassDesc = ClassDesc.of("java.util.Collection")
val moduleLayer: ClassDesc = ClassDesc.of("java.lang.ModuleLayer")

/**
 * Logs the topmost operand stack value using [java.util.logging.Logger].
 * The operand stack remains unchanged.
 * ```
 * L L     [DUP topmost stack entry]
 * L L S   [Push logger name]
 * L L G   [Create java.util.logging.Logger]
 * L G L   [SWAP]
 * L G S   [toString topmost stack entry]
 * L       [Log]
 * ```
 * @author Miko Elbrecht
 * @since 1.0.0
 * @receiver [CodeBuilder]; the code builder on which to inject this code onto.
 */
fun CodeBuilder.logImmediateStackValue() {
	this.dup() // L L
	this.pushString("Stack Logger") // L L S
	this.invokestatic(
		ClassDesc.of("java.util.logging.Logger"),
		"getLogger",
		MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)Ljava/util/logging/Logger;")
	) // L L G
	this.swap() // L G L
	this.invokevirtual(
		ClassDesc.of("java.lang.Object"),
		"toString",
		MethodTypeDesc.ofDescriptor("()Ljava/lang/String;")
	) // L G S
	this.invokevirtual(
		ClassDesc.of("java.util.logging.Logger"),
		"info",
		MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V")
	) // L
}

/**
 * Converts the topmost operand stack value (expected to be a [Collection]) into an [ArrayList].
 * This will consume the original [Collection]; the new [ArrayList] will take its place.
 * ```
 * L L     [DUP topmost stack entry]
 * L L M   [NEW ArrayList]
 * L M L M [DUP_X1 ArrayList]
 * L M M L [SWAP]
 * L M     [Initialize ArrayList]
 * M L     [SWAP]
 * M       [Pop]
 * ```
 * @author Miko Elbrecht
 * @since 1.0.0
 * @receiver [CodeBuilder]; the code builder on which to inject this code onto.
 */
fun CodeBuilder.mutableList() {
	this.dup() // L L
	this.new_(javaArrayList) // L L M
	this.dup_x1() // L M L M
	this.swap() // L M M L
	this.invokespecial(
		javaArrayList,
		"<init>",
		MethodTypeDesc.ofDescriptor("(Ljava/util/Collection;)V")
	) // L M
	this.swap() // M L
	this.pop() // M
}

fun CodeBuilder.pushMutableList() {
	this.new_(javaArrayList) // M
	this.dup() // M M
	this.invokespecial(
		javaArrayList,
		"<init>",
		MethodTypeDesc.ofDescriptor("()V")
	) // M
}


fun CodeBuilder.pushString(str: String) {
	val stringEntry = this.constantPool().stringEntry(str)
	this.loadConstant(stringEntry.constantValue()) // C
}