package main.kotlin.com.ninjacontrol.mal

sealed class MalException(override val message: String) : Throwable(message)

class ParseException(override val message: String) : MalException(message)
class InvalidArgumentException(override val message: String) : MalException(message)
class NotFoundException(override val message: String) : MalException(message)
class EvaluationException(override val message: String) : MalException(message)
class UserException(override val message: String) : MalException(message)
class IOException(override val message: String) : MalException(message)
class OutOfBoundsException(override val message: String) : MalException(message)
class ArithmeticException(override val message: String) : MalException(message)
