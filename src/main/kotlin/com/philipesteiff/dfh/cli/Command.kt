package com.philipesteiff.dfh.cli

import org.apache.commons.cli.*

class Command() {

    companion object {
        fun parse(
                init: Builder.() -> Unit,
                result: Builder.(CommandLine) -> Unit
        ) = Builder(init).build(result)
    }

    class Builder private constructor() {

        var args = emptyArray<String>()
        val options by lazy { Options() }

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