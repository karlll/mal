
STEPS=step0_repl

%.jar: 
	kotlinc src/main/kotlin/com/ninjacontrol/mal/${@:%.jar=%.kt} -include-runtime -d build/${@}

$(STEPS): %: %.jar

clean: 
	rm build/*.jar
