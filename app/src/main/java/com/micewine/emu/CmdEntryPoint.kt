package com.micewine.emu

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityThread
import android.app.IActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.system.Os
import android.util.Log
import android.view.Surface
import androidx.annotation.Keep
import java.io.DataInputStream
import java.io.OutputStream
import java.io.PrintStream
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.system.exitProcess

@Keep
@SuppressLint("StaticFieldLeak", "UnsafeDynamicallyLoadedCode")
class CmdEntryPoint internal constructor(args: Array<String>?, context: Context) : ICmdEntryInterface.Stub() {
    private val intent = createIntent()

    @SuppressLint("WrongConstant", "PrivateApi")
    private fun createIntent(): Intent {
        var targetPackage = Os.getenv("TERMUX_X11_OVERRIDE_PACKAGE")
        if (targetPackage == null) targetPackage = "com.micewine.emu"
        // We should not care about multiple instances, it should be called only by `Termux:X11` app
        // which is single instance...
        val bundle = Bundle()
        bundle.putBinder(null, this)

        val intent = Intent(ACTION_START)
        intent.putExtra(null, bundle)
        intent.setPackage(targetPackage)

        if (Os.getuid() == 0 || Os.getuid() == 2000) intent.setFlags(0x00400000 /* FLAG_RECEIVER_FROM_SHELL */)

        return intent
    }

    private fun sendBroadcast(context: Context) {
        val bundle = Bundle()
        bundle.putBinder("", this)
        val intent = Intent(ACTION_START)
        intent.putExtra("", bundle)

        context.sendBroadcast(intent)
    }

    // In some cases Android Activity part can not connect opened port.
    // In this case opened port works like a lock file.
    private fun sendBroadcastDelayed(context: Context) {
        if (!connected()) sendBroadcast(context)

        handler.postDelayed({ this.sendBroadcastDelayed(context) }, 1000)
    }

    private fun spawnListeningThread(context: Context) {
        Thread {
            // New thread is needed to avoid android.os.NetworkOnMainThreadException
            /*
                The purpose of this function is simple. If the application has not been launched
                before running micewine-emu, the initial sendBroadcast had no effect because no one
                received the intent. To allow the application to reconnect freely, we will listen on
                port `PORT` and when receiving a magic phrase, we will send another intent.
             */Log.e("CmdEntryPoint", "Listening port $PORT")
            try {
                ServerSocket(PORT, 0, InetAddress.getByName("127.0.0.1")).use { listeningSocket ->
                    listeningSocket.reuseAddress = true
                    while (true) {
                        try {
                            listeningSocket.accept().use { client ->
                                Log.e("CmdEntryPoint", "Somebody connected!")
                                // We should ensure that it is some
                                val b = ByteArray(MAGIC.size)
                                val reader = DataInputStream(client.getInputStream())
                                reader.readFully(b)
                                if (MAGIC.contentEquals(b)) {
                                    Log.e("CmdEntryPoint", "New client connection!")
                                    context.sendBroadcast(intent)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace(System.err)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace(System.err)
            }
        }.start()
    }

    external override fun windowChanged(surface: Surface)
    external override fun getXConnection(): ParcelFileDescriptor
    external override fun getLogcatOutput(): ParcelFileDescriptor
    private external fun listenForConnections()

    init {
        if (!start(args)) exitProcess(1)

        spawnListeningThread(context)
        sendBroadcastDelayed(context)
    }

    companion object {
        const val ACTION_START: String = "com.micewine.emu.CmdEntryPoint.ACTION_START"
        const val PORT = 7892
        val MAGIC = "0xDEADBEEF".toByteArray()
        @JvmField
        val handler: Handler
        var ctx: Context?

        /**
         * Command-line entry point.
         *
         * @param args The command-line arguments
         */
        @JvmStatic
        fun main(args: Array<String>) {
            handler.post { CmdEntryPoint(args, createContext()!!) }
            Looper.loop()
        }

        @SuppressLint("PrivateApi")
        @JvmStatic
        fun sendBroadcast(intent: Intent) {
            try {
                ctx!!.sendBroadcast(intent)
            } catch (e: Exception) {
                if (e is NullPointerException && ctx == null) Log.i(
                    "Broadcast",
                    "Context is null, falling back to manual broadcasting"
                )
                else Log.e(
                    "Broadcast",
                    "Falling back to manual broadcasting, failed to broadcast intent through Context:",
                    e
                )

                val packageName: String
                try {
                    packageName = ActivityThread.getPackageManager()
                        .getPackagesForUid(Os.getuid())[0]
                } catch (ex: RemoteException) {
                    throw RuntimeException(ex)
                }
                val am = try {
                    ActivityManager::class.java
                        .getMethod("getService")
                        .invoke(null) as IActivityManager
                } catch (e2: Exception) {
                    try {
                        Class.forName("android.app.ActivityManagerNative")
                            .getMethod("getDefault")
                            .invoke(null) as IActivityManager
                    } catch (e3: Exception) {
                        throw RuntimeException(e3)
                    }
                }

                val sender = am.getIntentSender(
                    1, packageName, null, null, 0, arrayOf(intent),
                    null, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_ONE_SHOT, null, 0
                )
                try {
                    IIntentSender::class.java
                        .getMethod(
                            "send",
                            Int::class.javaPrimitiveType,
                            Intent::class.java,
                            String::class.java,
                            IBinder::class.java,
                            IIntentReceiver::class.java,
                            String::class.java,
                            Bundle::class.java
                        )
                        .invoke(sender, 0, intent, null, null, object : IIntentReceiver.Stub() {
                            override fun performReceive(
                                i: Intent,
                                r: Int,
                                d: String,
                                e: Bundle,
                                o: Boolean,
                                s: Boolean,
                                a: Int
                            ) {
                            }
                        }, null, null)
                } catch (ex: Exception) {
                    throw RuntimeException(ex)
                }
            }
        }

        /** @noinspection DataFlowIssue
         */
        @SuppressLint("DiscouragedPrivateApi")
        fun createContext(): Context? {
            var context: Context?
            val err = System.err
            try {
                val f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
                f.isAccessible = true
                val unsafe = f[null]
                // Hiding harmless framework errors, like this:
                // java.io.FileNotFoundException: /data/system/theme_config/theme_compatibility.xml: open failed: ENOENT (No such file or directory)
                System.setErr(PrintStream(object : OutputStream() {
                    override fun write(arg0: Int) {}
                }))
                context = if (System.getenv("OLD_CONTEXT") != null) {
                    ActivityThread.systemMain().systemContext
                } else {
                    (Class.forName
                        ("sun.misc.Unsafe").getMethod
                        ("allocateInstance", Class::class.java).invoke
                        (unsafe, ActivityThread::class.java) as ActivityThread)
                        .systemContext
                }
            } catch (e: Exception) {
                Log.e("Context", "Failed to instantiate context:", e)
                context = null
            } finally {
                System.setErr(err)
            }
            return context
        }

        @JvmStatic
        external fun start(args: Array<String>?): Boolean

        @JvmStatic
        private external fun connected(): Boolean

        init {
            try {
                if (Looper.getMainLooper() == null) Looper.prepareMainLooper()
            } catch (e: Exception) {
                Log.e("CmdEntryPoint", "Something went wrong when preparing MainLooper", e)
            }
            handler = Handler()
            ctx = createContext()

            val path = "lib/" + Build.SUPPORTED_ABIS[0] + "/libXlorie.so"
            val loader = CmdEntryPoint::class.java.classLoader
            val res = loader?.getResource(path)
            val libPath = res?.file?.replace("file:", "")?.replace("-v8a", "")?.replace("/base.apk!", "")
            if (libPath != null) {
                try {
                    System.load(libPath)
                } catch (e: Exception) {
                    Log.e("CmdEntryPoint", "Failed to dlopen $libPath", e)
                    System.err.println("Failed to load native library. Did you install the right apk? Try the universal one.")
                    exitProcess(134)
                }
            }
        }
    }
}
