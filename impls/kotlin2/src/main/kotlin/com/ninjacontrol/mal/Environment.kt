package main.kotlin.com.ninjacontrol.mal

typealias EnvironmentMap = MutableMap<MalSymbol, MalType>

class Environment(private val outer: Environment? = null) {
    private val data: EnvironmentMap = mutableMapOf()
    fun set(symbol: MalSymbol, value: MalType): MalType {
        data[symbol] = value
        return value
    }

    fun find(symbol: MalSymbol): Environment? {
        return when (data.containsKey(symbol)) {
            true -> this
            false -> outer?.find(symbol)
        }
    }

    fun get(symbol: MalSymbol) = find(symbol)?.let { env ->
        env.data[symbol]
    } ?: MalError("Symbol '${symbol.name}' not found")

    fun add(environment: EnvironmentMap) {
        data.putAll(from = environment)
    }
}

val builtIn: EnvironmentMap = mutableMapOf(
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
