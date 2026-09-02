/*
 * Desarrollado por RelajoSoft · https://relajosoft.com
 * Autor: Yoel Enrique Estevez Gonzalez
 *
 * Servicio principal del escáner flotante. Mantiene la aplicación de trabajo
 * en primer plano, abre la cámara encima y entrega el resultado al IME propio.
 */
package com.relajosoft.floatingscanner

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * El botón y la cámara viven en ventanas TYPE_APPLICATION_OVERLAY.
 * No abrimos una Activity para escanear: así la aplicación de trabajo se queda
 * tranquilita debajo, conserva el input seleccionado y no sufre un refresco.
 *
 * Este servicio no almacena ni transmite las lecturas. El código válido pasa
 * directamente a [ScannerInputMethodService] y se descarta después de escribirlo.
 */
class FloatingBubbleService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private var bubble: View? = null
    private var scannerOverlay: View? = null
    private var previewView: PreviewView? = null
    private var overlayStatus: TextView? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var analysisExecutor: ExecutorService? = null
    private var barcodeScanner: BarcodeScanner? = null
    private val scanFinished = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        // El servicio es visible para Android mediante una notificación permanente.
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        startForeground(1001, buildNotification())

        if (Settings.canDrawOverlays(this)) showBubble() else stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_OPEN_SCANNER) {
            Handler(Looper.getMainLooper()).post { openScannerOverlay() }
        }
        return result
    }

    override fun onDestroy() {
        hideScannerOverlay()
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
        super.onDestroy()
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun showBubble() {
        if (bubble != null) return

        // Botón pequeño que el operario puede mover por la pantalla sin perder foco.
        val size = dp(58)
        val view = TextView(this).apply {
            text = "▣"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundResource(android.R.drawable.btn_default)
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF168E71.toInt())
            elevation = dp(10).toFloat()
            contentDescription = "Escanear código"
        }

        val params = WindowManager.LayoutParams(
            size,
            size,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - size - dp(18)
            y = resources.displayMetrics.heightPixels / 2
        }

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > dp(5) || abs(dy) > dp(5)) moved = true
                    params.x = startX + dx
                    params.y = startY + dy
                    windowManager.updateViewLayout(view, params)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!moved) openScannerOverlay()
                    true
                }

                else -> false
            }
        }

        windowManager.addView(view, params)
        bubble = view
    }

    private fun openScannerOverlay() {
        if (scannerOverlay != null) return

        // Sin una conexión IME activa no existe un destino seguro donde escribir.
        if (!ScannerInputMethodService.isConnected()) {
            Toast.makeText(this, "Selecciona RelajoSoft Scanner como teclado y toca primero el campo de texto", Toast.LENGTH_LONG).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Falta el permiso de cámara. Abre RelajoSoft Scanner una vez para concederlo.", Toast.LENGTH_LONG).show()
            return
        }

        // Cerramos el teclado antes de enseñar la cámara; el InputConnection sigue vivo.
        ScannerInputMethodService.prepareForScan()

        scanFinished.set(false)

        val root = FrameLayout(this).apply {
            setBackgroundColor(0xF20A111C.toInt())
            elevation = dp(18).toFloat()
        }

        val preview = PreviewView(this).apply {
            // COMPATIBLE ofrece mejor comportamiento en overlays de fabricantes diversos.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        previewView = preview
        root.addView(
            preview,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(13), dp(16), dp(12))
            setBackgroundColor(0xB0000000.toInt())
        }
        header.addView(TextView(this).apply {
            text = "RelajoSoft Scanner · cámara flotante"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        overlayStatus = TextView(this).apply {
            text = "Toda la pantalla es zona de lectura · admite etiquetas giradas"
            textSize = 13f
            setTextColor(0xFFC6D4E2.toInt())
        }
        header.addView(overlayStatus)
        root.addView(header, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP
        })

        val cancel = Button(this).apply {
            text = "Cancelar"
            isAllCaps = false
            setOnClickListener { hideScannerOverlay() }
        }
        root.addView(cancel, FrameLayout.LayoutParams(dp(120), dp(48)).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(14)
        })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            // NOT_FOCUSABLE es la pieza clave: la app de trabajo conserva el foco.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(root, params)
            scannerOverlay = root
            bubble?.visibility = View.GONE
            startOverlayCamera()
        } catch (e: Exception) {
            scannerOverlay = null
            previewView = null
            bubble?.visibility = View.VISIBLE
            Toast.makeText(this, "No se pudo abrir la cámara flotante", Toast.LENGTH_LONG).show()
        }
    }

    private fun startOverlayCamera() {
        val previewTarget = previewView ?: return
        analysisExecutor?.shutdownNow()
        analysisExecutor = Executors.newSingleThreadExecutor()

        // Lista cerrada de formatos admitidos. No se usa reconocimiento de texto ni nube.
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_AZTEC
            )
            .build()
        barcodeScanner?.close()
        barcodeScanner = BarcodeScanning.getClient(options)

        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (scannerOverlay == null) return@addListener
            try {
                val provider = future.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewTarget.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    // Si la PDA va justa, analizamos siempre la imagen más reciente.
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(1920, 1080))
                    .build()
                analysis.setAnalyzer(analysisExecutor!!) { proxy -> analyze(proxy) }

                provider.unbindAll()
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)

                previewTarget.postDelayed({
                    // Un enfoque inicial al centro ayuda con etiquetas pequeñas o gastadas.
                    val activeCamera = camera ?: return@postDelayed
                    val point = previewTarget.meteringPointFactory.createPoint(
                        previewTarget.width / 2f,
                        previewTarget.height / 2f
                    )
                    val focus = FocusMeteringAction.Builder(point)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()
                    activeCamera.cameraControl.startFocusAndMetering(focus)
                }, 350)

                if (Prefs.autoTorch(this) && camera?.cameraInfo?.hasFlashUnit() == true) {
                    camera?.cameraControl?.enableTorch(true)
                    overlayStatus?.text = "🔦 Toda la pantalla es zona de lectura · admite etiquetas giradas"
                } else {
                    overlayStatus?.text = "Toda la pantalla es zona de lectura · admite etiquetas giradas"
                }
            } catch (e: Exception) {
                overlayStatus?.text = "No se pudo iniciar la cámara"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyze(proxy: ImageProxy) {
        if (scanFinished.get()) {
            proxy.close()
            return
        }
        if (proxy.image == null || barcodeScanner == null) {
            proxy.close()
            return
        }

        processRotation(proxy, proxy.imageInfo.rotationDegrees, retryAtRightAngle = true)
    }

    private fun processRotation(proxy: ImageProxy, rotation: Int, retryAtRightAngle: Boolean) {
        val media = proxy.image
        val scanner = barcodeScanner
        if (media == null || scanner == null || scanFinished.get()) {
            proxy.close()
            return
        }

        var retryStarted = false
        fun retryIfNeeded() {
            // Segundo intento con el mismo fotograma girado 90 grados. Así resolvemos
            // las cajas con etiquetas laterales sin pedirle al operario que gire la PDA.
            if (retryAtRightAngle && !scanFinished.get()) {
                retryStarted = true
                processRotation(proxy, (rotation + 90) % 360, retryAtRightAngle = false)
            }
        }

        val image = InputImage.fromMediaImage(media, rotation)
        scanner.process(image)
            .addOnSuccessListener { list ->
                val code = selectCode(list)
                if (code != null) acceptScan(code) else retryIfNeeded()
            }
            .addOnFailureListener { retryIfNeeded() }
            .addOnCompleteListener {
                if (!retryStarted) proxy.close()
            }
    }

    private fun selectCode(list: List<Barcode>): String? {
        // Aplicamos las reglas de negocio antes de enviar nada al campo activo.
        val ignoreQrW = Prefs.ignoreQrWithW(this)
        val barcode = list.firstOrNull { item ->
            val value = item.rawValue
            if (value.isNullOrBlank()) return@firstOrNull false
            !(ignoreQrW && item.format == Barcode.FORMAT_QR_CODE && value.contains('W', ignoreCase = true))
        } ?: return null

        var code = barcode.rawValue.orEmpty()
        if (Prefs.stripMasterSuffix(this)) {
            code = code.replace(Regex("-M$", RegexOption.IGNORE_CASE), "")
        }
        return code.takeIf { it.isNotBlank() }
    }

    private fun acceptScan(code: String) {
        // AtomicBoolean evita escribir dos veces si llegan resultados casi simultáneos.
        if (!scanFinished.compareAndSet(false, true)) return

        Handler(Looper.getMainLooper()).post {
            overlayStatus?.text = "✓ $code"
            playSuccessFeedback()
            camera?.cameraControl?.enableTorch(false)

            // Cerramos SOLO nuestro overlay. La app de trabajo nunca fue sustituida.
            Handler(Looper.getMainLooper()).postDelayed({
                // Es importante escribir antes de ocultar definitivamente el IME:
                // algunas WebView lo solicitan de nuevo como efecto de commitText.
                hideScannerOverlay(keepImeHidden = false)
                val sendEnter = Prefs.sendEnter(this)
                if (!ScannerInputMethodService.writeCode(code, sendEnter)) {
                    Toast.makeText(this, "No hay un campo de texto activo para el IME", Toast.LENGTH_LONG).show()
                }
                ScannerInputMethodService.keepHiddenAfterScan()
            }, 140)
        }
    }

    private fun playSuccessFeedback() {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 250)

        if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
                .vibrate(VibrationEffect.createOneShot(90, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(90)
        }
    }

    private fun hideScannerOverlay(keepImeHidden: Boolean = true) {
        if (keepImeHidden) {
            ScannerInputMethodService.keepHiddenAfterScan()
        }
        // Liberamos cámara, analizador y vistas para no dejar recursos colgados.
        camera?.cameraControl?.enableTorch(false)
        camera = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        barcodeScanner?.close()
        barcodeScanner = null
        analysisExecutor?.shutdownNow()
        analysisExecutor = null

        scannerOverlay?.let { runCatching { windowManager.removeView(it) } }
        scannerOverlay = null
        previewView = null
        overlayStatus = null
        scanFinished.set(false)
        bubble?.visibility = View.VISIBLE
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("RelajoSoft Scanner activo")
            .setContentText("Botón y cámara flotante listos")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Escáner flotante", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL = "scanner_overlay"
        const val ACTION_OPEN_SCANNER = "com.relajosoft.floatingscanner.OPEN_SCANNER"
    }
}
