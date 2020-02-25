package codegen

import ast.*
import ast.BinaryOperator.*
import ast.BinaryOperator.EQ
import ast.BinaryOperator.GT
import ast.BinaryOperator.LT
import ast.Expression.*
import ast.Expression.PairElemFunction.FST
import ast.Expression.PairElemFunction.SND
import ast.Function
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
import codegen.arm.Instruction.ShiftModifier.ASR
import codegen.arm.Instruction.Terminator.*
import codegen.arm.Operand.*
import codegen.arm.Operand.ImmNum.Companion.immFalse
import codegen.arm.Operand.ImmNum.Companion.immNull
import codegen.arm.Operand.ImmNum.Companion.immTrue
import codegen.arm.Operand.Register.Reg
import codegen.arm.Operand.Register.SpecialReg
import codegen.arm.SpecialRegName.*
import utils.EscapeCharMap.Companion.fromEscape
import utils.LabelNameTable
import utils.Parameter
import utils.SymbolTable
import utils.VarWithSID
import java.util.*

/* AstToRawArmConverter takes the program AST and the generated symbol table, returns a "raw" ARM program.
*  The converter will generate a 'sort-of-functional' ARM program (meets ARM's syntax), except that it does
*  not free any registers and do any PUSH/POP to normal registers(R4..).
*  Thus, the generated program could work if it does not contain any normal register with its id > 11.
*  All of the normal registers in the generated raw ARM program are "virtual" registers. Which will be unified
*  with "real" registers by RegisterAllocator */
class AstToRawArmConverter(val ast: ProgramAST, private val symbolTable: SymbolTable) {
    private val labelNameTable = LabelNameTable()
    private val singletonStringConsts: MutableMap<String, Label> = mutableMapOf()
    private val commonStringConsts: MutableList<Pair<String, Label>> = mutableListOf()
    private val blocks: Deque<InstructionBlock> = ArrayDeque()
    private val instructions: MutableList<Instruction> = mutableListOf()
    private val varOffsetMap = mutableMapOf<VarWithSID, Int>()
    private val funcLabelMap = mutableMapOf<String, Label>()
    private val firstDefReachedScopes = mutableSetOf<Int>()
    private var spOffset = 0           // current stack-pointer offset (in negative form)
    private var currScopeOffset = 0    // pre-allocated scope offset for variables
    private val requiredPreludeFuncs = mutableSetOf<PreludeFunc>() // prelude definitions that needs to be run after codegen
    private val maxImmNum = 1024
    private var blockComplete = false
    private var virtualRegIdAcc = 4

    var currBlockLabel = Label("")

    fun export(): ArmProgram = ArmProgram(StringConst.fromCodegenCollections(singletonStringConsts, commonStringConsts), blocks.toList())

    fun translate(): AstToRawArmConverter = this.also { ast.toARM() }

    /** Converts WaccAst to ARM intermediate representation **/
    private fun ProgramAST.toARM() {
        funcLabelMap += "main" to Label("main")
        functions.map { funcLabelMap += it.name to getLabel("f_${it.name}") }
        Function(returnType = intType(),
                name = "main",
                args = mutableListOf(),
                body = mainProgram + BuiltinFuncCall(RETURN, IntLit(0))
        ).toARM()
        functions.map { it.toARM() }
        definePreludes()
    }

    private fun ast.Function.toARM() {
        virtualRegIdAcc = 4
        val originalOffset = spOffset
        spOffset = 0
        currScopeOffset = 0
        setBlock(funcLabelMap.getValue(name))
        push(SpecialReg(LR))
        args.firstOrNull()?.let { scopeEnterDef(it.second, args.toSet()) }
        var offsetAcc = -4
        args.map { arg ->
            markParam(arg.second, offsetAcc)
            offsetAcc -= sizeof(arg.first)
        }
        body.map { it.toARM() }
        if (!blockComplete) {
            packBlock(Unreachable)
        }
        addDirective(LTORG)
        spOffset = originalOffset
    }

    private fun markParam(paramIdent: Identifier, offset: Int) {
        varOffsetMap[paramIdent.getVarSID()] = offset
    }

    private fun Statement.toARM() {
        when (this) {
            is Skip -> Skip
            is Declaration -> {
                scopeEnterDef(variable)
                val reg = rhs.toARM().toReg()
                alloca(variable, reg, sizeof(type))
            }

            is Assignment -> {
                val reg = rhs.toARM().toReg()
                when (lhs) {
                    is Identifier -> {
                        val size = sizeof(lhs.getType(symbolTable))
                        store(reg, findVar(lhs), size)
                    }
                    is ArrayElem -> {
                        val dstOffset = getLhsAddress(lhs)
                        val size = sizeof(lhs.arrIdent.getType(symbolTable).unwrapArrayType()!!)
                        store(reg, dstOffset, size)
                    }
                    is PairElem -> {
                        val offset = getLhsAddress(lhs)
                        val size = sizeof(lhs.getType(symbolTable))
                        val ptr = getReg()
                        load(ptr, offset)
                        store(reg, Offset(ptr), size)
                    }
                }
            }

            is Read -> callScanf(target)

            is BuiltinFuncCall -> {
                when (func) {
                    RETURN -> {
                        val reg = expr.toARM()
                        moveSP(spOffset, false)
                        mov(Reg(0), reg)
                        pop(SpecialReg(PC))
                    }
                    FREE -> {
                        val reg = expr.toARM().toReg()
                        mov(Reg(0), reg)
                        when (expr.getType(symbolTable)) {
                            is ArrayType -> callPrelude(FREE_ARRAY)
                            is Type.PairType -> callPrelude(FREE_PAIR)
                            else -> throw IllegalArgumentException("Cannot free a non-heap-allocated object!")
                        }
                    }
                    EXIT -> {
                        val reg = expr.toARM()
                        mov(Reg(0), reg)
                        bl(AL, Label("exit"))
                    }
                    PRINT -> callPrintf(expr, false)
                    PRINTLN -> callPrintf(expr, true)
                }
            }

            is IfThen -> {
                val ifThen = getLabel("if_then")
                val ifEnd = getLabel("if_end")
                val cond = expr.toARM().toReg()
                cmp(cond, immFalse())
                branch(Condition.EQ, ifEnd)
                setBlock(ifThen)
                inScopeDo { thenBody.map { it.toARM() } }
                branch(ifEnd)
                setBlock(ifEnd)
            }

            is CondBranch -> {
                val ifend = getLabel("if_end")
                var nextElse = getLabel("if_else")
                packBlock()
                condStatsList.forEach { (expr, stats) ->
                    setBlock(getLabel("if_check"))
                    val cond = expr.toARM().toReg()
                    cmp(cond, immFalse())
                    branch(Condition.EQ, nextElse)

                    inScopeDo { stats.map { it.toARM() } }
                    branch(ifend)

                    setBlock(nextElse)
                    nextElse = getLabel("if_else")
                }
                inScopeDo { elseBody!!.map { it.toARM() } }
                branch(ifend)
                setBlock(ifend)
            }

            is WhileLoop -> {
                val lCheck = getLabel("loop_check")
                val lBody = getLabel("loop_body")
                val lEnd = getLabel("loop_end")
                branch(lCheck)

                setBlock(lCheck)
                val cond = expr.toARM().toReg()
                cmp(cond, immFalse())
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

    private fun Expression.toARM(): Operand {
        return when (this) {
            NullLit -> immNull()
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
            is StringLit -> defString(fromEscape(string).toString(), false)
            is Identifier -> load(getReg(), findVar(this), sizeof(this.getType(symbolTable)) == 1)
            is BinExpr -> {
                val op1 = left.toARM().toReg()
                val op2 = right.toARM()
                binop(op, op1, op1, op2, op2Destructive = true, setFlag = true)
            }
            is UnaryExpr -> when (op) {
                ORD -> expr.toARM()
                CHR -> expr.toARM()
                LEN -> load(getReg(), Offset(expr.toARM().toReg(), 0, false))
                NEG -> {
                    val reg = expr.toARM().toReg()
                    rsbs(reg, reg, ImmNum(0))
                    callPrelude(OVERFLOW_ERROR, VS)
                    reg
                }
                NOT -> not(expr.toARM().toReg())
            }
            is ArrayElem -> {
                var result = load(getReg(), findVar(arrIdent))
                for (expr in indices) {
                    val currType = arrIdent.getType(symbolTable).unwrapArrayType()!!
                    val indexReg = expr.toARM().toReg()
                    callCheckArrBound(indexReg, result)
                    val offset = binop(MUL, indexReg, indexReg, ImmNum(sizeof(currType)), op2Destructive = true)
                    result = binop(ADD, result, result, offset)
                    load(result, Offset(result, 4), sizeof(currType) == 1)
                }
                result
            }
            is PairElem -> {
                val temp = expr.toARM().toReg()
                mov(Reg(0), temp)
                callPrelude(CHECK_NULL_PTR)
                load(temp, Offset(temp, when (func) {
                    FST -> 0; SND -> 4
                }))
                load(temp, Offset(temp), sizeof(this.getType(symbolTable)) == 1)
            }
            is TypeMember -> {
                TODO()
            }
            is ArrayLiteral -> {
                // Malloc the memory for each element in the array
                val elemSize = elements.getOrNull(0)?.let { sizeof(it.getType(symbolTable)) } ?: 0
                val totalSize = elements.size * elemSize + 4

                val baseAddressReg = mov(getReg(), callMalloc(totalSize))

                // Store each element in the array
                elements.forEachIndexed { index, it ->
                    val tempElemReg = it.toARM().toReg()
                    store(tempElemReg, Offset(baseAddressReg, 4 + index * elemSize, false), elemSize)
                }
                // Store the size of the array at the end
                val tmp = getReg()
                load(AL, tmp, ImmNum(elements.size))
                store(tmp, Offset(baseAddressReg, 0, false))
                baseAddressReg
            }
            is NewPair -> {
                callMalloc(8)
                val pairAddr = mov(getReg(), Reg(0))
                val fst = first.toARM().toReg()
                callMalloc(sizeof(first.getType(symbolTable)))
                store(fst, Offset(Reg(0)), sizeof(first.getType(symbolTable)))
                store(Reg(0), Offset(pairAddr))
                val snd = second.toARM().toReg()
                callMalloc(sizeof(second.getType(symbolTable)))
                store(snd, Offset(Reg(0)), sizeof(second.getType(symbolTable)))
                store(Reg(0), Offset(pairAddr, 4))
                pairAddr
            }
            is FunctionCall -> {
                val oldSPOffset = spOffset
                for (arg in args.reversed()) {
                    val reg = arg.toARM().toReg()
                    val size = sizeof(arg.getType(symbolTable))
                    store(reg, Offset(SpecialReg(SP), -size, true), size)
                    spOffset += size
                }
                bl(AL, funcLabelMap.getValue(ident))
                moveSP(spOffset - oldSPOffset)
                mov(getReg(), Reg(0))
            }
            is EnumRange -> TODO()
            is IfExpr -> TODO()
        }
    }

    /* Pre-allocate space on stack for all variables defined in this scope, except for function parameters.
    *  It would be called on the first occurrence of any declaration of any variable in this scope.
    *  Calling this method twice would do no effect. */
    private fun scopeEnterDef(variable: Identifier, params: Set<Parameter> = emptySet()) {
        if (variable.scopeId !in firstDefReachedScopes) {
            val defs = symbolTable.scopeDefs[variable.scopeId]!! - params.map { it.second.name }
            var offsetAcc = 0
            defs.forEach { v ->
                val pair = v to variable.scopeId
                val size = sizeof(symbolTable.collect[pair]!!.type)
                varOffsetMap[v to variable.scopeId] = spOffset + offsetAcc + size
                offsetAcc += size
                currScopeOffset += size
            }
            moveSP(-currScopeOffset)
            firstDefReachedScopes += variable.scopeId
        }
    }

    /* Define all prelude functions that are used in this code. */
    private fun definePreludes() {
        for (func in requiredPreludeFuncs) {
            virtualRegIdAcc = 4
            setBlock(func.getLabel())
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
                    branch(GE, label1)
                    callPrintf(StringLit("ArrayIndexOutOfBoundsError: negative index"),
                            true)
                    bl(AL, RUNTIME_ERROR.getLabel(), Unreachable)
                    setBlock(label1)
                    load(Reg(1), Offset(Reg(1), 0))
                    cmp(Reg(0), Reg(1))
                    branch(Condition.LT, label2)
                    callPrintf(StringLit("ArrayIndexOutOfBoundsError: index too large"), true)
                    bl(AL, RUNTIME_ERROR.getLabel(), Unreachable)
                    setBlock(label2)
                    pop(SpecialReg(PC))
                }
                CHECK_DIV_BY_ZERO -> {
                    val noErr = getLabel("no_err")
                    push(SpecialReg(LR))
                    cmp(Reg(1), ImmNum(0))
                    branch(NE, noErr)
                    callPrintf(StringLit("DivideByZeroError: divide or modulo by zero"),
                            true)
                    bl(Condition.EQ, RUNTIME_ERROR.getLabel(), Unreachable)
                    setBlock(noErr)
                    pop(SpecialReg(PC))
                }
                CHECK_NULL_PTR -> {
                    push(SpecialReg(LR))
                    val notNullLabel = getLabel("not_null")
                    cmp(Reg(0), immNull())
                    branch(NE, notNullLabel)
                    callPrintf(StringLit("NullReferenceError: dereference a null reference"), true)
                    bl(AL, RUNTIME_ERROR.getLabel(), Unreachable)
                    setBlock(notNullLabel)
                    pop(SpecialReg(PC))
                }
                FREE_ARRAY -> {
                    push(SpecialReg(LR))
                    bl(AL, CHECK_NULL_PTR.getLabel())
                    bl(AL, Label("free"))
                    pop(SpecialReg(PC))
                }
                FREE_PAIR -> {
                    push(SpecialReg(LR))
                    bl(AL, CHECK_NULL_PTR.getLabel())
                    push(Reg(0))
                    load(Reg(0), Offset(Reg(0), 0))
                    bl(AL, Label("free"))
                    load(Reg(0), Offset(SpecialReg(SP), 0))
                    load(Reg(0), Offset(Reg(0), 4))
                    bl(AL, Label("free"))
                    pop(Reg(0))
                    bl(AL, Label("free"))
                    pop(SpecialReg(PC))
                }
            }
        }
    }

    /* Move the current position of sp by the given offset. */
    private fun moveSP(offset: Int, record: Boolean = true) {
        if (offset < 0) {
            binop(SUB, SpecialReg(SP), SpecialReg(SP), ImmNum(-offset))
        } else if (offset > 0) {
            binop(ADD, SpecialReg(SP), SpecialReg(SP), ImmNum(offset))
        }
        if (record) {
            spOffset -= offset
        }
    }

    /* Move the content within the given operand to a register.
    *  If the given operand is already a register, it would do nothing
    *  In other situations, it would automatically move its content to a reg using either MOV or LDR
    *  Return the register that stores the value of the operand. */
    private fun Operand.toReg(dst: Register? = null): Register = when (this) {
        is Register -> dst?.let { mov(dst, this) } ?: this
        is ImmNum -> if (num in 0..255) mov(dst ?: getReg(), this) else load(dst ?: getReg(), this)
        is Label -> {
            load(dst ?: getReg(), this)
        }
        else -> mov(dst ?: getReg(), this)
    }

    /* This method allocates some space on stack for a variable,
    *  returns the offset from the initial sp. */
    private fun alloca(varNode: Identifier, reg: Register? = null, byte: Int = 4) {
        val offset = spOffset - varOffsetMap[varNode.getVarSID()]!!
        val dest = Offset(SpecialReg(SP), offset)
        reg?.let { store(reg, dest, byte) }
    }

    /* Find the alloca-ed variable's offset from the current position of the sp
    *  by the given var node. */
    private fun findVar(varNode: Identifier): Offset =
            Offset(SpecialReg(SP), spOffset - varOffsetMap[varNode.name to varNode.scopeId]!!)

    /* Define a string constant and return its label. */
    private fun defString(content: String, isSingleton: Boolean = true): Label {
        val msgLabel = getLabel(if (isSingleton) "singleton" else "msg")
        if (isSingleton) {
            val prevDef = singletonStringConsts[content]
            if (prevDef != null) {
                return prevDef
            }
            singletonStringConsts[content] = msgLabel
        } else {
            commonStringConsts += content to msgLabel
        }
        return msgLabel
    }

    /* Run the provided action in the context of a new scope.
    *  The original scope offset is recorded and sp is moved back to it
    *  after the action is finished. */
    private inline fun inScopeDo(action: () -> Unit) {
        val prevScopeOffset = currScopeOffset
        currScopeOffset = 0
        action()
        moveSP(currScopeOffset)
        currScopeOffset = prevScopeOffset
    }

    /* Get the next avaliable register */
    private fun getReg(): Register = Reg(virtualRegIdAcc++)

    /* Set the current block's label to the given label.
    *  Indicates the beginning of a new instruction block. */
    private fun setBlock(label: Label) {
        if (blocks.isNotEmpty() && blocks.last.terminator == B(AL, label)) {
            blocks.last.terminator = FallThrough
        }
        currBlockLabel = label
        blockComplete = false
    }

    /* Finish building the current block, pack it up and record it. */
    private fun packBlock(terminator: Terminator = FallThrough) {
        val block = InstructionBlock(currBlockLabel, instructions.toMutableList(), terminator)
        blocks.addLast(block)
        instructions.clear()
        currBlockLabel = getLabel("${currBlockLabel.name}_seq")
        blockComplete = true
    }

    /* Get a new label based on the given name prefix */
    private fun getLabel(name: String): Label = Label(labelNameTable.getName(name))

    /** Instruction helper methods **/
    /* These methods are helper methods that inserts instructions, change block state, etc. */

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
        bl(cond, label)
        packBlock(terminator)
    }

    private fun load(cond: Condition, dst: Register, src: Operand, byte: Boolean = false): Register {
        instructions += if (src is ImmNum && src.num in 0..255) {
            Mov(cond, dst, src)
        } else {
            if (byte) {
                Ldrsb(cond, dst, src)
            } else {
                Ldr(cond, dst, src)
            }
        }
        return dst
    }

    private fun load(dst: Register, src: Operand, byte: Boolean = false): Register = load(AL, dst, src, byte)

    private fun store(src: Register, dst: Offset, byte: Int = 4): Operand {
        instructions += if (byte == 1) {
            Strb(src, dst)
        } else {
            Str(AL, src, dst)
        }
        return dst
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

    private fun binop(opType: BinaryOperator,
                      dst: Register,
                      rn: Register,
                      op2: Operand,
                      op2Destructive: Boolean = false,    // op2 can be used in a destructive way if it is a reg.
                      setFlag: Boolean = false): Register {
        var overflow = false
        // If the immediate value is greater than 1024, load it into a separate register first
        var tempOp2: Operand = op2
        if (op2 is ImmNum && op2.num > maxImmNum) {
            tempOp2 = load(getReg(), op2)
        }
        when (opType) {
            ADD -> instructions += Add(AL, dst, rn, tempOp2, setFlag).also { overflow = true }
            SUB -> instructions += Sub(AL, dst, rn, tempOp2, setFlag).also { overflow = true }
            MUL -> {
                val op2Reg = op2.toReg()
                val rdHi = if (op2Destructive) op2Reg else getReg()
                instructions += Smull(AL, dst, rdHi, rn, op2Reg)
                instructions += Cmp(rdHi, dst, ASR to 31)
                callPrelude(OVERFLOW_ERROR, NE)
            }
            DIV -> {
                mov(Reg(0), rn)
                mov(Reg(1), op2)
                callPrelude(CHECK_DIV_BY_ZERO)
                bl(AL, Label("__aeabi_idiv"))
                mov(dst, Reg(0))
            }
            MOD -> {
                mov(Reg(0), rn)
                mov(Reg(1), op2)
                callPrelude(CHECK_DIV_BY_ZERO)
                bl(AL, Label("__aeabi_idivmod"))
                mov(dst, Reg(1))
            }
            GTE -> instructions += listOf(
                    Cmp(rn, op2),
                    Mov(Condition.GE, dst, immTrue()),
                    Mov(Condition.LT, dst, immFalse()))
            LTE -> instructions += listOf(
                    Cmp(rn, op2),
                    Mov(Condition.LE, dst, immTrue()),
                    Mov(Condition.GT, dst, immFalse()))
            GT -> instructions += listOf(
                    Cmp(rn, op2),
                    Mov(Condition.GT, dst, immTrue()),
                    Mov(Condition.LE, dst, immFalse()))
            LT -> instructions += listOf(
                    Cmp(rn, op2),
                    Mov(Condition.LT, dst, immTrue()),
                    Mov(Condition.GE, dst, immFalse()))
            EQ -> instructions += listOf(
                    Cmp(rn, op2),
                    Mov(Condition.EQ, dst, immTrue()),
                    Mov(Condition.NE, dst, immFalse()))
            NEQ -> instructions += listOf(
                    Cmp(rn, op2),
                    Mov(Condition.NE, dst, immTrue()),
                    Mov(Condition.EQ, dst, immFalse()))
            AND -> instructions += And(AL, dst, rn, op2)
            OR -> instructions += Orr(AL, dst, rn, op2)
        }
        if (setFlag && overflow) {
            callPrelude(OVERFLOW_ERROR, VS)
        }
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

    /** Call Prelude Function Helper Methods **/

    private fun callCheckArrBound(expected: Operand, arrayPtr: Register) {
        mov(Reg(0), expected.toReg())
        mov(Reg(1), arrayPtr)
        callPrelude(CHECK_ARR_BOUND)
    }

    private fun callMalloc(size: Int): Register {
        mov(Reg(0), ImmNum(size))
        bl(AL, Label("malloc"))
        return Reg(0)
    }

    private fun callScanf(expr: Expression) {
        val exprOffset: Offset = getLhsAddress(expr)
        binop(ADD, Reg(1), exprOffset.src, ImmNum(exprOffset.offset))
        val type = expr.getType(symbolTable)
        val fmtStr = (if (type == charType()) " " else "") +
                getFormatString(type, false)
        load(Reg(0), defString(fmtStr, true))
        binop(ADD, Reg(0), Reg(0), ImmNum(4))
        bl(AL, Label("scanf"))
    }


    private fun callPrintf(expr: Expression, newline: Boolean) {
        val operand = expr.toARM().toReg()
        val exprType = expr.getType(symbolTable)
        when (exprType) {
            boolType() -> {
                cmp(operand, immFalse())
                load(NE, Reg(1), defString("true"))
                load(Condition.EQ, Reg(1), defString("false"))
                binop(ADD, Reg(1), Reg(1), ImmNum(4), false)
                load(Reg(0), defString(getFormatString(exprType, newline)))
                binop(ADD, Reg(0), Reg(0), ImmNum(4), false)
            }
            stringType(),
            ArrayType(charType()) -> {
                mov(Reg(1), operand)
                binop(ADD, Reg(1), Reg(1), ImmNum(4), false)
                load(Reg(0), defString(getFormatString(exprType, newline)))
                binop(ADD, Reg(0), Reg(0), ImmNum(4), false)
            }
            else -> {
                mov(Reg(1), operand)
                load(Reg(0), defString(getFormatString(exprType, newline)))
                binop(ADD, Reg(0), Reg(0), ImmNum(4))
            }
        }
        bl(AL, Label("printf"))
        mov(Reg(0), ImmNum(0))
        bl(AL, Label("fflush"))
    }

    /* Call a prelude function. */
    private fun callPrelude(func: PreludeFunc, cond: Condition = AL) {
        requiredPreludeFuncs += func.findDependencies()
        bl(cond, func.getLabel())
    }

    /* Get the address of a lhs expression, returns an offset. */
    private fun getLhsAddress(lhs: Expression): Offset = when (lhs) {
        is Identifier -> findVar(lhs)
        is ArrayElem -> {
            var result = findVar(lhs.arrIdent)
            val arrReg = getReg()
            var currType = lhs.arrIdent.getType(symbolTable)
            for (expr in lhs.indices) {
                val indexReg = expr.toARM().toReg()
                load(AL, arrReg, result)
                callCheckArrBound(indexReg, arrReg)
                currType = currType.unwrapArrayType()!!
                binop(MUL, indexReg, indexReg, ImmNum(sizeof(currType)), op2Destructive = true)
                binop(ADD, indexReg, indexReg, ImmNum(4))
                binop(ADD, arrReg, arrReg, indexReg)
                result = Offset(arrReg)
            }
            result
        }
        is PairElem -> {
            val pairAddr = lhs.expr.toARM().toReg()
            mov(Reg(0), pairAddr)
            callPrelude(CHECK_NULL_PTR)
            Offset(pairAddr, when (lhs.func) {
                FST -> 0; SND -> 4
            })
        }
        else -> {
            throw IllegalArgumentException("Target has to be a left-hand-side expression")
        }
    }

    private fun getFormatString(type: Type, newline: Boolean): String {
        val format = when (type) {
            is BaseType -> when (type.kind) {
                INT -> "%d"
                CHAR -> "%c"
                BOOL -> "%s"
                STRING -> "%s"
                else -> "%p"
            }
            ArrayType(charType()) -> "%s"
            else -> "%p"
        }
        return format + if (newline) "\\n" else ""
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
            is PairElem -> expr.getType(symbolTable).unwrapPairType(func)!!
            is ArrayLiteral -> elements.first().getType(symbolTable)
            is NewPair -> Type.PairType(first.getType(symbolTable), second.getType(symbolTable))
            is FunctionCall -> symbolTable.functions[ident]!!.type.retType
            is TypeMember -> TODO()
            is EnumRange -> TODO()
            is IfExpr -> TODO()
        }
    }

    private fun sizeof(type: Type): Int = when (type) {
        charType(), boolType() -> 1
        else -> 4
    }
}