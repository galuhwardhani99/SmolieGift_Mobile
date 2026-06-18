package nicolla.coco.smoliegiftmobile

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class AdminReviewActivity : AppCompatActivity() {

    private lateinit var llReviewContainer: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var pbLoading: ProgressBar

    private val REVIEW_IMAGE_BASE_URL = "http://192.168.1.28/toko-smolie/public/reviews/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_review)

        val toolbar = findViewById<Toolbar>(R.id.toolbarReview)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        llReviewContainer = findViewById(R.id.llReviewContainer)
        swipeRefresh      = findViewById(R.id.swipeRefresh)
        pbLoading         = findViewById(R.id.pbLoading)

        swipeRefresh.setOnRefreshListener { loadReviews() }
        loadReviews()
    }

    private fun loadReviews() {
        pbLoading.visibility = View.VISIBLE
        ApiClient.getAllReviews { response ->
            runOnUiThread {
                pbLoading.visibility      = View.GONE
                swipeRefresh.isRefreshing = false
                llReviewContainer.removeAllViews()
                android.util.Log.d("ReviewDebug", "Response: $response")

                if (response == null) {
                    Toast.makeText(this, "Gagal memuat ulasan", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                try {
                    val json     = JSONObject(response)
                    val data     = json.getJSONArray("data")
                    val inflater = LayoutInflater.from(this)

                    if (data.length() == 0) {
                        val tvEmpty = TextView(this).apply {
                            text = "Belum ada ulasan dari pelanggan."
                            setPadding(32, 64, 32, 32)
                            gravity = android.view.Gravity.CENTER
                        }
                        llReviewContainer.addView(tvEmpty)
                        return@runOnUiThread
                    }

                    for (i in 0 until data.length()) {
                        val item         = data.getJSONObject(i)
                        val kode         = item.optString("kode_transaksi", "-")
                        val nama         = item.optString("nama_pembeli", "-")
                        val hp           = item.optString("no_hp", "-")
                        val total        = item.optString("total_harga", "0")
                        val komentar     = item.optString("komentar", "-")
                        val rating       = item.optInt("rating", 5)
                        val foto         = item.optString("foto", "")
                        val video        = item.optString("video", "")
                        val tanggal      = item.optString("created_at", "-")
                        val itemsJsonStr = item.optString("items_json", "")

                        // Parse produk: coba dari array relasi dulu, fallback ke items_json
                        val produkArr  = item.optJSONArray("produk") ?: JSONArray()
                        val produkText = StringBuilder()

                        if (produkArr.length() > 0) {
                            for (j in 0 until produkArr.length()) {
                                val p = produkArr.getJSONObject(j)
                                produkText.append("• ${p.optString("nama_produk")} x${p.optInt("qty", 1)}\n")
                            }
                        } else if (itemsJsonStr.isNotEmpty() && itemsJsonStr != "null") {
                            try {
                                val arr = JSONArray(itemsJsonStr)
                                for (j in 0 until arr.length()) {
                                    val p    = arr.getJSONObject(j)
                                    val nama2 = p.optString("nama", p.optString("name", "Produk"))
                                    val qty  = p.optInt("jumlah", p.optInt("qty", 1))
                                    produkText.append("• $nama2 x$qty\n")
                                }
                            } catch (e: Exception) {
                                produkText.append("(data produk tidak tersedia)")
                            }
                        } else {
                            produkText.append("(data produk tidak tersedia)")
                        }

                        val itemView = inflater.inflate(R.layout.item_review, llReviewContainer, false)

                        itemView.findViewById<TextView>(R.id.tvReviewTransCode).text   = "#$kode"
                        itemView.findViewById<TextView>(R.id.tvReviewNamaPembeli).text = nama
                        itemView.findViewById<TextView>(R.id.tvReviewNoHp).text        = hp
                        itemView.findViewById<TextView>(R.id.tvReviewTotal).text       = "Total: Rp $total"
                        itemView.findViewById<TextView>(R.id.tvReviewProduk).text      = produkText.toString().trimEnd()
                        itemView.findViewById<TextView>(R.id.tvReviewText).text        = komentar
                        itemView.findViewById<TextView>(R.id.tvReviewDate).text        = tanggal.take(10)
                        itemView.findViewById<RatingBar>(R.id.ratingBarReview).rating  = rating.toFloat()

                        // Load foto preview
                        val ivPhoto = itemView.findViewById<ImageView>(R.id.ivReviewImage)
                        if (foto.isNotEmpty() && foto != "null") {
                            ivPhoto.visibility = View.VISIBLE
                            Thread {
                                try {
                                    val bmp = BitmapFactory.decodeStream(
                                        URL(REVIEW_IMAGE_BASE_URL + foto).openStream()
                                    )
                                    runOnUiThread { ivPhoto.setImageBitmap(bmp) }
                                } catch (e: Exception) {
                                    runOnUiThread { ivPhoto.visibility = View.GONE }
                                }
                            }.start()
                        } else {
                            ivPhoto.visibility = View.GONE
                        }

                        // Indikator video tidak ditampilkan

                        // Tombol Lihat Detail
                        itemView.findViewById<MaterialButton>(R.id.btnLihatDetail).setOnClickListener {
                            val intent = Intent(this, ReviewDetailActivity::class.java).apply {
                                putExtra("kode_transaksi", kode)
                                putExtra("nama_pembeli",   nama)
                                putExtra("no_hp",          hp)
                                putExtra("total_harga",    total)
                                putExtra("produk",         produkText.toString().trimEnd())
                                putExtra("tanggal",        tanggal)
                                putExtra("komentar",       komentar)
                                putExtra("rating",         rating)
                                putExtra("foto",           foto)
                                putExtra("video",          video)
                                putExtra("items_json",     itemsJsonStr)
                            }
                            startActivity(intent)
                        }

                        llReviewContainer.addView(itemView)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}