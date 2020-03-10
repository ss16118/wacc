package ast

import ast.Expression.PairElemFunction
import ast.Expression.PairElemFunction.FST
import ast.Expression.PairElemFunction.SND
import ast.Type.BaseTypeKind.*
import ast.Type.TypeVar.Companion.newTypeVar
import exceptions.SemanticException
import exceptions.SemanticException.NoUnificationFoundForTypesException
import utils.SymbolTable

sealed class Type {

    enum class BaseTypeKind(val symbol: String) {
        INT("int"),
        BOOL("bool"),
        CHAR("char"),
        STRING("string"),
        ANY("var")
    }

    companion object {
        fun anyArrayType() = arrayTypeOf(anyType())
        fun arrayTypeOf(type: Type) = NewType("array", type)
        fun pairTypeOf(left: Type, right: Type) = NewType("pair", left, right)
        fun anyPairType() = pairTypeOf(anyType(), anyType())
        fun newPairConstructorType(): FuncType {
            val fst = anyType()
            val snd = anyType()
            return FuncType(pairTypeOf(fst, snd), listOf(fst, snd))
        }
        fun arrLitConstructorType(length: Int): FuncType {
            val elemType = anyType()
            return FuncType(arrayTypeOf(elemType), (1..length).map { elemType })
        }

        fun anyType(): Type = newTypeVar()

        fun intType(): BaseType = BaseType(INT)
        fun boolType(): BaseType = BaseType(BOOL)
        fun charType(): BaseType = BaseType(CHAR)
        fun stringType(): BaseType = BaseType(STRING)
        fun rangeTypeOf(type: Type) = NewType("Range", type)
    }


    data class BaseType(val kind: BaseTypeKind) : Type() {
        override fun toString(): String = kind.symbol
        override fun containsTypeVar(name: String): Boolean = false
    }

    data class NewType(val name: String, val generics: List<Type> = emptyList()): Type() {
        constructor(name: String, vararg generics: Type): this(name, generics.asList())
        override fun toString(): String {
            if(name == "array") {
                return "${generics[0]}[]"
            } else if(name == "pair") {
                return "pair(${generics[0]}, ${generics[1]})"
            }
            return "${name}${if(generics.isEmpty())"" else "<${generics.joinToString(", ")}>"}"
        }

        override fun printAsLabel(): String {
            return "${name}${if(generics.isEmpty())"" else "_${generics.joinToString("_") { it.printAsLabel() }}_"}"
        }

        override fun bindConstraints(constraints: List<TypeConstraint>): Type {
            return NewType(name, generics.map { it.bindConstraints(constraints) })
        }

        override fun substitutes(substitutions: Map<Pair<String, Boolean>, Type>): Type {
            return NewType(name, generics.map { it.substitutes(substitutions) })
        }

        override fun isDetermined(): Boolean = generics.all { it.isDetermined() }
        override fun isGround(): Boolean = generics.all { it.isGround() }
        override fun reified(constraints: List<TypeConstraint>): Type {
            return NewType(name, generics.map { it.reified(constraints) })
        }
        override fun containsTypeVar(name: String): Boolean = generics.any { it.containsTypeVar(name) }
    }

    data class TypeVar(val name: String, val traits: List<Trait>, val isReified: Boolean = false): Type() {
        constructor(name: String, vararg traits: Trait): this(name, traits.toList())

        companion object {
            var nameGen = 0
            fun newTypeVar() = TypeVar("N${nameGen++}")
        }

        override fun toString(): String = if(isReified) "@_$name" else "_$name"
        override fun reified(constraints: List<TypeConstraint>): Type =
                TypeVar(name, constraints.filter { it.typeVar == name }.map { it.trait },true)
        override fun bindConstraints(constraints: List<TypeConstraint>): Type {
            return TypeVar(name, traits + constraints.filter { it.typeVar == name }.map { it.trait }, isReified)
        }
        override fun substitutes(substitutions: Map<Pair<String, Boolean>, Type>): Type {
            return substitutions[name to isReified]?:this
        }
        override fun equals(other: Any?): Boolean {
            return other is TypeVar && other.name == name && other.isReified == isReified
        }
        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + traits.hashCode()
            result = 31 * result + isReified.hashCode()
            return result
        }

        override fun isDetermined(): Boolean = isReified
        override fun isGround(): Boolean = false
        override fun containsTypeVar(name: String): Boolean = name == this.name
    }

    object ErrorType: Type() {
        override fun toString(): String = "_error"
        override fun isDetermined(): Boolean = true
        override fun isGround(): Boolean = true
        override fun containsTypeVar(name: String): Boolean = true
    }

    data class FuncType(val retType: Type,
                        val paramTypes: List<Type>) : Type() {
        companion object {
            fun binOpOf(type: Type): FuncType = FuncType(type, listOf(type, type))
            fun binCheckOf(type: Type): FuncType = FuncType(boolType(), listOf(type, type))
        }

        override fun toString(): String {
            val constraints = if (collectConstraints().isNotEmpty()) "[${collectConstraints().joinToString(", ")}] => " else ""
            return "$constraints(${paramTypes.joinToString(", ") { it.toString() }}) -> $retType"
        }

        override fun printAsLabel(): String {
            return "${paramTypes.joinToString("_") { it.printAsLabel() } }__${retType.printAsLabel()}"
        }

        override fun containsTypeVar(name: String): Boolean = (paramTypes + retType).any { it.containsTypeVar(name) }

        override fun reified(constraints: List<TypeConstraint>): Type = FuncType(
                retType.reified(constraints),
                paramTypes.map { it.reified(constraints) }
        )

        override fun bindConstraints(constraints: List<TypeConstraint>): Type {
            return FuncType(retType.bindConstraints(constraints), paramTypes.map { it.bindConstraints(constraints) })
        }

        override fun substitutes(substitutions: Map<Pair<String, Boolean>, Type>): Type {
            val params = paramTypes.map { it.substitutes(substitutions) }
            return FuncType(retType.substitutes(substitutions), params)
        }

        fun collectConstraints(): List<TypeConstraint> {
            val collect = mutableMapOf<String, List<Trait>>()
            (paramTypes + retType).filterIsInstance<TypeVar>().forEach { tvar ->
                if (tvar.name in collect && collect[tvar.name]!!.toHashSet() != tvar.traits.toHashSet() ) {
                    throw IllegalArgumentException("inconsistent type vars!")
                } else {
                    collect[tvar.name] = tvar.traits
                }
            }
            return collect.flatMap { (name, traits) -> traits.map { TypeConstraint(it, name) } }
        }

        override fun isDetermined(): Boolean = (paramTypes + retType).all { it.isDetermined() }
        override fun isGround(): Boolean = (paramTypes + retType).all { it.isGround() }
    }

    open fun reified(constraints: List<TypeConstraint>): Type = this

    open fun bindConstraints(constraints: List<TypeConstraint>): Type = this

    open fun substitutes(substitutions: Map<Pair<String, Boolean>, Type>): Type = this

    open fun isDetermined(): Boolean = true

    open fun isGround(): Boolean = true

    open fun printAsLabel(): String = toString()

    /* Find the mgu that unifies this type to the original type.
    *  Throws a semantic error when unable to find such unifier. */
    fun findUnifier(original: Type, oldMgu: Map<Pair<String, Boolean>, Type> = mutableMapOf()): Map<Pair<String, Boolean>, Type> {
        val actual = this.substitutes(oldMgu)
        if (actual == ErrorType) {
            return oldMgu
        }
        return when(original) {
            is BaseType -> if(actual == original) oldMgu else throw NoUnificationFoundForTypesException(actual, original)
            is NewType -> when {
                actual is NewType && actual.name == original.name -> {
                    actual.generics.zip(original.generics)
                            .fold(oldMgu) { mgu, (new, old) -> new.findUnifier(old, mgu) }
                }
                else -> throw NoUnificationFoundForTypesException(actual, original)
            }
            is TypeVar -> {
                val key = original.name to original.isReified
                if (key in oldMgu) {
                    val oldVal = oldMgu.getValue(key)
                    oldMgu + actual.findUnifier(oldVal)
                }
                oldMgu + (key to actual)
            }
            is FuncType -> when {
                actual is FuncType && actual.paramTypes.size == original.paramTypes.size -> {
                    (actual.paramTypes + actual.retType).zip(original.paramTypes + original.retType)
                            .fold(oldMgu) { mgu, (new, old) -> new.findUnifier(old, mgu)  }
                }
                else -> throw NoUnificationFoundForTypesException(actual, original)
            }
            is ErrorType -> oldMgu
        }
    }

    /* Determine whether this type is instance of all traits provided, in the given symbol table. */
    fun instanceOf(traits: List<Trait>, symbolTable: SymbolTable): Type {
        return if (traits.all { symbolTable.isInstance(this, it) }) {
            this
        } else {
            throw SemanticException.TypeNotSatisfyingTraitsException(this, traits)
        }
    }

    /* Infer the current type from the provided expecting type. */
    fun inferFrom(expecting: Type, symbolTable: SymbolTable): Type {
        val actual = this
        if (actual is ErrorType) {
            return actual
        }
        System.err.println("Inferring expected: $expecting <==> actual: $actual")
        return when(expecting) {
            is BaseType -> when(actual) {
                is BaseType -> if (expecting == actual) actual else throw SemanticException.TypeMismatchException(expecting, actual)
                is TypeVar -> expecting.instanceOf(actual.traits, symbolTable)
                else -> throw SemanticException.TypeMismatchException(expecting, actual)
            }
            is NewType -> when(actual) {
                is NewType -> {
                    if (actual.name == expecting.name && actual.generics.size == expecting.generics.size) {
                        NewType(actual.name, actual.generics
                                .zip(expecting.generics) { ga, ge -> ga.inferFrom(ge, symbolTable) })
                    } else {
                        throw SemanticException.TypeMismatchException(expecting, actual)
                    }
                }
                is TypeVar -> expecting.instanceOf(actual.traits, symbolTable)
                else -> throw SemanticException.TypeMismatchException(expecting, actual)
            }
            is TypeVar -> if(expecting.isReified) {
                when(actual) {
                    is TypeVar -> if(actual.isReified) {
                        if(actual == expecting) expecting else
                            throw SemanticException.TypeMismatchException(expecting, actual)
                    } else {
                        expecting.instanceOf(actual.traits, symbolTable)
                    }
                    else -> throw SemanticException.TypeMismatchException(expecting, actual)
                }

            } else {
                actual.instanceOf(expecting.traits, symbolTable)
            }
            is FuncType -> when(actual) {
                is TypeVar -> expecting.instanceOf(actual.traits, symbolTable)
                is FuncType -> {
                    if (actual.paramTypes.size == expecting.paramTypes.size) {
                        val mgu = actual.findUnifier(expecting)
                        actual.substitutes(mgu)
                    } else {
                        throw SemanticException.TypeMismatchException(expecting, actual)
                    }
                }
                else -> throw SemanticException.TypeMismatchException(expecting, actual)
            }
            ErrorType -> actual
        }.also { System.err.println("We get: $it") }
    }

    fun unwrapArrayType(): Type? = when {
        this is NewType && name == "array" -> generics[0]
        this is TypeVar && !this.isReified -> TypeVar(name + 1)
        else -> null
    }

    fun unwrapArrayType(count: Int): Type? {
        var t: Type? = this
        for (i in 0 until count) {
            t = t?.unwrapArrayType()
            if (t == null) {
                return null
            }
        }
        return t
    }

     abstract fun containsTypeVar(name: String): Boolean
}

