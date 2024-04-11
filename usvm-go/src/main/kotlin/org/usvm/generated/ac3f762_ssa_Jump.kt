package org.usvm.generated

import java.io.BufferedReader
import org.usvm.jacodb.*
import org.usvm.jacodb.GoInstLocationImpl
class ssa_Jump : ssaToJacoInst {

	var anInstruction: generatedInlineStruct_000? = null

	override fun createJacoDBInst(parent: GoMethod): GoJumpInst {
		if (structToPtrMap.containsKey(this) && ptrToJacoMap.containsKey(structToPtrMap[this])) {
            return ptrToJacoMap[structToPtrMap[this]] as GoJumpInst
        }


        val res = GoJumpInst(
            GoInstLocationImpl(
                anInstruction!!.block!!.Index!!.toInt(),
                0,
                parent,
            ),
            parent,
            GoInstRef(
                anInstruction!!.block!!.Index!!.toInt()
            )
        )
		if (structToPtrMap.containsKey(this)) {
            ptrToJacoMap[structToPtrMap[this]!!] = res
        }
        return res
    }
}

fun read_ssa_Jump(buffReader: BufferedReader, id: Int): ssa_Jump {
	val res = ssa_Jump()
    if (id != -1) {
        if (ptrMap.containsKey(id)) {
            return ptrMap[id] as ssa_Jump
        }
        ptrMap[id] = res
		structToPtrMap[res] = id
    }
    var line: String
    var split: List<String>
    var id: Int
    var readType: String

	line = buffReader.readLine()
	if (line == "end") {
        return res
    }
    split = line.split(" ")
    readType = split[1]
    id = -1
    if (split.size > 2) {
        id = split[2].toInt()
    }
    res.anInstruction = mapDec[readType]?.invoke(buffReader, id) as generatedInlineStruct_000?

	buffReader.readLine()
	return res
}
