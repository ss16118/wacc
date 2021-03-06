package exceptions

import ast.Trait
import ast.Type
import ast.Type.FuncType
import ast.TypeConstraint
import semantics.accessToUndefinedFunc
import semantics.accessToUndefinedVar
import semantics.typeMismatchError
import utils.Index

open class SemanticException(val msg: String) : Exception(msg) {

    class UndefinedVarException(variable: String) :
            SemanticException(accessToUndefinedVar(variable))

    class UndefinedFuncException(funcName: String) :
            SemanticException(accessToUndefinedFunc(funcName))

    class UndefinedTypeException(typeName: String):
            SemanticException("Unable to find definition for type: '$typeName'")

    class UndefinedTraitException(traitName: String):
            SemanticException("Unable to find a definiton for trait: '$traitName'")

    class MultipleVarDefInPatternException(vars: List<String>):
            SemanticException("Found multiple ${vars.joinToString(", ") { "'$it'" }} in a pattern!")

    class MultipleTraitDefException(trait: String, index: Index):
            SemanticException("Trait \"$trait\" has already been defined at $index")

    class MultipleFuncDefException(function: String, type: Type, index: Index) :
            SemanticException("Function \"$function :: $type\" has already been defined at $index!")

    class MultipleTypeDefException(type: String, index: Index):
            SemanticException("Type \"$type\" has already been defined at $index!")


    class TypeMismatchException(expected: Type, actual: Type) :
            SemanticException(typeMismatchError(expected, actual))

    class TypeNotSatisfyingTraitsException(type: Type, traits: Iterable<Trait>):
            SemanticException("$type does not implement ${traits.joinToString(", ") { it.toString() }}")

    class TypeDefEmptyBodyException(type: Type):
            SemanticException("Empty definition body for type \"$type\"")

    class NoUnificationFoundForTypesException(actual: Type, expected: Type):
            SemanticException("Unable to deduce a unification for '$actual' against '$expected'")

    class NotAStructTypeException(type: Type):
            SemanticException("$type is not a structural type!")

    class NotEnoughArrayRankException(arrName: String):
            SemanticException("'$arrName' does not have enough rank!")

    class FuncCallArgsMismatchException(funcName: String, expect: Int, actual: Int):
            SemanticException("'$funcName' expects $expect arguments, but $actual arguments are offered!")

    class UngroundTypeException(type: Type):
            SemanticException("Unable to deduce a ground type for ${if (type is Type.TypeVar) "$type:${type.traits}" else "$type"}")

    class ReturnInMainProgramException(indices: List<Index>) :
            SemanticException("Found return statement(s) in main program! at ${indices.joinToString(", ")}")

    class WriteToConstVarException(name: String):
            SemanticException("Attempt to write to a constant variable $name")

    class NotATraitRequiredFuncException(fName: String, traitName: String):
            SemanticException("Trait $traitName does not require function $fName")

    class TraitRequiredFuncTypeError(traitName: String, fName: String, required: FuncType, actual: FuncType):
            SemanticException("Trait $traitName expects $fName to be of type: \"$required\", yet the provided implementation is: \"$actual\"")

    class TypeNotInstanceOfADependentTraitException(type: Type, currentTrait: String, missingTrait: String):
            SemanticException("Trait $currentTrait requires $missingTrait, yet $type is not an instance of $missingTrait!")

    class InstanceWithGroundGenericTypeException(type: Type):
            SemanticException("Unsupported operation of implementing a trait for a type $type with a ground generic type,\n" +
                    "  Maybe you forgot to add a type-variable binding after the instance declaration? (e.g: where forall ...))")

    class ImplToTraitForTypeUnsupoortedException(type: Type):
            SemanticException("Implement trait for type \"$type\" is not supported!")

    class IrrelevantTraitFuncException(trait: String, fName: String, typeVar: String, fType: FuncType):
            SemanticException("Function $fName :: $fType in trait \"$trait\" is irrelevant to type variable \"$typeVar\"")

    class IrrelevantTraitDependencyVarException(typeVar: String, irrelevantEntry: TypeConstraint):
            SemanticException("Constraint $irrelevantEntry is irrelevant to type variable \"$typeVar\"")

    class TraitDependOnSelfException(traitName: String):
            SemanticException("Trait \"$traitName\" cannot depend on itself!")
}