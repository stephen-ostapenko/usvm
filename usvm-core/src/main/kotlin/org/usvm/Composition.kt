package org.usvm

import org.usvm.constraints.UTypeEvaluator
import org.usvm.memory.UReadOnlyHeap
import org.usvm.memory.USymbolicCollectionId
import org.usvm.memory.URegistersStackEvaluator
import org.usvm.memory.USymbolicCollection
import org.usvm.util.Region

@Suppress("MemberVisibilityCanBePrivate")
open class UComposer<Field, Type>(
    ctx: UContext,
    internal val stackEvaluator: URegistersStackEvaluator,
    internal val heapEvaluator: UReadOnlyHeap<Field, Type>,
    internal val typeEvaluator: UTypeEvaluator<Type>,
    internal val mockEvaluator: UMockEvaluator,
) : UExprTransformer<Field, Type>(ctx) {
    open fun <Sort : USort> compose(expr: UExpr<Sort>): UExpr<Sort> = apply(expr)

    override fun <Sort : USort> transform(expr: USymbol<Sort>): UExpr<Sort> =
        error("You must override `transform` function in org.usvm.UComposer for ${expr::class}")

    override fun <Sort : USort> transform(
        expr: URegisterReading<Sort>,
    ): UExpr<Sort> = with(expr) { stackEvaluator.eval(idx, sort) }

    override fun <Sort : USort> transform(expr: UCollectionReading<*, *, *>): UExpr<Sort> =
        error("You must override `transform` function in org.usvm.UComposer for ${expr::class}")

    override fun <Sort : USort> transform(expr: UMockSymbol<Sort>): UExpr<Sort> =
        error("You must override `transform` function in org.usvm.UComposer for ${expr::class}")

    override fun <Method, Sort : USort> transform(
        expr: UIndexedMethodReturnValue<Method, Sort>,
    ): UExpr<Sort> = mockEvaluator.eval(expr)
    
    override fun transform(expr: UIsExpr<Type>): UBoolExpr = with(expr) {
        val composedAddress = compose(ref)
        typeEvaluator.evalIs(composedAddress, type)
    }

    fun <CollectionId : USymbolicCollectionId<Key, Sort, CollectionId>, Key, Sort : USort> transformCollectionReading(
        expr: UCollectionReading<CollectionId, Key, Sort>,
        key: Key,
    ): UExpr<Sort> = with(expr) {
        val mappedCollectionId = collection.collectionId.map(this@UComposer)
        val mappedKey = mappedCollectionId.keyMapper(this@UComposer)(key)
        val decomposedKey = mappedCollectionId.rebindKey(mappedKey)
        if (decomposedKey != null) {
            @Suppress("UNCHECKED_CAST")
            // I'm terribly sorry to do this cast, but it's impossible to do it type safe way :(
            val mappedCollection = collection.mapTo(this@UComposer, decomposedKey.collectionId) as USymbolicCollection<*, Any?, Sort>
            return mappedCollection.read(decomposedKey.key)
        }
        return collection.mapTo(this@UComposer, mappedCollectionId).read(key)
    }

    override fun transform(expr: UInputArrayLengthReading<Type>): USizeExpr =
        transformCollectionReading(expr, expr.address)

    override fun <Sort : USort> transform(expr: UInputArrayReading<Type, Sort>): UExpr<Sort> =
        transformCollectionReading(expr, expr.address to expr.index)

    override fun <Sort : USort> transform(expr: UAllocatedArrayReading<Type, Sort>): UExpr<Sort> =
        transformCollectionReading(expr, expr.index)

    override fun <Sort : USort> transform(expr: UInputFieldReading<Field, Sort>): UExpr<Sort> =
        transformCollectionReading(expr, expr.address)

    override fun <KeySort : USort, Reg : Region<Reg>, Sort : USort> transform(
        expr: UAllocatedSymbolicMapReading<KeySort, Reg, Sort>
    ): UExpr<Sort> = transformCollectionReading(expr, expr.key)

    override fun <KeySort : USort, Reg : Region<Reg>, Sort : USort> transform(
        expr: UInputSymbolicMapReading<KeySort, Reg, Sort>
    ): UExpr<Sort> = transformCollectionReading(expr, expr.address to expr.key)

    override fun transform(expr: UInputSymbolicMapLengthReading): USizeExpr =
        transformCollectionReading(expr, expr.address)

    override fun transform(expr: UConcreteHeapRef): UExpr<UAddressSort> = expr

    override fun transform(expr: UNullRef): UExpr<UAddressSort> = heapEvaluator.nullRef()
}
