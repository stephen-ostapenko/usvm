package org.usvm.fuzzer.position

import org.jacodb.api.*
import org.jacodb.impl.features.classpaths.virtual.JcVirtualFieldImpl
import org.objectweb.asm.Opcodes
import org.usvm.fuzzer.strategy.Selectable
import org.usvm.instrumentation.util.getTypename

class SeedFieldsInfo() {

    private val info = HashSet<FieldInfo>()
    private val addedClasses = HashSet<JcClassOrInterface>()

    fun hasClassBeenParsed(jcClass: JcClassOrInterface) = addedClasses.contains(jcClass)

    fun addFieldInfo(
        targetMethod: JcMethod,
        jcField: JcField,
        score: Double,
        numberOfChooses: Int
    ) {
        addedClasses.add(jcField.enclosingClass)
        info.add(FieldInfo(targetMethod, jcField, score, numberOfChooses))
    }

    fun addArgInfo(
        targetMethod: JcMethod,
        argPosition: Int,
        argType: JcType,
        score: Double,
        numberOfChooses: Int
    ) {
        if (argType is JcClassType) {
            addedClasses.add(argType.jcClass)
        }
        info.add(
            FieldInfo(
                targetMethod,
                JcVirtualFieldImpl("arg$argPosition", Opcodes.ACC_PUBLIC, argType.getTypename()),
                score,
                numberOfChooses
            )
        )
    }

    fun getFieldInfo(jcField: JcField) = info.find { it.jcField == jcField }

    fun getBestField() = info.random()

    fun getBestField(condition: (JcField) -> Boolean): JcField? {
        val filteredFields = info.filter { condition(it.jcField) }
        return filteredFields.randomOrNull()?.jcField
    }

    fun getWorstField() = info.random()

    open class FieldInfo(
        val jcTargetMethod: JcMethod,
        val jcField: JcField,
        override var averageScore: Double,
        override var numberOfChooses: Int
    ): Selectable() {

        override fun toString(): String = "Field: ${jcField.name} || Score: $averageScore || NumberOfChoices: $numberOfChooses"


        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FieldInfo

            if (jcField.name != other.jcField.name) return false
            if (jcField.enclosingClass.name != other.jcField.enclosingClass.name) return false
            if (averageScore != other.averageScore) return false
            if (numberOfChooses != other.numberOfChooses) return false

            return true
        }

        override fun hashCode(): Int {
            var result = jcTargetMethod.hashCode()
            result = 31 * result + jcField.hashCode()
            result = 31 * result + averageScore.hashCode()
            result = 31 * result + numberOfChooses
            return result
        }


    }

}