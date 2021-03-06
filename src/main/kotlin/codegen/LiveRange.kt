package codegen

import codegen.arm.ArmProgram
import codegen.arm.Operand.Register.Reg

class LiveRange(firstDef: Int) {
    val defs = mutableListOf<Int>()
    val uses = mutableListOf<Int>()

    init {
        defs += firstDef
    }

    fun lastUsage(): Int = uses.lastOrNull() ?: defs.last()

    fun firstDef(): Int = defs.first()

    fun isNotUsedDuring(other: LiveRange): Boolean {
        return (defs + uses).all { it < other.firstDef() || it > other.lastUsage() }
    }

    fun mergeToPairs(): List<Pair<String, Int>> {
        val pairs = mutableListOf<Pair<String, Int>>()
        pairs += defs.map { "def" to it }
        pairs += uses.map { "use" to it }
        return pairs.sortedWith(compareBy { pair: Pair<String, Int> -> pair.second } then compareBy { it.first })
    }

    fun findAllCoexistingRegs(self: Reg, program: ArmProgram): Set<Reg> {
        val appearance = defs + uses
        val result = mutableSetOf<Reg>()
        appearance.map { program.findInstrAt(it) }.forEach { instr ->
            result += instr.getDefs().filterIsInstance<Reg>()
            result += instr.getUses().filterIsInstance<Reg>()
        }
        result -= self
        return result
    }

    override fun toString(): String {
        return mergeToPairs().joinToString(", ")
    }

}

typealias LiveRangeMap = Map<Reg, LiveRange>

/* Find an available virtual register to push to the stack. The to-be-pushed virtual register (Vp) mush satisfy:
*  1. Vp is not used in the live range of the waiting virtual reg(Vw).
*  2. Vp must not be dead.
*  3. Vp must not already be pushed to the stack.
*  4. If Vp is unified with a real register Rp, then Vw must not co-exist with any other virtual registers that
*     are also unified with Rp in any instruction. */
fun LiveRangeMap.findVirtualToPush(
        waitingVirtual: Reg,
        unificationMap: MutableMap<Reg, Reg>,
        pushedVirtuals: MutableList<Reg>,
        deadVirtuals: MutableSet<Reg>,
        realToVirtualsMap: Map<Reg, MutableSet<Reg>>,
        oldProgram: ArmProgram
): Reg {
    val vwLiveRange = this[waitingVirtual]
            ?: throw IllegalArgumentException("Given register $waitingVirtual is not in the live range map")
    unificationMap.keys
            .filterNot { it in pushedVirtuals || it in deadVirtuals } // find out not dead, not-on-stack virtuals
            .sortedBy { -it.id }
            .forEach { vp ->
        val vpLiveRange = this.getValue(vp)
        if (vpLiveRange.isNotUsedDuring(vwLiveRange)) {
            if (vp in unificationMap) {     // if vp has unified with a real reg
                val rp = unificationMap.getValue(vp)
                val coexists = vwLiveRange.findAllCoexistingRegs(waitingVirtual, oldProgram)
                if (coexists.all { it !in realToVirtualsMap.getValue(rp) }) {
                    return vp
                }
            } else {
                return vp
            }
        }
    }
    throw IllegalArgumentException("Unable to find such register which is not in use for $waitingVirtual")
}

fun LiveRangeMap.dump() {
    forEach { (reg, liveRange) ->
        println("$reg:, $liveRange")
    }
}