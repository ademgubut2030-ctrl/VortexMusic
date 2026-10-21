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
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

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
    private var statsDays = 7
    private var player: ExoPlayer? = null
    private var fx: AudioFxController? = null
    private val adapter = SongAdapter()
    private val libAdapter = RowAdapter()

    private val favorites = mutableSetOf<Long>()
    private val speeds = floatArrayOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private val chipLabels = arrayOf("Tümü", "Beğenilenler", "Klasörler", "Sanatçılar", "Albümler")
    private val chipTargets = intArrayOf(0, 1, 6, 2, 4)

    private lateinit var statusText: TextView
    private lateinit var searchBox: EditText
    private lateinit var searchButton: ImageView
    private lateinit var statsButton: ImageView
    private lateinit var audioButton: ImageView
    private lateinit var statsScreen: LinearLayout
    private lateinit var statsChips: LinearLayout
    private lateinit var statsContent: LinearLayout
    private lateinit var audioScreen: LinearLayout
    private lateinit var audioContent: LinearLayout
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
            val binder = service as? PlaybackService.LocalBinder ?: return
            val p = binder.getPlayer() ?: return
            fx = binder.getFx()
            attachPlayer(p)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            player?.removeListener(playerListener)
            player = null
            fx = null
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

    // ---------- Yardımcılar ----------

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun color(id: Int): Int = resources.getColor(id, theme)

    private fun fmt(ms: Long): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    private fun fmtLong(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return when {
            h > 0 -> "$h sa $m dk"
            m > 0 -> "$m dk $sec sn"
            else -> "$sec sn"
        }
    }

    private fun fmtFreq(f: Int): String =
        if (f >= 1000) {
            if (f % 1000 == 0) "${f / 1000} kHz" else String.format(Locale.US, "%.1f kHz", f / 1000f)
        } else {
            "$f Hz"
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

        // İstatistik ve ses ekranları koddan eklenir
        statsScreen = buildStatsScreen()
        (root.parent as FrameLayout).addView(statsScreen)
        audioScreen = buildAudioScreen()
        (root.parent as FrameLayout).addView(audioScreen)

        statsButton = ImageView(this).apply {
            setPadding(dp(11), dp(11), dp(11), dp(11))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
            setImageDrawable(icon(IconDrawable.CHART, color(R.color.vortex_text_secondary), 24))
            setOnClickListener { showStats() }
        }
        val headerRow = searchButton.parent as LinearLayout
        headerRow.addView(statsButton, headerRow.indexOfChild(searchButton))

        audioButton = ImageView(this).apply {
            setPadding(dp(11), dp(11), dp(11), dp(11))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
            setImageDrawable(icon(IconDrawable.EQ, color(R.color.vortex_text_secondary), 24))
            setOnClickListener { showAudio() }
        }
        headerRow.addView(audioButton, headerRow.indexOfChild(statsButton))

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

        // İzinler (Android sürümüne göre doğru müzik izni istenir)
        val needed = mutableListOf<String>()
        val audioOk = checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED
        if (!audioOk) needed.add(audioPermission())
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
        if (audioScreen.visibility == View.VISIBLE) {
            hideAudio()
        } else if (statsScreen.visibility == View.VISIBLE) {
            hideStats()
        } else if (nowPlayingScreen.visibility == View.VISIBLE) {
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

    // ---------- Ses ekranı (ekolayzır, efektler, profiller) ----------

    private fun buildAudioScreen(): LinearLayout {
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            isClickable = true
            isFocusable = true
            background = neonBackground()
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        screen.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                dp(16),
                insets.systemWindowInsetTop + dp(12),
                dp(16),
                insets.systemWindowInsetBottom + dp(12)
            )
            insets
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val close = ImageView(this).apply {
            setPadding(dp(11), dp(11), dp(11), dp(11))
            isClickable = true
            isFocusable = true
            setImageDrawable(icon(IconDrawable.CHEVRON, color(R.color.vortex_purple), 24))
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
            setOnClickListener { hideAudio() }
        }
        val title = TextView(this).apply {
            text = "Ses"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.vortex_text))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(4) }
        }
        header.addView(close)
        header.addView(title)

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            ).apply { topMargin = dp(8) }
        }
        audioContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(audioContent)

        screen.addView(header)
        screen.addView(scroll)
        return screen
    }

    private fun showAudio() {
        renderAudio()
        audioScreen.visibility = View.VISIBLE
    }

    private fun hideAudio() {
        audioScreen.visibility = View.GONE
    }

    private fun slider(
        label: String,
        maxValue: Int,
        startValue: Int,
        format: (Int) -> String,
        onChange: (Int) -> Unit,
        onDone: () -> Unit
    ): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(8), dp(2), dp(2))
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val name = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(color(R.color.vortex_text))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val value = TextView(this).apply {
            text = format(startValue)
            textSize = 13f
            setTextColor(color(R.color.vortex_text_secondary))
        }
        top.addView(name)
        top.addView(value)

        val bar = SeekBar(this)
        bar.max = maxValue
        bar.progress = startValue
        bar.progressTintList = ColorStateList.valueOf(color(R.color.vortex_purple))
        bar.thumbTintList = ColorStateList.valueOf(color(R.color.vortex_blue))
        bar.progressBackgroundTintList = ColorStateList.valueOf(color(R.color.vortex_surface_light))
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                value.text = format(progress)
                if (fromUser) onChange(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                onDone()
            }
        })
        box.addView(top)
        box.addView(bar)
        return box
    }

    private fun actionPill(label: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 13f
        gravity = Gravity.CENTER
        setTextColor(color(R.color.vortex_text))
        background = pillBackground(false)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        layoutParams = LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ).apply { marginEnd = dp(6) }
        setOnClickListener { onClick() }
    }

    private fun renderAudio() {
        audioContent.removeAllViews()
        val f = fx
        if (f == null) {
            audioContent.addView(
                noteText("Ses motoru henüz hazır değil. Birkaç saniye sonra tekrar aç.")
            )
            return
        }

        // Açma / kapama
        audioContent.addView(TextView(this).apply {
            text = if (f.enabled) "Ses efektleri: Açık" else "Ses efektleri: Kapalı"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(if (f.enabled) Color.WHITE else color(R.color.vortex_text))
            background = pillBackground(f.enabled)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            setOnClickListener {
                f.enabled = !f.enabled
                f.applyAll()
                f.save()
                renderAudio()
            }
        })

        // Hazır ayarlar
        audioContent.addView(sectionTitle("Hazır ayarlar"))
        val presetScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val presetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        for (preset in f.presets) {
            val active = f.enabled && f.matches(preset)
            presetRow.addView(TextView(this).apply {
                text = preset.name
                textSize = 14f
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setTextColor(if (active) Color.WHITE else color(R.color.vortex_text))
                background = pillBackground(active)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(8) }
                setOnClickListener {
                    f.setPreset(preset)
                    renderAudio()
                }
            })
        }
        presetScroll.addView(presetRow)
        audioContent.addView(presetScroll)

        // Profillerim
        audioContent.addView(sectionTitle("Profillerim"))
        if (f.profiles.isEmpty()) {
            audioContent.addView(
                noteText(
                    "Henüz profil yok. Ayarlarını yapıp \"Profil olarak kaydet\" de. " +
                        "Kulaklık adını yazarsan ileride Bluetooth'a göre otomatik seçilecek."
                )
            )
        } else {
            val profScroll = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
            }
            val profRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            for (prof in f.profiles.toList()) {
                val active = f.enabled && f.matchesProfile(prof)
                profRow.addView(TextView(this).apply {
                    text = if (prof.headphone.isEmpty()) prof.name else "${prof.name} · ${prof.headphone}"
                    textSize = 14f
                    setPadding(dp(16), dp(8), dp(16), dp(8))
                    setTextColor(if (active) Color.WHITE else color(R.color.vortex_text))
                    background = pillBackground(active)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { marginEnd = dp(8) }
                    setOnClickListener {
                        f.applyProfile(prof)
                        renderAudio()
                    }
                    setOnLongClickListener {
                        profileOptions(prof.name)
                        true
                    }
                })
            }
            profScroll.addView(profRow)
            audioContent.addView(profScroll)
            audioContent.addView(noteText("Silmek için profile uzun bas."))
        }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        actions.addView(actionPill("Profil olarak kaydet") { askSaveProfile() })
        actions.addView(actionPill("Dışa aktar") { exportProfiles() })
        actions.addView(actionPill("İçe aktar") { importProfiles() })
        audioContent.addView(actions)

        // Ekolayzır bantları
        audioContent.addView(sectionTitle("Ekolayzır"))
        if (!f.eqAvailable || f.bandCount == 0) {
            audioContent.addView(noteText("Bu telefon ekolayzırı desteklemiyor."))
        } else {
            val minLv = f.minLevel
            val span = f.maxLevel - f.minLevel
            for (i in 0 until f.bandCount) {
                audioContent.addView(
                    slider(
                        fmtFreq(f.bandFreqs.getOrElse(i) { 0 }),
                        span,
                        f.levels.getOrElse(i) { 0 } - minLv,
                        { p -> String.format(Locale.US, "%+.1f dB", (p + minLv) / 100f) },
                        { p -> f.setBand(i, p + minLv) },
                        { f.save() }
                    )
                )
            }
        }

        // Efektler
        audioContent.addView(sectionTitle("Efektler"))
        if (f.bassAvailable) {
            audioContent.addView(
                slider(
                    "Bas güçlendirici", 1000, f.bassStrength,
                    { p -> "${p / 10}%" },
                    { p ->
                        f.bassStrength = p
                        f.applyAll()
                    },
                    { f.save() }
                )
            )
        }
        if (f.loudAvailable) {
            audioContent.addView(
                slider(
                    "Ses yükseltme", 1000, f.loudnessMb,
                    { p -> String.format(Locale.US, "+%.1f dB", p / 100f) },
                    { p ->
                        f.loudnessMb = p
                        f.applyAll()
                    },
                    { f.save() }
                )
            )
        }
        if (f.virtAvailable) {
            audioContent.addView(
                slider(
                    "Sanal surround", 1000, f.virtStrength,
                    { p -> "${p / 10}%" },
                    { p ->
                        f.virtStrength = p
                        f.applyAll()
                    },
                    { f.save() }
                )
            )
        }
        if (!f.bassAvailable && !f.loudAvailable && !f.virtAvailable) {
            audioContent.addView(noteText("Bu telefon ek ses efektlerini desteklemiyor."))
        }
        audioContent.addView(
            noteText(
                "Efektlerin gücü telefon markasına ve kulaklığa göre değişir. " +
                    "Çok yüksek ses yükseltme sesi bozabilir."
            )
        )
    }

    private fun askSaveProfile() {
        val f = fx ?: return
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val nameInput = EditText(this).apply {
            hint = "Profil adı"
            setSingleLine(true)
        }
        val phoneInput = EditText(this).apply {
            hint = "Kulaklık (isteğe bağlı)"
            setSingleLine(true)
        }
        box.addView(nameInput)
        box.addView(phoneInput)
        AlertDialog.Builder(this)
            .setTitle("Profil olarak kaydet")
            .setView(box)
            .setPositiveButton("Kaydet") { _, _ ->
                if (f.saveCurrentAsProfile(nameInput.text.toString(), phoneInput.text.toString())) {
                    toast("Profil kaydedildi")
                    renderAudio()
                } else {
                    toast("Profil adı boş olamaz")
                }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun profileOptions(name: String) {
        val f = fx ?: return
        AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(arrayOf("Sil")) { _, _ ->
                f.deleteProfile(name)
                renderAudio()
            }
            .show()
    }

    @Suppress("DEPRECATION")
    private fun exportProfiles() {
        val f = fx ?: return
        if (f.profiles.isEmpty()) {
            toast("Dışa aktarılacak profil yok")
            return
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
            .putExtra(Intent.EXTRA_TITLE, "vortex-ses-profilleri.json")
        startActivityForResult(intent, REQ_EXPORT)
    }

    @Suppress("DEPRECATION")
    private fun importProfiles() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
        startActivityForResult(intent, REQ_IMPORT)
    }

    private fun readLimited(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { r ->
                val sb = StringBuilder()
                val buf = CharArray(4096)
                while (true) {
                    val n = r.read(buf)
                    if (n < 0) break
                    sb.append(buf, 0, n)
                    if (sb.length > 500000) return@use null
                }
                sb.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val f = fx ?: return
        if (requestCode == REQ_EXPORT) {
            try {
                contentResolver.openOutputStream(uri)?.use {
                    it.write(f.exportJson().toByteArray(Charsets.UTF_8))
                }
                toast("Profiller dışa aktarıldı")
            } catch (e: Exception) {
                toast("Dosya yazılamadı")
            }
        } else if (requestCode == REQ_IMPORT) {
            val text = readLimited(uri)
            if (text == null) {
                toast("Dosya okunamadı ya da çok büyük")
                return
            }
            val n = f.importJson(text)
            if (n > 0) toast("$n profil içe aktarıldı")
            else toast("Geçerli bir Vortex profil dosyası değil")
            if (audioScreen.visibility == View.VISIBLE) renderAudio()
        }
    }

    // ---------- İstatistikler ----------

    private fun buildStatsScreen(): LinearLayout {
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            isClickable = true
            isFocusable = true
            background = neonBackground()
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        screen.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                dp(16),
                insets.systemWindowInsetTop + dp(12),
                dp(16),
                insets.systemWindowInsetBottom + dp(12)
            )
            insets
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val close = ImageView(this).apply {
            setPadding(dp(11), dp(11), dp(11), dp(11))
            isClickable = true
            isFocusable = true
            setImageDrawable(icon(IconDrawable.CHEVRON, color(R.color.vortex_purple), 24))
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
            setOnClickListener { hideStats() }
        }
        val title = TextView(this).apply {
            text = "İstatistikler"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.vortex_text))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(4) }
        }
        header.addView(close)
        header.addView(title)

        statsChips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            ).apply { topMargin = dp(12) }
        }
        statsContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(statsContent)

        screen.addView(header)
        screen.addView(statsChips)
        screen.addView(scroll)
        return screen
    }

    private fun showStats() {
        updateStatsChips()
        renderStats()
        statsScreen.visibility = View.VISIBLE
    }

    private fun hideStats() {
        statsScreen.visibility = View.GONE
    }

    private fun updateStatsChips() {
        statsChips.removeAllViews()
        val labels = arrayOf("Bugün", "Hafta", "Ay", "Tümü")
        val values = intArrayOf(1, 7, 30, 0)
        labels.forEachIndexed { i, label ->
            val selected = statsDays == values[i]
            val chip = TextView(this).apply {
                text = label
                textSize = 14f
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setTextColor(if (selected) Color.WHITE else color(R.color.vortex_text))
                background = pillBackground(selected)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(8) }
                setOnClickListener {
                    statsDays = values[i]
                    updateStatsChips()
                    renderStats()
                }
            }
            statsChips.addView(chip)
        }
    }

    private fun sectionTitle(label: String): View = TextView(this).apply {
        text = label
        textSize = 16f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(color(R.color.vortex_text))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(22)
            bottomMargin = dp(8)
        }
    }

    private fun noteText(label: String): View = TextView(this).apply {
        text = label
        textSize = 13f
        setTextColor(color(R.color.vortex_text_secondary))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) }
    }

    private fun statRow(label: String, ms: Long, max: Long): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(6), dp(2), dp(6))
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val name = TextView(this).apply {
            text = label
            textSize = 15f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(color(R.color.vortex_text))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val time = TextView(this).apply {
            text = fmtLong(ms)
            textSize = 13f
            setTextColor(color(R.color.vortex_text_secondary))
        }
        top.addView(name)
        top.addView(time)

        val pct = ((ms * 100L) / max).toInt().coerceIn(3, 100)
        val track = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 100f
            background = GradientDrawable().apply {
                setColor(color(R.color.vortex_surface_light))
                cornerRadius = dp(4).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8)
            ).apply { topMargin = dp(6) }
        }
        val bar = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(color(R.color.vortex_purple), color(R.color.vortex_blue))
            ).apply { cornerRadius = dp(4).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, pct.toFloat())
        }
        track.addView(bar)
        if (pct < 100) {
            track.addView(
                View(this),
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (100 - pct).toFloat())
            )
        }
        box.addView(top)
        box.addView(track)
        return box
    }

    private fun renderStats() {
        statsContent.removeAllViews()
        val prefs = getSharedPreferences("vortex_stats", MODE_PRIVATE)
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val cutoff: Int = if (statsDays <= 0) {
            0
        } else {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -(statsDays - 1))
            dayFormat.format(c.time).toInt()
        }

        val perSong = HashMap<Long, Long>()
        val perDay = HashMap<Int, Long>()
        for ((k, v) in prefs.all) {
            if (!k.startsWith("ds_")) continue
            val parts = k.split("_")
            if (parts.size != 3) continue
            val day = parts[1].toIntOrNull() ?: continue
            val id = parts[2].toLongOrNull() ?: continue
            val ms = (v as? Long) ?: continue
            perDay[day] = (perDay[day] ?: 0L) + ms
            if (day >= cutoff) perSong[id] = (perSong[id] ?: 0L) + ms
        }

        val byId = songs.associateBy { it.id }
        val entries = perSong
            .filter { byId.containsKey(it.key) && it.value > 0L }
            .toList()
            .sortedByDescending { it.second }

        if (entries.isEmpty()) {
            statsContent.addView(
                noteText("Henüz dinleme kaydı yok. Bir şarkı çaldıkça ve duraklattıkça burada görünür.")
            )
            return
        }

        val periodLabel = when (statsDays) {
            1 -> "bugün"
            7 -> "son 7 gün"
            30 -> "son 30 gün"
            else -> "tüm zamanlar"
        }
        var total = 0L
        for (e in entries) total += e.second

        statsContent.addView(TextView(this).apply {
            text = fmtLong(total)
            textSize = 34f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.vortex_purple))
        })
        statsContent.addView(noteText("${entries.size} farklı şarkı · $periodLabel"))

        // En çok dinlenen şarkılar
        statsContent.addView(sectionTitle("En çok dinlenen şarkılar"))
        val topSongs = entries.take(10)
        val maxSong = topSongs.first().second
        for (e in topSongs) {
            val s = byId[e.first] ?: continue
            statsContent.addView(statRow(s.title, e.second, maxSong))
        }

        // En çok dinlenen sanatçılar
        val perArtist = HashMap<String, Long>()
        for (e in entries) {
            val s = byId[e.first] ?: continue
            perArtist[s.artist] = (perArtist[s.artist] ?: 0L) + e.second
        }
        val topArtists = perArtist.toList().sortedByDescending { it.second }.take(5)
        statsContent.addView(sectionTitle("En çok dinlenen sanatçılar"))
        val maxArtist = topArtists.first().second
        for (a in topArtists) {
            statsContent.addView(statRow(a.first, a.second, maxArtist))
        }

        // Son 7 gün
        val dayNames = arrayOf("Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")
        val weekValues = LongArray(7)
        val weekLabels = Array(7) { "" }
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            val key = dayFormat.format(cal.time).toInt()
            weekValues[i] = perDay[key] ?: 0L
            weekLabels[i] = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        statsContent.addView(sectionTitle("Son 7 gün"))
        statsContent.addView(
            BarChartView(
                this, weekValues, weekLabels,
                color(R.color.vortex_purple), color(R.color.vortex_blue),
                color(R.color.vortex_text_secondary),
                resources.displayMetrics.density
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Günün saatleri
        val hourValues = LongArray(24) { prefs.getLong("h_$it", 0L) }
        val hourLabels = Array(24) { if (it % 6 == 0) "$it" else "" }
        statsContent.addView(sectionTitle("Günün saatleri"))
        statsContent.addView(
            BarChartView(
                this, hourValues, hourLabels,
                color(R.color.vortex_blue), color(R.color.vortex_purple),
                color(R.color.vortex_text_secondary),
                resources.displayMetrics.density
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        var peak = 0
        var peakValue = 0L
        for (h in 0..23) {
            if (hourValues[h] > peakValue) {
                peakValue = hourValues[h]
                peak = h
            }
        }
        if (peakValue > 0L) {
            val persona = when (peak) {
                in 5..11 -> "Sabahçısın"
                in 12..17 -> "Öğleden sonra dinleyicisisin"
                in 18..21 -> "Akşam dinleyicisisin"
                else -> "Gece kuşusun"
            }
            statsContent.addView(
                noteText("En çok %02d:00 civarında dinliyorsun · $persona (tüm zamanlar)".format(peak))
            )
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
        if (checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED) {
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
        fx = null
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

    companion object {
        private const val REQ_EXPORT = 101
        private const val REQ_IMPORT = 102
    }
}