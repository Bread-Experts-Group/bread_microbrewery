package org.bread_experts_group.logging

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

object ColoredLogger : Handler() {
	private val formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm:ss.SSSSSS")
	private val levelNamePad = System.Logger.Level.entries.maxOf { it.name.length }
	private var closed: Boolean = false
	val writeback: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
	var coloring: Boolean = true

	override fun publish(record: LogRecord) {
		if (closed) return
		val prefix = ansi {
			setResets = coloring
			lightGray {
				append('[')
				when (record.level) {
					Level.FINEST -> ::darkGray
					Level.FINER -> ::lightGray
					Level.FINE -> ::default
					Level.INFO -> ::cyan
					Level.WARNING -> ::yellow
					Level.SEVERE -> ::red
					Level.CONFIG, Level.OFF, Level.ALL -> ::magenta
					else -> ::default
				}.invoke { append(record.level.name.padEnd(levelNamePad)) }
				append(" | ")
				yellow {
					append(
						formatter.format(
							ZonedDateTime.ofInstant(
								record.instant,
								ZoneId.systemDefault()
							)
						)
					)
				}
				append(" | ")
				default { append(record.loggerName) }
				append(" | ")
				default {
					append(record.sourceMethodName)
					lightGray {
						append('[')
						cyan { append(record.longThreadID.toString()) }
						append(']')
					}
				}
				append(']')
			}
			append(' ')
		}
		val spaced = " ".repeat(prefix.length())
		val paddedMessage = record.message.replace("\n", "\n$spaced", true)
		val fullMessage = if (record.thrown != null) {
			val exceptionName = record.thrown::class.simpleName ?: "???"
			val initialMessage = prefix.build() + paddedMessage
			val subSpaced = " ".repeat(prefix.length() - exceptionName.length - 3)
			val exceptionMessage = (record.thrown.localizedMessage ?: "<no message>").split('\n', limit = 2, ignoreCase = true)
			initialMessage + '\n' + subSpaced + ansi {
				setResets = coloring
				lightGray {
					append('[')
					red { append(exceptionName) }
					append(']')
				}
				append(' ')
				yellow {
					append(exceptionMessage[0])
					if (exceptionMessage.size > 1) append(
						('\n' + exceptionMessage[1]).replace("\n", "\n$spaced", true)
					)
				}
				lightGray {
					var moduleNamePad = 0
					var moduleVersionPad = 0
					var filePad = 0
					var classLoadPad = 0
					var classNamePad = 0
					var methodNamePad = 0
					record.thrown.stackTrace.forEach {
						if (it.className.length > classNamePad) classNamePad = it.className.length
						if (it.methodName.length > methodNamePad) methodNamePad = it.methodName.length
						if (it.moduleName != null && it.moduleName.length > moduleNamePad)
							moduleNamePad = it.moduleName.length
						if (it.moduleVersion != null && it.moduleVersion.length > moduleVersionPad)
							moduleVersionPad = it.moduleVersion.length
						val fileName = it.fileName
						if (fileName != null && fileName.length > filePad) filePad = fileName.length
						val classLoaderName = it.classLoaderName
						if (classLoaderName != null && classLoaderName.length > classLoadPad) classLoadPad = classLoaderName.length
					}

					record.thrown.stackTrace.forEachIndexed { i, trace ->
						append('\n')
						if (i == 0) red { append('.') }
						else append('^')
						append(" [")
						if (trace.moduleName != null || trace.moduleVersion != null) {
							append('[')
							blue {
								append((trace.moduleName ?: "").padEnd(moduleNamePad))
								lightGray { append(':') }
								append((trace.moduleVersion ?: "").padEnd(moduleVersionPad))
							}
							append("] ")
						} else append(" ".repeat(moduleNamePad + moduleVersionPad + 4))
						cyan {
							append((trace.fileName ?: "").padEnd(filePad))
							lightGray { append(" | ") }
							append((trace.classLoaderName ?: "").padEnd(classLoadPad))
						}
						append("] ")
						green { append(trace.className.padEnd(classNamePad)) }
						append('.')
						(if (trace.isNativeMethod) ::magenta
						else ::default).invoke { append(trace.methodName.padEnd(methodNamePad)) }
						if (trace.lineNumber > -1) {
							append(';')
							yellow { append(trace.lineNumber.toString()) }
						}
					}
				}
			}.build()
		} else prefix.build() + paddedMessage
		writeback.add(fullMessage)
		this.flush()
	}

	override fun flush() {
		while (writeback.isNotEmpty()) writeback.poll()?.also { println(it) }
	}

	override fun close() {
		closed = true
		this.flush()
	}

	fun newLogger(name: String): Logger = Logger.getLogger(name).also {
		it.useParentHandlers = false
		it.addHandler(this)
	}
}