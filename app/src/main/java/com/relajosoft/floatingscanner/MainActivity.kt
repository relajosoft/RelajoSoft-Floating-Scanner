/*
 * Desarrollado por RelajoSoft · https://relajosoft.com
 * Autor: Yoel Enrique Estevez Gonzalez
 *
 * Pantalla de configuración local. No muestra ni guarda códigos escaneados;
 * solamente guía al usuario para habilitar el IME, el overlay y sus opciones.
 */
package com.relajosoft.floatingscanner

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La interfaz se crea por código para mantener el proyecto pequeño y directo.
        buildUi()
        requestPermissionsIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // Al volver de Ajustes reflejamos inmediatamente los permisos concedidos.
        refreshStatus()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(0xFF07111F.toInt())
        }

        root.addView(TextView(this).apply {
            text = "RelajoSoft Scanner v0.8.0"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Escáner flotante y teclado manual en un único IME"
            textSize = 15f
            setTextColor(0xFF9FB3C8.toInt())
            setPadding(0, dp(4), 0, dp(20))
        })

        status = TextView(this).apply {
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundResource(R.drawable.bg_panel)
            setTextColor(Color.WHITE)
            textSize = 15f
        }
        root.addView(status, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(16)
        })

        root.addView(sectionTitle("Configuración del teclado"))
        root.addView(actionButton("1. Habilitar RelajoSoft Scanner") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(actionButton("2. Elegir método de entrada") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        root.addView(actionButton("3. Permitir botón sobre otras apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })

        root.addView(TextView(this).apply {
            text = "Método único y revisable: Android Input Method Service (IME). No se utiliza Accesibilidad."
            textSize = 14f
            setTextColor(0xFF9FB3C8.toInt())
            setPadding(0, dp(4), 0, dp(14))
        })

        root.addView(sectionTitle("Botón flotante"))
        root.addView(actionButton("Activar botón flotante") {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Primero permite mostrar sobre otras aplicaciones", Toast.LENGTH_LONG).show()
                return@actionButton
            }
            if (!isImeEnabled()) {
                Toast.makeText(this, "Primero habilita RelajoSoft Scanner como teclado", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                return@actionButton
            }
            if (!isImeSelected()) {
                Toast.makeText(this, "Selecciona RelajoSoft Scanner como método de entrada", Toast.LENGTH_LONG).show()
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                return@actionButton
            }
            ContextCompat.startForegroundService(this, Intent(this, FloatingBubbleService::class.java))
            Toast.makeText(this, "Botón flotante activado", Toast.LENGTH_SHORT).show()
        })
        root.addView(actionButton("Detener botón flotante", secondary = true) {
            stopService(Intent(this, FloatingBubbleService::class.java))
        })

        root.addView(sectionTitle("Opciones de escaneo"))
        root.addView(optionSwitch("Linterna automática al escanear", Prefs.autoTorch(this)) {
            Prefs.setAutoTorch(this, it)
        })
        root.addView(optionSwitch("Enviar ENTER después del código", Prefs.sendEnter(this)) {
            Prefs.setSendEnter(this, it)
        })
        root.addView(optionSwitch("Ignorar QR que contengan W / w", Prefs.ignoreQrWithW(this)) {
            Prefs.setIgnoreQrWithW(this, it)
        })
        root.addView(optionSwitch("Eliminar -M / -m al final del código", Prefs.stripMasterSuffix(this)) {
            Prefs.setStripMasterSuffix(this, it)
        })

        root.addView(TextView(this).apply {
            text = "Prueba recomendada: habilita y selecciona RelajoSoft Scanner como teclado, abre la app de trabajo y toca el input. El mismo teclado permite escribir manualmente o pulsar ESCANEAR. La cámara aparece encima sin sustituir la app de trabajo.\n\nDesarrollado por RelajoSoft\nrelajosoft.com\nYoel Enrique Estevez Gonzalez"
            textSize = 14f
            setTextColor(0xFF9FB3C8.toInt())
            setPadding(0, dp(18), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun refreshStatus() {
        // Estos estados son de configuración; no inspeccionan datos de otras apps.
        val overlay = Settings.canDrawOverlays(this)
        val camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val imeEnabled = isImeEnabled()
        val imeSelected = isImeSelected()
        status.text = "Estado · método único: IME\n" +
            mark(overlay) + " Mostrar sobre otras apps\n" +
            mark(camera) + " Cámara\n" +
            mark(imeEnabled) + " IME habilitado\n" +
            mark(imeSelected) + " IME seleccionado"
    }

    private fun isImeEnabled(): Boolean {
        // Android entrega la lista oficial de IME habilitados por el usuario.
        val component = ComponentName(this, ScannerInputMethodService::class.java)
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { ComponentName.unflattenFromString(it.id) == component }
    }

    private fun isImeSelected(): Boolean {
        // Comparamos el identificador seleccionado sin pedir permisos adicionales.
        val selected = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return ComponentName.unflattenFromString(selected) == ComponentName(this, ScannerInputMethodService::class.java)
    }

    private fun mark(ok: Boolean) = if (ok) "✓" else "✗"

    private fun requestPermissionsIfNeeded() {
        // Los permisos sensibles se solicitan en tiempo de ejecución y de uno en uno.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11)
        }
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 18f
        setTextColor(0xFF26D9A1.toInt())
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(14), 0, dp(10))
    }

    private fun optionSwitch(label: String, checked: Boolean, change: (Boolean) -> Unit) =
        Switch(this).apply {
            text = label
            textSize = 16f
            setTextColor(Color.WHITE)
            isChecked = checked
            setPadding(0, dp(6), 0, dp(6))
            setOnCheckedChangeListener { _, value -> change(value) }
        }

    private fun actionButton(label: String, secondary: Boolean = false, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 15f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundResource(if (secondary) R.drawable.bg_button_secondary else R.drawable.bg_button)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply {
                bottomMargin = dp(10)
            }
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
