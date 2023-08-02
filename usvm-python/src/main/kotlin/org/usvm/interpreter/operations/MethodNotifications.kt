package org.usvm.interpreter.operations

import org.usvm.interpreter.ConcolicRunContext
import org.usvm.interpreter.symbolicobjects.UninterpretedSymbolicPythonObject
import org.usvm.language.types.*

fun nbBoolKt(context: ConcolicRunContext, on: UninterpretedSymbolicPythonObject) {
    on.addSupertype(context, HasNbBool)
}

fun nbIntKt(context: ConcolicRunContext, on: UninterpretedSymbolicPythonObject) {
    on.addSupertype(context, HasNbInt)
}

fun nbAddKt(context: ConcolicRunContext, left: UninterpretedSymbolicPythonObject, right: UninterpretedSymbolicPythonObject) = with(context.ctx) {
    myAssert(context, left.evalIs(context, HasNbAdd) or right.evalIs(context, HasNbAdd))
}

fun mpSubscriptKt(context: ConcolicRunContext, on: UninterpretedSymbolicPythonObject) {
    on.addSupertype(context, HasMpSubscript)
}

fun tpRichcmpKt(context: ConcolicRunContext, left: UninterpretedSymbolicPythonObject) {
    myAssert(context, left.evalIs(context, HasTpRichcmp))
}