package org.usvm.fuzzer.generator.primitives

import org.jacodb.api.ext.boolean
import org.jacodb.api.ext.char
import org.usvm.fuzzer.generator.Generator
import org.usvm.fuzzer.generator.GeneratorContext
import org.usvm.fuzzer.generator.GeneratorSettings
import org.usvm.fuzzer.generator.random.nextInt
import org.usvm.fuzzer.util.UTestValueRepresentation
import org.usvm.instrumentation.testcase.api.UTestCharExpression

class CharGenerator(): Generator() {
    override val generationFun: GeneratorContext.(Int) -> UTestValueRepresentation = {
        val randomChar = random.nextInt().toChar()
        UTestValueRepresentation(UTestCharExpression(randomChar, jcClasspath.char))
    }
}