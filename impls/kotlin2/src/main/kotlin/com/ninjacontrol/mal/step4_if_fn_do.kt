package main.kotlin.com.ninjacontrol.mal

import main.kotlin.com.ninjacontrol.mal.test.runSuite
import kotlin.system.exitProcess

fun read(input: String) = readStr(input)
fun eval(ast: MalType, env: Environment) = when (ast) {
    is MalList -> when (ast.size) {
        0 -> ast
        else ->
            if (ast.head is MalSymbol) {
                val head = ast.head as MalSymbol
                when (head.name) {
                    "def!" -> define(ast.tail, env)
                    "let*" -> let(ast.tail, env)
                    "do" -> `do`(ast.tail, env)
                    "if" -> `if`(ast.tail, env)
                    "fn*" -> fn(ast.tail, env)
                    "quote" -> quote(ast.tail, env)
                    else -> applyFunction(list = ast, env)
                }
            } else {
                applyFunction(list = ast, env)
            }
    }
    else -> evalAst(ast, env)
}

fun fn(expressions: MalList, env: Environment): MalType {
    if (expressions.size != 2) {
        return MalError("Invalid number of arguments, expected 2")
    }
    if (expressions.get(0) !is MalList) {
        return MalError("Error creating bindings, invalid type, expected list.")
    }
    val functionBindings = expressions.get(0) as MalList
    return MalFunction { functionArguments ->
        val newEnv = Environment.withBindings(
            env,
            bindings = functionBindings.items,
            expressions = functionArguments.toList()
        )
        return@MalFunction newEnv?.let { eval(expressions.get(1), newEnv) }
            ?: MalError("Error creating environment.")
    }
}

fun `if`(expressions: MalList, env: Environment): MalType {
    if (expressions.size < 2) return MalError("Invalid conditional expression")
    return when (val condition = eval(expressions.get(0), env)) {
        is MalError -> MalError("Error when evaluating condition, ${condition.message}")
        is MalBoolean, is MalNil -> when (condition) {
            False, MalNil -> expressions.getOrNull(2)?.let { eval(it, env) } ?: MalNil
            else -> eval(expressions.get(1), env)
        }
        else -> eval(expressions.get(1), env)
    }
}

fun `do`(expressions: MalList, env: Environment): MalType {
    var evaluated: MalType? = null
    expressions.forEach { expression ->
        evaluated = evalAst(expression, env)
        if (evaluated is MalError) { // TODO: toggle break on error
            return@forEach
        }
    }
    return evaluated ?: MalNil
}

fun define(bindingList: MalList, env: Environment): MalType {
    return when (bindingList.size) {
        2 -> {
            when (val name = bindingList.get(0)) {
                is MalSymbol -> {
                    when (val value = eval(bindingList.get(1), env)) {
                        is MalError -> MalError("Could not define symbol '${name.name}' (${value.message})")
                        else -> env.set(name, value)
                    }
                }
                else -> {
                    MalError("Invalid argument (symbol)")
                }
            }
        }
        else -> MalError("Invalid arguments")
    }
}

fun let(expressions: MalList, env: Environment): MalType {

    fun evaluateWithBindings(
        expression: MalType,
        bindings: List<MalType>,
        env: Environment
    ): MalType =
        if (bindings.size % 2 == 0) {
            bindings.chunked(2).forEach {
                val key = it[0]
                val evaluated = eval(it[1], env)
                if (evaluated is MalError) {
                    return@evaluateWithBindings MalError("Error evaluating environment, ${evaluated.message}")
                } else if (key !is MalSymbol) {
                    return@evaluateWithBindings MalError("Error evaluating environment, key must be a symbol")
                } else {
                    env.set(key, evaluated)
                }
            }
            eval(expression, env)
        } else {
            MalError("Invalid binding list (odd number of items)")
        }

    return when (expressions.size) {
        2 -> {
            val newEnv = Environment(outer = env)
            val newBindings = expressions.get(0)
            val expression = expressions.get(1)
            when (newBindings) {
                is MalList -> evaluateWithBindings(expression, newBindings.items, newEnv)
                is MalVector -> evaluateWithBindings(expression, newBindings.items, newEnv)
                else -> MalError("Invalid binding (not a list or vector)")
            }
        }
        else -> MalError("Invalid number of arguments")
    }
}

fun applyFunction(list: MalList, env: Environment): MalType {
    return when (val evaluatedList = evalAst(list, env)) {
        is MalList -> {
            when (val head = evaluatedList.head) {
                is MalFunction ->
                    head.apply(evaluatedList.tail)
                is MalError ->
                    MalError("Cannot apply (${head.message})")
                else ->
                    MalError("Not a function")
            }
        }
        else -> MalError("Cannot apply")
    }
}

fun quote(ast: MalList, env: Environment): MalType {
    return ast.getOrNull(0) ?: MalNil
}

fun evalAst(ast: MalType, env: Environment): MalType = when (ast) {
    is MalSymbol -> env.get(ast)
    is MalList -> MalList(items = ast.items.map { item -> eval(item, env) }.toMutableList())
    is MalVector -> MalVector(items = ast.items.map { item -> eval(item, env) }.toMutableList())
    is MalMap -> MalMap(
        items = ast.items.mapValues { (_, value) -> eval(value, env) }
            .toMutableMap()
    )
    else -> ast
}

fun print(input: MalType, printReadably: Boolean = true) =
    out(printString(input, printReadably = printReadably))

fun re(input: String, env: Environment) = eval(read(input), env = env)
fun rep(input: String, env: Environment) = print(re(input, env = env))
val replExecutionEnv = Environment().apply { add(namespace) }

tailrec fun mainLoop() {
    out(prompt(), newLine = false)
    readLine()?.let { input ->
        rep(input, replExecutionEnv)
    } ?: run {
        out("** Exiting.")
        exitProcess(0)
    }
    mainLoop()
}

const val runTests = false

fun main() = if (!runTests) mainLoop() else runSuite()
