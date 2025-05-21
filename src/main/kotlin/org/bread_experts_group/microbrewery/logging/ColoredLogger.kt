package org.bread_experts_group.microbrewery.logging

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.math.max

object ColoredLogger : Handler() {
	private val formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm:ss.SSSSSS")
	private val levelNamePad = System.Logger.Level.entries.maxOf { it.name.length }
	private var closed: Boolean = false
	private val writeback: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
	var coloring: Boolean = true
	var stackTraceOnlyInLoggingModule: Boolean = false

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
			val exceptionName = record.thrown.javaClass.canonicalName ?: "???"
			val initialMessage = prefix.build() + paddedMessage
			val exceptionMessage = (record.thrown.localizedMessage ?: "<no message>").split('\n', limit = 2, ignoreCase = true)
			initialMessage + '\n' + ansi {
				setResets = coloring
				lightGray {
					val fromModuleName = try {
						if (record.sourceClassName == null) null
						else this::class.java.classLoader.loadClass(record.sourceClassName).module.name
					} catch (_: ClassNotFoundException) {
						null
					}

					val stack = record.thrown.stackTrace
					var moduleNamePad = 0
					var moduleVersionPad = 0
					var filePad = 0
					var classLoadPad = 0
					var classNamePad = 0
					var methodNamePad = 0

					var truncatedCount = 0
					var firstTruncatedSet = false
					for (trace in stack) {
						if (stackTraceOnlyInLoggingModule && trace.moduleName != fromModuleName) {
							if (!firstTruncatedSet) truncatedCount++
							continue
						} else firstTruncatedSet = true
						if (trace.className.length > classNamePad) classNamePad = trace.className.length
						if (trace.methodName.length > methodNamePad) methodNamePad = trace.methodName.length
						if (trace.moduleName != null && trace.moduleName.length > moduleNamePad)
							moduleNamePad = trace.moduleName.length
						if (trace.moduleVersion != null && trace.moduleVersion.length > moduleVersionPad)
							moduleVersionPad = trace.moduleVersion.length
						val fileName = trace.fileName
						if (fileName != null && fileName.length > filePad) filePad = fileName.length
						val classLoaderName = trace.classLoaderName
						if (classLoaderName != null && classLoaderName.length > classLoadPad) classLoadPad = classLoaderName.length
					}

					var traceIndex = 0

					fun traceString() = "<$truncatedCount excluded trace${if (truncatedCount > 1) 's' else ""}>"
					fun logTruncated() = if (truncatedCount > 0) {
						lightGray { append("\n${traceString()}") }
						truncatedCount = 0
					} else null

					lightGray {
						var additionalOffset = 0
						if (truncatedCount > 0) traceString().also {
							additionalOffset = it.length
							append(it)
						}
						append(" ".repeat(max(0, prefix.length() - exceptionName.length - 3 - additionalOffset)))
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
					for (trace in stack) {
						if (truncatedCount > 0) {
							truncatedCount--
							continue
						}
						if (stackTraceOnlyInLoggingModule && trace.moduleName != fromModuleName) {
							truncatedCount++
							traceIndex++
							continue
						}
						logTruncated()
						append('\n')
						if (traceIndex == 0) red { append('.') }
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
						traceIndex++
					}
					logTruncated()
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