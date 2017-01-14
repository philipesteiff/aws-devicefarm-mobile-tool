package com.philipesteiff.mobile.devicefarm.cli

import org.apache.commons.cli.*

class CommandInput {

    companion object {
        fun parse(
                init: Builder.() -> Unit,
                result: Builder.(CommandLine) -> Unit
        ) = Builder(init).build(result)
    }

    class Builder private constructor() {

        private var args = emptyArray<String>()
        private val options by lazy { Options() }

        constructor(init: Builder.() -> Unit) : this() {
            init()
        }

        fun addArgs(func: Builder.() -> Array<String>) = apply { args = func() }

        fun addOption(func: Builder.() -> Option) = apply { options.addOption(func()) }

        fun build(result: Builder.(CommandLine) -> Unit) {
            try {
                val line = DefaultParser().parse(options, args)
                result(this, line)
            } catch(exp: ParseException) {
                System.err.println("Parsing failed.  Reason: " + exp.message)
            }
        }
    }

}