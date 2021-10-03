package main.kotlin.com.ninjacontrol.mal.test

import main.kotlin.com.ninjacontrol.mal.int

class NamespaceTest : TestSuite {

    override val name = "Builtin functions in default namespace"

    private val tests = listOf(
        testReadEval {
            description = "list: no arguments returns empty list"
            input = """(list)"""
            expectedAst = main.kotlin.com.ninjacontrol.mal.emptyList()
        },
        testReadEval {
            description = "count: returns number of elements in list"
            input = """(count (list 1 2 3 4 5 6 7 8 9))"""
            expectedAst = int(9)
        },
        testReadEval {
            description = "count: empty list returns 0 elements"
            input = """(count (list))"""
            expectedAst = int(0)
        },
        testReadEval {
            description = "count: 'nil' counts as 0 elements"
            input = """(count nil)"""
            expectedAst = int(0)
        }
    )

    override fun run(): Boolean =
        verifyTests(tests)
}
