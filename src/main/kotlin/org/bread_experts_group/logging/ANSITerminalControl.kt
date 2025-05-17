package org.bread_experts_group.logging

class ANSIString(private var total: String = "") {
	private var length = 0
	private val ansiEscape = ''
	private var resetChar = "$ansiEscape[39m"

	fun append(c: Char) { this.total += c; length++ }
	fun append(s: String) { this.total += s; length += s.length }

	private fun colorize(n: Int, init: ANSIString.() -> Unit): ANSIString = this.also {
		val last = resetChar
		resetChar = "$ansiEscape[${n}m"
		reset()
		init()
		resetChar = last
		reset()
	}

	fun black(init: ANSIString.() -> Unit): ANSIString = colorize(30, init)
	fun red(init: ANSIString.() -> Unit): ANSIString = colorize(31, init)
	fun green(init: ANSIString.() -> Unit): ANSIString = colorize(32, init)
	fun yellow(init: ANSIString.() -> Unit): ANSIString = colorize(33, init)
	fun blue(init: ANSIString.() -> Unit): ANSIString = colorize(34, init)
	fun magenta(init: ANSIString.() -> Unit): ANSIString = colorize(35, init)
	fun cyan(init: ANSIString.() -> Unit): ANSIString = colorize(36, init)
	fun lightGray(init: ANSIString.() -> Unit): ANSIString = colorize(37, init)
	fun default(init: ANSIString.() -> Unit): ANSIString = colorize(39, init)
	fun darkGray(init: ANSIString.() -> Unit): ANSIString = colorize(90, init)

	var setResets = true
	fun reset() {
		if (setResets) this.total += resetChar
	}

	fun build() = this.total
	fun length() = this.length
}

fun ansi(init: ANSIString.() -> Unit): ANSIString = ANSIString().also { it.init() }