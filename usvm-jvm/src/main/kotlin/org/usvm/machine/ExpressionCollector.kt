package org.usvm.machine

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import io.ksmt.KContext
import io.ksmt.expr.KApp
import io.ksmt.expr.KBvAndExpr
import io.ksmt.expr.KBvLogicalShiftRightExpr
import io.ksmt.expr.KBvNAndExpr
import io.ksmt.expr.KBvNorExpr
import io.ksmt.expr.KBvOrExpr
import io.ksmt.expr.KBvXNorExpr
import io.ksmt.expr.KBvXorExpr
import io.ksmt.expr.KBvZeroExtensionExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.runner.serializer.AstSerializationCtx
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.time.Duration

fun serialize(ctx: KContext, expressions: List<KExpr<KBoolSort>>, outputStream: OutputStream) {
    val serializationCtx = AstSerializationCtx().apply { initCtx(ctx) }
    val marshaller = AstSerializationCtx.marshaller(serializationCtx)
    val emptyRdSerializationCtx = SerializationCtx(Serializers())

    val buffer = UnsafeBuffer(ByteArray(100_000)) // ???

    expressions.forEach { expr ->
        marshaller.write(emptyRdSerializationCtx, buffer, expr)
    }

    outputStream.write(buffer.getArray())
    outputStream.flush()
}

object MethodNameStorage {
    val methodName = ThreadLocal<String>()
}

class ExpressionCollector<C : KSolverConfiguration>(
    private val ctx: KContext, private val baseSolver: KSolver<C>
) : KSolver<C> by baseSolver {

    companion object {
        val counter = ThreadLocal<Long>()
    }

    init {
        File("formulas").mkdirs()
        File("formulas/${MethodNameStorage.methodName.get()}").mkdirs()

        counter.set(0)
    }

    private fun getNewFileCounter(): Long {
        val result = counter.get()
        counter.set(result + 1)
        return result
    }

    private fun getNewFileName(suffix: String): String {
        return "formulas/${MethodNameStorage.methodName.get()}/f-${getNewFileCounter()}-$suffix"
    }

    val stack = mutableListOf<MutableList<KExpr<KBoolSort>>>(mutableListOf())

    override fun assert(expr: KExpr<KBoolSort>) {
        stack.last().add(expr)
        baseSolver.assert(expr)
    }

    override fun assertAndTrack(expr: KExpr<KBoolSort>) {
        stack.last().add(expr)
        baseSolver.assertAndTrack(expr)
    }

    override fun push() {
        stack.add(mutableListOf())
        baseSolver.push()
    }

    override fun pop(n: UInt) {
        repeat(n.toInt()) {
            stack.removeLast()
        }
        baseSolver.pop(n)
    }

    override fun check(timeout: Duration): KSolverStatus {
        val result = baseSolver.check(timeout)
        serialize(ctx, stack.flatten(), FileOutputStream(getNewFileName(result.toString().lowercase())))
        return result
    }

    override fun checkWithAssumptions(assumptions: List<KExpr<KBoolSort>>, timeout: Duration): KSolverStatus {
        val result = baseSolver.checkWithAssumptions(assumptions, timeout)
        serialize(ctx, stack.flatten() + assumptions, FileOutputStream(getNewFileName(result.toString().lowercase())))
        return result
    }
}

/*
class ExpressionCollector<Config: KSolverConfiguration>(
    private val solver: KSolver<Config>
) : KSolver<Config> by solver {
    private var currentLevelAssertions = mutableListOf<KExpr<KBoolSort>>()
    private val assertions = mutableListOf(currentLevelAssertions)
    private val assertionsToDump = mutableListOf<KExpr<KBoolSort>>()
    private val dirPath = "usvm-exprs"

    init {
        File(dirPath).mkdir()
    }

    override fun push() {
        currentLevelAssertions = mutableListOf()
        assertions.add(currentLevelAssertions)

        solver.push()
    }

    override fun pop(n: UInt) {
        solver.pop(n)

        val currentScope = assertions.lastIndex.toUInt()
        require(n <= currentScope) {
            "Can not pop $n scope levels because current scope level is $currentScope"
        }

        if (n == 0u) return

        repeat(n.toInt()) {
            assertions.removeLast()
        }
        currentLevelAssertions = assertions.last()
    }

    override fun assert(expr: KExpr<KBoolSort>) {
        currentLevelAssertions.add(expr)

        solver.assert(expr)
    }

    override fun assert(exprs: List<KExpr<KBoolSort>>) {
        currentLevelAssertions.addAll(exprs)

        solver.assert(exprs)
    }

    override fun check(timeout: Duration): KSolverStatus {
        dumpAssertions(assertions.flatten())

        return solver.check(timeout)
    }

    override fun checkWithAssumptions(assumptions: List<KExpr<KBoolSort>>, timeout: Duration): KSolverStatus {
        dumpAssertions(assertions.flatten() + assumptions)

        return solver.checkWithAssumptions(assumptions, timeout)
    }

    private fun dumpAssertions(expressions: List<KExpr<KBoolSort>>) {
        if (expressions.isEmpty()) return

        val ctx = expressions.first().ctx
        val expr = ctx.mkAnd(expressions)

        if (expr is KInterpretedValue) return

        assertionsToDump.add(expr)
    }

    override fun close() {
        solver.close()

        if (assertionsToDump.isEmpty()) return

        val ctx = assertionsToDump.first().ctx
        val serializationCtx = AstSerializationCtx().apply { initCtx(ctx) }
        val marshaller = AstSerializationCtx.marshaller(serializationCtx)
        val emptyRdSerializationCtx = SerializationCtx(Serializers())
        val buffer = UnsafeBuffer(ByteArray(100))

        assertionsToDump.forEach { expr ->
            marshaller.write(emptyRdSerializationCtx, buffer, expr)
        }

        val wrapped = ByteBuffer.wrap(buffer.getArray(), 0, buffer.position)

        val filePath = "$dirPath/${(File(dirPath).listFiles()?.size ?: -1) + 1}"
        File(filePath).writeBytes(wrapped.array())
    }
}
 */