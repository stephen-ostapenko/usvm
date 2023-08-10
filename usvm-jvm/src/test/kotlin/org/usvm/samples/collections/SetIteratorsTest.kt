package org.usvm.samples.collections

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.usvm.samples.JavaMethodTestRunner
import org.usvm.test.util.checkers.between
import org.usvm.test.util.checkers.ge
import org.usvm.test.util.checkers.ignoreNumberOfAnalysisResults
import org.usvm.util.isException

class SetIteratorsTest : JavaMethodTestRunner() {
    @Test
    @Disabled("Some properties were not discovered at positions (from 0): [1]")
    fun testReturnIterator() {
        checkDiscoveredProperties(
            SetIterators::returnIterator,
            ignoreNumberOfAnalysisResults,
            { _, s, r -> s.isEmpty() && r != null && r.asSequence().toSet().isEmpty() },
            { _, s, r -> s.isNotEmpty() && r != null && r.asSequence().toSet() == s },
        )
    }

    @Test
    @Disabled("Expected number of executions in bounds 3..4, but 139 found")
    fun testIteratorHasNext() {
        checkDiscoveredProperties(
            SetIterators::iteratorHasNext,
            between(3..4),
            { _, set, _ -> set == null },
            { _, set, result -> set.isEmpty() && result == 0 },
            { _, set, result -> set.isNotEmpty() && result == set.size },
        )
    }

    @Test
    @Disabled("Expected number of executions in bounds 3..4, but 55 found")
    fun testIteratorNext() {
        checkDiscoveredPropertiesWithExceptions(
            SetIterators::iteratorNext,
            between(3..4),
            { _, set, result -> set == null && result.isException<NullPointerException>() },
            { _, set, result -> set != null && set.isEmpty() && result.isException<NoSuchElementException>() },
            // test should work as long as default class for set is LinkedHashSet
            { _, set, result -> set != null && set.isNotEmpty() && result.getOrNull() == set.first() },
        )
    }

    @Test
    @Disabled("Expected number of executions in bounds 3..4, but 277 found")
    fun testIteratorRemove() {
        checkDiscoveredPropertiesWithExceptions(
            SetIterators::iteratorRemove,
            between(3..4),
            { _, set, result -> set == null && result.isException<NullPointerException>() },
            { _, set, result -> set.isEmpty() && result.isException<NoSuchElementException>() },
            // test should work as long as default class for set is LinkedHashSet
            { _, set, result ->
                val firstElement = set.first()
                val resultSet = result.getOrNull()!!
                val resultDoesntContainFirstElement = resultSet.size == set.size - 1 && firstElement !in resultSet
                set.isNotEmpty() && set.containsAll(resultSet) && resultDoesntContainFirstElement
            },
        )
    }

    @Test
    @Disabled("Some properties were not discovered at positions (from 0): [3, 4]")
    fun testIteratorRemoveOnIndex() {
        checkDiscoveredPropertiesWithExceptions(
            SetIterators::iteratorRemoveOnIndex,
            ge(5),
            { _, _, i, result -> i == 0 && result.isSuccess && result.getOrNull() == null },
            { _, set, _, result -> set == null && result.isException<NullPointerException>() },
            { _, set, i, result -> set != null && i < 0 && result.isException<IllegalStateException>() },
            { _, set, i, result -> i > set.size && result.isException<NoSuchElementException>() },
            // test should work as long as default class for set is LinkedHashSet
            { _, set, i, result ->
                val ithElement = set.toList()[i - 1]
                val resultSet = result.getOrNull()!!
                val iInIndexRange = i in 0..set.size
                val resultDoesntContainIthElement = resultSet.size == set.size - 1 && ithElement !in resultSet
                iInIndexRange && set.containsAll(resultSet) && resultDoesntContainIthElement
            },
        )
    }

    @Test
    @Disabled("Some properties were not discovered at positions (from 0): [1]")
    fun testIterateForEach() {
        checkDiscoveredProperties(
            SetIterators::iterateForEach,
            ignoreNumberOfAnalysisResults,
            { _, set, _ -> set == null },
            { _, set, _ -> set != null && null in set },
            { _, set, result -> set != null && result == set.sum() },
        )
    }


    @Test
    @Disabled("Some properties were not discovered at positions (from 0): [1]")
    fun testIterateWithIterator() {
        checkDiscoveredProperties(
            SetIterators::iterateWithIterator,
            ignoreNumberOfAnalysisResults,
            { _, set, _ -> set == null },
            { _, set, _ -> set != null && null in set },
            { _, set, result -> set != null && result == set.sum() },
        )
    }
}