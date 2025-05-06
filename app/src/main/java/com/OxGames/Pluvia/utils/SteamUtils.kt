package com.OxGames.Pluvia.utils

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.text.Html
import com.OxGames.Pluvia.Constants
import `in`.dragonbra.javasteam.util.HardwareUtils
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object SteamUtils {

    private val sfd by lazy {
        SimpleDateFormat("MMM d - h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    /**
     * Converts steam time to actual time
     * @return a string in the 'MMM d - h:mm a' format.
     */
    // Note: Mostly correct, has a slight skew when near another minute
    fun fromSteamTime(rtime: Int): String = sfd.format(rtime * 1000L)

    /**
     * Converts steam time from the playtime of a friend into an approximate double representing hours.
     * @return A string representing how many hours were played, ie: 1.5 hrs
     */
    fun formatPlayTime(time: Int): String {
        val hours = time / 60.0
        return if (hours % 1 == 0.0) {
            hours.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", time / 60.0)
        }
    }

    // Steam strips all non-ASCII characters from usernames and passwords
    // source: https://github.com/steevp/UpdogFarmer/blob/8f2d185c7260bc2d2c92d66b81f565188f2c1a0e/app/src/main/java/com/steevsapps/idledaddy/LoginActivity.java#L166C9-L168C104
    // more: https://github.com/winauth/winauth/issues/368#issuecomment-224631002
    /**
     * Strips non-ASCII characters from String
     */
    fun removeSpecialChars(s: String): String = s.replace(Regex("[^\\u0000-\\u007F]"), "")

    /**
     * Replaces any existing `steam_api.dll` or `steam_api64.dll` in the app directory
     * with our pipe dll stored in assets
     */
    fun replaceSteamApi(context: Context, appId: Int) {
        // val appDirPath = SteamService.getAppDirPath(appId)
        //
        // File(appDirPath).walkTopDown().forEach {
        //     if (it.name == "steam_api.dll" && it.exists()) {
        //         it.delete()
        //         it.createNewFile()
        //         FileOutputStream(it.absolutePath).use { fos ->
        //             context.assets.open("steampipe/steam_api.dll").use { fs ->
        //                 fs.copyTo(fos)
        //             }
        //         }
        //     }
        //     if (it.name == "steam_api64.dll" && it.exists()) {
        //         it.delete()
        //         it.createNewFile()
        //         FileOutputStream(it.absolutePath).use { fos ->
        //             context.assets.open("steampipe/steam_api64.dll").use { fs ->
        //                 fs.copyTo(fos)
        //             }
        //         }
        //     }
        // }
    }

    /**
     * Gets the Android user-editable device name or falls back to [HardwareUtils.getMachineName]
     */
    fun getMachineName(context: Context): String {
        return try {
            // Try different methods to get device name
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                ?: Settings.System.getString(context.contentResolver, "device_name")
                // ?: Settings.Secure.getString(context.contentResolver, "bluetooth_name")
                // ?: BluetoothAdapter.getDefaultAdapter()?.name
                ?: HardwareUtils.getMachineName() // Fallback to machine name if all else fails
        } catch (e: Exception) {
            HardwareUtils.getMachineName() // Return machine name as last resort
        }
    }

    // Set LoginID to a non-zero value if you have another client connected using the same account,
    // the same private ip, and same public ip.
    // source: https://github.com/Longi94/JavaSteam/blob/08690d0aab254b44b0072ed8a4db2f86d757109b/javasteam-samples/src/main/java/in/dragonbra/javasteamsamples/_000_authentication/SampleLogonAuthentication.java#L146C13-L147C56
    /**
     * This ID is unique to the device and app combination
     */
    @SuppressLint("HardwareIds")
    fun getUniqueDeviceId(context: Context): Int {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        return androidId.hashCode()
    }

    /**
     * Gets the avatar URL from an avatar hash.
     */
    fun getAvatarURL(string: String?): String =
        string.orEmpty()
            .ifEmpty { null }
            ?.takeIf { str -> str.isNotEmpty() && !str.all { it == '0' } }
            ?.let { "${Constants.Persona.AVATAR_BASE_URL}${it.substring(0, 2)}/${it}_full.jpg" }
            ?: Constants.Persona.MISSING_AVATAR_URL

    fun fromHtml(string: String): String = Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY).toString()

    /**
     * Gets the profile URL from a steam id.
     * Steam should redirect to a vanity URL if applied.
     */
    fun getProfileUrl(id: Long): String = "${Constants.Persona.PROFILE_URL}$id/"
}
