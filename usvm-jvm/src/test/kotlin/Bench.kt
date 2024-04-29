import java.io.File
import org.jacodb.api.ext.findClass
import org.jacodb.impl.features.InMemoryHierarchy
import org.jacodb.impl.features.Usages
import org.jacodb.impl.features.classpaths.JcUnknownClass
import org.jacodb.impl.features.classpaths.UnknownClasses
import org.jacodb.impl.jacodb
import org.usvm.PathSelectionStrategy
import org.usvm.PathSelectorFairnessStrategy
import org.usvm.SolverType
import org.usvm.UMachineOptions
import org.usvm.machine.JcMachine
import org.usvm.machine.state.JcState
import org.usvm.statistics.collectors.StatesCollector
import org.usvm.types.ClassScorer
import org.usvm.types.TypeScorer
import org.usvm.types.scoreClassNode
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

//val pathToJar = "/home/pvl/.m2/repository/com/google/guava/guava/11.0.2/guava-11.0.2.jar"
val pathToJar = "classes"

suspend fun main() {
    val cpFiles = listOf(File(pathToJar))

    val db = jacodb {
        useProcessJavaRuntime()

        installFeatures(InMemoryHierarchy)
        installFeatures(Usages)
        installFeatures(ClassScorer(TypeScorer, ::scoreClassNode))

        loadByteCode(cpFiles)
    }

    db.awaitBackgroundJobs()

    val cp = db.classpath(cpFiles, listOf(UnknownClasses))

    val allFunctions = cp.registeredLocations.asSequence()
        .filter { it.jcLocation?.jarOrFolder in cpFiles }
        .flatMap { it.jcLocation?.classNames ?: emptySet() }
        .map { cp.findClass(it) }
        .filterNot { it is JcUnknownClass }
        .flatMap { it.declaredMethods }
        .filter { it.instList.size > 0 }
        .shuffled(Random(6))
        .take(150)
        .toList()

    val options = UMachineOptions(
        pathSelectionStrategies = listOf(PathSelectionStrategy.RANDOM_PATH),
        pathSelectorFairnessStrategy = PathSelectorFairnessStrategy.CONSTANT_TIME,
        solverType = SolverType.YICES,
        solverTimeout = 1.seconds,
        timeout = 1.minutes
    )

    JcMachine(cp, options).use { machine ->
        machine.analyze(allFunctions, object : StatesCollector<JcState> {
            override var count: Int = 0

            override fun addState(state: JcState) {
                count++
            }
        })
    }
}
