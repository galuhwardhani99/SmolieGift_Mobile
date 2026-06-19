package nicolla.coco.smoliegiftmobile

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.card.MaterialCardView

class ReviewDetailActivity : AppCompatActivity() {

    private lateinit var tvDetailKode: TextView
    private lateinit var tvDetailNama: TextView
    private lateinit var tvDetailHp: TextView
    private lateinit var tvDetailTotal: TextView
    private lateinit var tvDetailProduk: TextView
    private lateinit var tvDetailTanggal: TextView
    private lateinit var ratingBarDetail: RatingBar
    private lateinit var tvDetailRatingAngka: TextView
    private lateinit var tvDetailKomentar: TextView
    private lateinit var cvDetailFoto: MaterialCardView
    private lateinit var photoViewDetail: PhotoView
    private lateinit var cvDetailVideo: MaterialCardView
    private lateinit var vvDetailVideo: VideoView
    private lateinit var btnDetailPlay: ImageButton
    private lateinit var btnDetailPause: ImageButton
    private lateinit var sbDetailVideo: SeekBar


    private val REVIEW_MEDIA_BASE_URL = "http://192.168.43.3/toko-smolie/public/reviews/"

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var requestQueue: RequestQueue
    private var updateRunnable: Runnable? = null


    private var isVideoPrepared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_detail)

        requestQueue = Volley.newRequestQueue(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbarReviewDetail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tvDetailKode     = findViewById(R.id.tvDetailKode)
        tvDetailNama     = findViewById(R.id.tvDetailNama)
        tvDetailHp       = findViewById(R.id.tvDetailHp)
        tvDetailTotal    = findViewById(R.id.tvDetailTotal)
        tvDetailProduk   = findViewById(R.id.tvDetailProduk)
        tvDetailTanggal  = findViewById(R.id.tvDetailTanggal)
        ratingBarDetail  = findViewById(R.id.ratingBarDetail)
        tvDetailRatingAngka = findViewById(R.id.tvDetailRatingAngka)
        tvDetailKomentar = findViewById(R.id.tvDetailKomentar)

        cvDetailFoto    = findViewById(R.id.cvDetailFoto)
        photoViewDetail = findViewById(R.id.photoViewDetail)

        cvDetailVideo  = findViewById(R.id.cvDetailVideo)
        vvDetailVideo  = findViewById(R.id.vvDetailVideo)
        btnDetailPlay  = findViewById(R.id.btnDetailPlay)
        btnDetailPause = findViewById(R.id.btnDetailPause)
        sbDetailVideo  = findViewById(R.id.sbDetailVideo)

        val kode     = intent.getStringExtra("kode_transaksi") ?: "-"
        val nama     = intent.getStringExtra("nama_pembeli")   ?: "-"
        val hp       = intent.getStringExtra("no_hp")          ?: "-"
        val total    = intent.getStringExtra("total_harga")    ?: "0"
        val produk   = intent.getStringExtra("produk")         ?: "-"
        val tanggal  = intent.getStringExtra("tanggal")        ?: "-"
        val komentar = intent.getStringExtra("komentar")       ?: "-"
        val rating   = intent.getIntExtra("rating", 5)
        val foto     = intent.getStringExtra("foto")           ?: ""
        val video    = intent.getStringExtra("video")          ?: ""

        tvDetailKode.text      = "Kode: #$kode"
        tvDetailNama.text      = "Pembeli: $nama"
        tvDetailHp.text        = "No HP: $hp"
        tvDetailTotal.text     = "Total: Rp $total"

        tvDetailProduk.text    = if (produk.isBlank() || produk == "-") "Produk: (tidak ada data)" else "Produk:\n$produk"

        val tanggalFormatted = tanggal
            .replace("T", " ")
            .replace(Regex("\\.\\d+Z$"), "")
            .take(19)
        tvDetailTanggal.text   = "Tanggal: $tanggalFormatted"

        ratingBarDetail.rating = rating.toFloat()
        tvDetailRatingAngka.text = "$rating / 5"
        tvDetailKomentar.text  = komentar

        // Handle Foto
        if (foto.isNotEmpty() && foto != "null") {
            cvDetailFoto.visibility = View.VISIBLE
            val imageRequest = ImageRequest(
                REVIEW_MEDIA_BASE_URL + foto,
                { bitmap -> photoViewDetail.setImageBitmap(bitmap) },
                0, 0,
                ImageView.ScaleType.FIT_CENTER,
                Bitmap.Config.RGB_565,
                { cvDetailFoto.visibility = View.GONE }
            )
            requestQueue.add(imageRequest)
        } else {
            cvDetailFoto.visibility = View.GONE
        }

        // FIX 5: Handle Video dengan benar
        if (video.isNotEmpty() && video != "null") {
            cvDetailVideo.visibility  = View.VISIBLE
            btnDetailPause.visibility = View.GONE
            btnDetailPlay.visibility  = View.VISIBLE

            // FIX 6: Pastikan URL video benar — log untuk debug
            val videoUrl = REVIEW_MEDIA_BASE_URL + video
            android.util.Log.d("ReviewDetail", "Video URL: $videoUrl")

            // FIX 7: Set video URI setelah view ter-attach, bukan langsung
            vvDetailVideo.post {
                vvDetailVideo.setVideoURI(Uri.parse(videoUrl))
                setupVideoControls()
            }
        } else {
            cvDetailVideo.visibility = View.GONE
        }
    }

    private fun setupVideoControls() {
        vvDetailVideo.setOnPreparedListener { mp ->
            isVideoPrepared = true
            mp.setLooping(false)
            // FIX 8: Beberapa device return duration 0 sebelum start,
            // gunakan postDelayed sebagai fallback
            if (mp.duration > 0) {
                sbDetailVideo.max = mp.duration
            } else {
                handler.postDelayed({
                    if (vvDetailVideo.duration > 0) {
                        sbDetailVideo.max = vvDetailVideo.duration
                    }
                }, 500)
            }
            startUpdatingSeekBar()
        }

        btnDetailPlay.setOnClickListener {
            if (isVideoPrepared) {
                vvDetailVideo.start()
                btnDetailPlay.visibility  = View.GONE
                btnDetailPause.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Video sedang disiapkan...", Toast.LENGTH_SHORT).show()
            }
        }

        btnDetailPause.setOnClickListener {
            vvDetailVideo.pause()
            btnDetailPause.visibility = View.GONE
            btnDetailPlay.visibility  = View.VISIBLE
        }

        sbDetailVideo.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isVideoPrepared) vvDetailVideo.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        vvDetailVideo.setOnCompletionListener {
            sbDetailVideo.progress    = 0
            vvDetailVideo.seekTo(0)
            btnDetailPause.visibility = View.GONE
            btnDetailPlay.visibility  = View.VISIBLE
        }

        vvDetailVideo.setOnErrorListener { _, what, extra ->
            android.util.Log.e("ReviewDetail", "Video error: what=$what extra=$extra")
            isVideoPrepared = false
            Toast.makeText(
                this,
                "Gagal memuat video (error $what). Periksa koneksi jaringan.",
                Toast.LENGTH_LONG
            ).show()
            cvDetailVideo.visibility = View.GONE
            true
        }
    }

    // FIX 9: Update SeekBar juga saat video disiapkan tapi belum play (progress = 0)
    private fun startUpdatingSeekBar() {
        updateRunnable = object : Runnable {
            override fun run() {
                try {
                    if (isVideoPrepared) {
                        // Update max jika belum ter-set dengan benar
                        if (sbDetailVideo.max == 0 && vvDetailVideo.duration > 0) {
                            sbDetailVideo.max = vvDetailVideo.duration
                        }
                        if (vvDetailVideo.isPlaying) {
                            sbDetailVideo.progress = vvDetailVideo.currentPosition
                        }
                    }
                    handler.postDelayed(this, 250)
                } catch (e: Exception) {
                    android.util.Log.e("ReviewDetail", "SeekBar update error: ${e.message}")
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    override fun onPause() {
        super.onPause()
        if (isVideoPrepared && vvDetailVideo.isPlaying) {
            vvDetailVideo.pause()
            btnDetailPause.visibility = View.GONE
            btnDetailPlay.visibility  = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateRunnable?.let { handler.removeCallbacks(it) }
        if (isVideoPrepared) vvDetailVideo.stopPlayback()
        isVideoPrepared = false
    }
}