package org.usvm.machine

import io.ksmt.utils.asExpr
import org.usvm.UAddressSort
import org.usvm.UBv32Sort
import org.usvm.UComponents
import org.usvm.UConcreteHeapAddress
import org.usvm.UContext
import org.usvm.UExpr
import org.usvm.UPointer
import org.usvm.USort
import org.usvm.api.UnknownSortException
import org.usvm.machine.type.Type

internal typealias USizeSort = UBv32Sort

class GoContext(
    components: UComponents<GoType, USizeSort>,
) : UContext<USizeSort>(components) {
    private var argsCount: MutableMap<Long, Int> = mutableMapOf()

    fun getArgsCount(method: Long): Int = argsCount[method]!!

    fun setMethodInfo(method: Long, info: GoMethodInfo) {
        argsCount[method] = info.parametersCount
    }

    fun typeToSort(type: Type): USort = when (type) {
        Type.BOOL -> boolSort
        Type.INT8, Type.UINT8 -> bv8Sort
        Type.INT16, Type.UINT16 -> bv16Sort
        Type.INT32, Type.UINT32 -> bv32Sort
        Type.INT64, Type.UINT64 -> bv64Sort
        Type.FLOAT32 -> fp32Sort
        Type.FLOAT64 -> fp64Sort
        Type.ARRAY, Type.SLICE, Type.MAP, Type.STRUCT, Type.INTERFACE -> addressSort
        Type.POINTER -> pointerSort
        else -> throw UnknownSortException()
    }

    fun mkPointer(address: UConcreteHeapAddress): UExpr<USort> {
        return UPointer(this, address).asExpr(pointerSort)
    }
}
