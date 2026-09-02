/*
 * Desarrollado por RelajoSoft · https://relajosoft.com
 * Autor: Yoel Enrique Estevez Gonzalez
 *
 * Preferencias locales de la aplicación. Aquí no se guardan códigos escaneados,
 * credenciales ni datos de la empresa; solamente opciones elegidas por el usuario.
 */
package com.relajosoft.floatingscanner

import android.content.Context

object Prefs {
    // Archivo privado dentro del almacenamiento interno de la aplicación.
    private const val FILE = "scanner_prefs"
    private const val TORCH = "auto_torch"
    private const val ENTER = "send_enter"
    private const val IGNORE_QR_W = "ignore_qr_w"
    private const val STRIP_MASTER_SUFFIX = "strip_master_suffix"

    // Linterna automática: viene activada porque ayuda con etiquetas poco visibles.
    fun autoTorch(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(TORCH, true)

    fun setAutoTorch(context: Context, value: Boolean) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(TORCH, value).apply()

    // ENTER opcional después de insertar el código en la aplicación de trabajo.
    fun sendEnter(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(ENTER, false)

    fun setSendEnter(context: Context, value: Boolean) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(ENTER, value).apply()

    // Regla operativa: permite ignorar QR con W/w sin afectar códigos de barras 1D.
    fun ignoreQrWithW(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(IGNORE_QR_W, false)

    fun setIgnoreQrWithW(context: Context, value: Boolean) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(IGNORE_QR_W, value).apply()

    // Regla operativa: elimina solamente un sufijo final -M o -m.
    fun stripMasterSuffix(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(STRIP_MASTER_SUFFIX, false)

    fun setStripMasterSuffix(context: Context, value: Boolean) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(STRIP_MASTER_SUFFIX, value).apply()
}
