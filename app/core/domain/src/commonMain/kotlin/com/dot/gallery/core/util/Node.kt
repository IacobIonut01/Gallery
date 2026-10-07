package com.dot.gallery.core.util

sealed interface Node {
    class Eq(val lhs: Node, val rhs: Node) : Node
    class Or(val lhs: Node, val rhs: Node) : Node
    class And(val lhs: Node, val rhs: Node) : Node
    class Like(val lhs: Node, val rhs: Node) : Node
    class In(val lhs: Node, val rhs: Node) : Node
    class Literal<T>(val `val`: T) : Node

    fun build(): String = when (this) {
        is Eq -> "${lhs.build()} = ${rhs.build()}"
        is Or -> "(${lhs.build()}) OR (${rhs.build()})"
        is And -> "(${lhs.build()}) AND (${rhs.build()})"
        is Like -> "${lhs.build()} LIKE ${rhs.build()}"
        is In -> "${lhs.build()} IN ${rhs.build()}"
        is Literal<*> -> "$`val`"
    }
}
