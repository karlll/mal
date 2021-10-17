package main.kotlin.com.ninjacontrol.mal

sealed class MalType
data class MalList(val items: MutableList<MalType>) : MalType() {
    val head: MalType
        get() = items.firstOrNull() ?: MalNil
    val tail: MalList
        get() = MalList(items = items.drop(1).toMutableList())
    val last: MalType
        get() = items.lastOrNull() ?: MalNil
    val size: Int
        get() = items.size

    fun isEmpty() = items.isEmpty()
    fun getOrNull(index: Int) = items.getOrNull(index)
    fun get(index: Int) = items[index]
    fun forEach(function: (MalType) -> Unit) {
        items.forEach(function)
    }

    fun subList(fromIndex: Int, toIndex: Int) = MalList(items.subList(fromIndex, toIndex))
}

data class MalVector(val items: MutableList<MalType>) : MalType() {
    val size: Int
        get() = items.size

    fun isEmpty() = items.isEmpty()
}

fun MalList.asTupleList(): List<List<MalType>> =
    items.windowed(size = 2, step = 2, partialWindows = false)

fun MalList.asVector(): MalVector = MalVector(items = items)
fun MalVector.asList(): MalList = MalList(items = items)

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
fun list(vararg items: MalType): MalList = MalList(items.toMutableList())
fun string(value: String) = MalString(value)
fun int(value: Int) = MalInteger(value)
fun emptyList() = MalList(items = mutableListOf())

typealias Arguments = Array<MalType>
typealias FunctionBody = (args: Arguments) -> MalType

class MalFunction(private val functionBody: FunctionBody) : MalType() {
    fun apply(args: MalList): MalType = functionBody.invoke(args.items.toTypedArray())
    override fun hashCode() = functionBody.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MalFunction

        if (functionBody != other.functionBody) return false

        return true
    }
}

data class MalFunctionContainer(
    val ast: MalType,
    val params: MalType,
    val environment: Environment,
    val fn: MalFunction
) : MalType()

operator fun MalInteger.plus(other: MalInteger): MalInteger = MalInteger(value + other.value)
operator fun MalInteger.minus(other: MalInteger): MalInteger = MalInteger(value - other.value)
operator fun MalInteger.times(other: MalInteger): MalInteger = MalInteger(value * other.value)
operator fun MalInteger.div(other: MalInteger): MalInteger = MalInteger(value / other.value)
operator fun MalInteger.rem(other: MalInteger): MalInteger = MalInteger(value % other.value)
operator fun MalInteger.compareTo(other: MalInteger): Int = when {
    value < other.value -> -1
    value > other.value -> 1
    else -> 0
}

val MalInteger.isZero
    get() = value == 0
