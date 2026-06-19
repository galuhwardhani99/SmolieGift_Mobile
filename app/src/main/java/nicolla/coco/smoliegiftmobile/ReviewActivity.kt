package nicolla.coco.smoliegiftmobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class ReviewActivity : AppCompatActivity() {

    private lateinit var etReviewText: TextInputEditText
    private lateinit var ivReviewPhoto: ImageView
    private lateinit var vvReviewVideo: VideoView
    private lateinit var cvPhotoPreview: MaterialCardView
    private lateinit var cvVideoPreview: MaterialCardView
    private lateinit var sbVideoProgress: SeekBar
    private lateinit var btnVideoPlayPause: ImageButton
    private lateinit var tvReviewKode: TextView
    private lateinit var ratingBarReview: RatingBar

    private var selectedPhotoUri: Uri? = null
    private var selectedVideoUri: Uri? = null
    private var transaksiId: Int = -1
    private var kodeTransaksi: String = "-"

    private var playbackSpeed = 1.0f
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private var scaleFactor = 1.0f
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    // Batas uk vid
    private val MAX_VIDEO_SIZE = 20 * 1024 * 1024L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        transaksiId   = intent.getIntExtra("TRANSAKSI_ID", -1)
        kodeTransaksi = intent.getStringExtra("KODE_TRANSAKSI") ?: "-"

        etReviewText      = findViewById(R.id.etReviewText)
        ivReviewPhoto     = findViewById(R.id.ivReviewPhoto)
        vvReviewVideo     = findViewById(R.id.vvReviewVideo)
        cvPhotoPreview    = findViewById(R.id.cvPhotoPreview)
        cvVideoPreview    = findViewById(R.id.cvVideoPreview)
        sbVideoProgress   = findViewById(R.id.sbVideoProgress)
        btnVideoPlayPause = findViewById(R.id.btnVideoPlayPause)
        tvReviewKode      = findViewById(R.id.tvReviewKode)
        ratingBarReview   = findViewById(R.id.ratingBarReview)

        tvReviewKode.text = "Transaksi: #$kodeTransaksi"

        findViewById<MaterialButton>(R.id.btnAmbilFoto).setOnClickListener { checkCameraPermission() }
        findViewById<MaterialButton>(R.id.btnPilihFoto).setOnClickListener { pickPhotoGallery.launch("image/*") }
        findViewById<MaterialButton>(R.id.btnPilihVideo).setOnClickListener { pickVideoGallery.launch("video/*") }

        setupVideoControls()
        setupPhotoZoom()

        findViewById<MaterialButton>(R.id.btnSubmitReview).setOnClickListener {
            submitReview()
        }
    }

    // Kamera

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        } else {
            takePhoto()
        }
    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) takePhoto()
            else Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        }

    private fun takePhoto() {
        val cacheDir  = externalCacheDir ?: cacheDir
        val photoFile = File(cacheDir, "review_photo_${System.currentTimeMillis()}.jpg")
        try {
            val photoUri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
            selectedPhotoUri = photoUri
            takePhotoLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menyiapkan kamera", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cvPhotoPreview.visibility = View.VISIBLE
                selectedPhotoUri?.let { ivReviewPhoto.setImageURI(it) }
                resetPhotoZoom()
            }
        }

    // foto dr galeri

    private val pickPhotoGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedPhotoUri = it
                cvPhotoPreview.visibility = View.VISIBLE
                ivReviewPhoto.setImageURI(it)
                resetPhotoZoom()
            }
        }

    // vidio dr galeri
    private val pickVideoGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val fileSize = contentResolver.openFileDescriptor(it, "r")?.statSize ?: 0L
                if (fileSize > MAX_VIDEO_SIZE) {
                    Toast.makeText(
                        this,
                        "Video terlalu besar (maks 20MB). Pilih video yang lebih pendek.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@let
                }

                selectedVideoUri          = it
                cvVideoPreview.visibility = View.VISIBLE
                vvReviewVideo.setVideoURI(it)
                vvReviewVideo.requestFocus()
                vvReviewVideo.start()
                btnVideoPlayPause.setImageResource(R.drawable.stop)
            }
        }


    private fun setupVideoControls() {
        vvReviewVideo.setOnPreparedListener { mp ->
            mediaPlayer = mp
            sbVideoProgress.max = vvReviewVideo.duration
            updateSeekBar()
        }

        btnVideoPlayPause.setOnClickListener {
            if (vvReviewVideo.isPlaying) {
                vvReviewVideo.pause()
                btnVideoPlayPause.setImageResource(R.drawable.play)
            } else {
                vvReviewVideo.start()
                btnVideoPlayPause.setImageResource(R.drawable.stop)
            }
        }

        findViewById<ImageButton>(R.id.btnVideoStop).setOnClickListener {
            vvReviewVideo.stopPlayback()
            selectedVideoUri?.let { vvReviewVideo.setVideoURI(it) }
            btnVideoPlayPause.setImageResource(R.drawable.play)
        }

        findViewById<ImageButton>(R.id.btnVideoRewind).setOnClickListener {
            val current = vvReviewVideo.currentPosition
            vvReviewVideo.seekTo((current - 5000).coerceAtLeast(0))
        }

        findViewById<ImageButton>(R.id.btnVideoForward).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { mp ->
                    try {
                        playbackSpeed = when (playbackSpeed) {
                            1.0f -> 2.0f
                            2.0f -> 0.5f
                            else -> 1.0f
                        }
                        val params = mp.playbackParams
                        params.speed = playbackSpeed
                        mp.playbackParams = params
                        Toast.makeText(this, "Kecepatan: ${playbackSpeed}x", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Gagal mengubah kecepatan", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                vvReviewVideo.seekTo(vvReviewVideo.currentPosition + 5000)
            }
        }

        sbVideoProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) vvReviewVideo.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        vvReviewVideo.setOnCompletionListener {
            sbVideoProgress.progress = 0
            vvReviewVideo.seekTo(0)
            btnVideoPlayPause.setImageResource(R.drawable.play)
        }
    }

    private fun updateSeekBar() {
        handler.post(object : Runnable {
            override fun run() {
                try {
                    if (vvReviewVideo.isPlaying) {
                        sbVideoProgress.progress = vvReviewVideo.currentPosition
                    }
                    handler.postDelayed(this, 500)
                } catch (e: Exception) { /* activity destroyed */ }
            }
        })
    }



    private fun setupPhotoZoom() {
        scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor  = scaleFactor.coerceIn(0.1f, 5.0f)
                    ivReviewPhoto.scaleX = scaleFactor
                    ivReviewPhoto.scaleY = scaleFactor
                    return true
                }
            }
        )

        ivReviewPhoto.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) v.performClick()
            true
        }
    }

    private fun resetPhotoZoom() {
        scaleFactor          = 1.0f
        ivReviewPhoto.scaleX = 1.0f
        ivReviewPhoto.scaleY = 1.0f
    }


    private fun uriToBase64Photo(uri: Uri): String? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            val maxSize = 800
            var scale   = 1
            if (options.outWidth > maxSize || options.outHeight > maxSize) {
                scale = Math.pow(
                    2.0,
                    Math.ceil(
                        Math.log(maxSize.toDouble() / maxOf(options.outWidth, options.outHeight))
                                / Math.log(0.5)
                    )
                ).toInt()
            }

            val options2 = BitmapFactory.Options().apply { inSampleSize = scale }
            val bitmap   = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options2)
            } ?: return null

            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uriToBase64Video(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun submitReview() {
        val ulasan = etReviewText.text.toString().trim()
        val rating = ratingBarReview.rating.toInt()

        if (ulasan.isEmpty()) {
            Toast.makeText(this, "Harap isi ulasan", Toast.LENGTH_SHORT).show()
            return
        }

        if (transaksiId == -1) {
            Toast.makeText(this, "ID Transaksi tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Mengirim ulasan...", Toast.LENGTH_SHORT).show()

        Thread {
            val fotoBase64  = selectedPhotoUri?.let { uriToBase64Photo(it) }

            val videoBase64 = selectedVideoUri?.let { uriToBase64Video(it) }

            ApiClient.submitReview(
                transaksiId, kodeTransaksi, ulasan, rating, fotoBase64, videoBase64
            ) { success, message ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Ulasan berhasil dikirim!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Gagal: ${message ?: "Periksa koneksi atau ukuran file"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }.start()
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onPause() {
        super.onPause()
        if (vvReviewVideo.isPlaying) {
            vvReviewVideo.pause()
            btnVideoPlayPause.setImageResource(R.drawable.play)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        vvReviewVideo.stopPlayback()
    }
}