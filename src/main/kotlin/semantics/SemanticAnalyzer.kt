package semantics

import ast.*
import ast.BinaryOperator.*
import ast.Expression.*
import ast.Expression.PairElemFunction.FST
import ast.Expression.PairElemFunction.SND
import ast.Function
import ast.Statement.BuiltinFunc.*
import ast.Statement.Declaration
import ast.Type.Companion.anyArrayType
import ast.Type.Companion.anyoutArrayType
import ast.Type.Companion.boolType
import ast.Type.Companion.charType
import ast.Type.Companion.intType
import ast.Type.Companion.anyPairType
import ast.Type.Companion.nullType
import ast.Type.Companion.stringType
import exceptions.SemanticException
import exceptions.SemanticException.*
import exceptions.SemanticException.TypeException.*
import semantics.LhsTypeCheckResult.Failure
import semantics.LhsTypeCheckResult.Success
import semantics.TypeChecker.Companion.match
import semantics.TypeChecker.Companion.matchPairByElem
import semantics.TypeChecker.Companion.pass
import semantics.TypeChecker.Companion.unwrapArray
import semantics.TypeChecker.Companion.unwrapPair
import utils.AstIndexMap
import utils.SymbolTable

class SemanticAnalyzer(val astIndexMap: AstIndexMap) {

    val symbolTable = SymbolTable()

    fun doCheck(ast: ProgramAST) {
        val errors = ast.check()
        if (errors.isNotEmpty()) {
            throw SemanticExceptionBundle(errors)
        }
    }

    private fun ProgramAST.check(): List<SemanticException> {
        functions.map { symbolTable.defineFunc(
                        it.name,
                        Type.FuncType(it.returnType, it.args.map { a -> a.second }),
                        astIndexMap[it] ?: throw UnknownError())
        }
        return functions.flatMap { it.checkFunc(match(it.returnType)) } +
                mainProgram.checkBlock()
    }

    private fun Function.checkFunc(retCheck: TypeChecker): List<SemanticException> {
        symbolTable.pushScope()
        args.map { param ->
            symbolTable.defineVar(param.first, param.second, astIndexMap.getValue(this))
        }
        val errors = this.body.flatMap { it.check(retCheck) }
        symbolTable.popScope()
        return errors
    }

    private fun Statements.checkBlock(retCheck: TypeChecker = pass()): List<SemanticException> {
        symbolTable.pushScope()
        val errors = this.flatMap { it.check(retCheck) }
        symbolTable.popScope()
        return errors
    }

    private fun Statement.check(retCheck: TypeChecker): List<SemanticException> {
        return when(this@check) {
            Statement.Skip -> emptyList()
            is Declaration -> {
                val prevAttr
                        = symbolTable.defineVar(variable.ident, type, astIndexMap.getValue(this))
                if (prevAttr != null) {
                    listOf(MultipleVarDefException(variable.ident, prevAttr.type, prevAttr.index))
                } else {
                    rhs.check(match(type))
                }
            }
            is Statement.Assignment -> {
                val result = lhs.checkLhs()
                when (result) {
                    is Success -> rhs.check(match(result.result))
                    is Failure -> result.errors
                }
            }
            is Statement.Read -> {
                val result =  target.checkLhs()
                val readChecker = match(intType())`||` match(charType()) `||` match(stringType())
                when (result) {
                    is Success -> readChecker.check(result.result)
                    is Failure -> result.errors
                }
            }
            is Statement.BuiltinFuncCall -> when (func) {
                PRINT, PRINTLN -> expr.check(pass())
                FREE -> expr.check(match(anyArrayType()) `||` match(anyPairType()))
                EXIT -> expr.check(match(intType()))
                RETURN -> expr.check(retCheck)
            }
            is Statement.CondBranch -> {
                expr.check(match(boolType())) +
                        trueBranch.checkBlock(retCheck) +
                        falseBranch.checkBlock(retCheck)
            }
            is Statement.WhileLoop ->  {
                expr.check(match(boolType())) + body.checkBlock(retCheck)
            }
            is Statement.Block -> body.checkBlock(retCheck)
        }
    }

    private fun Expression.checkLhs(tc: TypeChecker = pass()): LhsTypeCheckResult<Type> {
        return when(this) {
            is Identifier -> {
                val actual = symbolTable.lookupVar(ident)?.type
                if (actual != null) {
                    val errors = tc.check(actual)
                    if (errors.isEmpty()) {
                        Success(actual)
                    } else {
                        Failure(errors)
                    }
                } else {
                    Failure(listOf(UndefinedVarException(ident)))
                }
            }
            is PairElem -> {
                val errors = expr.check(matchPairByElem(func, tc))
                if (errors.isNotEmpty()) {
                    Failure(errors)
                } else {
                    val result = expr.getType(symbolTable).unwrapPairType(func)
                    if (result != null) {
                        Success(result)
                    } else {
                        throw UnknownError()
                    }
                }
            }
            is ArrayElem -> {
                val entry = symbolTable.lookupVar(arrayName)
                if (entry != null) {
                    val actual = entry.type.unwrapArrayType(indices.count())
                    if (actual != null) {
                        val errors = tc.check(actual)
                        if (errors.isEmpty()) {
                            Success(actual)
                        } else {
                            Failure(errors)
                        }
                    } else {
                        Failure(listOf(InsufficientArrayRankException(entry.type, indices.count())))
                    }
                } else {
                    Failure(listOf(UndefinedVarException(arrayName)))
                }
            }
            else -> Failure(listOf())
        }
    }

    private fun Expression.check(tc: TypeChecker): List<SemanticException> {
        val errors = when(this) {
            is NullLit -> tc.check(nullType())
            is IntLit -> tc.check(intType())
            is BoolLit -> tc.check(boolType())
            is CharLit -> tc.check(charType())
            is StringLit -> tc.check(stringType())
            is Identifier -> {
                val actual = symbolTable.lookupVar(ident)?.type
                if (actual != null) {
                    tc.check(actual)
                } else {
                    listOf(UndefinedVarException(ident))
                }
            }
            is BinExpr -> {
                when (op) {
                    EQ, NEQ -> tc.check(boolType()) + listOf(
                            left.check(match(right.getType(symbolTable))),
                            right.check(match(left.getType(symbolTable)))
                    ).anyEmptyOrAll()
                    else -> {
                        val allCases = BinaryOperator.typeMap.getValue(op).map { entry ->
                            tc.check(entry.type) + left.check(entry.lhsChecker) + right.check(entry.rhsChecker)
                        }
                        allCases.anyEmptyOrAll()
                    }
                }
            }
            is ArrayElem -> {
                val entry = symbolTable.lookupVar(arrayName)
                if (entry != null) {
                    val newtype = entry.type.unwrapArrayType(indices.count())
                    if (newtype != null) {
                        tc.check(newtype)
                    } else {
                        listOf(InsufficientArrayRankException(entry.type, indices.count()))
                    }
                } else {
                    listOf(UndefinedVarException(arrayName))
                }

//                symbolTable.lookupVar(arrayName)
//                        ?.let { entry -> entry.type.unwrapArrayType(indices.count())
//                                ?.let { newtype -> tc.check(newtype) }
//                                ?:listOf(InsufficientArrayRankException(entry.type, indices.count())) }
//                        ?: listOf(UndefinedVarException(arrayName))
            }
            is PairElem -> {
                if (expr == NullLit) {
                    listOf(AccessMemberOfNullLitException(func))
                } else {
                    expr.check(matchPairByElem(func, tc))
                }
            }
            is ArrayLiteral -> {
                if (elements.isEmpty()) {
                    tc.check(anyoutArrayType())
                } else {
                    elements.flatMap { it.check(unwrapArray(tc)) }
                }
            }
            is NewPair -> first.check(unwrapPair(FST, tc)) + second.check(unwrapPair(SND, tc))
            is UnaryExpr -> {
                val entry = UnaryOperator.typeMap().getValue(op)
                val checker = entry.first
                val retType = entry.second
                tc.check(retType) + expr.check(checker)
            }
            is FunctionCall -> {
                val funcEntry = symbolTable.lookupFunc(ident)
                if (funcEntry == null) {
                    listOf(UndefinedFuncException(ident))
                } else {
                    val funcType = funcEntry.type
                    val retType = funcType.retType
                    val expectedCount = args.size
                    val actualCount = funcType.paramTypes.size
                    if (expectedCount != actualCount) {
                        listOf(FuncCallArgCountMismatchException(ident, funcType, expectedCount, actualCount))
                    } else {
                        tc.check(retType) + args.zip(funcType.paramTypes) { arg, t -> arg.check(match(t))}.flatten()
                    }

                }

            }
        }
        return errors.map { err -> err.forwardWith("an expression", this, astIndexMap) }
    }

}

