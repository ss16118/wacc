package utils

import ast.*
import ast.Expression.Identifier
import ast.Function
import ast.Type.*
import ast.Type.Companion.boolType
import ast.Type.Companion.charType
import exceptions.SemanticException
import exceptions.SemanticException.*
import utils.SymbolTable.TypeAttributes.Companion.arrayAttributes
import java.lang.IllegalArgumentException
import java.util.*

class SymbolTable {
    private val scopeStack: Deque<MutableMap<String, VarAttributes>> = ArrayDeque()
    private val scopeIdStack: Deque<Int> = ArrayDeque()
    private val traitDefs: MutableMap<Trait, TraitAttributes> = mutableMapOf()
    val typedefs: MutableMap<String, TypeAttributes> = mutableMapOf()
    val unionIdMap: MutableMap<String, Int> = mutableMapOf()
    val functions: MutableMap<String, FuncAttributes> = hashMapOf()
    val collect: MutableMap<VarWithSID, VarAttributes> = hashMapOf()
    val scopeDefs: MutableMap<Int, Set<String>> = hashMapOf()
    private var scopeIdGen = 0

    init {
        typedefs += "array" to arrayAttributes()
        traitDefs += Trait("Eq") to TraitAttributes("A", emptySet(), emptyList(), mutableMapOf())
        traitDefs += Trait("Ord") to TraitAttributes("A", emptySet(), emptyList(), mutableMapOf())
        traitDefs += Trait("Malloc") to TraitAttributes("A", emptySet(), emptyList(), mutableMapOf())
        traitDefs += Trait("Show") to TraitAttributes("A", emptySet(), emptyList(), mutableMapOf())
    }

    fun defineVar(type: Type, identNode: Identifier, isConst: Boolean): VarAttributes? {
        val name = identNode.name
        val currScope = this.scopeStack.first()
        val entry = currScope[name]
        if (entry != null) {
            return entry
        }
        val sid = getCurrScopeId()
        identNode.scopeId = sid
        currScope[name] = VarAttributes(type, isConst , getCurrScopeId(), identNode.startIndex)
        return null
    }

    fun defineFunc(func: Function) {
        val entry = functions[func.name]
        if (entry != null) {
            throw MultipleFuncDefException(func.name, entry.type, entry.index)
        }
        functions[func.name] = FuncAttributes(func.getFuncType(), func.args, null, func.startIndex)
    }

    fun defineType(def: NewTypeDef) {
        when (def) {
            is NewTypeDef.StructTypeDef -> {
                val entry = functions[def.name()]
                if (entry != null) {
                    throw MultipleFuncDefException(def.name(), entry.type, entry.index)
                }
                val defaultMallocMap = mutableMapOf(
                        Trait("Malloc") to (1..def.type.generics.size).map { emptySet<Trait>() }
                )
                typedefs[def.type.name] = TypeAttributes(false, defaultMallocMap, setOf(def.name()), def.startIndex)
                functions[def.name()] = FuncAttributes(def.constructorFuncType(), def.members, null, def.startIndex)
            }

            is NewTypeDef.UnionTypeDef -> {
                val entry = typedefs[def.type.name]
                if (entry != null) {
                    throw MultipleFuncDefException("def.name()", def.type, entry.index)
                }
                val defaultMallocMap = mutableMapOf(
                        Trait("Malloc") to (1..def.type.generics.size).map { emptySet<Trait>() }
                )
                typedefs[def.type.name] = TypeAttributes(true, defaultMallocMap, def.memberMap.keys, def.startIndex)
                var unionId = 0
                def.memberMap.forEach { (constructor, params) ->
                    val fentry = functions[constructor]
                    if (fentry != null) {
                        throw MultipleFuncDefException(constructor, fentry.type, fentry.index)
                    }
                    val funType = FuncType(def.type, params.map { it.first })
                    functions[constructor] = FuncAttributes(funType, params, null, def.startIndex)
                    unionIdMap[constructor] = unionId++
                }
            }
        }
    }

    fun defineTrait(traitDef: TraitDef) {
        val trait = Trait(traitDef.traitName)
        val entry = traitDefs[trait]
        if (entry != null) {
            throw MultipleTraitDefException(traitDef.traitName, traitDef.startIndex)
        }
        if(traitDef.typeConstraints.any { it.typeVar != traitDef.traitVar }) {
            throw MultipleTraitDefException(traitDef.traitName, traitDef.startIndex)
        }
        val dups = traitDef.typeConstraints.countDuplicates()
        if(dups.isNotEmpty()) {
            TODO() //error: duplicate dependencies.
        }
        traitDefs[trait] = TraitAttributes(
                traitDef.traitVar,
                traitDef.typeConstraints.map { it.trait }.toSet(),
                traitDef.requiredFuncs
        )
        traitDef.requiredFuncs.forEach { header ->
            functions[header.name] = FuncAttributes(header.getFuncType(), header.args, trait, header.startIndex)
        }
    }

    fun implementTrait(instance: TraitInstance) {
        when(instance.targetType) {
            is PairType -> TODO()
            is TypeVar -> TODO()
            is NewType -> {
                val traitEntry = traitDefs[instance.trait]
                        ?: throw UndefinedTraitException(instance.trait.traitName)
                // check if all dependencies are implemented.
                for (dependency in traitEntry.dependencies) {
                    if (!isInstance(instance.targetType, dependency)) {
                        throw TypeNotInstanceOfADependentTraitException(
                                instance.targetType,
                                instance.trait.traitName,
                                dependency.traitName
                        )
                    }
                }
                // check if required are all defined.
                val sub = mapOf((traitEntry.traitVar to false) to instance.targetType)
                val required = traitEntry.requiredFuncs
                        .map { it.name to it.getFuncType().substitutes(sub) as FuncType }
                        .toMap().toMutableMap()
                val definedFuncNames = mutableSetOf<String>()
                instance.functions.forEach { func ->
                    val funcHeader = func.extractHeader()
                    if (funcHeader.name !in required) {
                        throw UndefinedFuncException(funcHeader.name)
                    }
                    if (funcHeader.name in definedFuncNames) {
                        throw MultipleFuncDefException(funcHeader.name, funcHeader.getFuncType(), 1 to 1)
                    }
                    val requiredType = required.getValue(funcHeader.name)
                    if (requiredType != funcHeader.getFuncType()) {
                        throw TraitRequiredFuncTypeError(instance.trait.traitName, funcHeader.name, requiredType, funcHeader.getFuncType())
                    }
                    definedFuncNames += funcHeader.name

                }
                val typeEntry = typedefs[instance.targetType.name]
                        ?: throw UndefinedTypeException(instance.targetType.name)
                val traitList = instance.targetType.generics.map { tvar ->
                    when{
                        tvar !is TypeVar -> throw InstanceWithGroundGenericTypeException(tvar)
                        else -> {
                            instance.typeConstraints
                                    .filter { it.typeVar == tvar.name }
                                    .map { it.trait }
                                    .toSet()
                        }
                    }
                }
                typeEntry.implementations[instance.trait] = traitList
                for (impl in instance.functions) {
                    if (impl.name in traitEntry.implementations) {
                        traitEntry.implementations[impl.name]!! += impl
                    } else {
                        traitEntry.implementations[impl.name] = mutableListOf(impl)
                    }

                }
            }
            else -> TODO() // error
        }
    }

    fun pushScope() {
        scopeStack.addFirst(hashMapOf())
        scopeIdStack.addFirst(scopeIdGen++)
    }

    fun popScope(): List<String>? {
        val prevId = scopeIdStack.pollFirst()
        return scopeStack.pollFirst()
                ?.also { prev -> collectPrevScope(prevId, prev) }
                ?.filter { (_, attrs) -> attrs.occurrences == 1 }
                ?.map { (ident, attr) ->
                    "Unused variable $ident at ${attr.index}: variable defined but its value is never used"
                }
    }

    fun lookupVar(ident: Identifier, isWrite: Boolean): VarAttributes? {
        val attr = scopeStack
                .mapNotNull { it[ident.name] }
                .firstOrNull()
        if (attr != null) {
            if (attr.isConst && isWrite) {
                throw SemanticException.WriteToConstVarException(ident.name)
            }
            return attr.addOccurrence().also { ident.scopeId = it.scopeId }
        }
        return null
    }

    fun lookupFunc(ident: String): FuncAttributes? = functions[ident]?.addOccurrence()

    fun lookupType(type: NewType): TypeAttributes? = typedefs[type.name]

    fun findConstructorType(constructor: String): NewType? = lookupFunc(constructor)?.type?.retType as? NewType

    fun dumpTable(): String = "${getFuncTable()}\n${getVarTable()}"

    fun dump() {
        println(getVarTable())
        println(getFuncTable())
    }

    fun getVarTable(): String {
        val tp = TablePrinter("variable", "scope id", "type", "defined at", "ref count")
                .markIntColumn(1, 4)
                .sortBy(0, 1)
        collect.map { (pair, attr) ->
            tp.addColumn(pair.first, pair.second, attr.type, attr.index, attr.occurrences)
        }

        return tp.print()
    }

    private fun getFuncTable(): String {
        val tp = TablePrinter("function", "type", "defined at", "ref count")
                .sortBy(0, 2)
        functions.map { (name, attr) ->
            tp.addColumn(name, attr.type, attr.index, attr.occurrences)
        }
        return tp.print()
    }

    fun isInstance(type: Type, trait: Trait): Boolean {
        return when(type) {
            is BaseType -> when(type.kind) {
                BaseTypeKind.INT -> trait.traitName in setOf("Eq", "Ord", "Show", "Num", "Enum", "Read")
                BaseTypeKind.BOOL -> trait.traitName in setOf("Eq", "Ord", "Show", "Enum")
                BaseTypeKind.CHAR -> trait.traitName in setOf("Eq", "Ord", "Show", "Num", "Enum", "Read")
                BaseTypeKind.STRING -> trait.traitName in setOf("Eq", "Ord", "Show", "Read")
                BaseTypeKind.ANY -> throw IllegalArgumentException("Base type ANY is deprecated, only used in var")
            }
            is PairType -> TODO()
            is NewType -> {
                val entry = lookupType(type)?:throw UndefinedTypeException(type.name)
                trait in getAllDependencies(entry.implementations.keys) && type.generics.withIndex().all { (i, type) ->
                    val traits = entry.implementations.getValue(trait)[i]
                    traits.all { isInstance(type, it) }
                }
            }
            is TypeVar -> trait in getAllDependencies(type.traits)
            is FuncType -> false
        }
    }

    fun getAllDependencies(traits: Iterable<Trait>): Set<Trait> {
        val set = mutableSetOf<Trait>()
        fun dfs(trait: Trait) {
            if(trait !in set) {
                set += trait
                traitDefs.getValue(trait).dependencies.map { dfs(it) }
            }
        }
        traits.map { dfs(it) }
        return set
    }

    /* Find the correct trait func def for the given ground type. */
    fun findTraitFuncDef(fName: String, groundType: FuncType): Function {
        val fEntry = functions[fName] ?: throw UndefinedFuncException(fName)
        if(fEntry.trait != null) {
            val tEntry = traitDefs[fEntry.trait]
                    ?: throw UndefinedTraitException(fEntry.trait.traitName)
            for (impl in tEntry.implementations.getValue(fName)) {
                try { groundType.inferFrom(impl.getFuncType(), this) }
                catch (sme: SemanticException) {
                    continue
                }
                return impl
            }
        }
        TODO() // error: not a trait function
    }

    fun getTypeMemberType(type: Type, member: String): Type {
        return when {
            type !is NewType -> throw TypeMismatchException(type, type)
            typedefs.getValue(type.name).isUnion -> throw NotAStructTypeException(type)
            else -> functions[type.name]?.members?.find { it.second.name == member }?.first
                    ?: throw UndefinedFuncException(type.toString())
        }
    }

    fun sizeof(type: Type): Int {
        return when(type) {
            boolType(), charType() -> 1
            else -> 4
        }
    }

    fun mallocSize(constructor: String, isTaggedUnion: Boolean = false): Int {
        val s = functions[constructor]?.members?.sumBy { sizeof(it.first) }
                ?: throw UndefinedFuncException(constructor)
        return s + if (isTaggedUnion) 4 else 0
    }

    fun getMemberOffset(name: String, type: Type): Int {
        if (type is NewType) {
            var acc = 0
            for (member in functions.getValue(type.name).members) {
                if (member.second.name == name) {
                    return acc
                }
                acc += sizeof(member.first)
            }
        }
        throw UndefinedFuncException(type.toString())
    }

    private fun getCurrScopeId(): Int = scopeIdStack.peekFirst()

    private fun collectPrevScope(prevId: Int, prev: MutableMap<String, VarAttributes>) {
        prev.forEach { (ident, attr) ->
            collect[ident to prevId] = attr
        }
        scopeDefs[prevId] = prev.keys
    }

    data class FuncAttributes(
            val type: FuncType,
            val members: List<Parameter>,
            val trait: Trait?,
            val index: Index,
            var occurrences: Int = 1
    ) {
        fun addOccurrence(): FuncAttributes = this.also { occurrences++ }
    }
    data class VarAttributes(
            val type: Type,
            val isConst: Boolean,
            val scopeId: Int,
            val index: Index,
            var occurrences: Int = 1
    ) {
        fun addOccurrence(): VarAttributes = this.also { occurrences++ }
    }
    data class TypeAttributes(
            val isUnion: Boolean,
            val implementations: MutableMap<Trait, List<Set<Trait>>>,
            val constructors: Set<String>,
            val index: Index,
            var occurrences: Int = 1
    ) {
        companion object {
            fun arrayAttributes(): TypeAttributes {
                val impls = mutableMapOf(
                        Trait("Eq") to listOf(setOf(Trait("Eq"))),
                        Trait("Show") to listOf(setOf(Trait("Show"))),
                        Trait("Malloc") to listOf(emptySet())
                )
                return TypeAttributes(false, impls, emptySet(), -1 to -1)
            }
        }
        fun addOccurrence(): TypeAttributes = this.also { occurrences++ }
    }
    data class TraitAttributes(
            val traitVar: String,
            val dependencies: Set<Trait>,
            val requiredFuncs: List<FunctionHeader>,
            val implementations: MutableMap<String, MutableList<Function>> = mutableMapOf()
    )

}

