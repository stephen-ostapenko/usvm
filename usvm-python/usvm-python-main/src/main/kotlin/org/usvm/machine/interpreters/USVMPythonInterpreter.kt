package org.usvm.machine.interpreters

import mu.KLogging
import org.usvm.*
import org.usvm.interpreter.ConcolicRunContext
import org.usvm.language.PythonPinnedCallable
import org.usvm.machine.interpreters.operations.basic.BadModelException
import org.usvm.machine.interpreters.operations.basic.UnregisteredVirtualOperation
import org.usvm.machine.symbolicobjects.*
import org.usvm.language.PythonUnpinnedCallable
import org.usvm.language.SymbolForCPython
import org.usvm.language.types.PythonTypeSystem
import org.usvm.machine.*
import org.usvm.machine.interpreters.operations.tracing.CancelledExecutionException
import org.usvm.machine.interpreters.operations.tracing.InstructionLimitExceededException
import org.usvm.machine.model.PyModel
import org.usvm.machine.utils.PyModelHolder
import org.usvm.machine.utils.PythonMachineStatisticsOnFunction
import org.usvm.utils.PythonObjectSerializer
import org.usvm.utils.ReprObjectSerializer

class USVMPythonInterpreter<PythonObjectRepresentation>(
    private val ctx: UPythonContext,
    private val typeSystem: PythonTypeSystem,
    private val unpinnedCallable: PythonUnpinnedCallable,
    private val pinnedCallable: PythonPinnedCallable,
    private val iterationCounter: IterationCounter,
    private val printErrorMsg: Boolean,
    private val statistics: PythonMachineStatisticsOnFunction,
    private val maxInstructions: Int,
    private val isCancelled: (Long) -> Boolean,
    private val allowPathDiversion: Boolean = true,
    private val serializer: PythonObjectSerializer<PythonObjectRepresentation>,
    private val saveRunResult: (PythonAnalysisResult<PythonObjectRepresentation>) -> Unit
) : UInterpreter<PythonExecutionState>() {
    private fun getSeeds(
        concolicRunContext: ConcolicRunContext,
        symbols: List<UninterpretedSymbolicPythonObject>
    ): List<InterpretedInputSymbolicPythonObject> =
        symbols.map { interpretSymbolicPythonObject(it, concolicRunContext.modelHolder) as InterpretedInputSymbolicPythonObject }

    private fun getConcrete(
        converter: ConverterToPythonObject,
        seeds: List<InterpretedInputSymbolicPythonObject>,
        symbols: List<UninterpretedSymbolicPythonObject>
    ): List<PythonObject> =
        (seeds zip symbols).map { (seed, _) -> converter.convert(seed) }

    private fun getInputs(
        converter: ConverterToPythonObject,
        concrete: List<PythonObject>,
        seeds: List<InterpretedInputSymbolicPythonObject>
    ): List<InputObject<PythonObjectRepresentation>>? =
        if (converter.numberOfVirtualObjectUsages() == 0) {
            val serializedInputs = concrete.map { serializer.serialize(it) }
            (seeds zip unpinnedCallable.signature zip serializedInputs).map { (p, z) ->
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
        val start = System.currentTimeMillis()
        val concolicRunContext =
            ConcolicRunContext(
                state,
                ctx,
                modelHolder,
                typeSystem,
                allowPathDiversion,
                statistics,
                maxInstructions
            ) {
                isCancelled(start)
                // timeoutPerRunMs?.let { System.currentTimeMillis() - start > timeoutPerRunMs } ?: false
            }
        state.meta.objectsWithoutConcreteTypes = null
        state.meta.lastConverter?.restart()
        try {
            logger.debug("Step on state: {}", state)
            logger.debug("Source of the state: {}", state.meta.generatedFrom)
            require(state.pyModel.uModel is PyModel) {
                "Did not call .toPyModel on model from solver"
            }
            val validator = ObjectValidator(concolicRunContext)
            val symbols = state.inputSymbols
            symbols.forEach { validator.check(it) }
            val seeds = getSeeds(concolicRunContext, symbols)
            val converter = concolicRunContext.converter
            val concrete = getConcrete(converter, seeds, symbols)
            val virtualObjects = converter.getPythonVirtualObjects()
            val inputs = runCatching {
                getInputs(converter, concrete, seeds)
            }.getOrElse {
                logger.debug(
                    "Error while serializing inputs. Types: {}. Omitting step...",
                    seeds.map { it.getConcreteType() }
                )
                return StepResult(emptySequence(), false)
            }
            concolicRunContext.usesVirtualInputs = inputs == null

            if (logger.isDebugEnabled) {  // getting __repr__ might be slow
                logger.debug(
                    "Generated inputs: {}",
                    concrete.joinToString(", ") {
                        ReprObjectSerializer.serialize(it)
                    }
                )
            }

            try {
                val result = ConcretePythonInterpreter.concolicRun(
                    pinnedCallable.asPythonObject,
                    concrete,
                    virtualObjects,
                    symbols.map { SymbolForCPython(it, 0) },
                    concolicRunContext,
                    printErrorMsg
                )
                if (inputs != null) {
                    val serializedResult = serializer.serialize(result)
                    saveRunResult(PythonAnalysisResult(converter, inputs, Success(serializedResult)))
                }
                logger.debug("Step result: Successful run. Returned ${ReprObjectSerializer.serialize(result)}")

            } catch (exception: CPythonExecutionException) {
                require(exception.pythonExceptionType != null)
                require(exception.pythonExceptionValue != null)
                if (ConcretePythonInterpreter.isJavaException(exception.pythonExceptionValue)) {
                    throw ConcretePythonInterpreter.extractException(exception.pythonExceptionValue)
                }
                logger.debug(
                    "Step result: exception from CPython: {} - {}",
                    ConcretePythonInterpreter.getNameOfPythonType(exception.pythonExceptionType),
                    ConcretePythonInterpreter.getPythonObjectRepr(exception.pythonExceptionValue)
                )
                if (inputs != null) {
                    val serializedException = serializer.serialize(exception.pythonExceptionType)
                    saveRunResult(PythonAnalysisResult(converter, inputs, Fail(serializedException)))
                }
            }

            iterationCounter.iterations += 1
            val resultState = concolicRunContext.curState
            if (resultState != null) {
                resultState.meta.wasExecuted = true

                if (resultState.delayedForks.isEmpty() && inputs == null) {
                    resultState.meta.objectsWithoutConcreteTypes = converter.getUSVMVirtualObjects()
                    resultState.meta.lastConverter = converter
                }
                logger.debug("Finished step on state: {}", concolicRunContext.curState)
                return StepResult(concolicRunContext.forkedStates.asSequence(), !state.isTerminated())

            } else {
                logger.debug("Ended step with path diversion")
                return StepResult(emptySequence(), !state.isTerminated())
            }

        } catch (_: BadModelException) {

            iterationCounter.iterations += 1
            logger.debug("Step result: Bad model")
            return StepResult(concolicRunContext.forkedStates.asSequence(), !state.isTerminated())

        } catch (_: UnregisteredVirtualOperation) {

            iterationCounter.iterations += 1
            logger.debug("Step result: Unregistrered virtual operation")
            concolicRunContext.curState?.meta?.modelDied = true
            concolicRunContext.statistics.addUnregisteredVirtualOperation()
            return StepResult(concolicRunContext.forkedStates.asSequence(), !state.isTerminated())

        } catch (_: InstructionLimitExceededException) {

            iterationCounter.iterations += 1
            logger.debug("Step result: InstructionLimitExceededException")
            concolicRunContext.curState?.meta?.wasInterrupted = true
            return StepResult(concolicRunContext.forkedStates.reversed().asSequence(), !state.isTerminated())

        } catch (_: CancelledExecutionException) {

            logger.debug("Step result: execution cancelled")
            concolicRunContext.curState?.meta?.wasInterrupted = true
            return StepResult(concolicRunContext.forkedStates.reversed().asSequence(), !state.isTerminated())

        } finally {
            concolicRunContext.converter.restart()
        }
    }

    companion object {
        val logger = object : KLogging() {}.logger
    }
}