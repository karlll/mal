package main.kotlin.com.ninjacontrol.mal

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
                    else -> applyFunction(list = ast, env)
                }
            } else {
                ast // Or error?
            }
    }
    else -> evalAst(ast, env)
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

fun let(arguments: MalList, env: Environment): MalType {

    fun evaluateWithBindings(expression: MalType, bindings: List<MalType>, env: Environment): MalType =
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

    return when (arguments.size) {
        2 -> {
            val newEnv = Environment(outer = env)
            val newBindings = arguments.get(0)
            val expression = arguments.get(1)
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

fun print(input: MalType) = printString(input)
fun rep(input: String, env: Environment) = print(eval(read(input), env = env))
val replExecutionEnv = Environment().apply { add(builtIn) }

tailrec fun mainLoop() {
    print(prompt())
    readLine()?.let { input ->
        val output = rep(input, replExecutionEnv)
        println(output)
    } ?: run {
        println("** Exiting.")
        exitProcess(0)
    }
    mainLoop()
}

fun main() = mainLoop()
