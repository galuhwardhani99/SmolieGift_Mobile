package nicolla.coco.smoliegiftmobile

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.json.JSONArray
import org.json.JSONObject

class AdminKatalogViewActivity : AppCompatActivity() {

    private lateinit var gvKatalog: GridView
    private val listProduk = mutableListOf<JSONObject>()
    private val listKategori = mutableListOf<Pair<Int, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_katalog_view)

        val toolbar = findViewById<Toolbar>(R.id.toolbarAdminKatalog)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setNavigationOnClickListener { finish() }
        }

        gvKatalog = findViewById(R.id.gvKatalogAdmin)

        loadKategori {
            loadDataProduk()
        }
    }

    private fun loadKategori(onDone: () -> Unit) {
        ApiClient.getKategori { list ->
            runOnUiThread {
                listKategori.clear()
                listKategori.addAll(list)
                onDone()
            }
        }
    }

    private fun loadDataProduk() {
        ApiClient.getAllProducts { response ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                
                if (response == null) {
                    Toast.makeText(this@AdminKatalogViewActivity, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                try {
                    val json = JSONObject(response)
                    val data = json.optJSONArray("data") ?: JSONArray()
                    listProduk.clear()
                    for (i in 0 until data.length()) {
                        listProduk.add(data.getJSONObject(i))
                    }

                    val adapter = object : BaseAdapter() {
                        override fun getCount() = listProduk.size
                        override fun getItem(pos: Int) = listProduk[pos]
                        override fun getItemId(pos: Int) = pos.toLong()

                        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
                            val view = convertView ?: LayoutInflater.from(this@AdminKatalogViewActivity)
                                .inflate(R.layout.item_produk_pembeli, parent, false)

                            val produk = listProduk[pos]
                            val nama = produk.optString("nama_produk", "Produk")
                            val kategoriId = produk.optString("kategori_id", "-")
                            val harga = produk.optString("harga", "0")
                            val image = produk.optString("gambar", "")

                            val namaKategori = listKategori.find { it.first.toString() == kategoriId }?.second ?: kategoriId

                            view.findViewById<TextView>(R.id.tvPembeliProdName)?.text = nama
                            view.findViewById<TextView>(R.id.tvPembeliProdCat)?.text = namaKategori
                            view.findViewById<TextView>(R.id.tvPembeliProdPrice)?.text = "Rp $harga"
                            
                            // Sembunyikan tombol Pesan karena ini hanya tampilan admin
                            view.findViewById<Button>(R.id.btnPesanKatalog)?.visibility = View.GONE

                            val ivProduk = view.findViewById<ImageView>(R.id.ivPembeliProdImage)
                            if (ivProduk != null && image.isNotEmpty()) {
                                Thread {
                                    try {
                                        val url = java.net.URL(ApiClient.IMAGE_BASE_URL + image)
                                        val bmp = BitmapFactory.decodeStream(url.openStream())
                                        runOnUiThread { if (!isFinishing) ivProduk.setImageBitmap(bmp) }
                                    } catch (e: Exception) {
                                        runOnUiThread { if (!isFinishing) ivProduk.setImageResource(android.R.drawable.ic_menu_gallery) }
                                    }
                                }.start()
                            }

                            return view
                        }
                    }
                    gvKatalog.adapter = adapter

                } catch (e: Exception) {
                    Toast.makeText(this@AdminKatalogViewActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}