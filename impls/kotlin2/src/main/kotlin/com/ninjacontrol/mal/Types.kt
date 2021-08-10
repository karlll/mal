package main.kotlin.com.ninjacontrol.mal

sealed class MalType
data class MalList(val items: MutableList<MalType>) : MalType()
data class MalVector(val items: MutableList<MalType>) : MalType()
data class MalMap(val items: MutableMap<MalType, MalType>) : MalType()
data class MalError(val message: String) : MalType()
data class MalSymbol(val name: String) : MalType()
data class MalInteger(val value: Int) : MalType()
data class MalBoolean(val value: Boolean) : MalType()
data class MalString(val value: String) : MalType()
data class MalKeyword(val name: String) : MalType()
object MalEOF : MalType()
object MalNil : MalType()

val True = MalBoolean(value = true)
val False = MalBoolean(value = false)

fun symbol(name: String) = MalSymbol(name)

typealias Arguments = Array<MalType>
typealias FunctionBody = (args: Arguments) -> MalType

class MalFunction(private val functionBody: FunctionBody) : MalType() {
    fun apply(args: Array<MalType>): MalType = functionBody.invoke(args)
}

operator fun MalInteger.plus(other: MalInteger): MalInteger = MalInteger(value + other.value)
operator fun MalInteger.minus(other: MalInteger): MalInteger = MalInteger(value - other.value)
operator fun MalInteger.times(other: MalInteger): MalInteger = MalInteger(value * other.value)
operator fun MalInteger.div(other: MalInteger): MalInteger = MalInteger(value / other.value)
operator fun MalInteger.rem(other: MalInteger): MalInteger = MalInteger(value % other.value)
val MalInteger.isZero
    get() = value == 0

fun func(precondition: ((Arguments) -> MalError?)? = null, function: FunctionBody): MalFunction =
    MalFunction { args ->
        if (precondition != null) {
            when (val preconditionResult = precondition.invoke(args)) {
                is MalError -> return@MalFunction preconditionResult
            }
        }
        function.invoke(args)
    }

inline fun <reified T> assertArgumentType(args: Arguments) = args.all { arg -> arg is T }

fun integerFunction(function: FunctionBody): MalFunction = func(
    precondition = { args ->
        if (!assertArgumentType<MalInteger>(args)) {
            MalError("Invalid argument type")
        } else null
    }
) { args ->
    function.invoke(args)
}
