package com.fredsystems.sportsviewer

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.*

class CalculatorActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var display: TextView
    private lateinit var expression: TextView
    private lateinit var advancedButton: Button
    private lateinit var themeButton: Button

    private var value = "0"
    private var stored: Double? = null
    private var operation: String? = null
    private var resetInput = true
    private var light = true
    private var memory = 0.0

    private data class State(val value:String,val stored:Double?,val operation:String?,val resetInput:Boolean,val memory:Double)
    private val history = ArrayDeque<State>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        light = getSharedPreferences("sportsview", MODE_PRIVATE).getBoolean("calculator_light", true)
        build()
    }

    private fun saveState() {
        history.addLast(State(value, stored, operation, resetInput, memory))
        if (history.size > 80) history.removeFirst()
    }

    private fun undo() {
        if (history.isEmpty()) return
        val s = history.removeLast()
        value=s.value; stored=s.stored; operation=s.operation; resetInput=s.resetInput; memory=s.memory
        render()
    }

    private fun build() {
        root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(14,14,14,12)
        }

        val top=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            gravity=Gravity.CENTER_VERTICAL
        }

        advancedButton=Button(this).apply {
            text="Advanced"
            textSize=11f
            isAllCaps=false
            setOnClickListener { showAdvanced() }
        }
        themeButton=Button(this).apply {
            text="☀"
            textSize=16f
            isAllCaps=false
            setOnClickListener {
                light=!light
                getSharedPreferences("sportsview", MODE_PRIVATE).edit().putBoolean("calculator_light",light).apply()
                applyTheme()
            }
        }
        top.addView(advancedButton,LinearLayout.LayoutParams(0,42,1f))
        top.addView(themeButton,LinearLayout.LayoutParams(54,42))
        root.addView(top)

        expression=TextView(this).apply {
            text="Ready"
            textSize=15f
            gravity=Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(10,12,10,0)
        }
        root.addView(expression,LinearLayout.LayoutParams(-1,40))

        display=TextView(this).apply {
            text="0"
            textSize=42f
            typeface=Typeface.DEFAULT_BOLD
            gravity=Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(10,0,10,8)
            minHeight=80
        }
        root.addView(display,LinearLayout.LayoutParams(-1,86))

        val grid=GridLayout(this).apply { columnCount=4; setPadding(0,4,0,0) }
        root.addView(grid,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        buildStandardGrid(grid)
        applyTheme()
    }

    private fun buildStandardGrid(grid:GridLayout) {
        grid.removeAllViews()
        val keys=listOf("C","⌫","%","÷","7","8","9","×","4","5","6","−","1","2","3","+","0",".","=")
        for(key in keys) {
            val b=Button(this).apply {
                text=key
                textSize=if(key=="⌫")18f else 20f
                isAllCaps=false
                setOnClickListener { press(key) }
            }
            val lp=GridLayout.LayoutParams().apply {
                width=0; height=0
                columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)
                rowSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)
                setMargins(4,4,4,4)
            }
            if(key=="0") lp.columnSpec=GridLayout.spec(0,2,1f)
            grid.addView(b,lp)
        }
    }

    private fun showAdvanced() {
        val dialog=AlertDialog.Builder(this).create()
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(14,10,14,14)
        }
        val title=TextView(this).apply {
            text="Advanced functions"
            textSize=18f
            typeface=Typeface.DEFAULT_BOLD
            setPadding(4,4,4,12)
        }
        box.addView(title)
        val grid=GridLayout(this).apply { columnCount=4 }
        val keys=listOf("sin","cos","tan","√","asin","acos","atan","ln","log","x²","xʸ","x!","1/x","abs","floor","ceil","π","e","M+","M−","MR","MC","DEG","RAD")
        for(key in keys) {
            val b=Button(this).apply {
                text=key
                textSize=if(key.length>3)12f else 15f
                isAllCaps=false
                setOnClickListener {
                    saveState()
                    when(key) {
                        "DEG","RAD" -> { expression.text=key; dialog.dismiss() }
                        "M+" -> { memory+=parse(); expression.text="Memory +"; dialog.dismiss() }
                        "M−" -> { memory-=parse(); expression.text="Memory −"; dialog.dismiss() }
                        "MR" -> { value=format(memory); resetInput=true; expression.text="Memory recall"; dialog.dismiss() }
                        "MC" -> { memory=0.0; expression.text="Memory cleared"; dialog.dismiss() }
                        "xʸ" -> { setOperation("^"); dialog.dismiss() }
                        else -> { applyFunction(key); dialog.dismiss() }
                    }
                    render()
                }
            }
            val lp=GridLayout.LayoutParams().apply {
                width=0; height=54
                columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)
                setMargins(3,3,3,3)
            }
            grid.addView(b,lp)
        }
        box.addView(grid)
        dialog.setView(box)
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"Close"){_,_->}
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels*0.94).toInt(),-2)
    }

    private fun press(key:String) {
        when(key) {
            "C"->{saveState(); clearAll()}
            "⌫"->{undo()}
            "."->{saveState(); decimal()}
            "%"->{saveState(); percent()}
            "+","−","×","÷"->{saveState(); setOperation(key)}
            "="->{saveState(); equalsKey()}
            else->{saveState(); number(key)}
        }
        render()
    }

    private fun number(n:String) {
        if(value=="Error"||resetInput){value=n;resetInput=false}
        else if(value=="0") value=n
        else if(value.length<18) value+=n
    }

    private fun decimal() {
        if(resetInput||value=="Error"){value="0.";resetInput=false}
        else if(!value.contains(".")) value+="."
    }

    private fun clearAll(){value="0";stored=null;operation=null;resetInput=true;expression.text="Ready"}

    private fun setOperation(op:String) {
        val n=parse()
        if(stored!=null&&operation!=null&&!resetInput) calculate()
        stored=parse() ?: n
        operation=op
        resetInput=true
        expression.text=format(stored ?: 0.0)+" "+displayOp(op)
    }

    private fun calculate() {
        val a=stored ?: return
        val b=parse() ?: return
        val r=when(operation) {
            "+"->a+b
            "−"->a-b
            "×"->a*b
            "÷"->if(b==0.0) Double.NaN else a/b
            "^"->a.pow(b)
            else->Double.NaN
        }
        if(!r.isFinite()){value="Error";expression.text="Math error"}
        else value=format(r)
        stored=null;operation=null;resetInput=true
    }

    private fun equalsKey() {
        if(operation!=null&&stored!=null) {
            val a=stored ?: 0.0
            val op=displayOp(operation!!)
            val b=value
            calculate()
            expression.text=format(a)+" "+op+" "+b+" ="
            return
        }
        val code=value
        val saved=getSharedPreferences("sportsview",MODE_PRIVATE).getString("calculator_code",null)
        if(saved!=null&&code.matches(Regex("[0-9]{4,12}"))&&code==saved) {
            getSharedPreferences("sportsview",MODE_PRIVATE).edit().putBoolean("calculator_authenticated",true).apply()
            startActivity(android.content.Intent(this,MainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        } else if(saved!=null&&code.matches(Regex("[0-9]{4,12}"))) {
            value="0";resetInput=true;expression.text="Incorrect code"
        } else expression.text="="
    }

    private fun applyFunction(fn:String) {
        val n=parse()
        val r=when(fn) {
            "sin"->sin(toRadians(n))
            "cos"->cos(toRadians(n))
            "tan"->tan(toRadians(n))
            "asin"->fromRadians(asin(n))
            "acos"->fromRadians(acos(n))
            "atan"->fromRadians(atan(n))
            "ln"->if(n>0) ln(n) else Double.NaN
            "log"->if(n>0) log10(n) else Double.NaN
            "√"->if(n>=0) sqrt(n) else Double.NaN
            "x²"->n*n
            "x!"->factorial(n)
            "1/x"->if(n!=0.0) 1.0/n else Double.NaN
            "abs"->abs(n)
            "floor"->floor(n)
            "ceil"->ceil(n)
            "π"->Math.PI
            "e"->Math.E
            else->n
        }
        value=if(r.isFinite()) format(r) else "Error"
        resetInput=true
        expression.text=fn+"("+format(n)+") = "+value
    }

    private fun percent(){val n=parse();value=format(n/100.0);resetInput=true;expression.text=format(n)+"% = "+value}
    private fun parse():Double=value.toDoubleOrNull() ?: 0.0
    private fun toRadians(n:Double)=Math.toRadians(n)
    private fun fromRadians(n:Double)=Math.toDegrees(n)
    private fun factorial(n:Double):Double{if(n<0||n>170||n%1.0!=0.0)return Double.NaN;var r=1.0;var i=2;while(i<=n.toInt()){r*=i;i++};return r}
    private fun format(n:Double):String{if(!n.isFinite())return "Error";if(abs(n)<1e-12)return "0";return BigDecimal.valueOf(n).setScale(12,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}
    private fun displayOp(op:String)=when(op){"*"->"×";"/"->"÷";"^"->"xʸ";else->op}

    private fun applyTheme() {
        val bg=if(light)Color.rgb(247,248,250) else Color.rgb(14,17,23)
        val fg=if(light)Color.rgb(30,35,42) else Color.WHITE
        val sub=if(light)Color.rgb(100,108,120) else Color.rgb(160,170,185)
        root.setBackgroundColor(bg)
        display.setTextColor(fg);expression.setTextColor(sub)
        advancedButton.setTextColor(fg);themeButton.setTextColor(fg)
        themeButton.text=if(light)"☾" else "☀"
        themeButton.setBackgroundColor(if(light)Color.WHITE else Color.rgb(30,35,45))
        advancedButton.setBackgroundColor(if(light)Color.WHITE else Color.rgb(30,35,45))
    }

    private fun render() {
        display.text=value
        if(expression.text.isNullOrBlank()) expression.text="Ready"
    }
}
