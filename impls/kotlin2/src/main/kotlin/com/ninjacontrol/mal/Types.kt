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
