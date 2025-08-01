package com.example.basic_calculator_app

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import net.objecthunter.exp4j.ExpressionBuilder

class MainActivity : AppCompatActivity() {

    private var justEvaluated: Boolean = false
    private var errorOccurred: Boolean = false

    private lateinit var buttonsIds: List<Int>

    private lateinit var btnAdd: Button
    private lateinit var btnSubtract: Button
    private lateinit var btnMultiply: Button
    private lateinit var btnDivide: Button
    private lateinit var btnParenthesis: Button
    private lateinit var btnPercentage: Button

    private lateinit var btn_equal: Button
    private lateinit var btn_remove: Button
    private lateinit var btn_clearAll: Button

    private lateinit var inputEditText: EditText
    private lateinit var resultEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Set status bar icon brightness (light icons for dark background)
        window.statusBarColor = getColor(R.color.dark_purple)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false // false = white icons


        inputEditText = findViewById(R.id.inputEditText)
        resultEditText = findViewById(R.id.resultEditText)

        btnAdd = findViewById(R.id.btn_add)
        btnSubtract = findViewById(R.id.btn_subtract)
        btnMultiply = findViewById(R.id.btn_multiply)
        btnDivide = findViewById(R.id.btn_divide)
        btnParenthesis = findViewById(R.id.btn_parenthesis)
        btnPercentage = findViewById(R.id.btn_percentage)

        btn_equal = findViewById(R.id.btn_equals)
        btn_remove = findViewById(R.id.btn_remove)
        btn_clearAll = findViewById(R.id.btn_clearAll)

        inputEditText.showSoftInputOnFocus = false
        inputEditText.requestFocus()

        buttonsIds = listOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4, R.id.btn_5,
            R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_dot
        )

        buttonsIds.forEach { id ->

            val button: Button = findViewById(id)

            button.setOnClickListener {

                playTapFeedback()
                val text = button.text.toString()

                // If user presses dot, only allow it once per number
                if (text == ".") {
                    val cursorPos = inputEditText.selectionStart
                    val currentText = inputEditText.text.toString()

                    // Get the text before the cursor
                    val textBeforeCursor = currentText.substring(0, cursorPos)

                    // Use regex to find the current number before the cursor (no loop!)
                    val lastNumber = textBeforeCursor.split(Regex("[+\\-*/%()]")).last()

                    // If the number already contains a dot, block this dot
                    if (lastNumber.contains(".")) {
                        return@setOnClickListener
                    }
                }

                if (errorOccurred) {

                    inputEditText.setText("")
                    resultEditText.setText("")
                    resultEditText.alpha = 0f
                    errorOccurred = false
                    justEvaluated = false

                } else if (justEvaluated) {

                    if (text.first().isDigit() || text == ".") {
                        inputEditText.setText("")
                        resultEditText.setText("")
                        resultEditText.alpha = 0f
                    }

                    justEvaluated = false

                }

                val cursorPos = inputEditText.selectionStart.coerceAtLeast(0)
                inputEditText.text.insert(cursorPos, text)

                resultEditText.alpha = 1f
                evaluateExpression(false)
            }
        }

        btnAdd.setOnClickListener { appendOperator("+") }
        btnSubtract.setOnClickListener { appendOperator("-") }
        btnMultiply.setOnClickListener { appendOperator("*") }
        btnDivide.setOnClickListener { appendOperator("/") }
        btnPercentage.setOnClickListener { appendOperator("%") }

        btn_clearAll.setOnClickListener {
            playTapFeedback()
            inputEditText.text.clear()
            resultEditText.text.clear()
            justEvaluated = false
            errorOccurred = false
        }

        btn_remove.setOnClickListener {
            playTapFeedback()
            val cursorPos = inputEditText.selectionStart
            if (cursorPos > 0) {
                inputEditText.text.delete(cursorPos - 1, cursorPos)
                evaluateExpression(false)
            }
        }

        btnParenthesis.setOnClickListener {
            playTapFeedback()

            // Get the current input text
            val expr = inputEditText.text.toString()

            // Count how many opening and closing parentheses are currently in the input
            val openCount = expr.count { it == '(' }
            val closeCount = expr.count { it == ')' }

            // Decide which parenthesis to insert
            val paren: String
            if (openCount > closeCount) {
                // More '(' than ')' → insert closing one
                paren = ")"
            } else {
                // Otherwise, insert opening one
                paren = "("
            }

            // Insert the parenthesis at the current cursor position
            val cursorPos = inputEditText.selectionStart
            inputEditText.text.insert(cursorPos, paren)

            // Show the result field and reset state flags
            resultEditText.alpha = 1f
            justEvaluated = false
            errorOccurred = false

            // Evaluate the current expression without finalizing it
            evaluateExpression(false)
        }


        btn_equal.setOnClickListener {
            playTapFeedback()
            evaluateExpression(true)
            val result = resultEditText.text.toString()

            if (result != "Syntax Error" && result != "Divide not possible") {
                inputEditText.setText(result)
                inputEditText.setSelection(result.length)
                resultEditText.alpha = 0f
                justEvaluated = true
                errorOccurred = false
            } else {
                justEvaluated = false
                errorOccurred = true
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun appendOperator(op: String) {

        playTapFeedback()

        if (errorOccurred) {
            inputEditText.setText("")
            resultEditText.setText("")
            resultEditText.alpha = 0f
            errorOccurred = false
        }

        if (justEvaluated) {
            justEvaluated = false
        }

        val cursorPos = inputEditText.selectionStart
        val text = inputEditText.text.toString()

        // Below Code is for Restricting only One Operator at a time with Digits
        if (cursorPos > 0) {
            val lastChar = text[cursorPos - 1]
            if (lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/' || lastChar == '%') {
                // Replace the last operator with the new one
                inputEditText.text.replace(cursorPos - 1, cursorPos, op)
            } else {
                // Insert the operator normally
                inputEditText.text.insert(cursorPos, op)
            }
        } else {
            // Cursor is at the start, just insert
            inputEditText.text.insert(cursorPos, op)

        }       // Restricting Operator Code Ends right here

        resultEditText.alpha = 1f
        evaluateExpression(false)
    }

    private fun evaluateExpression(isFinal: Boolean) {
        val expression = inputEditText.text.toString()
            .replace("÷", "/")
            .replace("×", "*")
            .replace(Regex("""(\d+(\.\d+)?)%""")) { matchResult ->
                val number = matchResult.groupValues[1]
                "($number/100)"
            }

        if (expression.isEmpty()) {
            resultEditText.setText("")
            resultEditText.alpha = 0f
            return
        }

        try {
            val result = ExpressionBuilder(expression).build().evaluate()

//            if (result.isInfinite() || result.isNaN()) {
//                if (isFinal) {
//                    resultEditText.setText("Divide not possible")
//                    resultEditText.alpha = 1f
//                    errorOccurred = true
//                }
//                return
//            }

            val resultStr: String

            if (result == result.toLong().toDouble()) {
                // If result is like 5.0 (no decimal digits), show as "5"
                resultStr = result.toLong().toString()
            } else {
                // Otherwise, show the full decimal number like "3.14"
                resultStr = result.toString()
            }


            resultEditText.setText(resultStr)
            resultEditText.alpha = 1f
            errorOccurred = false

        } catch (e: ArithmeticException) {
            if (isFinal) {
                resultEditText.setText("Divide not possible")
                resultEditText.alpha = 1f
                errorOccurred = true
            }
        } catch (e: Exception) {
            if (isFinal) {
                resultEditText.setText("Syntax Error")
                resultEditText.alpha = 1f
                errorOccurred = true
            }
        }
    }

    fun playTapFeedback() {

        // Vibration
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
