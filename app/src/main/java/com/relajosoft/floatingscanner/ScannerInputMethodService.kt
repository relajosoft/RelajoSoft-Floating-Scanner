/*
 * Desarrollado por RelajoSoft · https://relajosoft.com
 * Autor: Yoel Enrique Estevez Gonzalez
 *
 * Teclado Android propio (IME). Es el único canal autorizado para escribir
 * los códigos en el campo que el usuario haya seleccionado voluntariamente.
 */
package com.relajosoft.floatingscanner

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethod
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout

/**
 * IME de RelajoSoft. Comparte la misma InputConnection para el escaneo y para
 * la escritura manual, evitando cambiar continuamente de teclado en la PDA.
 *
 * Ojo, esto es importante para la revisión: el servicio no busca campos de otras
 * aplicaciones. Android le entrega únicamente la conexión del campo activo.
 */
class ScannerInputMethodService : InputMethodService() {

    private val letterButtons = mutableListOf<Pair<Button, Char>>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var uppercase = true
    private var suppressShowUntil = 0L
    private var hideRequestGeneration = 0

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
        if (SystemClock.uptimeMillis() < suppressShowUntil) {
            val explicitlyRequested = flags and (InputMethod.SHOW_EXPLICIT or InputMethod.SHOW_FORCED) != 0
            if (explicitlyRequested) {
                // Un toque real del usuario tiene prioridad y cancela los cierres pendientes.
                suppressShowUntil = 0L
                hideRequestGeneration++
            } else {
                return false
            }
        }
        return super.onShowInputRequested(flags, configChange)
    }

    override fun onCreateInputView(): View {
        // Construimos un teclado compacto pensado para códigos de almacén.
        letterButtons.clear()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Deja libre la barra de navegación del Blackview (gesto, ocultar e IME).
            setPadding(dp(4), dp(5), dp(4), dp(36))
            setBackgroundColor(0xFF07111F.toInt())
        }

        root.addView(createRow().apply {
            addView(keyButton("📷  ESCANEAR", 2.2f) { requestScan() })
            addView(keyButton("Ocultar", 1f) { requestHideSelf(0) })
            addView(keyButton("Otro teclado", 1.25f) { switchKeyboard() })
        })

        root.addView(createCharacterRow("1234567890"))
        root.addView(createLetterRow("qwertyuiop"))
        root.addView(createLetterRow("asdfghjklñ"))

        root.addView(createRow().apply {
            addView(keyButton("⇧", 1.2f) { toggleCase() })
            "zxcvbnm".forEach { letter ->
                val button = keyButton(letter.uppercase(), 1f) { commitLetter(letter) }
                letterButtons += button to letter
                addView(button)
            }
            addView(keyButton("⌫", 1.35f) { backspace() })
        })

        root.addView(createRow().apply {
            listOf("-", "_", ".", "/").forEach { symbol ->
                addView(keyButton(symbol, 0.8f) { commit(symbol) })
            }
            addView(keyButton("espacio", 2.8f) { commit(" ") })
            addView(keyButton("ENTER", 2.2f) { sendEnter() })
        })

        refreshLetterLabels()
        return root
    }

    private fun createCharacterRow(characters: String): LinearLayout = createRow().apply {
        characters.forEach { character ->
            addView(keyButton(character.toString(), 1f) { commit(character.toString()) })
        }
    }

    private fun createLetterRow(characters: String): LinearLayout = createRow().apply {
        characters.forEach { letter ->
            val button = keyButton(letter.uppercase(), 1f) { commitLetter(letter) }
            letterButtons += button to letter
            addView(button)
        }
    }

    private fun createRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(47)
        )
    }

    private fun keyButton(label: String, weight: Float, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = if (label.length > 3) 11f else 16f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = keyBackground()
        setPadding(0, 0, 0, 0)
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
        setOnClickListener { action() }
    }

    private fun commitLetter(letter: Char) {
        commit(if (uppercase) letter.uppercase() else letter.lowercase())
    }

    private fun commit(value: String) {
        // commitText es una API oficial del IME; no hay inyección de eventos globales.
        currentInputConnection?.commitText(value, 1)
    }

    private fun toggleCase() {
        uppercase = !uppercase
        refreshLetterLabels()
    }

    private fun refreshLetterLabels() {
        letterButtons.forEach { (button, letter) ->
            button.text = if (uppercase) letter.uppercase() else letter.lowercase()
        }
    }

    private fun backspace() {
        val connection = currentInputConnection ?: return
        val selected = connection.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            connection.commitText("", 1)
        } else if (!connection.deleteSurroundingText(1, 0)) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
    }

    private fun sendEnter() {
        val connection = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            connection.performEditorAction(action)
        } else {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun requestScan() {
        // El IME solicita la cámara al servicio interno, que no está exportado.
        val intent = Intent(this, FloatingBubbleService::class.java)
            .setAction(FloatingBubbleService.ACTION_OPEN_SCANNER)
        startForegroundService(intent)
    }

    private fun keyBackground(): StateListDrawable {
        fun shape(color: Int, border: Int) = GradientDrawable().apply {
            cornerRadius = dp(7).toFloat()
            setColor(color)
            setStroke(dp(1), border)
        }
        return StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_pressed),
                shape(0xFF168E71.toInt(), 0xFF26D9A1.toInt())
            )
            addState(
                intArrayOf(),
                shape(0xFF17283A.toInt(), 0xFF36516A.toInt())
            )
        }
    }

    private fun prepareForScanner() {
        requestHideSelf(0)
    }

    private fun keepHiddenAfterScanner() {
        // commitText/ENTER puede provocar que ciertas WebView vuelvan a pedir el IME.
        // Se rechazan esas peticiones automáticas y se repite el cierre mientras
        // se estabiliza el foco. Una petición explícita del usuario lo cancela.
        suppressShowUntil = SystemClock.uptimeMillis() + 1_800L
        val generation = ++hideRequestGeneration
        requestHideSelf(0)
        listOf(90L, 260L, 650L, 1_100L).forEach { delay ->
            mainHandler.postDelayed({
                if (
                    generation == hideRequestGeneration &&
                    SystemClock.uptimeMillis() < suppressShowUntil
                ) {
                    requestHideSelf(0)
                }
            }, delay)
        }
    }

    private fun switchKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToNextInputMethod(false)
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
    }

    private fun commitScannedCode(code: String, enterAfterCode: Boolean): Boolean {
        // La lectura existe en memoria solo el tiempo necesario para escribirla.
        val connection = currentInputConnection
        if (connection == null) return false
        val written = connection.commitText(code, 1)
        if (written && enterAfterCode) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
        return written
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        @Volatile private var instance: ScannerInputMethodService? = null

        fun isConnected(): Boolean = instance != null

        fun prepareForScan() {
            instance?.prepareForScanner()
        }

        fun keepHiddenAfterScan() {
            instance?.keepHiddenAfterScanner()
        }

        fun writeCode(code: String, sendEnter: Boolean): Boolean {
            val service = instance ?: return false
            return service.commitScannedCode(code, sendEnter)
        }
    }
}
