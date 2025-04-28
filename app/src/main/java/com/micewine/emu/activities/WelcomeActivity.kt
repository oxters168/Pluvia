package com.micewine.emu.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.micewine.emu.R
import com.micewine.emu.activities.MainActivity.Companion.ACTION_SETUP
import com.micewine.emu.activities.MainActivity.Companion.appBuiltinRootfs
import com.micewine.emu.adapters.AdapterWelcomeFragments
import com.micewine.emu.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {
    private var binding: ActivityWelcomeBinding? = null
    private var button: FloatingActionButton? = null
    private var viewPager: ViewPager2? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        viewPager = findViewById(R.id.viewPager)
        viewPager?.isUserInputEnabled = false
        viewPager?.adapter = AdapterWelcomeFragments(this)


        button = findViewById(R.id.continueButton)
        button?.setOnClickListener {
            if (viewPager!!.currentItem == 0) {
                if (!isStoragePermissionGranted(this)) {
                    requestPermission()
                }
            }

            when (viewPager!!.currentItem) {
                1 -> {
                    if (appBuiltinRootfs) {
                        sendBroadcast(
                            Intent(ACTION_SETUP)
                        )
                    }

                    finish()
                }
            }

            viewPager!!.currentItem++
        }
    }

    override fun onResume() {
        super.onResume()

        if (viewPager!!.currentItem == 1 && !isStoragePermissionGranted(this)) {
            button?.isEnabled = false

            requestPermission()

            Toast.makeText(this, getString(R.string.grant_files_permission_error), Toast.LENGTH_SHORT).show()
        } else {
            button?.isEnabled = true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KEYCODE_BACK) {
            viewPager!!.currentItem--
            button?.isEnabled = true

            return true
        }

        finishAffinity()

        return super.onKeyDown(keyCode, event)
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.setData(uri)
                startActivity(intent)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
            }
        }
    }

    private fun isStoragePermissionGranted(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()

            else -> {
                val readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                val writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 1000
    }
}