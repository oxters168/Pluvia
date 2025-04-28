package com.micewine.emu.fragments

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.micewine.emu.R
import com.micewine.emu.activities.MainActivity.Companion.deviceArch
import com.micewine.emu.activities.MainActivity.Companion.tmpDir
import com.micewine.emu.adapters.AdapterRatPackage
import com.micewine.emu.core.RatPackageManager.checkPackageInstalled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class RatDownloaderFragment(private val prefix: String, private val type: Int, private val anotherPrefix: String = "?") : Fragment() {
    private val ratList: MutableList<AdapterRatPackage.Item> = mutableListOf()
    private var rootView: View? = null
    private var recyclerView: RecyclerView? = null
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_general_settings, container, false)
        recyclerView = rootView?.findViewById(R.id.recyclerViewGeneralSettings)

        setAdapter()

        return rootView
    }

    private fun setAdapter() {
        recyclerView?.setAdapter(
            AdapterRatPackage(ratList, requireActivity(), true)
        )

        ratList.clear()

        lifecycleScope.launch {
            val packages = fetchPackages()

            if (packages != null) {
                packages.forEach {
                    if (it.value.category == prefix || it.value.category == anotherPrefix) {
                        addToAdapter(it.value.name, it.value.version, "", false, it.key, checkPackageInstalled(it.value.name, it.value.category, it.value.version))
                    }
                }

                recyclerView?.adapter?.notifyItemRangeChanged(0, packages.size - 1)
            }
        }
    }

    private fun addToAdapter(title: String, description: String, driverFolderId: String, canDelete: Boolean, repoRatName: String, installed: Boolean) {
        ratList.add(
            AdapterRatPackage.Item(title, description, driverFolderId, type, canDelete, repoRatName, installed)
        )
    }

    private suspend fun fetchPackages(): Map<String, RatPackageModel>? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder().url("https://github.com/KreitinnSoftware/MiceWine-Repository/releases/download/default/index.json").build()
            val type = object : TypeToken<Map<String, RatPackageModel>>() {}.type

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext gson.fromJson<Map<String, RatPackageModel>?>(response.body?.string()!!, type).toSortedMap().filter { it.value.architecture == "any" || it.value.architecture == deviceArch }
                    }
                }
            } catch (_: IOException) {
            }
            return@withContext null
        }
    }

    data class RatPackageModel(
        val name: String,
        val category: String,
        val version: String,
        val architecture: String,
        val vkDriverLib: String
    )

    companion object {
        interface ProgressListener {
            fun onProgress(bytesRead: Long, contentLength: Long, done: Boolean)
        }

        class ProgressResponseBody(
            private val responseBody: ResponseBody,
            private val progressListener: ProgressListener
        ) : ResponseBody() {

            private var bufferedSource: BufferedSource? = null

            override fun contentType(): MediaType? = responseBody.contentType()

            override fun contentLength(): Long = responseBody.contentLength()

            override fun source(): BufferedSource {
                if (bufferedSource == null) {
                    bufferedSource = source(responseBody.source()).buffer()
                }
                return bufferedSource!!
            }

            private fun source(source: Source): Source {
                return object : ForwardingSource(source) {
                    var totalBytesRead = 0L

                    override fun read(sink: Buffer, byteCount: Long): Long {
                        val bytesRead = super.read(sink, byteCount)
                        totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                        progressListener.onProgress(totalBytesRead, responseBody.contentLength(), bytesRead == -1L)
                        return bytesRead
                    }
                }
            }
        }

        suspend fun downloadPackage(name: String, progressBar: ProgressBar, activity: Activity): Boolean {
            activity.runOnUiThread {
                progressBar.visibility = View.VISIBLE
            }

            return withContext(Dispatchers.IO) {
                val client = OkHttpClient.Builder()
                    .addNetworkInterceptor { chain ->
                        val originalResponse = chain.proceed(chain.request())
                        originalResponse.newBuilder()
                            .body(ProgressResponseBody(originalResponse.body!!, object : ProgressListener {
                                override fun onProgress(bytesRead: Long, contentLength: Long, done: Boolean) {
                                    val progress = if (contentLength > 0) {
                                        (bytesRead * 100 / contentLength).toInt()
                                    } else 0

                                    activity.runOnUiThread {
                                        progressBar.progress = progress
                                    }
                                }
                            }))
                            .build()
                    }
                    .build()

                val request = Request.Builder().url("https://github.com/KreitinnSoftware/MiceWine-Repository/releases/download/default/$name").build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext false
                        val file = File("$tmpDir/$name")
                        response.body?.byteStream()?.use { input ->
                            FileOutputStream(file).use { output -> input.copyTo(output) }
                        }
                        return@withContext true
                    }
                } catch (_: IOException) {
                    false
                } finally {
                    activity.runOnUiThread {
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }
}
