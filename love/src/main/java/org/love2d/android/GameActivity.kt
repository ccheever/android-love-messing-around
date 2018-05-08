package org.love2d.android

import org.libsdl.app.SDLActivity

import java.util.Arrays
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.PowerManager
import android.os.ResultReceiver
import android.os.Vibrator
import android.support.annotation.Keep
import android.util.Log
import android.util.DisplayMetrics
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import android.view.*
import android.content.pm.PackageManager

class GameActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this.applicationContext
        val tv = TextView(context)
        tv.text = stringFromJNI()
        tv.setTextColor(Color.WHITE)
        tv.setTextSize(2, 25f)
        val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        lp.width = 300
        lp.height = 300
        lp.topMargin = 50
        lp.leftMargin = 20
        tv.layoutParams = lp
        setContentView(tv)
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}

/*
internal class GameActivity2 : SDLActivity() {

    // The API getWindow().getDecorView().setSystemUiVisibility() was
    // added in Android 11 (a.k.a. Honeycomb, a.k.a. 3.0.x). If we run
    // on this we do nothing.
    var immersiveMode: Boolean
        get() = immersiveActive
        set(immersive_mode) {
            if (android.os.Build.VERSION.SDK_INT < 11) {
                return
            }

            immersiveActive = immersive_mode

            val lock = Any()
            val immersive_enabled = immersive_mode
            synchronized(lock) {
                runOnUiThread {
                    synchronized(lock) {
                        if (immersive_mode) {
                            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                        } else {
                            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        }

                        lock.notify()
                    }
                }
            }
        }

    override fun getLibraries(): Array<String> {
        return arrayOf("gnustl_shared", "mpg123", "openal", "love")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("GameActivity", "started")

        context = this.applicationContext

        val permission = "android.permission.VIBRATE"
        val res = context!!.checkCallingOrSelfPermission(permission)
        if (res == PackageManager.PERMISSION_GRANTED) {
            vibrator = context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        } else {
            Log.d("GameActivity", "Vibration disabled: could not get vibration permission.")
        }

        handleIntent(this.intent)

        super.onCreate(savedInstanceState)
        windowManager.defaultDisplay.getMetrics(metrics)
    }

    override fun onNewIntent(intent: Intent) {
        Log.d("GameActivity", "onNewIntent() with $intent")
        handleIntent(intent)
        resetNative()
        startNative()
    }

    protected fun handleIntent(intent: Intent) {
        val game = intent.data

        if (game != null) {
            // If we have a game via the intent data we we try to figure out how we have to load it. We
            // support the following variations:
            // * a main.lua file: set gamePath to the directory containing main.lua
            // * otherwise: set gamePath to the file
            if (game.scheme == "file") {
                Log.d("GameActivity", "Received intent with path: " + game.path)
                // If we were given the path of a main.lua then use its
                // directory. Otherwise use full path.
                val path_segments = game.pathSegments
                if (path_segments[path_segments.size - 1] == "main.lua") {
                    gamePath = game.path.substring(0, game.path.length - "main.lua".length)
                } else {
                    gamePath = game.path
                }
            } else {
                Log.e("GameActivity", "Unsupported scheme: '" + game.scheme + "'.")

                val alert_dialog = AlertDialog.Builder(this)
                alert_dialog.setMessage("Could not load LÖVE game '" + game.path
                        + "' as it uses unsupported scheme '" + game.scheme
                        + "'. Please contact the developer.")
                alert_dialog.setTitle("LÖVE for Android Error")
                alert_dialog.setPositiveButton("Exit"
                ) { dialog, id -> finish() }
                alert_dialog.setCancelable(false)
                alert_dialog.create().show()
            }
        } else {
            // No game specified via the intent data -> check whether we have a game.love in our assets.
            var game_love_in_assets = false
            try {
                val assets = Arrays.asList(*assets.list(""))
                game_love_in_assets = assets.contains("game.love")
            } catch (e: Exception) {
                Log.d("GameActivity", "could not list application assets:" + e.message)
            }

            if (game_love_in_assets) {
                // If we have a game.love in our assets folder copy it to the cache folder
                // so that we can load it from native LÖVE code
                val destination_file = this.cacheDir.path + "/game.love"
                if (mustCacheArchive && copyAssetFile("game.love", destination_file))
                    gamePath = destination_file
                else
                    gamePath = "game.love"
            } else {
                // If no game.love was found fall back to the game in <external storage>/lovegame
                val ext = Environment.getExternalStorageDirectory()
                if (File(ext, "/lovegame/main.lua").exists()) {
                    gamePath = ext.path + "/lovegame/"
                }
            }
        }

        Log.d("GameActivity", "new gamePath: $gamePath")
    }

    override fun onDestroy() {
        if (vibrator != null) {
            Log.d("GameActivity", "Cancelling vibration")
            vibrator!!.cancel()
        }
        super.onDestroy()
    }

    override fun onPause() {
        if (vibrator != null) {
            Log.d("GameActivity", "Cancelling vibration")
            vibrator!!.cancel()
        }
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()

        if (immersiveActive) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    /**
     * Copies a given file from the assets folder to the destination.
     *
     * @return true if successful
     */
    fun copyAssetFile(fileName: String, destinationFileName: String): Boolean {
        var success = false

        // open source and destination streams
        var source_stream: InputStream? = null
        try {
            source_stream = assets.open(fileName)
        } catch (e: IOException) {
            Log.d("GameActivity", "Could not open game.love from assets: " + e.message)
        }

        var destination_stream: BufferedOutputStream? = null
        try {
            destination_stream = BufferedOutputStream(FileOutputStream(destinationFileName, false))
        } catch (e: IOException) {
            Log.d("GameActivity", "Could not open destination file: " + e.message)
        }

        // perform the copying
        var chunk_read = 0
        var bytes_written = 0

        assert(source_stream != null && destination_stream != null)

        try {
            val buf = ByteArray(1024)
            chunk_read = source_stream!!.read(buf)
            do {
                destination_stream!!.write(buf, 0, chunk_read)
                bytes_written += chunk_read
                chunk_read = source_stream.read(buf)
            } while (chunk_read != -1)
        } catch (e: IOException) {
            Log.d("GameActivity", "Copying failed:" + e.message)
        }

        // close streams
        try {
            if (source_stream != null) source_stream.close()
            if (destination_stream != null) destination_stream.close()
            success = true
        } catch (e: IOException) {
            Log.d("GameActivity", "Copying failed: " + e.message)
        }

        Log.d("GameActivity", "Successfully copied " + fileName
                + " to " + destinationFileName
                + " (" + bytes_written + " bytes written).")
        return success
    }

    @Keep
    fun hasBackgroundMusic(): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.isMusicActive
    }

    companion object {
        val metrics = DisplayMetrics()
        private var gamePath = ""
        private var context: Context? = null
        private var vibrator: Vibrator? = null
        private var immersiveActive = false
        private val mustCacheArchive = false

        fun getGamePath(): String {
            Log.d("GameActivity", "called getGamePath(), game path = $gamePath")
            return gamePath
        }

        fun vibrate(seconds: Double) {
            if (vibrator != null) {
                vibrator!!.vibrate((seconds * 1000.0).toLong())
            }
        }

        fun openURL(url: String) {
            Log.d("GameActivity", "opening url = $url")
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context!!.startActivity(i)
        }
    }
}
*/