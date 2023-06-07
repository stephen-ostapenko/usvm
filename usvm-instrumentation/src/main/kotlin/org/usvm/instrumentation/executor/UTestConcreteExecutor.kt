package org.usvm.instrumentation.executor

import UTestExecutorInitException
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.reactive.RdFault
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.jacodb.api.JcClasspath
import org.usvm.instrumentation.jacodb.transform.JcInstrumenterFactory
import org.usvm.instrumentation.testcase.UTest
import org.usvm.instrumentation.testcase.statement.UTestExecutionFailedResult
import org.usvm.instrumentation.testcase.statement.UTestExecutionResult
import org.usvm.instrumentation.testcase.statement.UTestExecutionTimedOutResult
import org.usvm.instrumentation.util.InstrumentationModuleConstants
import java.util.concurrent.TimeoutException
import kotlin.reflect.KClass
import kotlin.time.Duration

class UTestConcreteExecutor(
    private val instrumentationClassFactory: KClass<out JcInstrumenterFactory<*>>,
    private val testingProjectClasspath: String,
    private val jcClasspath: JcClasspath,
    private val timeout: Duration
) : AutoCloseable {

    private val lifetime = LifetimeDefinition()

    private val instrumentationProcessRunner =
        InstrumentationProcessRunner(testingProjectClasspath, jcClasspath, instrumentationClassFactory)

    suspend fun ensureRunnerAlive() {
        check(lifetime.isAlive) { "Executor already closed" }
        for (i in 0..InstrumentationModuleConstants.triesToRecreateRdProcess) {
            if (instrumentationProcessRunner.isAlive()) {
                return
            }
            try {
                instrumentationProcessRunner.init(lifetime)
            } catch (e: Throwable) {
                //TODO replace to logger
                println("Cant init rdProcess:(")
            }
        }
        if (!instrumentationProcessRunner.isAlive()) {
            throw UTestExecutorInitException()
        }
    }

    fun executeSync(uTest: UTest): UTestExecutionResult {
        return try {
            instrumentationProcessRunner.executeUTestSync(uTest, timeout)
        } catch (e: TimeoutException) {
            UTestExecutionTimedOutResult(e.message ?: "timeout")
        } catch (e: RdFault) {
            UTestExecutionFailedResult(e.reasonAsText)
        }
    }

    suspend fun execute(uTest: UTest): UTestExecutionResult {
        ensureRunnerAlive()
        return try {
            withTimeout(timeout) {
                instrumentationProcessRunner.executeUTest(uTest)
            }
        } catch (e: TimeoutCancellationException) {
            UTestExecutionTimedOutResult(e.message ?: "timeout")
        } catch (e: RdFault) {
            UTestExecutionFailedResult(e.reasonAsText)
        }
    }

    override fun close() {
        lifetime.terminate()
    }

}