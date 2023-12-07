package org.usvm.fuzzer.generator.reflection

import org.jacodb.api.ext.boolean
import org.jacodb.api.ext.objectType
import org.usvm.fuzzer.generator.Generator
import org.usvm.fuzzer.generator.GeneratorContext
import org.usvm.fuzzer.util.UTestValueRepresentation
import org.usvm.instrumentation.testcase.api.UTestBooleanExpression
import org.usvm.instrumentation.testcase.api.UTestClassExpression

class ClassGenerator: Generator() {
    override val generationFun: GeneratorContext.() -> UTestValueRepresentation = {
        UTestValueRepresentation(UTestClassExpression(jcClasspath.objectType))
    }
}