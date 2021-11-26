package main.kotlin.com.ninjacontrol.mal.test

import main.kotlin.com.ninjacontrol.mal.MalException
import main.kotlin.com.ninjacontrol.mal.MalType
import main.kotlin.com.ninjacontrol.mal.re
import main.kotlin.com.ninjacontrol.mal.replExecutionEnv
import kotlin.system.exitProcess

interface TestSuite {

    val name: String

    val pass: String
        get() = "[✅]"
    val fail: String
        get() = "[❌]"
    val passText: String
        get() = "PASS"
    val failText: String
        get() = "FAIL"

    fun log(message: String, newline: Boolean = true) {
        if (newline) println(message) else print(message)
    }

    fun getTests(): List<TestCase>
    fun verifyTests(testCases: List<TestCase>): Boolean {
        return testCases.map { testCase ->
            val context = testCase.description ?: "(no description)"
            try {

                testCase.verify()
                log("$pass $context")
                true
            } catch (e: AssertionException) {
                e.message?.let { log("$fail $context: $it") } ?: log("$fail $context")
                false
            }
        }.reduce { acc, result -> acc && result }
    }

    fun run(): Boolean
}

abstract class TestCase(var description: String? = null) {
    var verify: () -> Unit = { assertNeverExecuted() }
    var only = false
}

class ReadEvalTestCase : TestCase() {
    lateinit var input: String
    lateinit var expectedAst: MalType
}

class DefaultTestCase : TestCase()

class CustomSuite(private val testCases: List<TestCase>) : TestSuite {
    override val name = "Custom"
    override fun getTests(): List<TestCase> = testCases
    override fun run(): Boolean = verifyTests(testCases)
}

class AllTests : TestSuite {

    override val name = "All tests"

    private val testSuites = listOf(
        EnvironmentTest(),
        NamespaceTest(),
        StringTest(),
        EvaluationTest(),
        FunctionsTest(),
        QuoteTest(),
        MacroTest(),
        MetadataTest()
    )

    override fun getTests() = testSuites.flatMap { testSuite -> testSuite.getTests() }

    override fun run(): Boolean {
        val only = getTests().filter { it.only }
        val suite = when {
            only.isNotEmpty() -> listOf(CustomSuite(testCases = only))
            else -> testSuites
        }
        return suite.map { testSuite ->
            log(testSuite.name)
            testSuite.run()
        }
            .reduce { acc, result -> acc && result }
    }
}

fun test(case: DefaultTestCase.() -> Unit): TestCase {
    val t = DefaultTestCase()
    t.case()
    return t
}

fun testReadEval(case: ReadEvalTestCase.() -> Unit): TestCase {
    val t = ReadEvalTestCase()
    t.case()
    t.verify = { assertReadEval(input = t.input, result = t.expectedAst) }
    return t
}

inline fun <reified T : MalException> testReadEvalThrows(
    exception: T,
    crossinline case: ReadEvalTestCase.() -> Unit
): TestCase {
    val t = ReadEvalTestCase()
    var caught: Throwable? = null
    t.case()
    t.verify = {
        try {
            re(t.input, replExecutionEnv)
        } catch (e: Throwable) {
            caught = e
        }
        when {
            caught == null -> throw AssertionException("No exception thrown")
            caught !is T -> throw AssertionException("Unexpected exception, $caught")
            (caught as T).message != exception.message -> throw AssertionException("Expected exception with message '${exception.message}' but got '${(caught as T).message}' instead")
        }
    }
    return t
}

fun runSuite() {
    val tests = AllTests()
    var result = false
    try {
        result = tests.run()
    } catch (e: Throwable) {
        result = false
        tests.log("*** Got exception ${e.javaClass}, '${e.message}'")
    } finally {
        tests.log("--------------------")
        tests.log("Test finished: ", newline = false)
        when (result) {
            true -> {
                tests.log(tests.passText); exitProcess(0)
            }
            false -> {
                tests.log(tests.failText); exitProcess(1)
            }
        }
    }
}
