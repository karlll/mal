package main.kotlin.com.ninjacontrol.mal

import kotlin.system.exitProcess

/*
    Add a main loop that repeatedly prints a prompt (needs to be "user> "
    for later tests to pass), gets a line of input from the user, calls rep
    with that line of input, and then prints out the result from rep.
    It should also exit when you send it an EOF (often Ctrl-D).
 */

fun rep(input: String) = input

tailrec fun mainLoop() {
    print(prompt())
    readLine()?.let {
        println(rep(it))
    } ?: run {
        println("** Exiting.")
        exitProcess(0)
    }
    mainLoop()
}

fun main() = mainLoop()
