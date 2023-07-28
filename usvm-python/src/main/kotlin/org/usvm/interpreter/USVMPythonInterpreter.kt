package org.usvm.interpreter

import mu.KLogging
import org.usvm.*
import org.usvm.interpreter.operations.BadModelException
import org.usvm.interpreter.symbolicobjects.*
import org.usvm.language.PythonUnpinnedCallable
import org.usvm.language.SymbolForCPython

class USVMPythonInterpreter<PYTHON_OBJECT_REPRESENTATION>(
    private val ctx: UPythonContext,
    namespace: PythonNamespace,
    private val callable: PythonUnpinnedCallable,
    private val iterationCounter: IterationCounter,
    private val printErrorMsg: Boolean,
    private val pythonObjectSerialization: (PythonObject) -> PYTHON_OBJECT_REPRESENTATION,
    private val saveRunResult: (PythonAnalysisResult<PYTHON_OBJECT_REPRESENTATION>) -> Unit
) : UInterpreter<PythonExecutionState>() {
    private val pinnedCallable = callable.reference(namespace)

    private fun getSeeds(
        concolicRunContext: ConcolicRunContext,
        symbols: List<SymbolForCPython>
    ): List<InterpretedInputSymbolicPythonObject> =
        symbols.map { interpretSymbolicPythonObject(it.obj, concolicRunContext.modelHolder) as InterpretedInputSymbolicPythonObject }

    private fun getConcrete(
        converter: ConverterToPythonObject,
        seeds: List<InterpretedInputSymbolicPythonObject>,
        symbols: List<SymbolForCPython>
    ): List<PythonObject> =
        (seeds zip symbols).map { (seed, _) -> converter.convert(seed) }

    private fun getInputs(
        converter: ConverterToPythonObject,
        concrete: List<PythonObject?>,
        seeds: List<InterpretedInputSymbolicPythonObject>
    ): List<InputObject<PYTHON_OBJECT_REPRESENTATION>>? =
        if (converter.numberOfVirtualObjectUsages() == 0) {
            val serializedInputs = concrete.map { it!! }.map(pythonObjectSerialization)
            (seeds zip callable.signature zip serializedInputs).map { (p, z) ->
                val (x, y) = p
                InputObject(x, y, z)
            }
        } else {
            null
        }

    override fun step(state: PythonExecutionState): StepResult<PythonExecutionState> = with(ctx) {
        val modelHolder =
            if (state.meta.lastConverter != null)
                state.meta.lastConverter!!.modelHolder
            else
                PyModelHolder(state.pyModel)
        val concolicRunContext = ConcolicRunContext(state, ctx, modelHolder)
        state.meta.objectsWithoutConcreteTypes = null
        state.meta.lastConverter?.restart()
        try {
            logger.debug("Step on state: {}", state)
            val validator = ObjectValidator(concolicRunContext)
            val symbols = state.inputSymbols
            symbols.forEach { validator.check(it.obj) }
            val seeds = getSeeds(concolicRunContext, symbols)
            val converter = state.meta.lastConverter ?: ConverterToPythonObject(ctx, concolicRunContext.modelHolder)
            val concrete = getConcrete(converter, seeds, symbols)
            val virtualObjects = converter.getPythonVirtualObjects()
            val inputs = getInputs(converter, concrete, seeds)

            if (logger.isDebugEnabled) {  // getting __repr__ might be slow
                logger.debug(
                    "Generated inputs: {}",
                    concrete.joinToString(", ") { ConcretePythonInterpreter.getPythonObjectRepr(it) }
                )
            }

            try {
                val result = ConcretePythonInterpreter.concolicRun(
                    pinnedCallable,
                    concrete,
                    virtualObjects,
                    symbols,
                    concolicRunContext,
                    printErrorMsg
                )
                if (inputs != null) {
                    val serializedResult = pythonObjectSerialization(result)
                    saveRunResult(PythonAnalysisResult(converter, inputs, Success(serializedResult)))
                }
                logger.debug("Step result: Successful run")

            } catch (exception: CPythonExecutionException) {
                require(exception.pythonExceptionValue != null)
                if (ConcretePythonInterpreter.isJavaException(exception.pythonExceptionValue)) {
                    throw ConcretePythonInterpreter.extractException(exception.pythonExceptionValue)
                }
                logger.debug(
                    "Step result: exception from CPython: {}",
                    ConcretePythonInterpreter.getPythonObjectRepr(exception.pythonExceptionValue)
                )
                if (inputs != null) {
                    val serializedException = pythonObjectSerialization(exception.pythonExceptionValue)
                    saveRunResult(PythonAnalysisResult(converter, inputs, Fail(serializedException)))
                }
            }

            concolicRunContext.curState.meta.wasExecuted = true
            iterationCounter.iterations += 1

            if (concolicRunContext.curState.delayedForks.isEmpty() && inputs == null) {
                concolicRunContext.curState.meta.objectsWithoutConcreteTypes = converter.getUSVMVirtualObjects()
                concolicRunContext.curState.meta.lastConverter = converter
            }
            logger.debug((concolicRunContext.curState == state).toString())

            return StepResult(concolicRunContext.forkedStates.asSequence(), !state.meta.modelDied)

        } catch (_: BadModelException) {

            iterationCounter.iterations += 1
            logger.debug("Step result: Bad model")
            return StepResult(concolicRunContext.forkedStates.asSequence(), !state.meta.modelDied)
        }
    }

    companion object {
        val logger = object : KLogging() {}.logger
    }
}