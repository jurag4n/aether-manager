package dev.aether.manager.license

import android.content.Context

object LicensePrefs {

    private const val PREFS_NAME = "aether_license"
    private const val KEY_LICENSE = "license_key"
    private const val KEY_EXPIRY  = "license_expiry"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Simpan license key dan expiry (epoch ms, atau -1 untuk lifetime). */
    fun save(ctx: Context, key: String, expiresAt: Long) {
        prefs(ctx).edit()
            .putString(KEY_LICENSE, key)
            .putLong(KEY_EXPIRY, expiresAt)
            .apply()
    }

    /** Ambil license key yang tersimpan, atau null jika belum ada. */
    fun getKey(ctx: Context): String? =
        prefs(ctx).getString(KEY_LICENSE, null)

    /**
     * Ambil waktu expiry (epoch ms).
     * Mengembalikan -1 jika lifetime, 0 jika belum pernah disimpan.
     */
    fun getExpiry(ctx: Context): Long =
        prefs(ctx).getLong(KEY_EXPIRY, 0L)

    /** Hapus semua data license dari device. */
    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }
}
