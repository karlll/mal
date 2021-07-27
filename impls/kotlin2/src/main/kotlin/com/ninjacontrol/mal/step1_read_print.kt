package main.kotlin.com.ninjacontrol.mal

import kotlin.system.exitProcess

/**
 * Change the `READ` function in `step1_read_print.qx` to call
 * `reader.read_str` and the `PRINT` function to call `printer.pr_str`.
 * `EVAL` continues to simply return its input but the type is now
 * a mal data type.*/

fun read(input: String) = readStr(input)
fun eval(input: MalType) = input
fun print(input: MalType) = printString(input)
fun rep(input: String) = print(eval(read(input)))

tailrec fun mainLoop() {
    print(prompt())
    readLine()?.let { input ->
        val output = rep(input)
        println(output)
    } ?: run {
        println("** Exiting.")
        exitProcess(0)
    }
    mainLoop()
}

fun main() = mainLoop()
