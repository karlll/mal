package main.kotlin.com.ninjacontrol.mal

import kotlin.system.exitProcess

fun read(input: String) = readStr(input)
fun eval(ast: MalType, env: Environment) = when (ast) {
    is MalList -> when (ast.items.size) {
        0 -> ast
        else -> applyFunction(ast, env)
    }
    else -> evalAst(ast, env)
}

fun applyFunction(list: MalList, env: Environment): MalType {
    return when (val evaluatedList = evalAst(list, env)) {
        is MalList -> {
            val items = evaluatedList.items
            val function = items.head
            val args = when (items.size) {
                1 -> emptyList()
                else -> items.tail
            }
            when (function) {
                is MalFunction ->
                    function.apply(args.toTypedArray())
                is MalError ->
                    MalError("Cannot apply ('${function.message}')")
                else ->
                    MalError("Not a function")
            }
        }
        else -> MalError("Cannot apply")
    }
}

fun evalAst(ast: MalType, env: Environment): MalType = when (ast) {
    is MalSymbol -> env.getOrElse(ast) { return MalError("Symbol not found: '${ast.name}'") }
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

typealias Environment = Map<MalSymbol, MalFunction>

val replExecutionEnv: Environment = mapOf(
    symbol("+") to arithmeticFunction(ArithmeticOperation.Add),
    symbol("-") to arithmeticFunction(ArithmeticOperation.Subtract),
    symbol("*") to arithmeticFunction(ArithmeticOperation.Multiply),
    symbol("/") to arithmeticFunction(ArithmeticOperation.Divide),
    symbol("%") to arithmeticFunction(ArithmeticOperation.Modulo),
)

enum class ArithmeticOperation {
    Add,
    Subtract,
    Multiply,
    Divide,
    Modulo
}

fun arithmeticFunction(operation: ArithmeticOperation) = integerFunction { args ->
    if (operation == ArithmeticOperation.Divide && args.drop(1)
        .any { (it as MalInteger).isZero }
    ) {
        return@integerFunction MalError("Division by zero")
    }
    args.reduce { acc, arg ->
        val op1 = acc as MalInteger
        val op2 = arg as MalInteger
        when (operation) {
            ArithmeticOperation.Add -> op1 + op2
            ArithmeticOperation.Subtract -> op1 - op2
            ArithmeticOperation.Multiply -> op1 * op2
            ArithmeticOperation.Divide -> op1 / op2
            ArithmeticOperation.Modulo -> op1 % op2
        }
    }
}

val <T> List<T>.tail: List<T>
    get() = drop(1)

val <T> List<T>.head: T
    get() = first()
