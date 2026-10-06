package com.fredsystems.sportsviewer

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode

class CalculatorActivity : Activity() {
    private lateinit var display: TextView
    private var value = "0"
    private var stored: BigDecimal? = null
    private var operation: String? = null
    private var reset = false
    private var expression = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Calculator"
        buildCalculator()
    }

    private fun buildCalculator() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 22, 18, 18)
            setBackgroundColor(Color.rgb(245, 245, 245))
        }

        display = TextView(this).apply {
            text = "0"
            textSize = 42f
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setTextColor(Color.BLACK)
            setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
            setPadding(14, 8, 14, 8)
            setBackgroundColor(Color.WHITE)
            minHeight = 125
        }
        root.addView(display, LinearLayout.LayoutParams(-1, 0, 1f))

        val grid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 5
            setPadding(0, 12, 0, 0)
        }

        val keys = arrayOf(
            "C", "⌫", "%", "÷",
            "7", "8", "9", "×",
            "4", "5", "6", "−",
            "1", "2", "3", "+",
            "0", ".", "="
        )

        for (key in keys) {
            val button = Button(this).apply {
                text = key
                textSize = 21f
                isAllCaps = false
                setOnClickListener { press(key) }
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }
            if (key == "0") {
                lp.columnSpec = GridLayout.spec(0, 2, 1f)
            }
            grid.addView(button, lp)
        }

        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 4f))
        setContentView(root)
    }

    private fun press(key: String) {
        when (key) {
            "C" -> clear()
            "⌫" -> backspace()
            "%" -> percent()
            "." -> decimal()
            "+", "−", "×", "÷" -> setOperation(key)
            "=" -> equals()
            else -> number(key)
        }
        display.text = value
    }

    private fun number(n: String) {
        if (reset || value == "0") {
            value = n
            reset = false
        } else if (value.length < 18) {
            value += n
        }
    }

    private fun decimal() {
        if (reset) {
            value = "0."
            reset = false
        } else if (!value.contains(".")) {
            value += "."
        }
    }

    private fun clear() {
        value = "0"
        stored = null
        operation = null
        reset = false
        expression = ""
    }

    private fun backspace() {
        if (reset) return
        value = if (value.length > 1) value.dropLast(1) else "0"
    }

    private fun percent() {
        runCatching {
            value = BigDecimal(value).divide(BigDecimal("100")).stripTrailingZeros().toPlainString()
        }.onFailure { value = "Error"; reset = true }
    }

    private fun setOperation(op: String) {
        val number = parse() ?: return
        if (stored != null && operation != null && !reset) calculate()
        stored = parse()
        operation = op
        expression = value + " " + op
        reset = true
    }

    private fun calculate() {
        val a = stored ?: return
        val b = parse() ?: return
        val result = when (operation) {
            "+" -> a.add(b)
            "−" -> a.subtract(b)
            "×" -> a.multiply(b)
            "÷" -> if (b.compareTo(BigDecimal.ZERO) == 0) null else a.divide(b, 12, RoundingMode.HALF_UP)
            else -> null
        }
        if (result == null) {
            value = "Error"
            stored = null
            operation = null
            reset = true
        } else {
            value = result.stripTrailingZeros().toPlainString()
            stored = null
            operation = null
            reset = true
        }
    }

    private fun equals() {
        if (operation != null && stored != null) {
            calculate()
            return
        }

        val code = value
        val prefs = getSharedPreferences("sportsview", MODE_PRIVATE)
        val saved = prefs.getString("calculator_code", null)
        if (saved != null && code.matches(Regex("\\d{4,12}")) && code == saved) {
            prefs.edit().putBoolean("calculator_authenticated", true).apply()
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        } else if (saved != null && code.matches(Regex("\\d{4,12}"))) {
            value = "0"
        }
    }

    private fun parse(): BigDecimal? = runCatching { BigDecimal(value) }.getOrNull()
}
