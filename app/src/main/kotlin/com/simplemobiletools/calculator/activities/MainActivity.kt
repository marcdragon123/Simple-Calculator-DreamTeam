package com.simplemobiletools.calculator.activities

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.simplemobiletools.calculator.BuildConfig
import com.simplemobiletools.calculator.R
import com.simplemobiletools.calculator.extensions.config
import com.simplemobiletools.calculator.extensions.updateViewColors
import com.simplemobiletools.calculator.helpers.CONSTANT.ABSOLUTE_VALUE
import com.simplemobiletools.calculator.helpers.CONSTANT.ARCCOS
import com.simplemobiletools.calculator.helpers.CONSTANT.ARCSINE
import com.simplemobiletools.calculator.helpers.CONSTANT.ARCTANGENT
import com.simplemobiletools.calculator.helpers.CONSTANT.CEILING
import com.simplemobiletools.calculator.helpers.CONSTANT.COSINE
import com.simplemobiletools.calculator.helpers.CONSTANT.CUBE
import com.simplemobiletools.calculator.helpers.CONSTANT.DIVIDE
import com.simplemobiletools.calculator.helpers.CONSTANT.E
import com.simplemobiletools.calculator.helpers.CONSTANT.FLOOR
import com.simplemobiletools.calculator.helpers.CONSTANT.LEFT_BRACKET
import com.simplemobiletools.calculator.helpers.CONSTANT.LOGARITHM
import com.simplemobiletools.calculator.helpers.CONSTANT.MEMORY_ONE
import com.simplemobiletools.calculator.helpers.CONSTANT.MEMORY_THREE
import com.simplemobiletools.calculator.helpers.CONSTANT.MEMORY_TWO
import com.simplemobiletools.calculator.helpers.CONSTANT.MINUS
import com.simplemobiletools.calculator.helpers.CONSTANT.MODULO
import com.simplemobiletools.calculator.helpers.CONSTANT.MULTIPLY
import com.simplemobiletools.calculator.helpers.CONSTANT.NATURAL_LOGARITHM
import com.simplemobiletools.calculator.helpers.CONSTANT.NEGATION
import com.simplemobiletools.calculator.helpers.CONSTANT.PI
import com.simplemobiletools.calculator.helpers.CONSTANT.PLUS
import com.simplemobiletools.calculator.helpers.CONSTANT.POWER
import com.simplemobiletools.calculator.helpers.CONSTANT.RANDOM
import com.simplemobiletools.calculator.helpers.CONSTANT.RECIPROCAL
import com.simplemobiletools.calculator.helpers.CONSTANT.RIGHT_BRACKET
import com.simplemobiletools.calculator.helpers.CONSTANT.ROOT
import com.simplemobiletools.calculator.helpers.CONSTANT.ROUNDING
import com.simplemobiletools.calculator.helpers.CONSTANT.SINE
import com.simplemobiletools.calculator.helpers.CONSTANT.SQUARE
import com.simplemobiletools.calculator.helpers.CONSTANT.TANGENT
import com.simplemobiletools.calculator.helpers.Calculator
import com.simplemobiletools.calculator.helpers.CalculatorImpl
import com.simplemobiletools.calculator.helpers.Formatter
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.LICENSE_AUTOFITTEXTVIEW
import com.simplemobiletools.commons.helpers.LICENSE_ESPRESSO
import com.simplemobiletools.commons.helpers.LICENSE_KOTLIN
import com.simplemobiletools.commons.helpers.LICENSE_ROBOLECTRIC
import kotlinx.android.synthetic.main.activity_main.*
import me.grantland.widget.AutofitHelper

class MainActivity : SimpleActivity(), Calculator {
    private var storedTextColor = 0
    private var vibrateOnButtonPress = true
    private var storedUseEnglish = false

    private lateinit var calc: CalculatorImpl

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched()

        calc = CalculatorImpl(this, applicationContext)

        var shiftClicked = false
        changeButtonFunctionality(shiftClicked)

        //Never changes
        btn_plus.setOnClickListener { calc.handleOperationOnFormula(PLUS); checkHaptic(it) }
        btn_minus.setOnClickListener { calc.handleOperationOnFormula(MINUS); checkHaptic(it) }
        btn_multiply.setOnClickListener { calc.handleOperationOnFormula(MULTIPLY); checkHaptic(it) }
        btn_divide.setOnClickListener { calc.handleOperationOnFormula(DIVIDE); checkHaptic(it) }
        btn_memory_1.setOnClickListener { calc.handleViewValue(MEMORY_ONE)}
        btn_memory_1.setOnLongClickListener{ calc.handleStore(result.text.toString(), MEMORY_ONE); true }
        btn_memory_2.setOnClickListener { calc.handleViewValue(MEMORY_TWO)}
        btn_memory_2.setOnLongClickListener{ calc.handleStore(result.text.toString(), MEMORY_TWO); true }
        btn_memory_3.setOnClickListener { calc.handleViewValue(MEMORY_THREE) }
        btn_memory_3.setOnLongClickListener{calc.handleStore(result.text.toString(), MEMORY_THREE); true }
        btn_del.setOnClickListener {calc.handleClear(formula.text.toString()); checkHaptic(it) }
        btn_all_clear.setOnClickListener { calc.handleReset()}
        btn_left_bracket.setOnClickListener { calc.handleOperationOnFormula(LEFT_BRACKET); checkHaptic(it) }
        btn_right_bracket.setOnClickListener { calc.handleOperationOnFormula(RIGHT_BRACKET); checkHaptic(it) }

        btn_shift.setOnClickListener {
            shiftClicked = !shiftClicked
            changeButtonFunctionality(shiftClicked)
        }

        getButtonIds().forEach {
            it.setOnClickListener { calc.numpadClicked(it.id); checkHaptic(it) }
        }

        btn_equals.setOnClickListener {

            try {
                calc.handleEquals(formula.text.toString()); checkHaptic(it)
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
            }

        }

        formula.setOnLongClickListener { copyToClipboard(false) }
        formula.setOnLongClickListener{ pasteFromClipBoard()}
        result.setOnLongClickListener { copyToClipboard(true) }

        AutofitHelper.create(result)
        AutofitHelper.create(formula)
        storeStateVariables()
        updateViewColors(calculator_holder, config.textColor)
    }

    @SuppressLint("MissingSuperCall")
    override fun onResume() {
        super.onResume()
        if (storedUseEnglish != config.useEnglish) {
            restartActivity()
            return
        }

        if (storedTextColor != config.textColor) {
            updateViewColors(calculator_holder, config.textColor)
        }
        vibrateOnButtonPress = config.vibrateOnButtonPress
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            R.id.History -> launchHistory()
            R.id.unit_conversion -> launchUnitConversion()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun storeStateVariables() {
        config.apply {
            storedTextColor = textColor
            storedUseEnglish = useEnglish
        }
    }

    private fun checkHaptic(view: View) {
        if (vibrateOnButtonPress) {
            view.performHapticFeedback()
        }
    }

    private fun launchHistory() {
        startActivity(Intent(applicationContext, HistoryActivity::class.java))
    }

    private fun launchUnitConversion(){
        startActivity(Intent(applicationContext, UnitConversionActivity::class.java))
    }

    private fun launchSettings() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        startAboutActivity(R.string.app_name, LICENSE_KOTLIN or LICENSE_AUTOFITTEXTVIEW or LICENSE_ROBOLECTRIC or LICENSE_ESPRESSO, BuildConfig.VERSION_NAME)
    }

    private fun getButtonIds() = arrayOf(btn_decimal, btn_0, btn_1, btn_2, btn_3, btn_4, btn_5, btn_6, btn_7, btn_8, btn_9)


    private fun pasteFromClipBoard(): Boolean {
        //check clipboard
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.primaryClip.getItemAt(0).coerceToText(this).toString().isNum()){
            setFormula(clipboard.primaryClip.getItemAt(0).coerceToText(this).toString(), this)
            Toast.makeText(applicationContext,"Pasted from clipboard", Toast.LENGTH_LONG).show()
            return true
        }
        else {
            //do nothing

            return false
        }
    }

    private fun String.isNum() = matches(Regex("\\d|\\d{2}|\\d{3}(\\d{3},)+(.|)(\\d)+"))

    private fun copyToClipboard(copyResult: Boolean): Boolean {
        var value = formula.value
        if (copyResult) {
            value = result.value
        }

        return if (value.isEmpty()) {
            false
        } else {
            copyToClipboard(value)
            true
        }
    }

    override fun setValue(value: String, context: Context) {
        result.text = value
    }

    // used only by Robolectric
    override fun setValueDouble(d: Double) {
        calc.setValue(Formatter.doubleToString(d))
    }

    override fun setFormula(value: String, context: Context) {
        val input = formula.text.toString() + value
        formula.text = input

        if (value == "")
            formula.text = ""
    }

    private fun changeButtonFunctionality(shiftClicked: Boolean){
        if(shiftClicked){
            btn_shift.setBackgroundResource(R.drawable.shift2btn)
            btn_pi_rand.setBackgroundResource(R.drawable.randbtn)
            btn_sin_asin.setBackgroundResource(R.drawable.cosin)
            btn_cos_acos.setBackgroundResource(R.drawable.cocos)
            btn_tan_atan.setBackgroundResource(R.drawable.cotan)
            btn_reciprocal_round.setBackgroundResource(R.drawable.round)
            btn_log_ceil.setBackgroundResource(R.drawable.ceil)
            btn_root_square.setBackgroundResource(R.drawable.xsquare)
            btn_mod_cube.setBackgroundResource(R.drawable.xcubed)
            btn_power_abs.setBackgroundResource(R.drawable.abs)
            btn_e_neg.setBackgroundResource(R.drawable.plusminus)
            btn_ln_floor.setBackgroundResource(R.drawable.floor)
            btn_mod_cube.setOnClickListener { calc.handleOperationOnFormula(CUBE); checkHaptic(it) }
            btn_power_abs.setOnClickListener { calc.handleOperationOnFormula(ABSOLUTE_VALUE); checkHaptic(it) }
            btn_root_square.setOnClickListener { calc.handleOperationOnFormula(SQUARE); checkHaptic(it) }
            btn_pi_rand.setOnClickListener { calc.handleOperationsOnResult(RANDOM); checkHaptic(it) }
            btn_sin_asin.setOnClickListener { calc.handleOperationOnFormula(ARCSINE); checkHaptic(it) }
            btn_cos_acos.setOnClickListener { calc.handleOperationOnFormula(ARCCOS); checkHaptic(it) }
            btn_tan_atan.setOnClickListener { calc.handleOperationOnFormula(ARCTANGENT); checkHaptic(it) }
            btn_log_ceil.setOnClickListener { calc.handleOperationOnFormula(CEILING); checkHaptic(it) }
            btn_ln_floor.setOnClickListener { calc.handleOperationOnFormula(FLOOR); checkHaptic(it) }
            btn_e_neg.setOnClickListener { calc.handleOperationsOnResult(NEGATION); checkHaptic(it) }
            btn_reciprocal_round.setOnClickListener { calc.handleOperationOnFormula(ROUNDING); checkHaptic(it) }
        }
        else {
            btn_shift.setBackgroundResource(R.drawable.shiftbtn)
            btn_pi_rand.setBackgroundResource(R.drawable.pibtn)
            btn_sin_asin.setBackgroundResource(R.drawable.sinbtn)
            btn_cos_acos.setBackgroundResource(R.drawable.cosbtn)
            btn_tan_atan.setBackgroundResource(R.drawable.tanbtn)
            btn_reciprocal_round.setBackgroundResource(R.drawable.invbtn)
            btn_log_ceil.setBackgroundResource(R.drawable.logbtn)
            btn_root_square.setBackgroundResource(R.drawable.sqrbtn)
            btn_mod_cube.setBackgroundResource(R.drawable.modbtn)
            btn_power_abs.setBackgroundResource(R.drawable.powerbtn)
            btn_e_neg.setBackgroundResource(R.drawable.ebtn)
            btn_ln_floor.setBackgroundResource(R.drawable.lnbtn)
            btn_mod_cube.setOnClickListener { calc.handleOperationOnFormula(MODULO); checkHaptic(it) }
            btn_power_abs.setOnClickListener { calc.handleOperationOnFormula(POWER); checkHaptic(it) }
            btn_root_square.setOnClickListener { calc.handleOperationOnFormula(ROOT); checkHaptic(it) }
            btn_pi_rand.setOnClickListener { calc.handleOperationOnFormula(PI); checkHaptic(it) }
            btn_sin_asin.setOnClickListener { calc.handleOperationOnFormula(SINE); checkHaptic(it) }
            btn_cos_acos.setOnClickListener { calc.handleOperationOnFormula(COSINE); checkHaptic(it) }
            btn_tan_atan.setOnClickListener { calc.handleOperationOnFormula(TANGENT); checkHaptic(it) }
            btn_log_ceil.setOnClickListener { calc.handleOperationOnFormula(LOGARITHM); checkHaptic(it) }
            btn_ln_floor.setOnClickListener { calc.handleOperationOnFormula(NATURAL_LOGARITHM); checkHaptic(it) }
            btn_e_neg.setOnClickListener { calc.handleOperationOnFormula(E); checkHaptic(it) }
            btn_reciprocal_round.setOnClickListener { calc.handleOperationsOnResult(RECIPROCAL); checkHaptic(it) }
        }
    }
}