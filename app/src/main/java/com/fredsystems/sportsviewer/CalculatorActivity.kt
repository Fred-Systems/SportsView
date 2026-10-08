package com.fredsystems.sportsviewer

import android.app.Activity
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
    private lateinit var valueView: TextView
    private lateinit var expressionView: TextView
    private lateinit var modeButton: Button
    private lateinit var angleButton: Button
    private var value = "0"
    private var stored: Double? = null
    private var operation: String? = null
    private var resetInput = true
    private var expression = "Ready"
    private var advanced = false
    private var degrees = true
    private var memory = 0.0

    private data class Snapshot(val value:String,val stored:Double?,val operation:String?,val resetInput:Boolean,val expression:String,val advanced:Boolean,val degrees:Boolean)
    private val history = ArrayDeque<Snapshot>()

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); title="Calculator"; buildCalculator() }

    private fun saveState() {
        history.addLast(Snapshot(value,stored,operation,resetInput,expression,advanced,degrees))
        if(history.size>60) history.removeFirst()
    }
    private fun undo() {
        if(history.isEmpty()) return
        val s=history.removeLast()
        value=s.value; stored=s.stored; operation=s.operation; resetInput=s.resetInput; expression=s.expression; advanced=s.advanced; degrees=s.degrees
        rebuildGrid(); render()
    }

    private fun buildCalculator() {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(14,16,14,14); setBackgroundColor(Color.rgb(10,14,22)) }
        val top=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        modeButton=Button(this).apply { text="ADVANCED"; isAllCaps=false; textSize=12f; setOnClickListener { saveState(); advanced=!advanced; expression=if(advanced)"Advanced mode" else "Standard mode"; rebuildGrid(); render() } }
        angleButton=Button(this).apply { text="DEG"; isAllCaps=false; textSize=12f; visibility=View.GONE; setOnClickListener { saveState(); degrees=!degrees; angleButton.text=if(degrees)"DEG" else "RAD"; expression=if(degrees)"Degrees" else "Radians"; render() } }
        top.addView(modeButton,LinearLayout.LayoutParams(0,48,1f)); top.addView(angleButton,LinearLayout.LayoutParams(72,48)); root.addView(top)
        expressionView=TextView(this).apply { text="Ready"; textSize=14f; setTextColor(Color.rgb(145,157,178)); gravity=Gravity.END or Gravity.CENTER_VERTICAL; setPadding(12,8,12,2) }
        root.addView(expressionView,LinearLayout.LayoutParams(-1,40))
        valueView=TextView(this).apply { text="0"; textSize=42f; typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD); setTextColor(Color.WHITE); gravity=Gravity.END or Gravity.CENTER_VERTICAL; setPadding(12,2,12,12); minHeight=86 }
        root.addView(valueView,LinearLayout.LayoutParams(-1,88))
        val grid=GridLayout(this).apply { columnCount=4; setPadding(0,6,0,0) }
        root.addView(grid,LinearLayout.LayoutParams(-1,0,1f)); setContentView(root); rebuildGrid(); render()
    }

    private fun rebuildGrid() {
        val grid=(valueView.parent as LinearLayout).getChildAt(3) as GridLayout
        grid.removeAllViews()
        val keys=if(advanced) listOf("C","⌫","M+","M-","MR","MC","sin","cos","tan","÷","asin","acos","atan","×","ln","log","√","−","x²","xʸ","x!","1/x","+","abs","floor","ceil","π","e","%","=","7","8","9",".","4","5","6","±","1","2","3","","0","","","")
        else listOf("C","⌫","%","÷","7","8","9","×","4","5","6","−","1","2","3","+","0",".","=","")
        for(key in keys) {
            val b=Button(this).apply {
                text=key; textSize=if(key.length>2)13f else 20f; isAllCaps=false
                setTextColor(if(key in listOf("+","−","×","÷","=","sin","cos","tan","asin","acos","atan","ln","log","√","x²","xʸ","x!","1/x","abs","floor","ceil","M+","M-","MR","MC")) Color.rgb(42,210,174) else Color.WHITE)
                setOnClickListener { if(key.isNotEmpty()) press(key) }
                if(key.isEmpty()) visibility=View.INVISIBLE
            }
            val lp=GridLayout.LayoutParams().apply { width=0; height=0; columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); rowSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); setMargins(3,3,3,3) }
            if(key=="0") lp.columnSpec=GridLayout.spec(0,2,1f)
            grid.addView(b,lp)
        }
        modeButton.text=if(advanced)"STANDARD" else "ADVANCED"
        angleButton.visibility=if(advanced)View.VISIBLE else View.GONE
        angleButton.text=if(degrees)"DEG" else "RAD"
    }

    private fun press(key:String) {
        when(key) {
            "C"->{saveState();clear()}
            "⌫"->undo()
            "."->{saveState();decimal()}
            "%"->{saveState();percent()}
            "±"->{saveState();toggleSign()}
            "M+"->{saveState();memory += parse() ?: 0.0; expression="Memory +" }
            "M-"->{saveState();memory -= parse() ?: 0.0; expression="Memory −" }
            "MR"->{saveState();value=format(memory);resetInput=true;expression="Memory recall"}
            "MC"->{saveState();memory=0.0;expression="Memory cleared"}
            "+","−","×","÷"->{saveState();setOperation(key)}
            "="->{saveState();equalsKey()}
            "sin","cos","tan","asin","acos","atan","ln","log","√","x²","x!","1/x","abs","floor","ceil"->{saveState();unary(key)}
            "xʸ"->{saveState();setOperation("^")}
            "π"->{saveState();constant(Math.PI,"π")}
            "e"->{saveState();constant(Math.E,"e")}
            "("->{saveState();expression="Opening parenthesis";resetInput=true}
            ")"->{saveState();expression="Closing parenthesis";resetInput=true}
            else->{saveState();number(key)}
        }
        render()
    }

    private fun number(n:String){ if(value=="Error"||resetInput){value=n;resetInput=false}else if(value=="0")value=n else if(value.length<18)value+=n }
    private fun decimal(){ if(value=="Error"||resetInput){value="0.";resetInput=false}else if(!value.contains("."))value+="." }
    private fun clear(){value="0";stored=null;operation=null;resetInput=true;expression="Ready"}
    private fun percent(){val n=parse()?:return;value=format(n/100.0);expression="Percent (%)";resetInput=true}
    private fun toggleSign(){val n=parse()?:return;value=format(-n);expression="Sign change (±)"}
    private fun constant(n:Double,name:String){value=format(n);resetInput=true;expression=name}

    private fun setOperation(op:String) {
        val n=parse()?:return
        if(stored!=null&&operation!=null&&!resetInput) calculate()
        stored=parse(); operation=op; resetInput=true; expression=format(stored?:n)+" "+displayOp(op)
    }

    private fun calculate() {
        val a=stored?:return; val b=parse()?:return
        val r=when(operation){"+"->a+b;"−"->a-b;"×"->a*b;"÷"->if(b==0.0)Double.NaN else a/b;"^"->a.pow(b);else->Double.NaN}
        if(!r.isFinite()){value="Error";stored=null;operation=null;resetInput=true;expression="Math error"}else{value=format(r);stored=null;operation=null;resetInput=true}
    }

    private fun unary(fn:String) {
        val n=parse()?:return
        val r=when(fn){"sin"->sin(angle(n));"cos"->cos(angle(n));"tan"->tan(angle(n));"asin"->inverseAngle(asin(n));"acos"->inverseAngle(acos(n));"atan"->inverseAngle(atan(n));"ln"->if(n>0)ln(n)else Double.NaN;"log"->if(n>0)log10(n)else Double.NaN;"√"->if(n>=0)sqrt(n)else Double.NaN;"x²"->n*n;"x!"->factorial(n)
            "1/x"->if(n!=0.0)1.0/n else Double.NaN
            "abs"->abs(n)
            "floor"->floor(n)
            "ceil"->ceil(n)
            else->Double.NaN}
        if(!r.isFinite()){value="Error";expression=fn+"("+format(n)+") → Error";resetInput=true}else{value=format(r);expression=fn+"("+format(n)+") =";resetInput=true}
    }
    private fun factorial(n:Double):Double { if(n<0||n>170||n%1.0!=0.0)return Double.NaN; var r=1.0; var i=2; while(i<=n.toInt()){r*=i;i++}; return r }
    private fun angle(n:Double)=if(degrees)Math.toRadians(n)else n
    private fun inverseAngle(n:Double)=if(degrees)Math.toDegrees(n)else n

    private fun equalsKey() {
        if(operation!=null&&stored!=null) {
            val a=stored; val opText=displayOp(operation!!); val before=value; calculate(); expression=format(a?:0.0)+" "+opText+" "+before+" ="; return
        }
        val code=value; val saved=getSharedPreferences("sportsview",MODE_PRIVATE).getString("calculator_code",null)
        if(saved!=null&&code.matches(Regex("[0-9]{4,12}"))&&code==saved) {
            getSharedPreferences("sportsview",MODE_PRIVATE).edit().putBoolean("calculator_authenticated",true).apply()
            startActivity(android.content.Intent(this,MainActivity::class.java).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK) })
        } else if(saved!=null&&code.matches(Regex("[0-9]{4,12}"))) { value="0"; expression="Incorrect code"; resetInput=true }
        else expression="Equals"
    }

    private fun format(n:Double):String { if(!n.isFinite())return "Error"; if(abs(n)<1e-12)return "0"; return BigDecimal.valueOf(n).setScale(12,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() }
    private fun parse():Double?=value.toDoubleOrNull()
    private fun displayOp(op:String)=when(op){"*"->"×";"/"->"÷";"^"->"^";else->op}
    private fun render(){valueView.text=value;expressionView.text=expression}
}
