SUPPORT=Prompt.kt ReadLine.kt Reader.kt Printer.kt Types.kt StringParser.kt
MAKEFILE_PATH=$(dir $(abspath $(lastword $(MAKEFILE_LIST))))
SOURCE_PATH=${MAKEFILE_PATH}src/main/kotlin/com/ninjacontrol/mal
SOURCES=${SUPPORT:%.kt=${SOURCE_PATH}/%.kt}
BUILD_PATH=${MAKEFILE_PATH}build
STEPS=step1_read_print

%.jar: 
	kotlinc ${SOURCE_PATH}/${@:%.jar=%.kt} ${SOURCES} -include-runtime -d ${BUILD_PATH}/${@}

$(STEPS): %: %.jar

clean: 
	rm build/*.jar
