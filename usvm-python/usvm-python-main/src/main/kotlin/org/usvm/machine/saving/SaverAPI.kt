package org.usvm.machine.saving

fun createStandardSaver(): PythonRepresentationSaver<PythonObjectInfo> =
    PythonRepresentationSaver(StandardPythonObjectSerializer)

fun createReprSaver(): PythonRepresentationSaver<String> =
    PythonRepresentationSaver(ReprObjectSerializer)

fun createDictSaver(): PythonRepresentationSaver<String> =
    PythonRepresentationSaver(ObjectWithDictSerializer)

fun createPickleSaver(): PythonRepresentationSaver<String?> =
    PythonRepresentationSaver(PickleObjectSerializer)