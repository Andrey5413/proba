package com.example.mathlock

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Единая точка доступа к настройкам приложения (SharedPreferences).
 */
object Prefs {

    const val OP_ADD = "add"
    const val OP_SUB = "sub"
    const val OP_MUL = "mul"
    const val OP_DIV = "div"

    const val DIFF_EASY = "easy"
    const val DIFF_MEDIUM = "medium"
    const val DIFF_HARD = "hard"

    fun get(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    /** Выбранные пользователем арифметические операции. */
    fun selectedOperations(context: Context): Set<Operation> {
        val prefs = get(context)
        val result = mutableSetOf<Operation>()
        if (prefs.getBoolean(OP_ADD, true)) result.add(Operation.ADD)
        if (prefs.getBoolean(OP_SUB, true)) result.add(Operation.SUB)
        if (prefs.getBoolean(OP_MUL, false)) result.add(Operation.MUL)
        if (prefs.getBoolean(OP_DIV, false)) result.add(Operation.DIV)
        // Если пользователь снял все галочки — возвращаем сложение,
        // чтобы генератор всегда мог создать пример.
        if (result.isEmpty()) result.add(Operation.ADD)
        return result
    }

    /** Выбранная сложность (по умолчанию «Легко»). */
    fun difficulty(context: Context): Difficulty {
        val value = get(context).getString("difficulty", DIFF_EASY) ?: DIFF_EASY
        return when (value) {
            DIFF_MEDIUM -> Difficulty.MEDIUM
            DIFF_HARD -> Difficulty.HARD
            else -> Difficulty.EASY
        }
    }

    /** Блокировать ли экран при включении экрана. */
    fun lockOnScreenOn(context: Context): Boolean =
        get(context).getBoolean("lock_on_screen_on", true)
}
