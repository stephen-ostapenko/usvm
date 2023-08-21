package org.usvm.samples.enums

import org.junit.jupiter.api.Test
import org.usvm.PathSelectionStrategy
import org.usvm.SolverType
import org.usvm.UMachineOptions
import org.usvm.samples.JavaMethodTestRunner
import org.usvm.samples.enums.ComplexEnumExamples.Color
import org.usvm.samples.enums.ComplexEnumExamples.Color.BLUE
import org.usvm.samples.enums.ComplexEnumExamples.Color.GREEN
import org.usvm.samples.enums.ComplexEnumExamples.Color.RED
import org.usvm.test.util.checkers.eq
import org.usvm.test.util.checkers.ignoreNumberOfAnalysisResults
import org.usvm.util.Options
import org.usvm.util.UsvmTest
import org.usvm.util.disableTest


class ComplexEnumExamplesTest : JavaMethodTestRunner() {
    @Test
    fun testEnumToEnumMapCountValues() = disableTest("Some properties were not discovered at positions (from 0): [1, 2]") {
        checkDiscoveredProperties(
            ComplexEnumExamples::enumToEnumMapCountValues,
            ignoreNumberOfAnalysisResults,
            { _, m, r -> m.isEmpty() && r == 0 },
            { _, m, r -> m.isNotEmpty() && !m.values.contains(RED) && r == 0 },
            { _, m, r -> m.isNotEmpty() && m.values.contains(RED) && m.values.count { it == RED } == r }
        )
    }

    @Test
    fun testEnumToEnumMapCountKeys() = disableTest("java.lang.OutOfMemoryError: Java heap space") {
        checkDiscoveredProperties(
            ComplexEnumExamples::enumToEnumMapCountKeys,
            ignoreNumberOfAnalysisResults,
            { _, m, r -> m.isEmpty() && r == 0 },
            { _, m, r -> m.isNotEmpty() && !m.keys.contains(GREEN) && !m.keys.contains(BLUE) && r == 0 },
            { _, m, r ->
                m.isNotEmpty() && m.keys.intersect(setOf(BLUE, GREEN))
                    .isNotEmpty() && m.keys.count { it == BLUE || it == GREEN } == r
            }
        )
    }

    @UsvmTest([Options(solverType = SolverType.Z3, strategies = [PathSelectionStrategy.FORK_DEPTH])])
    fun testEnumToEnumMapCountMatches(options: UMachineOptions) = withOptions(options) {
        checkDiscoveredProperties(
            ComplexEnumExamples::enumToEnumMapCountMatches,
            ignoreNumberOfAnalysisResults,
            { _, m, r -> m.isEmpty() && r == 0 },
            { _, m, r -> m.entries.count { it.key == it.value } == r }
        )
    }

    @Test
    fun testCountEqualColors() = disableTest("Some properties were not discovered at positions (from 0): [2]") {
        checkDiscoveredProperties(
            ComplexEnumExamples::countEqualColors,
            ignoreNumberOfAnalysisResults,
            { _, a, b, c, r -> a == b && a == c && r == 3 },
            { _, a, b, c, r -> setOf(a, b, c).size == 2 && r == 2 },
            { _, a, b, c, r -> a != b && b != c && a != c && r == 1 }
        )
    }

    @Test
    fun testCountNullColors() = disableTest("Some properties were not discovered at positions (from 0): [0]") {
        checkDiscoveredProperties(
            ComplexEnumExamples::countNullColors,
            eq(3),
            { _, a, b, r -> a == null && b == null && r == 2 },
            { _, a, b, r -> (a == null) != (b == null) && r == 1 },
            { _, a, b, r -> a != null && b != null && r == 0 },
        )
    }

    @Test
    fun testFindState() {
        checkDiscoveredProperties(
            ComplexEnumExamples::findState,
            ignoreNumberOfAnalysisResults,
            { _, c, r -> c in setOf(0, 127, 255) && r != null && r.code == c }
        )
    }

    @Test
    fun testCountValuesInArray() = disableTest("slow on CI") {
        fun Color.isCorrectlyCounted(inputs: Array<Color>, counts: Map<Color, Int>): Boolean =
            inputs.count { it == this } == (counts[this] ?: 0)

        checkDiscoveredProperties(
            ComplexEnumExamples::countValuesInArray,
            ignoreNumberOfAnalysisResults,
            { _, cs, r -> cs.isEmpty() && r != null && r.isEmpty() },
            { _, cs, r -> cs.toList().isEmpty() && r != null && r.isEmpty() },
            { _, cs, r -> cs.toList().isNotEmpty() && r != null && Color.values().all { it.isCorrectlyCounted(cs, r) } }
        )
    }

    @Test
    fun testCountRedInArray() {
        checkDiscoveredProperties(
            ComplexEnumExamples::countRedInArray,
            eq(3),
            { _, colors, result -> colors.count { it == RED } == result }
        )
    }
}
