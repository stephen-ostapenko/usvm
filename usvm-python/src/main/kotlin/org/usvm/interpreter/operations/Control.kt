package org.usvm.interpreter.operations

import io.ksmt.sort.KBoolSort
import org.usvm.UExpr
import org.usvm.fork
import org.usvm.forkMulti
import org.usvm.interpreter.ConcolicRunContext
import org.usvm.interpreter.DelayedFork
import org.usvm.interpreter.PythonExecutionState
import org.usvm.interpreter.operations.tracing.withTracing
import org.usvm.interpreter.symbolicobjects.SymbolicPythonObject
import org.usvm.interpreter.symbolicobjects.UninterpretedSymbolicPythonObject
import org.usvm.interpreter.symbolicobjects.interpretSymbolicPythonObject
import org.usvm.isTrue
import org.usvm.language.PythonPinnedCallable
import org.usvm.language.SymbolForCPython

fun myFork(ctx: ConcolicRunContext, cond: UExpr<KBoolSort>) {
    val model = ctx.curState.pyModel
    val oldCurState = ctx.curState
    val forkResult = fork(ctx.curState, cond)
    if (forkResult.positiveState?.pyModel == model) {
        ctx.curState = forkResult.positiveState
    } else if (forkResult.negativeState?.pyModel == model) {
        ctx.curState = forkResult.negativeState
    } else {
        error("Should not be reachable")
    }
    if (forkResult.negativeState != oldCurState)
        forkResult.negativeState?.let { ctx.forkedStates.add(it) }
}

fun myAssert(ctx: ConcolicRunContext, cond: UExpr<KBoolSort>) {
    val oldModel = ctx.curState.pyModel
    val forkResult = forkMulti(ctx.curState, listOf(cond)).single()
    if (forkResult == null)
        ctx.curState.modelDied = true

    if (forkResult?.pyModel != oldModel)
        throw BadModelException
}

fun addDelayedFork(context: ConcolicRunContext, on: UninterpretedSymbolicPythonObject, clonedState: PythonExecutionState) {
    context.curState.delayedForks = context.curState.delayedForks.add(
        DelayedFork(
            clonedState,
            on,
            context.curState.delayedForks
        )
    )
}

fun handlerForkKt(ctx: ConcolicRunContext, cond: UninterpretedSymbolicPythonObject) {
    if (cond.getConcreteTypeInModel(ctx) == null) {
        addDelayedFork(ctx, cond, ctx.curState.clone())
    }
    val expr = cond.getToBoolValue(ctx) ?: return
    myFork(ctx, expr)
}

fun handlerFunctionCallKt(ctx: ConcolicRunContext, function: PythonPinnedCallable) {
    ctx.curState.callStack.push(function, ctx.curState.lastHandlerEvent)
}

fun handlerReturnKt(ctx: ConcolicRunContext) {
    ctx.curState.callStack.pop()
}

object BadModelException: Exception()