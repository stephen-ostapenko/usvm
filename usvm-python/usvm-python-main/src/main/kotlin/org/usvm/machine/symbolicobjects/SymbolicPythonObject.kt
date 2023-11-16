package org.usvm.machine.symbolicobjects

import io.ksmt.sort.KIntSort
import org.usvm.*
import org.usvm.api.*
import org.usvm.constraints.UTypeConstraints
import org.usvm.interpreter.ConcolicRunContext
import org.usvm.language.PythonCallable
import org.usvm.machine.utils.PyModelHolder
import org.usvm.machine.interpreters.operations.basic.myAssert
import org.usvm.language.types.*
import org.usvm.machine.UPythonContext
import org.usvm.memory.UMemory
import org.usvm.types.USingleTypeStream
import org.usvm.types.UTypeStream
import org.usvm.types.first

sealed class SymbolicPythonObject(
    open val address: UHeapRef,
    val typeSystem: PythonTypeSystem
) {
    override fun equals(other: Any?): Boolean {
        if (other !is SymbolicPythonObject)
            return false
        return address == other.address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }
}

class UninterpretedSymbolicPythonObject(
    address: UHeapRef,
    typeSystem: PythonTypeSystem
): SymbolicPythonObject(address, typeSystem) {
    fun addSupertype(ctx: ConcolicRunContext, type: PythonType) {
        if (address is UConcreteHeapRef)
            return
        require(ctx.curState != null)
        myAssert(ctx, evalIs(ctx, type))
    }

    fun addSupertypeSoft(ctx: ConcolicRunContext, type: PythonType) {
        if (address is UConcreteHeapRef)
            return
        require(ctx.curState != null)
        myAssert(ctx, evalIsSoft(ctx, type))
    }

    fun evalIs(ctx: ConcolicRunContext, type: PythonType): UBoolExpr {
        require(ctx.curState != null)
        val result = evalIs(ctx.ctx, ctx.curState!!.pathConstraints.typeConstraints, type)
        if (resolvesToNullInCurrentModel(ctx) && ctx.curState!!.pyModel.eval(result).isTrue) {
            ctx.curState!!.possibleTypesForNull = ctx.curState!!.possibleTypesForNull.filterBySupertype(type)
        }
        return result
    }

    fun evalIs(
        ctx: UPythonContext,
        typeConstraints: UTypeConstraints<PythonType>,
        type: PythonType
    ): UBoolExpr {
        if (type is ConcretePythonType) {
            return with(ctx) {
                typeConstraints.evalIsSubtype(address, ConcreteTypeNegation(type)).not()
            }
        }
        return typeConstraints.evalIsSubtype(address, type)
    }

    fun evalIsSoft(ctx: ConcolicRunContext, type: PythonType): UBoolExpr {
        require(ctx.curState != null)
        return evalIsSoft(ctx.ctx, ctx.curState!!.pathConstraints.typeConstraints, type)
    }

    fun evalIsSoft(
        ctx: UPythonContext,
        typeConstraints: UTypeConstraints<PythonType>,
        type: PythonType
    ): UBoolExpr {
        var result: UBoolExpr = typeConstraints.evalIsSubtype(address, type)
        if (type is ConcretePythonType)
            result = with(ctx) { result and mkHeapRefEq(address, nullRef).not() }
        return result
    }

    fun getTypeIfDefined(ctx: ConcolicRunContext): PythonType? {
        val interpreted = interpretSymbolicPythonObject(ctx, this)
        return interpreted.getConcreteType()
    }

    private fun resolvesToNullInCurrentModel(ctx: ConcolicRunContext): Boolean {
        val interpreted = interpretSymbolicPythonObject(ctx, this)
        return interpreted.address.address == 0
    }

    fun getTimeOfCreation(ctx: ConcolicRunContext): UExpr<KIntSort> {  // must not be called on nullref
        require(ctx.curState != null)
        return ctx.curState!!.memory.readField(address, TimeOfCreation, ctx.ctx.intSort)
    }

    fun setMinimalTimeOfCreation(ctx: UPythonContext, memory: UMemory<PythonType, PythonCallable>) {  // must not be called on nullref
        memory.writeField(address, TimeOfCreation, ctx.intSort, ctx.mkIntNum(-1_000_000_000), ctx.trueExpr)
    }

    fun isAllocatedObject(ctx: ConcolicRunContext): Boolean {
        val evaluated = ctx.modelHolder.model.eval(address) as UConcreteHeapRef
        return evaluated.address > 0
    }
}

sealed class InterpretedSymbolicPythonObject(
    override val address: UConcreteHeapRef,
    typeSystem: PythonTypeSystem
): SymbolicPythonObject(address, typeSystem) {
    abstract fun getConcreteType(): ConcretePythonType?
    abstract fun getFirstType(): PythonType?
    abstract fun getTypeStream(): UTypeStream<PythonType>?
}

class InterpretedInputSymbolicPythonObject(
    address: UConcreteHeapRef,
    val modelHolder: PyModelHolder,
    typeSystem: PythonTypeSystem
): InterpretedSymbolicPythonObject(address, typeSystem) {
    init {
        require(!isStaticHeapRef(address) && !isAllocatedConcreteHeapRef(address))
    }
    override fun getFirstType(): PythonType? {
        if (address.address == 0)
            return MockType
        return modelHolder.model.getFirstType(address)
    }
    override fun getConcreteType(): ConcretePythonType? {
        if (address.address == 0)
            return null
        return modelHolder.model.getConcreteType(address)
    }

    override fun getTypeStream(): UTypeStream<PythonType>? {
        if (address.address == 0)
            return null
        return modelHolder.model.uModel.typeStreamOf(address)
    }
}

class InterpretedAllocatedOrStaticSymbolicPythonObject(
    override val address: UConcreteHeapRef,
    val type: ConcretePythonType,
    typeSystem: PythonTypeSystem
): InterpretedSymbolicPythonObject(address, typeSystem) {
    init {
        require(isAllocatedConcreteHeapRef(address) || isStaticHeapRef(address))
    }
    override fun getConcreteType(): ConcretePythonType = type

    override fun getFirstType(): PythonType = type

    override fun getTypeStream(): UTypeStream<PythonType> = USingleTypeStream(typeSystem, type)
}

fun interpretSymbolicPythonObject(
    ctx: ConcolicRunContext,
    obj: UninterpretedSymbolicPythonObject
): InterpretedSymbolicPythonObject {
    require(ctx.curState != null)
    val evaluated = ctx.modelHolder.model.eval(obj.address) as UConcreteHeapRef
    if (isAllocatedConcreteHeapRef(evaluated) || isStaticHeapRef(evaluated)) {
        val typeStream = ctx.curState!!.memory.typeStreamOf(evaluated)
        val type = typeStream.first()
        require(typeStream.take(2).size == 1 && type is ConcretePythonType) {
            "Static and allocated objects must have concrete types"
        }
        return InterpretedAllocatedOrStaticSymbolicPythonObject(evaluated, type, obj.typeSystem)
    }
    return InterpretedInputSymbolicPythonObject(evaluated, ctx.modelHolder, obj.typeSystem)
}