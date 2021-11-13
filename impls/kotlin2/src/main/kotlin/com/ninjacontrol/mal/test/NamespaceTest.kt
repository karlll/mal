package main.kotlin.com.ninjacontrol.mal.test

import main.kotlin.com.ninjacontrol.mal.MalString
import main.kotlin.com.ninjacontrol.mal.True
import main.kotlin.com.ninjacontrol.mal.atom
import main.kotlin.com.ninjacontrol.mal.err
import main.kotlin.com.ninjacontrol.mal.int
import main.kotlin.com.ninjacontrol.mal.list
import main.kotlin.com.ninjacontrol.mal.string

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
        },
        testReadEval {
            description = "pr-str: 0 arguments returns empty string"
            input = """(pr-str)"""
            expectedAst = MalString("")
        },
        testReadEval {
            description = "read-string: eval string"
            input = """(read-string "(1 2 3)")"""
            expectedAst = list(int(1), int(2), int(3))
        },
        testReadEval {
            description = "slurp: invalid filename returns error"
            input = """(slurp "this-does-not-exist")"""
            expectedAst = err("File \"this-does-not-exist\" does not exist")
        },
        testReadEval {
            description = "atom: create atom"
            input = """(do (def! a (atom 43)) a)"""
            expectedAst = atom(int(43))
        },
        testReadEval {
            description = "atom: deref atom"
            input = """(do (def! a (atom 42)) (deref a))"""
            expectedAst = int(42)
        },
        testReadEval {
            description = "atom: deref atom, shorthand"
            input = """(do (def! a (atom 49)) @a)"""
            expectedAst = int(49)
        },
        testReadEval {
            description = "atom: is atom?"
            input = """(do (def! a (atom 49)) (atom? a))"""
            expectedAst = True
        },
        testReadEval {
            description = "atom: reset value"
            input = """(do (def! a (atom 49)) (reset! a 11))"""
            expectedAst = int(11)
        },
        testReadEval {
            description = "atom: swap atom"
            input = """(do (def! a (atom 49)) (swap! a (fn* [x y] (+ x y)) 10) @a)"""
            expectedAst = int(59)
        },
        testReadEval {
            description = "read-string: read string w. newline"
            input = """(read-string "\"\n\"")"""
            expectedAst = string("\n")
        },
        testReadEval {
            description = "atom: closures retains atoms"
            input = """(do (def! f (let* (a (atom 2)) (fn* () (deref a)))) (def! a (atom 3)) (f))"""
            expectedAst = int(2)
        },
        testReadEval {
            description = "cons: returns list with new element prepended"
            input = """(cons 0 '(1 2 3 4))"""
            expectedAst = list(int(0), int(1), int(2), int(3), int(4))
        },
        testReadEval {
            description = "cons: vector as second argument"
            input = """(cons 0 [1 2 3 4])"""
            expectedAst = list(int(0), int(1), int(2), int(3), int(4))
        },
        testReadEval {
            description = "concat: returns concatenated list"
            input = """(concat '(1 2) '(3 4) '(5 6) '(7 8))"""
            expectedAst = list(int(1), int(2), int(3), int(4), int(5), int(6), int(7), int(8))
        },
        testReadEval {
            description = "concat: vector parameter should return list"
            input = """(concat [99 98])"""
            expectedAst = list(int(99), int(98))
        },
        testReadEval {
            description = "concat: vector + list + vector"
            input = """(concat [99 98] (list 97 96) [95 94])"""
            expectedAst = list(int(99), int(98), int(97), int(96), int(95), int(94))
        }

    )

    override fun getTests(): List<TestCase> = tests
    override fun run(): Boolean =
        verifyTests(tests)
}
