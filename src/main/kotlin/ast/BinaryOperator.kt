package ast

import parser.exceptions.ParseException.UnknownBinaryOpException

enum class BinaryOperator(val op : String) {

    MUL("*"),
    DIV("/"),
    MOD("%"),
    ADD("+"),
    SUB("-"),
    GTE(">="),
    LTE("<="),
    GT (">"),
    LT ("<"),
    EQ ("=="),
    NEQ("!="),
    AND("&&"),
    OR ("||");

    companion object {
        private val keyValueMap = values().map { it.op }.zip(values()).toMap()
        fun read(op : String) : BinaryOperator =
                keyValueMap[op]?: throw UnknownBinaryOpException(op)
    }

    fun getPriority() : Int = when(this) {
        MUL, DIV, MOD -> 1
        ADD, SUB -> 2
        GTE, GT, LTE, LT -> 3
        EQ, NEQ -> 4
        AND, OR -> 5
    }
}