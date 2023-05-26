import org.usvm.interpreter.ConcretePythonInterpreter
import org.usvm.interpreter.PythonMachine
import org.usvm.language.Callable
import org.usvm.language.PythonProgram

fun main() {
    val globals = ConcretePythonInterpreter.getNewNamespace()
    ConcretePythonInterpreter.concreteRun(globals, "x = 10 ** 100")
    ConcretePythonInterpreter.concreteRun(globals, "print('Hello from Python!\\nx is', x, flush=True)")

    val program = PythonProgram(
        """
            def f(x):
                return x*2 if x > 0 else -x*2
        """.trimIndent()
    )
    val function = Callable.constructCallableFromName(1, "f")
    val machine = PythonMachine(program)
    machine.analyze(function)
    machine.close()
}