package com.winlator.core

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.winlator.core.FileUtils.chmod
import com.winlator.core.FileUtils.copy
import com.winlator.xenvironment.ImageFs
import java.io.File
import java.util.regex.Pattern

class WineInfo : Parcelable {

    companion object {
        val MAIN_WINE_VERSION: WineInfo = WineInfo("9.2", "x86_64")

        private val pattern: Pattern = Pattern.compile("^wine\\-([0-9\\.]+)\\-?([0-9\\.]+)?\\-(x86|x86_64)$")

        @JvmField
        val CREATOR = object : Parcelable.Creator<WineInfo> {
            override fun createFromParcel(parcel: Parcel): WineInfo {
                return WineInfo(parcel)
            }

            override fun newArray(size: Int): Array<WineInfo?> {
                return arrayOfNulls(size)
            }
        }

        fun fromIdentifier(context: Context, identifier: String): WineInfo {
            if (identifier == MAIN_WINE_VERSION.identifier()) {
                return MAIN_WINE_VERSION
            }

            val matcher = pattern.matcher(identifier)

            if (matcher.find()) {
                val installedWineDir = ImageFs.find(context).installedWineDir
                val path = (File(installedWineDir, identifier)).path

                return WineInfo(matcher.group(1), matcher.group(2), matcher.group(3), path)
            } else {
                return MAIN_WINE_VERSION
            }
        }

        fun isMainWineVersion(wineVersion: String?): Boolean {
            return wineVersion == null || wineVersion == MAIN_WINE_VERSION.identifier()
        }
    }

    val version: String?
    val subversion: String?
    val path: String?
    var arch: String?

    constructor(version: String?, arch: String?) {
        this.version = version
        this.subversion = null
        this.arch = arch
        this.path = null
    }

    constructor(version: String?, subversion: String?, arch: String?, path: String?) {
        this.version = version
        this.subversion = if (!subversion.isNullOrEmpty()) subversion else null
        this.arch = arch
        this.path = path
    }

    private constructor(`in`: Parcel) {
        version = `in`.readString()
        subversion = `in`.readString()
        arch = `in`.readString()
        path = `in`.readString()
    }

    val isWin64: Boolean
        get() = arch == "x86_64"

    fun getExecutable(context: Context, wow64Mode: Boolean): String {
        if (this == MAIN_WINE_VERSION) {
            val wineBinDir = File(ImageFs.find(context).rootDir, "/opt/wine/bin")
            val wineBinFile = File(wineBinDir, "wine")
            val winePreloaderBinFile = File(wineBinDir, "wine-preloader")

            copy(File(wineBinDir, if (wow64Mode) "wine-wow64" else "wine32"), wineBinFile)
            copy(File(wineBinDir, if (wow64Mode) "wine-preloader-wow64" else "wine32-preloader"), winePreloaderBinFile)

            chmod(wineBinFile, 505)
            chmod(winePreloaderBinFile, 505)

            return if (wow64Mode) {
                "wine"
            } else {
                "wine64"
            }
        } else {
            return if ((File(path, "/bin/wine64")).isFile) {
                "wine64"
            } else {
                "wine"
            }
        }
    }

    fun identifier(): String = "wine-" + fullVersion() + "-" + (if (this == MAIN_WINE_VERSION) "custom" else arch)

    fun fullVersion(): String = version + (if (subversion != null) "-$subversion" else "")

    override fun toString(): String = "Wine " + fullVersion() + (if (this == MAIN_WINE_VERSION) " (Custom)" else "")

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(version)
        dest.writeString(subversion)
        dest.writeString(arch)
        dest.writeString(path)
    }
}
