package com.example.vortexmusic

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlin.math.abs

// ================== NEON ZEMİN ==================

class NeonBackgroundDrawable(
    private val top: Int,
    private val bottom: Int,
    private val purple: Int,
    private val blue: Int,
    private val density: Float
) : Drawable() {

    private val fillPaint = Paint()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha shl 24)

    private fun glowLine(
        canvas: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int
    ) {
        val widths = floatArrayOf(14f, 8f, 4f, 1.6f)
        val alphas = intArrayOf(14, 28, 56, 140)
        for (i in widths.indices) {
            linePaint.strokeWidth = widths[i] * density
            linePaint.color = withAlpha(color, alphas[i])
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        fillPaint.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(bounds, fillPaint)
        glowLine(canvas, -0.05f * w, 0.19f * h, 1.05f * w, 0.00f * h, purple)
        glowLine(canvas, -0.05f * w, 0.25f * h, 1.05f * w, 0.06f * h, purple)
        glowLine(canvas, -0.05f * w, 0.86f * h, 0.55f * w, 1.04f * h, blue)
        glowLine(canvas, -0.05f * w, 0.93f * h, 0.30f * w, 1.04f * h, blue)
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

// ================== MİNİMALİST İKONLAR ==================

class IconDrawable(
    private val kind: Int,
    private val tint: Int,
    private val sizePx: Int,
    private val filled: Boolean = false,
    private val glowColor: Int = 0
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = buildPath()

    override fun getIntrinsicWidth(): Int = sizePx
    override fun getIntrinsicHeight(): Int = sizePx

    override fun draw(canvas: Canvas) {
        val w = bounds.width()
        val h = bounds.height()
        if (w <= 0 || h <= 0) return
        val s = minOf(w, h) / 24f
        canvas.save()
        canvas.translate(
            bounds.left + (w - 24f * s) / 2f,
            bounds.top + (h - 24f * s) / 2f
        )
        canvas.scale(s, s)
        val base = if (kind == PAUSE) 3.2f else 1.9f
        val doFill = filled && (kind == PLAY || kind == HEART || kind == NEXT || kind == PREV)
        if (glowColor != 0) {
            drawPass(canvas, glowColor, base + 4.5f, 40, false)
            drawPass(canvas, glowColor, base + 2.2f, 80, false)
        }
        drawPass(canvas, tint, base, 255, doFill)
        canvas.restore()
    }

    private fun drawPass(canvas: Canvas, color: Int, width: Float, alpha: Int, fill: Boolean) {
        paint.color = color
        paint.alpha = alpha
        paint.strokeWidth = width
        paint.style = if (fill) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
        canvas.drawPath(path, paint)
    }

    private fun buildPath(): Path {
        val p = Path()
        when (kind) {
            HOME -> {
                p.moveTo(3.5f, 11f); p.lineTo(12f, 4f); p.lineTo(20.5f, 11f)
                p.moveTo(6f, 9.5f); p.lineTo(6f, 20f); p.lineTo(18f, 20f); p.lineTo(18f, 9.5f)
            }
            SEARCH -> {
                p.addCircle(10.5f, 10.5f, 6.5f, Path.Direction.CW)
                p.moveTo(15.5f, 15.5f); p.lineTo(20.5f, 20.5f)
            }
            NOTE -> {
                p.moveTo(9.5f, 17.5f); p.lineTo(9.5f, 6f); p.lineTo(18.5f, 4f); p.lineTo(18.5f, 15.5f)
                p.addCircle(7f, 17.5f, 2.5f, Path.Direction.CW)
                p.addCircle(16f, 15.5f, 2.5f, Path.Direction.CW)
            }
            HEART -> {
                p.moveTo(12f, 20.5f)
                p.cubicTo(5f, 15f, 3f, 11.5f, 3f, 8.5f)
                p.cubicTo(3f, 5.7f, 5f, 4f, 7.2f, 4f)
                p.cubicTo(9.2f, 4f, 11f, 5.2f, 12f, 7f)
                p.cubicTo(13f, 5.2f, 14.8f, 4f, 16.8f, 4f)
                p.cubicTo(19f, 4f, 21f, 5.7f, 21f, 8.5f)
                p.cubicTo(21f, 11.5f, 19f, 15f, 12f, 20.5f)
                p.close()
            }
            PLAY -> {
                p.moveTo(8f, 5f); p.lineTo(19f, 12f); p.lineTo(8f, 19f); p.close()
            }
            PAUSE -> {
                p.moveTo(8.5f, 5.5f); p.lineTo(8.5f, 18.5f)
                p.moveTo(15.5f, 5.5f); p.lineTo(15.5f, 18.5f)
            }
            NEXT -> {
                p.moveTo(6f, 5.5f); p.lineTo(15.5f, 12f); p.lineTo(6f, 18.5f); p.close()
                p.moveTo(18.5f, 5.5f); p.lineTo(18.5f, 18.5f)
            }
            PREV -> {
                p.moveTo(18f, 5.5f); p.lineTo(8.5f, 12f); p.lineTo(18f, 18.5f); p.close()
                p.moveTo(5.5f, 5.5f); p.lineTo(5.5f, 18.5f)
            }
            SHUFFLE -> {
                p.moveTo(3f, 7f); p.lineTo(7f, 7f)
                p.cubicTo(12.5f, 7f, 11f, 17f, 16.5f, 17f); p.lineTo(20.5f, 17f)
                p.moveTo(3f, 17f); p.lineTo(7f, 17f)
                p.cubicTo(8.7f, 17f, 9.9f, 16.1f, 10.8f, 14.8f)
                p.moveTo(13.2f, 9.2f)
                p.cubicTo(14.1f, 7.9f, 15.3f, 7f, 16.5f, 7f); p.lineTo(20.5f, 7f)
                p.moveTo(18f, 14.5f); p.lineTo(20.5f, 17f); p.lineTo(18f, 19.5f)
                p.moveTo(18f, 4.5f); p.lineTo(20.5f, 7f); p.lineTo(18f, 9.5f)
            }
            REPEAT -> {
                p.moveTo(4f, 11f); p.lineTo(4f, 9f)
                p.cubicTo(4f, 7.3f, 5.3f, 6f, 7f, 6f); p.lineTo(20f, 6f)
                p.moveTo(17f, 3f); p.lineTo(20f, 6f); p.lineTo(17f, 9f)
                p.moveTo(20f, 13f); p.lineTo(20f, 15f)
                p.cubicTo(20f, 16.7f, 18.7f, 18f, 17f, 18f); p.lineTo(4f, 18f)
                p.moveTo(7f, 15f); p.lineTo(4f, 18f); p.lineTo(7f, 21f)
            }
            FOLDER, FOLDER_PLUS -> {
                p.moveTo(3f, 7f); p.lineTo(3f, 18.5f)
                p.cubicTo(3f, 19.3f, 3.7f, 20f, 4.5f, 20f); p.lineTo(19.5f, 20f)
                p.cubicTo(20.3f, 20f, 21f, 19.3f, 21f, 18.5f); p.lineTo(21f, 9.5f)
                p.cubicTo(21f, 8.7f, 20.3f, 8f, 19.5f, 8f); p.lineTo(11f, 8f)
                p.lineTo(9f, 5.5f); p.lineTo(4.5f, 5.5f)
                p.cubicTo(3.7f, 5.5f, 3f, 6.2f, 3f, 7f)
                p.close()
                if (kind == FOLDER_PLUS) {
                    p.moveTo(12f, 11.5f); p.lineTo(12f, 17f)
                    p.moveTo(9.25f, 14.25f); p.lineTo(14.75f, 14.25f)
                }
            }
            PLUS -> {
                p.moveTo(12f, 5f); p.lineTo(12f, 19f)
                p.moveTo(5f, 12f); p.lineTo(19f, 12f)
            }
            ARTIST -> {
                p.addCircle(12f, 8f, 3.8f, Path.Direction.CW)
                p.moveTo(5f, 20f)
                p.cubicTo(5f, 15.5f, 8f, 14f, 12f, 14f)
                p.cubicTo(16f, 14f, 19f, 15.5f, 19f, 20f)
            }
            DISC -> {
                p.addCircle(12f, 12f, 8.5f, Path.Direction.CW)
                p.addCircle(12f, 12f, 2.5f, Path.Direction.CW)
            }
            DOWNLOAD -> {
                p.moveTo(12f, 4f); p.lineTo(12f, 15f)
                p.moveTo(7f, 10.5f); p.lineTo(12f, 15.5f); p.lineTo(17f, 10.5f)
                p.moveTo(5f, 20f); p.lineTo(19f, 20f)
            }
            CLOCK -> {
                p.addCircle(12f, 13.5f, 7.5f, Path.Direction.CW)
                p.moveTo(12f, 13.5f); p.lineTo(12f, 9.5f)
                p.moveTo(9.5f, 3.5f); p.lineTo(14.5f, 3.5f)
            }
            CHEVRON -> {
                p.moveTo(15f, 5f); p.lineTo(8f, 12f); p.lineTo(15f, 19f)
            }
        }
        return p
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        const val HOME = 0
        const val SEARCH = 1
        const val NOTE = 2
        const val HEART = 3
        const val PLAY = 4
        const val PAUSE = 5
        const val NEXT = 6
        const val PREV = 7
        const val SHUFFLE = 8
        const val REPEAT = 9
        const val FOLDER = 10
        const val FOLDER_PLUS = 11
        const val PLUS = 12
        const val ARTIST = 13
        const val DISC = 14
        const val DOWNLOAD = 15
        const val CLOCK = 16
        const val CHEVRON = 17
    }
}

// ================== ARKA PLAN ÇALMA SERVİSİ ==================

class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    inner class LocalBinder : Binder() {
        fun getPlayer(): ExoPlayer? = exoPlayer
    }

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer = exo
        val newSession = MediaSession.Builder(this, exo).build()
        session = newSession
        addSession(newSession)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onBind(intent: Intent?): IBinder? {
        return if (intent?.action == ACTION_LOCAL) LocalBinder() else super.onBind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = exoPlayer
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.release()
        exoPlayer?.release()
        session = null
        exoPlayer = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_LOCAL = "com.example.vortexmusic.LOCAL"
    }
}

// ================== ANA EKRAN ==================

class MainActivity : Activity() {

    private class Song(val id: Long, val title: String, val artist: String, val album: String) {
        val uri: Uri
            get() = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
            )
    }

    // iconStyle: 0 neon gradyanlı kare, 1 koyu kare, 2 koyu daire
    private class Row(
        val kind: Int,
        val iconStyle: Int,
        val title: String,
        val subtitle: String,
        val enabled: Boolean,
        val key: String,
        val onClick: () -> Unit
    )

    private val songs = mutableListOf<Song>()
    private val shown = mutableListOf<Song>()
    private val libRows = mutableListOf<Row>()
    private val folders = linkedMapOf<String, MutableSet<Long>>()
    private var loaded = false
    private var mode = 0 // 0 ana sayfa, 1 ara, 2 kütüphane
    // Kütüphane: 0 ana, 1 beğenilenler, 2 sanatçılar, 3 sanatçı şarkıları,
    // 4 albümler, 5 albüm şarkıları, 6 klasörler, 7 klasör şarkıları
    private var libView = 0
    private var selectedGroup = ""
    private var player: ExoPlayer? = null
    private val adapter = SongAdapter()
    private val libAdapter = RowAdapter()

    private val favorites = mutableSetOf<Long>()
    private val speeds = floatArrayOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private val chipLabels = arrayOf("Tümü", "Beğenilenler", "Klasörler", "Sanatçılar", "Albümler")
    private val chipTargets = intArrayOf(0, 1, 6, 2, 4)

    private lateinit var statusText: TextView
    private lateinit var searchBox: EditText
    private lateinit var searchButton: ImageView
    private lateinit var chipScroll: HorizontalScrollView
    private lateinit var chipContainer: LinearLayout
    private lateinit var nowTitle: TextView
    private lateinit var nowArtist: TextView
    private lateinit var songList: ListView
    private lateinit var playPauseButton: ImageView
    private lateinit var tabIcons: Array<ImageView>
    private lateinit var tabLabels: Array<TextView>

    private lateinit var nowPlayingScreen: LinearLayout
    private lateinit var npTitle: TextView
    private lateinit var npArtist: TextView
    private lateinit var npSeek: SeekBar
    private lateinit var npPosition: TextView
    private lateinit var npDuration: TextView
    private lateinit var npPlayPause: ImageView
    private lateinit var npLikeIcon: ImageView
    private lateinit var npLikeLabel: TextView
    private lateinit var npFolderIcon: ImageView
    private lateinit var npFolderLabel: TextView
    private lateinit var npShuffleIcon: ImageView
    private lateinit var npShuffleLabel: TextView
    private lateinit var npRepeatIcon: ImageView
    private lateinit var npRepeatLabel: TextView
    private lateinit var npSpeedIcon: ImageView
    private lateinit var npSpeedLabel: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var dragging = false
    private var backCallback: Any? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayIcons(isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateNowPlayingUi()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            updatePills()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            updatePills()
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val p = (service as? PlaybackService.LocalBinder)?.getPlayer() ?: return
            attachPlayer(p)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            player?.removeListener(playerListener)
            player = null
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            val p = player
            if (p != null && nowPlayingScreen.visibility == View.VISIBLE && !dragging) {
                val dur = p.duration
                if (dur > 0) {
                    npSeek.max = dur.toInt()
                    npSeek.progress = p.currentPosition.toInt()
                    npDuration.text = fmt(dur)
                }
                npPosition.text = fmt(p.currentPosition)
            }
            handler.postDelayed(this, 500)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun color(id: Int): Int = resources.getColor(id, theme)
    private fun fmt(ms: Long): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun icon(
        kind: Int,
        tint: Int,
        sizeDp: Int,
        filled: Boolean = false,
        glow: Boolean = false
    ): IconDrawable = IconDrawable(
        kind,
        tint,
        dp(sizeDp),
        filled,
        if (glow) color(R.color.vortex_purple) else 0
    )

    private fun neonGradient(oval: Boolean, radiusDp: Int = 16): GradientDrawable {
        val g = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(color(R.color.vortex_purple), color(R.color.vortex_blue))
        )
        if (oval) g.shape = GradientDrawable.OVAL else g.cornerRadius = dp(radiusDp).toFloat()
        return g
    }

    private fun circleBackground(active: Boolean): GradientDrawable {
        if (active) return neonGradient(true)
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color(R.color.vortex_surface_light))
        }
    }

    private fun pillBackground(active: Boolean): GradientDrawable {
        if (active) return neonGradient(false, 24)
        return GradientDrawable().apply {
            setColor(color(R.color.vortex_surface_light))
            cornerRadius = dp(24).toFloat()
        }
    }

    private fun neonBackground(): NeonBackgroundDrawable = NeonBackgroundDrawable(
        color(R.color.vortex_background),
        color(R.color.vortex_background_end),
        color(R.color.vortex_purple),
        color(R.color.vortex_blue),
        resources.displayMetrics.density
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadFavorites()
        loadFolders()

        val root = findViewById<LinearLayout>(R.id.root)
        statusText = findViewById(R.id.statusText)
        searchBox = findViewById(R.id.searchBox)
        searchButton = findViewById(R.id.searchButton)
        nowTitle = findViewById(R.id.nowTitle)
        nowArtist = findViewById(R.id.nowArtist)
        songList = findViewById(R.id.songList)
        playPauseButton = findViewById(R.id.playPauseButton)
        val miniPlayer = findViewById<LinearLayout>(R.id.miniPlayer)
        val navBar = findViewById<LinearLayout>(R.id.navBar)

        // Süzgeç düğmeleri satırı (Kütüphane için), arama kutusunun altına eklenir
        chipScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        chipContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        chipScroll.addView(chipContainer)
        root.addView(chipScroll, root.indexOfChild(searchBox) + 1)

        tabIcons = arrayOf<ImageView>(
            findViewById(R.id.iconHome),
            findViewById(R.id.iconLibrary)
        )
        tabLabels = arrayOf<TextView>(
            findViewById(R.id.labelHome),
            findViewById(R.id.labelLibrary)
        )
        findViewById<View>(R.id.tabHome).setOnClickListener { setMode(0) }
        findViewById<View>(R.id.tabLibrary).setOnClickListener { setMode(2) }
        searchButton.setOnClickListener { setMode(if (mode == 1) 0 else 1) }

        nowPlayingScreen = findViewById(R.id.nowPlayingScreen)
        npTitle = findViewById(R.id.npTitle)
        npArtist = findViewById(R.id.npArtist)
        npSeek = findViewById(R.id.npSeek)
        npPosition = findViewById(R.id.npPosition)
        npDuration = findViewById(R.id.npDuration)
        npPlayPause = findViewById(R.id.npPlayPause)
        npLikeIcon = findViewById(R.id.npLikeIcon)
        npLikeLabel = findViewById(R.id.npLikeLabel)
        npFolderIcon = findViewById(R.id.npFolderIcon)
        npFolderLabel = findViewById(R.id.npFolderLabel)
        npShuffleIcon = findViewById(R.id.npShuffleIcon)
        npShuffleLabel = findViewById(R.id.npShuffleLabel)
        npRepeatIcon = findViewById(R.id.npRepeatIcon)
        npRepeatLabel = findViewById(R.id.npRepeatLabel)
        npSpeedIcon = findViewById(R.id.npSpeedIcon)
        npSpeedLabel = findViewById(R.id.npSpeedLabel)
        val npClose = findViewById<ImageView>(R.id.npClose)
        val npArt = findViewById<ImageView>(R.id.npArt)
        val npPrev = findViewById<ImageView>(R.id.npPrev)
        val npNext = findViewById<ImageView>(R.id.npNext)

        root.background = neonBackground()
        nowPlayingScreen.background = neonBackground()

        root.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                dp(16),
                insets.systemWindowInsetTop + dp(12),
                dp(16),
                insets.systemWindowInsetBottom + dp(12)
            )
            insets
        }
        nowPlayingScreen.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                dp(24),
                insets.systemWindowInsetTop + dp(12),
                dp(24),
                insets.systemWindowInsetBottom + dp(24)
            )
            insets
        }

        miniPlayer.background = GradientDrawable().apply {
            setColor(color(R.color.vortex_surface))
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), color(R.color.vortex_purple))
        }
        navBar.background = GradientDrawable().apply {
            setColor(color(R.color.vortex_surface))
            cornerRadius = dp(26).toFloat()
            setStroke(dp(1), color(R.color.vortex_surface_light))
        }
        searchBox.background = GradientDrawable().apply {
            setColor(color(R.color.vortex_surface_light))
            cornerRadius = dp(24).toFloat()
        }
        playPauseButton.background = neonGradient(true)

        npArt.background = neonGradient(false, 36)
        npArt.setImageDrawable(icon(IconDrawable.NOTE, Color.WHITE, 64))
        npClose.setImageDrawable(icon(IconDrawable.CHEVRON, color(R.color.vortex_purple), 24))
        npPlayPause.background = neonGradient(true)
        npPrev.setImageDrawable(icon(IconDrawable.PREV, Color.WHITE, 24, filled = true))
        npNext.setImageDrawable(icon(IconDrawable.NEXT, Color.WHITE, 24, filled = true))
        npSeek.progressTintList = ColorStateList.valueOf(color(R.color.vortex_purple))
        npSeek.thumbTintList = ColorStateList.valueOf(color(R.color.vortex_blue))
        npSeek.progressBackgroundTintList = ColorStateList.valueOf(color(R.color.vortex_surface_light))

        playPauseButton.setOnClickListener { togglePlay() }
        npPlayPause.setOnClickListener { togglePlay() }

        npPrev.setOnClickListener { player?.seekToPrevious() }
        npNext.setOnClickListener { player?.seekToNext() }

        findViewById<View>(R.id.cellLike).setOnClickListener { toggleFavoriteId(currentId()) }
        findViewById<View>(R.id.cellFolder).setOnClickListener {
            if (currentId() >= 0) addToFolderDialog(currentId())
        }
        findViewById<View>(R.id.cellShuffle).setOnClickListener {
            val p = player ?: return@setOnClickListener
            p.shuffleModeEnabled = !p.shuffleModeEnabled
            updatePills()
        }
        findViewById<View>(R.id.cellRepeat).setOnClickListener {
            val p = player ?: return@setOnClickListener
            p.repeatMode = when (p.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            updatePills()
        }
        findViewById<View>(R.id.cellSpeed).setOnClickListener {
            val p = player ?: return@setOnClickListener
            p.setPlaybackSpeed(speeds[(currentSpeedIndex() + 1) % speeds.size])
            updatePills()
        }

        findViewById<View>(R.id.miniPlayer).setOnClickListener { showNowPlaying() }
        npClose.setOnClickListener { hideNowPlaying() }

        npSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) npPosition.text = fmt(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                dragging = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                dragging = false
                player?.seekTo((seekBar?.progress ?: 0).toLong())
            }
        })

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                refreshList()
            }
        })

        songList.adapter = adapter
        songList.setOnItemClickListener { _, _, position, _ ->
            if (songList.adapter === libAdapter) {
                libRows.getOrNull(position)?.onClick?.invoke()
            } else {
                playFromShown(position)
            }
        }
        songList.setOnItemLongClickListener { _, _, position, _ ->
            var handled = false
            if (songList.adapter === libAdapter) {
                val row = libRows.getOrNull(position)
                if (libView == 6 && row != null && row.key.isNotEmpty()) {
                    folderOptions(row.key)
                    handled = true
                }
            } else {
                val song = shown.getOrNull(position)
                if (song != null) {
                    songOptions(song)
                    handled = true
                }
            }
            handled
        }

        updateTabs()
        updatePlayIcons(false)
        updatePills()
        handler.post(ticker)

        // Geri tuşu (Android 13 ve üstü)
        if (Build.VERSION.SDK_INT >= 33) {
            val cb = android.window.OnBackInvokedCallback { handleBack() }
            backCallback = cb
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb
            )
        }

        // İzinler
        val needed = mutableListOf<String>()
        val audioOk = checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!audioOk) needed.add(Manifest.permission.READ_MEDIA_AUDIO)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (audioOk) loadSongs()
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 1)

        // Arka plan servisini başlat ve ona bağlan
        startService(Intent(this, PlaybackService::class.java))
        bindService(
            Intent(this, PlaybackService::class.java).setAction(PlaybackService.ACTION_LOCAL),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    // ---------- Çalar ----------

    private fun attachPlayer(p: ExoPlayer) {
        player?.removeListener(playerListener)
        player = p
        p.addListener(playerListener)
        updatePlayIcons(p.isPlaying)
        updateNowPlayingUi()
    }

    private fun currentId(): Long =
        player?.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L

    private fun toMediaItem(s: Song): MediaItem = MediaItem.Builder()
        .setMediaId(s.id.toString())
        .setUri(s.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(s.title)
                .setArtist(s.artist)
                .build()
        )
        .build()

    private fun playFromShown(position: Int) {
        val p = player ?: return
        if (position !in shown.indices) return
        p.setMediaItems(shown.map { toMediaItem(it) }, position, 0L)
        p.prepare()
        p.play()
    }

    private fun togglePlay() {
        val p = player ?: return
        if (p.mediaItemCount == 0) return
        if (p.isPlaying) {
            p.pause()
        } else {
            if (p.playbackState == Player.STATE_ENDED) p.seekToDefaultPosition(0)
            p.play()
        }
    }

    private fun currentSpeedIndex(): Int {
        val cur = player?.playbackParameters?.speed ?: 1f
        val idx = speeds.indexOfFirst { abs(it - cur) < 0.01f }
        return if (idx >= 0) idx else 1
    }

    // ---------- Gezinme ----------

    private fun setMode(m: Int) {
        mode = m
        searchBox.visibility = if (m == 1) View.VISIBLE else View.GONE
        chipScroll.visibility = if (m == 2) View.VISIBLE else View.GONE
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (m == 1) {
            searchBox.requestFocus()
            imm.showSoftInput(searchBox, 0)
        } else {
            imm.hideSoftInputFromWindow(searchBox.windowToken, 0)
        }
        updateTabs()
        refreshList()
    }

    private fun setLibView(v: Int) {
        libView = v
        refreshList()
    }

    private fun handleBack() {
        if (nowPlayingScreen.visibility == View.VISIBLE) {
            hideNowPlaying()
        } else if (mode == 2 && libView != 0) {
            setLibView(
                when (libView) {
                    3 -> 2
                    5 -> 4
                    7 -> 6
                    else -> 0
                }
            )
        } else if (mode != 0) {
            setMode(0)
        } else {
            finish()
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        handleBack()
    }

    private fun updateTabs() {
        val secondary = color(R.color.vortex_text_secondary)
        val blue = color(R.color.vortex_blue)
        val homeSel = mode == 0
        val libSel = mode == 2
        tabIcons[0].setImageDrawable(
            icon(IconDrawable.HOME, if (homeSel) Color.WHITE else secondary, 24, glow = homeSel)
        )
        tabIcons[1].setImageDrawable(
            icon(IconDrawable.NOTE, if (libSel) Color.WHITE else secondary, 24, glow = libSel)
        )
        tabLabels[0].setTextColor(if (homeSel) blue else secondary)
        tabLabels[1].setTextColor(if (libSel) blue else secondary)
        searchButton.setImageDrawable(
            icon(IconDrawable.SEARCH, if (mode == 1) Color.WHITE else secondary, 24, glow = mode == 1)
        )
    }

    private fun chipIndexFor(view: Int): Int = when (view) {
        0 -> 0
        1 -> 1
        6, 7 -> 2
        2, 3 -> 3
        else -> 4
    }

    private fun updateChips() {
        chipContainer.removeAllViews()
        val sel = chipIndexFor(libView)
        chipLabels.forEachIndexed { i, label ->
            val chip = TextView(this).apply {
                text = label
                textSize = 14f
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setTextColor(if (i == sel) Color.WHITE else color(R.color.vortex_text))
                background = pillBackground(i == sel)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(8) }
                setOnClickListener { setLibView(chipTargets[i]) }
            }
            chipContainer.addView(chip)
        }
    }

    private fun buildLibraryRows() {
        libRows.clear()
        when (libView) {
            0 -> {
                val likedCount = songs.count { favorites.contains(it.id) }
                val artistCount = songs.map { it.artist }.distinct().size
                val albumCount = songs.map { it.album }.distinct().size
                libRows.add(
                    Row(IconDrawable.HEART, 0, "Beğenilen Şarkılar", "$likedCount şarkı", true, "") {
                        setLibView(1)
                    }
                )
                libRows.add(
                    Row(IconDrawable.FOLDER, 1, "Klasörler", "${folders.size} klasör", true, "") {
                        setLibView(6)
                    }
                )
                libRows.add(
                    Row(IconDrawable.ARTIST, 1, "Sanatçılar", "$artistCount sanatçı", true, "") {
                        setLibView(2)
                    }
                )
                libRows.add(
                    Row(IconDrawable.DISC, 1, "Albümler", "$albumCount albüm", true, "") {
                        setLibView(4)
                    }
                )
                libRows.add(
                    Row(IconDrawable.DOWNLOAD, 1, "İndirilenler", "Yakında", false, "") {
                        toast("Çok yakında")
                    }
                )
            }
            2 -> {
                songs.groupBy { it.artist }
                    .toList()
                    .sortedBy { it.first.lowercase() }
                    .forEach { (name, list) ->
                        libRows.add(
                            Row(IconDrawable.ARTIST, 2, name, "${list.size} şarkı", true, "") {
                                selectedGroup = name
                                setLibView(3)
                            }
                        )
                    }
            }
            4 -> {
                songs.groupBy { it.album }
                    .toList()
                    .sortedBy { it.first.lowercase() }
                    .forEach { (name, list) ->
                        libRows.add(
                            Row(
                                IconDrawable.DISC, 1, name,
                                "${list.first().artist} · ${list.size} şarkı", true, ""
                            ) {
                                selectedGroup = name
                                setLibView(5)
                            }
                        )
                    }
            }
            6 -> {
                libRows.add(
                    Row(IconDrawable.FOLDER_PLUS, 0, "Yeni klasör", "Şarkılarını düzenle", true, "") {
                        askNewFolder()
                    }
                )
                val existing = songs.map { it.id }.toSet()
                folders.keys.sortedBy { it.lowercase() }.forEach { name ->
                    val count = folders[name]?.count { existing.contains(it) } ?: 0
                    libRows.add(
                        Row(IconDrawable.FOLDER, 1, name, "$count şarkı", true, name) {
                            selectedGroup = name
                            setLibView(7)
                        }
                    )
                }
            }
        }
    }

    private fun refreshList() {
        shown.clear()
        var rowsMode = false
        when (mode) {
            1 -> {
                val q = searchBox.text.toString().trim()
                shown.addAll(
                    if (q.isEmpty()) songs
                    else songs.filter {
                        it.title.contains(q, ignoreCase = true) ||
                            it.artist.contains(q, ignoreCase = true)
                    }
                )
            }
            2 -> {
                when (libView) {
                    0, 2, 4, 6 -> {
                        rowsMode = true
                        buildLibraryRows()
                    }
                    1 -> shown.addAll(songs.filter { favorites.contains(it.id) })
                    3 -> shown.addAll(songs.filter { it.artist == selectedGroup })
                    5 -> shown.addAll(songs.filter { it.album == selectedGroup })
                    else -> {
                        val ids = folders[selectedGroup] ?: emptySet<Long>()
                        shown.addAll(songs.filter { ids.contains(it.id) })
                    }
                }
                updateChips()
            }
            else -> shown.addAll(songs)
        }
        val target: BaseAdapter = if (rowsMode) libAdapter else adapter
        if (songList.adapter !== target) songList.adapter = target
        adapter.notifyDataSetChanged()
        libAdapter.notifyDataSetChanged()
        if (!loaded) return
        statusText.text = when {
            songs.isEmpty() -> "Telefonda müzik bulunamadı"
            mode == 1 -> "${shown.size} sonuç"
            mode == 2 -> when (libView) {
                0 -> "Kütüphane"
                1 ->
                    if (shown.isEmpty()) "Henüz beğenilen şarkı yok"
                    else "${shown.size} beğenilen şarkı"
                2 -> "${libRows.size} sanatçı"
                4 -> "${libRows.size} albüm"
                6 -> "${folders.size} klasör · uzun basarak düzenle"
                7 ->
                    if (shown.isEmpty()) "$selectedGroup · boş, şarkıya uzun basıp ekle"
                    else "$selectedGroup · ${shown.size} şarkı"
                else -> "$selectedGroup · ${shown.size} şarkı"
            }
            else -> "${songs.size} şarkı"
        }
    }

    // ---------- Beğenilenler (telefonda kalıcı saklanır) ----------

    private fun loadFavorites() {
        val saved = getSharedPreferences("vortex", MODE_PRIVATE)
            .getStringSet("favs", emptySet()) ?: emptySet()
        favorites.clear()
        favorites.addAll(saved.mapNotNull { it.toLongOrNull() })
    }

    private fun saveFavorites() {
        getSharedPreferences("vortex", MODE_PRIVATE).edit()
            .putStringSet("favs", favorites.map { it.toString() }.toSet())
            .apply()
    }

    private fun toggleFavoriteId(id: Long) {
        if (id < 0) return
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        saveFavorites()
        updatePills()
        refreshList()
    }

    // ---------- Klasörler (telefonda kalıcı saklanır) ----------

    private fun loadFolders() {
        val prefs = getSharedPreferences("vortex", MODE_PRIVATE)
        folders.clear()
        val names = prefs.getStringSet("folders", emptySet()) ?: emptySet()
        names.sorted().forEach { n ->
            val ids = prefs.getStringSet("f_$n", emptySet()) ?: emptySet()
            folders[n] = ids.mapNotNull { it.toLongOrNull() }.toMutableSet()
        }
    }

    private fun saveFolders(removed: List<String> = emptyList()) {
        val e = getSharedPreferences("vortex", MODE_PRIVATE).edit()
        e.putStringSet("folders", folders.keys.toSet())
        folders.forEach { (n, ids) ->
            e.putStringSet("f_$n", ids.map { it.toString() }.toSet())
        }
        removed.forEach { e.remove("f_$it") }
        e.apply()
    }

    private fun createFolder(name: String): Boolean {
        if (folders.containsKey(name)) {
            toast("Bu isimde bir klasör zaten var")
            return false
        }
        folders[name] = mutableSetOf()
        saveFolders()
        return true
    }

    private fun askFolderName(title: String, initial: String, onOk: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(initial)
            setSelection(text.length)
            setSingleLine(true)
            hint = "Klasör adı"
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Tamam") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) onOk(name)
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun askNewFolder() {
        askFolderName("Yeni klasör", "") { n ->
            if (createFolder(n)) refreshList()
        }
    }

    private fun renameFolder(old: String) {
        askFolderName("Klasörü yeniden adlandır", old) { n ->
            if (n == old) return@askFolderName
            if (folders.containsKey(n)) {
                toast("Bu isimde bir klasör zaten var")
                return@askFolderName
            }
            val ids = folders.remove(old) ?: return@askFolderName
            folders[n] = ids
            saveFolders(listOf(old))
            refreshList()
        }
    }

    private fun deleteFolder(name: String) {
        AlertDialog.Builder(this)
            .setTitle("\"$name\" silinsin mi?")
            .setMessage("Şarkılar telefondan silinmez, sadece klasör kalkar.")
            .setPositiveButton("Sil") { _, _ ->
                folders.remove(name)
                saveFolders(listOf(name))
                refreshList()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun folderOptions(name: String) {
        val items = arrayOf("Yeniden adlandır", "Sil")
        AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(items) { _, which ->
                if (which == 0) renameFolder(name) else deleteFolder(name)
            }
            .show()
    }

    private fun addToFolderDialog(songId: Long) {
        if (songId < 0) return
        val names = folders.keys.sortedBy { it.lowercase() }
        val items = (listOf("+ Yeni klasör") + names).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Klasöre ekle")
            .setItems(items) { _, which ->
                if (which == 0) {
                    askFolderName("Yeni klasör", "") { n ->
                        if (createFolder(n)) {
                            folders[n]?.add(songId)
                            saveFolders()
                            toast("\"$n\" klasörüne eklendi")
                            refreshList()
                        }
                    }
                } else {
                    val n = names[which - 1]
                    folders[n]?.add(songId)
                    saveFolders()
                    toast("\"$n\" klasörüne eklendi")
                    refreshList()
                }
            }
            .show()
    }

    private fun songOptions(song: Song) {
        val items = mutableListOf(
            "Klasöre ekle",
            if (favorites.contains(song.id)) "Beğenmeyi kaldır" else "Beğen"
        )
        if (mode == 2 && libView == 7) items.add("Klasörden çıkar")
        AlertDialog.Builder(this)
            .setTitle(song.title)
            .setItems(items.toTypedArray()) { _, which ->
                when (items[which]) {
                    "Klasöre ekle" -> addToFolderDialog(song.id)
                    "Klasörden çıkar" -> {
                        folders[selectedGroup]?.remove(song.id)
                        saveFolders()
                        refreshList()
                    }
                    else -> toggleFavoriteId(song.id)
                }
            }
            .show()
    }

    // ---------- Arayüz güncellemeleri ----------

    private fun updateNowPlayingUi() {
        val meta = player?.currentMediaItem?.mediaMetadata
        val t = meta?.title?.toString()
        val a = meta?.artist?.toString()
        nowTitle.text = t ?: "Çalan şarkı yok"
        nowArtist.text = a ?: "Bir şarkıya dokun"
        npTitle.text = t ?: "Çalan şarkı yok"
        npArtist.text = a ?: ""
        updatePills()
        adapter.notifyDataSetChanged()
    }

    private fun styleCell(
        iconView: ImageView,
        label: TextView,
        kind: Int,
        active: Boolean,
        text: String,
        fillIcon: Boolean = false
    ) {
        iconView.background = circleBackground(active)
        iconView.setImageDrawable(
            icon(kind, if (active) Color.WHITE else color(R.color.vortex_text), 22, filled = fillIcon && active)
        )
        label.text = text
        label.setTextColor(
            if (active) color(R.color.vortex_blue) else color(R.color.vortex_text_secondary)
        )
    }

    private fun updatePills() {
        val p = player
        val liked = favorites.contains(currentId())
        styleCell(
            npLikeIcon, npLikeLabel, IconDrawable.HEART, liked,
            if (liked) "Beğenildi" else "Beğen", true
        )
        styleCell(npFolderIcon, npFolderLabel, IconDrawable.FOLDER_PLUS, false, "Klasör")

        val shuffleOn = p?.shuffleModeEnabled == true
        styleCell(npShuffleIcon, npShuffleLabel, IconDrawable.SHUFFLE, shuffleOn, "Karıştır")

        val repeatMode = p?.repeatMode ?: Player.REPEAT_MODE_OFF
        styleCell(
            npRepeatIcon, npRepeatLabel, IconDrawable.REPEAT,
            repeatMode != Player.REPEAT_MODE_OFF,
            when (repeatMode) {
                Player.REPEAT_MODE_ALL -> "Hepsi"
                Player.REPEAT_MODE_ONE -> "Tek şarkı"
                else -> "Döngü"
            }
        )

        val speed = speeds[currentSpeedIndex()]
        styleCell(npSpeedIcon, npSpeedLabel, IconDrawable.CLOCK, speed != 1.0f, "${speed}x")
    }

    private fun updatePlayIcons(isPlaying: Boolean) {
        val kind = if (isPlaying) IconDrawable.PAUSE else IconDrawable.PLAY
        playPauseButton.setImageDrawable(icon(kind, Color.WHITE, 24, filled = true))
        npPlayPause.setImageDrawable(icon(kind, Color.WHITE, 32, filled = true))
    }

    private fun showNowPlaying() {
        if (nowPlayingScreen.visibility == View.VISIBLE) return
        updateNowPlayingUi()
        nowPlayingScreen.visibility = View.VISIBLE
    }

    private fun hideNowPlaying() {
        nowPlayingScreen.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            if (!loaded) loadSongs()
        } else {
            statusText.text = "Müzik izni verilmedi. Şarkıları göstermek için izin gerekli."
        }
    }

    private fun loadSongs() {
        songs.clear()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                songs.add(
                    Song(
                        it.getLong(0),
                        it.getString(1) ?: "Bilinmeyen",
                        it.getString(2) ?: "Bilinmeyen sanatçı",
                        it.getString(3) ?: "Bilinmeyen albüm"
                    )
                )
            }
        }
        loaded = true
        refreshList()
    }

    // ---------- Liste satırları ----------

    private fun buildRowView(row: Row): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val iconBg: GradientDrawable = when (row.iconStyle) {
            0 -> neonGradient(false, 14)
            2 -> GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color(R.color.vortex_surface_light))
            }
            else -> GradientDrawable().apply {
                setColor(color(R.color.vortex_surface_light))
                cornerRadius = dp(14).toFloat()
            }
        }

        val iconView = ImageView(this).apply {
            setPadding(dp(15), dp(15), dp(15), dp(15))
            background = iconBg
            setImageDrawable(
                icon(row.kind, if (row.iconStyle == 0) Color.WHITE else color(R.color.vortex_purple), 26)
            )
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(14) }
        }

        val title = TextView(this).apply {
            text = row.title
            textSize = 16f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(
                if (row.enabled) color(R.color.vortex_text) else color(R.color.vortex_text_secondary)
            )
        }

        val subtitle = TextView(this).apply {
            text = row.subtitle
            textSize = 13f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(color(R.color.vortex_text_secondary))
        }

        column.addView(title)
        column.addView(subtitle)
        box.addView(iconView)
        box.addView(column)
        return box
    }

    private inner class RowAdapter : BaseAdapter() {
        override fun getCount() = libRows.size
        override fun getItem(position: Int) = libRows[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return buildRowView(libRows[position])
        }
    }

    private inner class SongAdapter : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = shown[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val song = shown[position]
            val isCurrent = song.id == currentId()

            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                if (isCurrent) {
                    background = GradientDrawable().apply {
                        setColor(color(R.color.vortex_surface_light))
                        cornerRadius = dp(18).toFloat()
                    }
                }
            }

            val iconView = ImageView(this@MainActivity).apply {
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = circleBackground(isCurrent).also {
                    if (!isCurrent) it.setColor(color(R.color.vortex_surface))
                }
                setImageDrawable(
                    icon(
                        IconDrawable.NOTE,
                        if (isCurrent) Color.WHITE else color(R.color.vortex_purple),
                        22
                    )
                )
                layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
            }

            val column = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginStart = dp(12) }
            }

            val title = TextView(this@MainActivity).apply {
                text = song.title
                textSize = 16f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(if (isCurrent) color(R.color.vortex_blue) else color(R.color.vortex_text))
            }

            val artist = TextView(this@MainActivity).apply {
                text = song.artist
                textSize = 13f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(color(R.color.vortex_text_secondary))
            }

            column.addView(title)
            column.addView(artist)
            row.addView(iconView)
            row.addView(column)

            if (favorites.contains(song.id)) {
                row.addView(ImageView(this@MainActivity).apply {
                    setImageDrawable(icon(IconDrawable.HEART, color(R.color.vortex_purple), 18, filled = true))
                    layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                        marginStart = dp(8)
                    }
                })
            }
            return row
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        player?.removeListener(playerListener)
        player = null
        if (Build.VERSION.SDK_INT >= 33) {
            (backCallback as? android.window.OnBackInvokedCallback)?.let {
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
            }
            backCallback = null
        }
        try {
            unbindService(connection)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}