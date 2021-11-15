SUPPORT=Prompt.kt ReadLine.kt Reader.kt Printer.kt Types.kt Environment.kt Namespace.kt Compare.kt String.kt Exception.kt
TEST=Assertions.kt EnvironmentTest.kt FunctionsTest.kt NamespaceTest.kt TestSuite.kt StringTest.kt EvaluationTest.kt QuoteTest.kt MacroTest.kt
MAKEFILE_PATH=$(dir $(abspath $(lastword $(MAKEFILE_LIST))))
SOURCE_PATH=${MAKEFILE_PATH}src/main/kotlin/com/ninjacontrol/mal
SOURCES=${SUPPORT:%.kt=${SOURCE_PATH}/%.kt}
TEST_SOURCES=${TEST:%.kt=${SOURCE_PATH}/test/%.kt}
BUILD_PATH=${MAKEFILE_PATH}build
STEPS=step9_try

%.jar: 
	kotlinc ${SOURCE_PATH}/${@:%.jar=%.kt} ${SOURCES} ${TEST_SOURCES} -include-runtime -d ${BUILD_PATH}/${@}

$(STEPS): %: %.jar

clean: 
	rm build/*.jar
