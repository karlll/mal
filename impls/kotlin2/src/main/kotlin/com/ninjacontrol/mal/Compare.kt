package main.kotlin.com.ninjacontrol.mal

infix fun MalType.eq(other: MalType): Boolean = isEqual(this, other)

fun isEqual(a: MalType, b: MalType): Boolean {
    return when {
        a is MalSymbol && b is MalSymbol -> a.name == b.name
        a is MalInteger && b is MalInteger -> a.value == b.value
        a is MalVector && b is MalVector -> compareList(a.items, b.items)
        a is MalList && b is MalList -> compareList(a.items, b.items)
        a is MalList && b is MalVector -> compareList(a.items, b.items)
        a is MalVector && b is MalList -> compareList(a.items, b.items)
        a is MalNil && b is MalNil -> true
        a is MalEOF && b is MalEOF -> true
        a is MalError && b is MalError -> a.message == b.message
        a is MalBoolean && b is MalBoolean -> a.value == b.value
        a is MalFunction && b is MalFunction -> a == b
        a is MalString && b is MalString -> a.value == b.value
        a is MalKeyword && b is MalKeyword -> a.name == b.name
        a is MalMap && b is MalMap -> false // FIXME: tbd
        a is MalAtom && b is MalAtom -> isEqual(a.value, b.value)
        else -> false
    }
}

fun compareList(aItems: List<MalType>, bItems: List<MalType>): Boolean {
    if (aItems.size != bItems.size) return false
    return aItems.zip(bItems).none { (a, b) -> !isEqual(a, b) }
}
