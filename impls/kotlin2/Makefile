SUPPORT=Prompt.kt ReadLine.kt
SOURCE_PATH=src/main/kotlin/com/ninjacontrol/mal
SOURCES=${SUPPORT:%.kt=${SOURCE_PATH}/%.kt}
BUILD_PATH=build
STEPS=step0_repl

%.jar: 
	kotlinc ${SOURCE_PATH}/${@:%.jar=%.kt} ${SOURCES} -include-runtime -d ${BUILD_PATH}/${@}

$(STEPS): %: %.jar

clean: 
	rm build/*.jar
