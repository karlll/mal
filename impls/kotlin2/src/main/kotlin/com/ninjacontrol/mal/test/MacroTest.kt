package main.kotlin.com.ninjacontrol.mal.test

import main.kotlin.com.ninjacontrol.mal.int

class MacroTest : TestSuite {

    override val name = "Macros"

    private val tests = listOf(
        testReadEval {
            description = "defmacro: define macro"
            input = """(do (defmacro! leet (fn* () 1337)) (leet))"""
            expectedAst = int(1337)
        },
        testReadEval {
            description = "macroexpand: expand macro"
            input = """(do (defmacro! leet (fn* () 1337)) (macroexpand (leet)))"""
            expectedAst = int(1337)
            only = true
        },

    )

    override fun getTests(): List<TestCase> = tests
    override fun run(): Boolean =
        verifyTests(tests)
}
