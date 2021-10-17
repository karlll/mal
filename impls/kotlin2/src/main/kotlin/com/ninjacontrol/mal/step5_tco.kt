package main.kotlin.com.ninjacontrol.mal

import main.kotlin.com.ninjacontrol.mal.test.runSuite
import kotlin.system.exitProcess

fun read(input: String) = readStr(input)
fun __eval(ast: MalType, env: Environment) = when (ast) {
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

object Symbols {
    val def = MalSymbol("def!")
    val let = MalSymbol("let*")
    val `do` = MalSymbol("do")
    val `if` = MalSymbol("if")
    val fn = MalSymbol("fn*")
    val quote = MalSymbol("quote")
}

fun eval(ast: MalType, env: Environment): MalType {
    var currentAst = ast
    var nextAst = ast
    var currentEnv = env
    var nextEnv = currentEnv

    /**
     * Returns null when a new environment and ast has been set, MalType otherwise
     */
    val apply: () -> MalType? = {
        when (val evaluatedList = evalAst(currentAst, currentEnv)) {
            is MalList -> {
                when (val f = evaluatedList.head) {
                    is MalFunctionContainer -> {
                        val params = when (f.params) {
                            is MalList -> f.params
                            is MalVector -> f.params.asList()
                            else -> null
                        }
                        if (params == null) MalError("Invalid parameter type")
                        else {
                            when (
                                val newEnv = Environment.withBindings(
                                    outer = currentEnv,
                                    bindings = params.items,
                                    expressions = evaluatedList.tail.items
                                )
                            ) {
                                null -> MalError("Error creating environment")
                                else -> {
                                    nextEnv = newEnv
                                    nextAst = f.ast
                                    null
                                }
                            }
                        }
                    }
                    is MalFunction ->
                        f.apply(evaluatedList.tail)
                    is MalError ->
                        MalError("Cannot apply (${f.message})")
                    else ->
                        MalError("Not a function")
                }
            }
            else -> MalError("Cannot apply")
        }
    }
    while (true) {
        if (currentAst !is MalList) return evalAst(currentAst, currentEnv)
        if (currentAst.isEmpty()) return currentAst
        val head = currentAst.head

        when {
            head eq Symbols.def -> return define(currentAst.tail, currentEnv)
            head eq Symbols.let -> {
                val (resultAst, newEnv) = let2(currentAst.tail, currentEnv)
                if (resultAst is MalError) {
                    return resultAst
                }
                nextAst = resultAst
                newEnv?.let { nextEnv = newEnv }
            }
            head eq Symbols.`do` -> nextAst = `do2`(currentAst.tail, currentEnv)
            head eq Symbols.`if` -> nextAst = `if2`(currentAst.tail, currentEnv)
            head eq Symbols.fn -> nextAst = fn2(currentAst.tail, currentEnv)
            head eq Symbols.quote -> return quote(currentAst.tail, currentEnv)
            else -> apply()?.let {
                return it
            }
        }
        currentAst = nextAst
        currentEnv = nextEnv
    }
}

fun fn2(expressions: MalList, env: Environment): MalType {

    if (expressions.size != 2) {
        return MalError("Invalid number of arguments, expected 2")
    }
    val functionBindings: List<MalType> = when (val bindings = expressions.get(0)) {
        is MalList -> bindings.items
        is MalVector -> bindings.items
        else -> return MalError("Error creating bindings, invalid type, expected list or vector")
    }
    val fn = MalFunction { functionArguments ->
        val newEnv = Environment.withBindings(
            env,
            bindings = functionBindings,
            expressions = functionArguments.toList()
        )
        return@MalFunction newEnv?.let { eval(expressions.get(1), newEnv) }
            ?: MalError("Error creating environment.")
    }
    return MalFunctionContainer(
        ast = expressions.get(1),
        params = expressions.get(0),
        environment = env,
        fn = fn
    )
}

fun fn(expressions: MalList, env: Environment): MalType {
    if (expressions.size != 2) {
        return MalError("Invalid number of arguments, expected 2")
    }

    val functionBindings: List<MalType> = when (val bindings = expressions.get(0)) {
        is MalList -> bindings.items
        is MalVector -> bindings.items
        else -> return MalError("Error creating bindings, invalid type, expected list or vector")
    }
    return MalFunction { functionArguments ->
        val newEnv = Environment.withBindings(
            env,
            bindings = functionBindings,
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

fun `if2`(expressions: MalList, env: Environment): MalType {
    if (expressions.size < 2) return MalError("Invalid conditional expression")
    return when (val condition = eval(expressions.get(0), env)) {
        is MalError -> MalError("Error when evaluating condition, ${condition.message}")
        is MalBoolean, is MalNil -> when (condition) {
            False, MalNil -> expressions.getOrNull(2) ?: MalNil
            else -> expressions.get(1)
        }
        else -> expressions.get(1)
    }
}

fun `do`(expressions: MalList, env: Environment): MalType {
    return when (val evaluated = evalAst(expressions, env)) {
        is MalList -> evaluated.last
        else -> evaluated
    }
}

fun `do2`(expressions: MalList, env: Environment): MalType {
    if (expressions.isEmpty()) return MalNil
    evalAst(expressions.subList(0, expressions.size), env)
    return expressions.last
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

fun let2(expressions: MalList, env: Environment): Pair<MalType, Environment?> {

    fun evaluateWithBindings(
        expression: MalType,
        bindings: List<MalType>,
        env: Environment
    ): Pair<MalType, Environment?> =
        if (bindings.size % 2 == 0) {
            bindings.chunked(2).forEach {
                val key = it[0]
                val evaluated = eval(it[1], env)
                if (evaluated is MalError) {
                    return@evaluateWithBindings MalError("Error evaluating environment, ${evaluated.message}") to null
                } else if (key !is MalSymbol) {
                    return@evaluateWithBindings MalError("Error evaluating environment, key must be a symbol") to null
                } else {
                    env.set(key, evaluated)
                }
            }
            expression to env // TCO
        } else {
            MalError("Invalid binding list (odd number of items)") to null
        }

    return when (expressions.size) {
        2 -> {
            val newEnv = Environment(outer = env)
            val newBindings = expressions.get(0)
            val expression = expressions.get(1)
            when (newBindings) {
                is MalList -> evaluateWithBindings(expression, newBindings.items, newEnv)
                is MalVector -> evaluateWithBindings(expression, newBindings.items, newEnv)
                else -> MalError("Invalid binding (not a list or vector)") to null
            }
        }
        else -> MalError("Invalid number of arguments") to null
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

fun applyFunction2(list: MalList, env: Environment): MalType {
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

fun main(args: Array<String>) = when {
    args.contains("--runTests") -> runSuite()
    else -> mainLoop()
}
