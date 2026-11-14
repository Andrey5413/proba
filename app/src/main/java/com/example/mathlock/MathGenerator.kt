package com.example.mathlock

import kotlin.math.max
import kotlin.random.Random

/** Арифметическая операция. */
enum class Operation(val symbol: String) {
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/")
}

/** Уровень сложности. */
enum class Difficulty(val maxTerms: Int, val maxNumber: Int, val allowParens: Boolean) {
    EASY(1, 10, false),
    MEDIUM(2, 20, true),
    HARD(3, 50, true)
}

/** Сгенерированный пример: текст для показа и правильный ответ. */
data class MathProblem(val text: String, val answer: Int)

/**
 * Генератор математических примеров.
 * Гарантии:
 *  - используются ТОЛЬКО операции из переданного набора;
 *  - результат ВСЕГДА целое число (проверка на каждом шаге вычисления);
 *  - деление только нацело и никогда не на ноль;
 *  - вычитание без отрицательного результата (в любой момент вычисления);
 *  - приоритет операций как в математике; скобки — на «Средне» и «Сложно».
 * Алгоритм: случайная генерация с проверкой («отбраковка»), до 1000 попыток,
 * затем гарантированно корректный запасной вариант из одного действия.
 */
object MathGenerator {

    private const val MAX_ATTEMPTS = 1000

    fun generate(operations: Set<Operation>, difficulty: Difficulty): MathProblem {
        val ops = operations.ifEmpty { setOf(Operation.ADD) }
        repeat(MAX_ATTEMPTS) {
            build(ops, difficulty)?.let { return it }
        }
        // Гарантированный запасной вариант: одно действие, всегда корректное.
        val op = if (Operation.ADD in ops) Operation.ADD else ops.first()
        return simpleProblem(op, difficulty.maxNumber)
    }

    /** Одна попытка построить корректный пример; null — если попытка неудачна. */
    private fun build(ops: Set<Operation>, d: Difficulty): MathProblem? {
        val count = if (d.maxTerms == 1) 1 else Random.nextInt(2, d.maxTerms + 1)
        val numbers = MutableList(count + 1) { Random.nextInt(1, d.maxNumber + 1) }
        val operators = MutableList(count) { ops.random() }

        var parensIndex = -1
        if (d.allowParens && count >= 2 && Random.nextFloat() < 0.4f) {
            parensIndex = Random.nextInt(0, count)
        }

        // --- Шаг 1. Вычисление с учётом приоритета (левая ассоциативность). ---
        val values = numbers.toMutableList()
        val opsLeft = operators.toMutableList()

        // Сначала выполняем умножение и деление.
        var i = 0
        while (i < opsLeft.size) {
            if (opsLeft[i] == Operation.MUL || opsLeft[i] == Operation.DIV) {
                val a = values[i]
                val b = values[i + 1]
                val v = if (opsLeft[i] == Operation.MUL) a * b else {
                    if (b == 0 || a % b != 0) return null
                    a / b
                }
                values[i] = v
                values.removeAt(i + 1)
                opsLeft.removeAt(i)
                if (parensIndex >= 0) {
                    if (parensIndex == i || parensIndex == i + 1) return null
                    if (parensIndex > i) parensIndex--
                }
            } else {
                i++
            }
        }

        // Затем сложение и вычитание слева направо.
        var j = 0
        while (j < opsLeft.size) {
            val a = values[j]
            val b = values[j + 1]
            val v = when (opsLeft[j]) {
                Operation.ADD -> a + b
                Operation.SUB -> if (a < b) return null else a - b
                else -> return null
            }
            values[j] = v
            values.removeAt(j + 1)
            opsLeft.removeAt(j)
            if (parensIndex >= 0) {
                if (parensIndex == j || parensIndex == j + 1) return null
                if (parensIndex > j) parensIndex--
            }
        }

        val answer = values[0]

        // --- Шаг 2. Формирование текста примера (со скобками, если они нужны). ---
        val text = StringBuilder()
        for (k in 0..numbers.lastIndex) {
            if (k == parensIndex) text.append("(")
            text.append(numbers[k])
            if (k == parensIndex + 1) text.append(")")
            if (k <= operators.lastIndex) {
                text.append(" ").append(operators[k].symbol).append(" ")
            }
        }

        return MathProblem(text.toString(), answer)
    }

    /** Простой корректный пример из одного действия (запасной вариант). */
    private fun simpleProblem(op: Operation, maxNumber: Int): MathProblem {
        return when (op) {
            Operation.ADD -> {
                val a = Random.nextInt(1, maxNumber + 1)
                val b = Random.nextInt(1, maxNumber + 1)
                MathProblem("$a + $b", a + b)
            }
            Operation.SUB -> {
                var a = Random.nextInt(1, maxNumber + 1)
                var b = Random.nextInt(1, maxNumber + 1)
                if (b > a) { val t = a; a = b; b = t }
                MathProblem("$a - $b", a - b)
            }
            Operation.MUL -> {
                val lim = max(2, minOf(maxNumber, 9))
                val a = Random.nextInt(2, lim + 1)
                val b = Random.nextInt(2, lim + 1)
                MathProblem("$a * $b", a * b)
            }
            Operation.DIV -> {
                val b = Random.nextInt(1, 10)
                val q = Random.nextInt(1, 10)
                MathProblem("${b * q} / $b", q)
            }
        }
    }
}
