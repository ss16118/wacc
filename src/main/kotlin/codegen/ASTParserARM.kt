package codegen

import ast.*
import ast.BinaryOperator.*
import ast.BinaryOperator.DIV
import ast.BinaryOperator.EQ
import ast.BinaryOperator.GT
import ast.BinaryOperator.LT
import ast.BinaryOperator.MUL
import ast.Expression.*
import ast.Statement.*
import ast.Statement.BuiltinFunc.*
import ast.Type.ArrayType
import ast.Type.BaseType
import ast.Type.BaseTypeKind.*
import ast.Type.Companion.anyPairType
import ast.Type.Companion.boolType
import ast.Type.Companion.charType
import ast.Type.Companion.intType
import ast.Type.Companion.stringType
import ast.UnaryOperator.*
import codegen.PreludeFunc.*
import codegen.arm.*
import codegen.arm.DirectiveType.LTORG
import codegen.arm.Instruction.*
import codegen.arm.Instruction.Condition.*
import codegen.arm.Instruction.Terminator.*
import codegen.arm.Operand.*
import codegen.arm.Operand.ImmNum.Companion.immFalse
import codegen.arm.Operand.ImmNum.Companion.immNull
import codegen.arm.Operand.ImmNum.Companion.immTrue
import codegen.arm.Operand.Register.Reg
import codegen.arm.Operand.Register.SpecialReg
import codegen.arm.SpecialRegName.*
import utils.LabelNameTable
import utils.SymbolTable
import utils.VarWithSID
import java.util.*

class ASTParserARM(val ast: ProgramAST, private val symbolTable: SymbolTable) {
    private val labelNameTable = LabelNameTable()
    private val stringConsts: MutableMap<String, Label> = mutableMapOf()
    private val blocks: Deque<InstructionBlock> = ArrayDeque()
    private val instructions: MutableList<Instruction> = mutableListOf()
    private val varOffsetMap = mutableMapOf<VarWithSID, Int>()
    private val funcLabelMap = mutableMapOf<String, Label>()
    private val firstDefReachedScopes = mutableSetOf<Int>()
    var spOffset = 0           // current stack-pointer offset (in negative form)
    var currScopeOffset = 0    // pre-allocated scope offset for variables
    private val requiredPreludeFuncs = mutableSetOf<PreludeFunc>() // prelude definitions that needs to be run after codegen

    private val availableRegIds = TreeSet<Int>()

    var currBlockLabel = Label("")

    private fun getDataTypeSize(type: Type): Int = when(type) {
        charType() -> 1
        boolType() -> 1
        else -> 4
    }

    private fun resetRegs() {
        availableRegIds.clear()
        availableRegIds += (4..36)
    }

    fun printARM(): String = ".data\n\n" +
            StringConst.fromMap(stringConsts).joinToString("\n") + "\n.text\n\n" +
            ".global main\n" +
            blocks.joinToString("\n")

    fun translate(): ASTParserARM = this.also { ast.toARM() }

    private fun ProgramAST.toARM() {
        funcLabelMap += "main" to Label("main")
        functions.map { funcLabelMap += it.name to getLabel(it.name) }
        Function(Type.intType(),
                "main",
                mutableListOf(),
                mainProgram + BuiltinFuncCall(RETURN, IntLit(0))
        ).toARM()
        functions.map { it.toARM() }
        definePreludes()
    }

    private fun ast.Function.toARM() {
        resetRegs()
        spOffset = 0
        setBlock(funcLabelMap.getValue(name))
        push(SpecialReg(LR))
        args.firstOrNull()?.let { scopeEnterDef(it.second) }
        args.map { alloca(it.second) }
        body.map { it.toARM() }
        addDirective(LTORG)
    }

    private fun Statement.toARM() {
        when(this) {
            is Skip -> Skip
            is Declaration -> {
                scopeEnterDef(variable)
                val reg = rhs.toARM().toReg()
                alloca(variable, reg)
                reg.recycleReg()
            }

            is Assignment -> {
                val reg = rhs.toARM().toReg()
                when(lhs) {
                    is Identifier -> store(reg, findVar(lhs))
                    is ArrayElem -> {
                        val rhsReg = rhs.toARM().toReg()
                        load(AL, rhsReg, Offset(rhsReg, 0))
                        val lhsReg = lhs.toARM().toReg()
                        store(rhsReg, Offset(lhsReg, 0))

                        rhsReg.recycleReg()
                        lhsReg.recycleReg()
                    }
                    is PairElem -> TODO()
                }
                reg.recycleReg()
            }

            is Read -> {

            }

            is BuiltinFuncCall -> {
                when(func) {
                    RETURN -> {
                        val reg = expr.toARM()
                        moveSP(spOffset)
                        mov(Reg(0), reg)
                        pop(SpecialReg(PC))
                    }
                    FREE -> {
                        val reg = expr.toARM().toReg()
                        mov(Reg(0), reg)
                        when(expr.getType(symbolTable)) {
                            is ArrayType -> callPrelude(FREE_ARRAY)
                            is Type.PairType -> callPrelude(FREE_PAIR)
                            else -> throw IllegalArgumentException("Cannot free a non-heap-allocated object!")
                        }
                        reg.recycleReg()
                    }
                    EXIT -> {
                        val reg = expr.toARM()
                        mov(Reg(0), reg)
                        bl(AL, Label("exit"))
                    }
                    PRINT -> {
                        callPrintf(expr, false)
                    }
                    PRINTLN -> {
                        callPrintf(expr, true)
                    }
                }
            }

            is CondBranch -> {
                val ifthen = getLabel("if_then")
                val ifelse = getLabel("if_else")
                val ifend  = getLabel("if_end")
                val cond = expr.toARM().toReg()
                cmp(cond, immFalse())
                branch(Condition.EQ, ifelse)
                cond.recycleReg()

                setBlock(ifthen)
                inScopeDo { trueBranch.map { it.toARM() } }
                branch(ifend)

                setBlock(ifelse)
                inScopeDo { falseBranch.map { it.toARM() } }
                branch(ifend)

                setBlock(ifend)
            }

            is WhileLoop -> {
                val lCheck = getLabel("loop_check")
                val lBody = getLabel("loop_body")
                val lEnd  = getLabel("loop_end")
                branch(lCheck)

                setBlock(lCheck)
                val cond = expr.toARM().toReg()
                cmp(cond, immFalse())
                cond.recycleReg()
                branch(Condition.EQ, lEnd)

                setBlock(lBody)
                inScopeDo {
                    body.map { it.toARM() }
                }
                branch(lCheck)

                setBlock(lEnd)
            }

            is Block -> inScopeDo { body.map { it.toARM() } }
        }
    }

    private fun scopeEnterDef(variable: Identifier) {
        if (variable.scopeId !in firstDefReachedScopes) {
            val defs = symbolTable.scopeDefs[variable.scopeId]!!
            currScopeOffset = 4 * defs.size
            defs.withIndex().forEach { (i, v) ->
                varOffsetMap[v to variable.scopeId] = spOffset + (i + 1) * 4
            }
            moveSP(-currScopeOffset)
            firstDefReachedScopes += variable.scopeId
        }
    }

    private fun Expression.toARM(): Operand {
        return when(this) {
            NullLit   -> immNull()
            is IntLit -> {
                if (x in 0..255) {
                    ImmNum(x)
                } else {
                    val reg = getReg()
                    load(reg, ImmNum(x))
                }
            }
            is BoolLit -> if (b) immTrue() else immFalse()
            is CharLit -> ImmNum(c.toInt())
            is StringLit -> defString(string)
            is Identifier -> load(getReg(), findVar(this))
            is BinExpr -> {
                val op1 = left.toARM().toReg()
                val op2 = right.toARM()
                binop(op, op1, op1, op2)
            }
            is UnaryExpr -> when(op) {
                ORD -> expr.toARM()
                CHR -> expr.toARM()
                LEN -> load(getReg(), Offset(expr.toARM().toReg(), 0, false))
                NEG -> {
                    val reg = expr.toARM().toReg()
                    rsbs(reg, reg, ImmNum(0)).also { callPrelude(OVERFLOW_ERROR, VS) }
                }
                NOT -> not(expr.toARM().toReg())
            }
            is ArrayElem -> {
                var result = load(getReg(), findVar(arrIdent))
                for (expr in indices) {
                    val indexReg = expr.toARM().toReg()
                    callCheckArrBound(indexReg, result)
                    val offset = binop(MUL, indexReg, indexReg,
                            ImmNum(getDataTypeSize(arrIdent.getType(symbolTable).unwrapArrayType()!!)))
                    result = binop(ADD, result, result, offset)
                    load(result, Offset(result, 4))
                }
                result
            }
            is PairElem -> TODO()
            is ArrayLiteral -> {
                // Malloc the memory for each element in the array
                val elemSize = getDataTypeSize(elements[0].getType(symbolTable))
                val totalSize = elements.size * elemSize + 4

                val baseAddressReg = callMalloc(totalSize)

                // Store each element in the array
                elements.forEachIndexed { index, it ->
                    val tempElemReg = it.toARM().toReg()
                    store(tempElemReg, Offset(baseAddressReg, (index + 1) * elemSize, false))
                    tempElemReg.recycleReg()
                }
                // Store the size of the array at the end
                val tempElemReg = getReg()
                load(AL, tempElemReg, ImmNum(elements.size))
                store(tempElemReg, Offset(baseAddressReg, 0, false))
                tempElemReg.recycleReg()
                baseAddressReg
            }
            is NewPair -> TODO()
            is FunctionCall -> {
                for (arg in args) {
                    val reg = arg.toARM().toReg()
                    store(reg, Offset(SpecialReg(SP), -4, true))
                    reg.recycleReg()
                }
                bl(AL, funcLabelMap.getValue(ident))
                binop(ADD, SpecialReg(SP), SpecialReg(SP), ImmNum(args.size * 4))
                Reg(0)
            }
        }
    }

    private fun callCheckArrBound(expected: Operand, arrayPtr: Register) {
        mov(Reg(0), expected.toReg())
        mov(Reg(1), arrayPtr)
        callPrelude(CHECK_ARR_BOUND)
    }

    /* Define all prelude functions that are used in this code. */
    private fun definePreludes() {
        for (func in requiredPreludeFuncs) {
            resetRegs()
            setBlock(Label(func.name.toLowerCase()))
            when (func) {
                RUNTIME_ERROR -> {
                    mov(Reg(0), ImmNum(-1))
                    bl(AL, Label("exit"))
                    packBlock()
                }
                OVERFLOW_ERROR -> {
                    callPrintf(StringLit("OverflowError: " +
                            "the result is too small/large to store in a 4-byte signed-integer."),
                            true)
                    bl(AL, RUNTIME_ERROR.getLabel(), Unreachable)
                }
                CHECK_ARR_BOUND -> {
                    val label1 = getLabel("check_too_large")
                    val label2 = getLabel("check_finish")
                    push(SpecialReg(LR))
                    cmp(Reg(0), ImmNum(0))
                    branch(Condition.GE, label1)
                    callPrintf(StringLit("ArrayIndexOutOfBoundsError: negative index"),
                            true)
                    bl(AL, RUNTIME_ERROR.getLabel(), Unreachable)
                    setBlock(label1)
                    load(Reg(1), Offset(Reg(1), 0))
                    cmp(Reg(0), Reg(1))
                    branch(Condition.LT, label2)
                    callPrintf(StringLit("ArrayIndexOutOfBoundsError: index too large"), true)
                    bl(AL, RUNTIME_ERROR.getLabel(), Unreachable)
                    packBlock()
                    setBlock(label2)
                    pop(SpecialReg(PC))
                }
                FREE_ARRAY -> {
                    val notNullLabel = getLabel("free_not_null")
                    push(SpecialReg(LR))
                    cmp(Reg(0), immNull())
                    branch(Condition.NE, notNullLabel)
                    callPrintf(StringLit("NullReferenceError: dereference a null reference"), true)
                    bl(AL, RUNTIME_ERROR.getLabel(), Unreachable)
                    setBlock(notNullLabel)
                    bl(AL, Label("free"))
                    pop(SpecialReg(PC))
                }
            }
        }
    }

    /* Move the current position of sp by the given offset. */
    private fun moveSP(offset: Int) {
        if (offset < 0) {
            binop(SUB, SpecialReg(SP), SpecialReg(SP), ImmNum(-offset))
            spOffset -= offset
        } else if (offset > 0) {
            binop(ADD, SpecialReg(SP), SpecialReg(SP), ImmNum(offset))
            spOffset -= offset
        }
    }

    /* Move the content within the given operand to a register.
    *  If the given operand is already a register, it would do nothing
    *  In other situations, it would automatically move its content to a reg using either MOV or LDR
    *  Return the register that stores the value of the operand. */
    private fun Operand.toReg(): Register = when(this) {
        is Register -> this
        is ImmNum -> if (num in 0..255) mov(this) else load(getReg(), this)
        is Label -> {
            val reg = getReg()
            load(reg, this)
            binop(ADD, reg, reg, ImmNum(4));
        }
        else -> mov(this)
    }


    /* This method allocates some space on stack for a variable,
    *  returns the offset from the initial sp. */
    private fun alloca(varNode: Identifier, reg: Register? = null) {
        val offset = spOffset - varOffsetMap[varNode.getVarSID()]!!
        val dest = Offset(SpecialReg(SP), offset)
        reg?.let { store(reg, dest) }
    }

    /* Find the alloca-ed variable's offset from the current position of the sp
    *  by the given var node. */
    private fun findVar(varNode: Identifier): Offset {
        return Offset(SpecialReg(SP), spOffset - varOffsetMap[varNode.name to varNode.scopeId]!!)
    }

    /* Define a string constant and return its label. */
    private fun defString(content: String): Label {
        val prevDef = stringConsts[content]
        if (prevDef != null) {
            return prevDef
        }
        val msgLabel = getLabel("msg")
        stringConsts[content] = msgLabel
        return msgLabel
    }

    /* Run the provided action in the context of a new scope.
    *  The original scope offset is recorded and sp is moved back to it
    *  after the action is finished. */
    private fun inScopeDo(action: () -> Unit) {
        val prevScopeOffset = currScopeOffset
        currScopeOffset = 0
        action()
        moveSP(currScopeOffset)
        currScopeOffset = prevScopeOffset
    }

    /* Call a prelude function. */
    private fun callPrelude(func: PreludeFunc, cond: Condition = AL) {
        requiredPreludeFuncs += func.findDependencies()
        bl(cond, func.getLabel())
    }

    /* Get the next avaliable register */
    private fun getReg(): Register = Reg(availableRegIds.pollFirst()!!)

    /* 'Recycle' the given register so that it can be re-used in the future. */
    private fun Register.recycleReg() {
        if (this is Reg) {
            availableRegIds += id
        } else {
            throw IllegalArgumentException("$this is not a recyclable register!")
        }
    }

    /* Set the current block's label to the given label.
    *  Indicates the beginning of a new instruction block. */
    private fun setBlock(label: Label) {
        currBlockLabel = label
    }

    /* Finish building the current block, pack it up and record it. */
    private fun packBlock(terminator: Terminator = FallThrough) {
        val block = InstructionBlock(currBlockLabel, instructions.toMutableList(), terminator)
        blocks.addLast(block)
        instructions.clear()
        currBlockLabel = getLabel("${currBlockLabel.name}_seq")
    }

    /* Get a new label based on the given name prefix */
    private fun getLabel(name: String): Label = Label(labelNameTable.getName(name))

    /** Instruction helper methods **/

    private fun branch(label: Label) {
        packBlock(B(AL, label))
    }

    private fun branch(cond: Condition, label: Label) {
        packBlock(B(cond, label))
    }

    private fun bl(cond: Condition, label: Label) {
        instructions += BL(cond, label)
    }

    private fun bl(cond: Condition, label: Label, terminator: Terminator) {
        bl(cond, RUNTIME_ERROR.getLabel())
        packBlock(terminator)
    }

    private fun load(cond: Condition, dst: Register, src: Operand): Register {
        instructions += if (src is ImmNum && src.num in 0..255) {
            Mov(cond, dst, src)
        } else {
            Ldr(cond, dst, src)
        }
        return dst
    }

    private fun load(dst: Register, src: Operand): Register = load(AL, dst, src)

    private fun store(src: Register, dst: Operand): Operand {
        instructions += Str(AL, src, dst)
        return dst
    }

    private fun mov(src: Operand): Register{
        return mov(getReg(), src)
    }

    private fun mov(dst: Register, src: Operand): Register {
        if (dst != src) {
            instructions += Mov(AL, dst, src)
        }
        return dst
    }

    private fun push(vararg regs: Register) {
        instructions += Push(regs.toMutableList())
    }

    private fun pop(vararg regs: Register) {
        if (regs.contentEquals(arrayOf(SpecialReg(PC)))) {
            packBlock(PopPC)
        } else {
            instructions += Pop(regs.toMutableList())
        }
    }

    private fun addDirective(type: DirectiveType) {
        blocks.last.tails += Directive(type)
    }

    private fun binop(opType: BinaryOperator, dst: Register, rn: Register, op2: Operand): Register {
        val instrs = when (opType) {
            ADD -> listOf(Add(AL, dst, rn, op2))
            SUB -> listOf(Sub(AL, dst, rn, op2))
            MUL -> listOf(Mul(AL, dst, rn, op2.toReg().also { it.recycleReg() }))
            DIV -> TODO()
            MOD -> TODO()
            GTE -> listOf(
                    Cmp(rn, op2),
                    Mov(Condition.GE, dst, immTrue()),
                    Mov(Condition.LT, dst, immFalse()))
            LTE -> listOf(
                    Cmp(rn, op2),
                    Mov(Condition.LE, dst, immTrue()),
                    Mov(Condition.GT, dst, immFalse()))
            GT -> listOf(
                    Cmp(rn, op2),
                    Mov(Condition.GT, dst, immTrue()),
                    Mov(Condition.LE, dst, immFalse()))
            LT -> listOf(
                    Cmp(rn, op2),
                    Mov(Condition.LT, dst, immTrue()),
                    Mov(Condition.GE, dst, immFalse()))
            EQ -> listOf(
                    Cmp(rn, op2),
                    Mov(Condition.EQ, dst, immTrue()),
                    Mov(Condition.NE, dst, immFalse()))
            NEQ -> listOf(
                    Cmp(rn, op2),
                    Mov(Condition.NE, dst, immTrue()),
                    Mov(Condition.EQ, dst, immFalse()))
            AND ->  listOf(And(AL, dst, rn, op2))
            OR ->   listOf(Orr(AL, dst, rn, op2))
        }
        instructions += instrs
        return dst
    }

    private fun cmp(op1: Register, op2: Operand) {
        instructions += Cmp(op1, op2)
    }

    private fun not(reg: Register): Register {
        instructions += Eor(AL, reg, reg, immTrue())
        return reg
    }

    private fun rsbs(dst: Register, src: Register, op2: Operand): Register {
        instructions += Rsb(true, AL, dst, src, op2)
        return dst
    }

    private fun callPrintf(expr: Expression, newline: Boolean) {
        val operand = expr.toARM()
        val exprType = expr.getType(symbolTable) // TODO Symbol Table needs to be persistent
        when(exprType) {
            boolType() -> {
                cmp(operand.toReg(), immFalse())
                load(NE, Reg(1), defString("true"))
                load(Condition.EQ, Reg(1), defString("false"))
                binop(ADD, Reg(1), Reg(1), ImmNum(4))
                load(Reg(0), defString(getFormatString(exprType, newline)))
                binop(ADD, Reg(0), Reg(0), ImmNum(4))
            }
            else -> {
                mov(Reg(1), operand.toReg())
                load(Reg(0), defString(getFormatString(exprType, newline)))
                binop(ADD, Reg(0), Reg(0), ImmNum(4))
            }
        }
        bl(AL, Label("printf"))
        mov(Reg(0), ImmNum(0))
        bl(AL, Label("fflush"))
    }

    private fun callMalloc(size: Int): Register {
        val target = ImmNum(size).toReg()
        mov(Reg(0), target)
        bl(AL, Label("malloc"))
        return mov(target, Reg(0))
    }

    private fun getFormatString(type: Type, newline: Boolean): String {
        val format = when(type) {
            is BaseType -> when(type.kind) {
                INT -> "%d"
                CHAR -> "%c"
                BOOL -> "%s"
                STRING -> "%s"
                else -> "%p"
            }
            is ArrayType -> if (type.type == charType()) "%s" else "%p"
            is Type.PairType -> "%p"
            is Type.FuncType -> "%p"
        }
        return format + if (newline) "\\n\\0" else "\\0"
    }

    private fun Expression.getType(symbolTable: SymbolTable): Type {
        val map = symbolTable.collect
        return when (this) {
            NullLit -> anyPairType()
            is IntLit -> intType()
            is BoolLit -> boolType()
            is CharLit -> charType()
            is StringLit -> stringType()
            is Identifier -> map[getVarSID()]!!.type
            is BinExpr -> op.retType
            is UnaryExpr -> op.retType
            is ArrayElem -> map[arrIdent.getVarSID()]!!.type.unwrapArrayType(indices.size)!!
            is PairElem -> TODO()
            is ArrayLiteral -> TODO()
            is NewPair -> TODO()
            is FunctionCall -> symbolTable.functions[ident]!!.type.retType
        }
    }
}




