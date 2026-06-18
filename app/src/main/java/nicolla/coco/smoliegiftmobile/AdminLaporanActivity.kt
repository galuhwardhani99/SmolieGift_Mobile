package nicolla.coco.smoliegiftmobile

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AdminLaporanActivity : AppCompatActivity() {

    private var llDaftar: LinearLayout? = null
    private var tvPendapatan: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_admin_laporan)

            val toolbar = findViewById<Toolbar>(R.id.toolbarLaporan)
            if (toolbar != null) {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                toolbar.setNavigationOnClickListener { finish() }
            }

            llDaftar = findViewById(R.id.llDaftarLaporan)
            tvPendapatan = findViewById(R.id.tvTotalPendapatanAdmin)

            muatDataLaporan()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal memuat Laporan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun muatDataLaporan() {
        val container = llDaftar ?: return
        container.removeAllViews()

        val tvLoading = TextView(this).apply {
            text = "Memuat laporan..."
            setPadding(32, 32, 32, 32)
            gravity = android.view.Gravity.CENTER
        }
        container.addView(tvLoading)

        ApiClient.getAllTransaksiLengkap { response ->
            runOnUiThread {
                container.removeAllViews()

                if (response == null) {
                    val tv = TextView(this).apply {
                        text = "Gagal memuat laporan. Periksa koneksi internet."
                        setPadding(32, 32, 32, 32)
                        gravity = android.view.Gravity.CENTER
                    }
                    container.addView(tv)
                    return@runOnUiThread
                }

                try {
                    val json = JSONObject(response)
                    val data = json.getJSONArray("data")
                    val inflater = LayoutInflater.from(this)

                    if (data.length() == 0) {
                        val tvKosong = TextView(this).apply {
                            text = "Belum ada riwayat transaksi."
                            setPadding(32, 64, 32, 32)
                            gravity = android.view.Gravity.CENTER
                        }
                        container.addView(tvKosong)
                        tvPendapatan?.text = "Rp 0"
                        return@runOnUiThread
                    }

                    var totalPendapatan = 0

                    val itemsSorted = (0 until data.length())
                        .map { data.getJSONObject(it) }
                        .sortedByDescending { it.optString("created_at", "") }

                    for (item in itemsSorted) {
                        val id         = item.optInt("id", 0)
                        val nama       = item.optString("nama_pembeli", "-")
                        val noHp       = item.optString("no_hp", "-")
                        val metode     = item.optString("metode_pembayaran", "-")
                        val totalHarga = item.optInt("total_harga", item.optString("total_harga", "0").toDoubleOrNull()?.toInt() ?: 0)
                        val status     = item.optString("status", "pending")
                        val jenis      = item.optString("jenis_pesanan", "")
                        val rawDate    = item.optString("created_at", "-")
                        val itemsJson  = item.optString("items_json", "")

                        if (status == "selesai") {
                            totalPendapatan += totalHarga
                        }

                        val itemView = inflater.inflate(R.layout.item_transaksi_admin, container, false)

                        // ── ID ────────────────────────────────────────────────
                        itemView.findViewById<TextView>(R.id.tvAdminTransId)?.text = "#INV-0$id"

                        // ── Status ────────────────────────────────────────────
                        val tvStatus = itemView.findViewById<TextView>(R.id.tvAdminTransStatusLabel)
                        when (status) {
                            "selesai" -> {
                                tvStatus?.text = "SELESAI"
                                tvStatus?.setTextColor(Color.parseColor("#2E7D32"))
                            }
                            "dibatalkan" -> {
                                tvStatus?.text = "DIBATALKAN"
                                tvStatus?.setTextColor(Color.parseColor("#94A3B8"))
                            }
                            else -> {
                                tvStatus?.text = "PENDING"
                                tvStatus?.setTextColor(Color.parseColor("#EF4444"))
                            }
                        }

                        // ── Info dasar ────────────────────────────────────────
                        itemView.findViewById<TextView>(R.id.tvAdminTransNama)?.text = "Pemesan: $nama"
                        itemView.findViewById<TextView>(R.id.tvAdminTransTotal)?.text = "Rp $totalHarga"
                        itemView.findViewById<TextView>(R.id.tvAdminTransWa)?.text = "HP: $noHp"
                        itemView.findViewById<TextView>(R.id.tvAdminTransMetode)?.text = "Bayar: $metode"

                        // ── Jenis pesanan: Online / Offline ───────────────────
                        itemView.findViewById<TextView>(R.id.tvAdminTransCatatan)?.text =
                            if (jenis.equals("offline", ignoreCase = true) ||
                                jenis.equals("Beli di Toko", ignoreCase = true))
                                "Pesanan Offline"
                            else
                                "Pesanan Online"

                        // ── Daftar produk ─────────────────────────────────────
                        val tvProduk = itemView.findViewById<TextView>(R.id.tvAdminTransProduk)
                        if (itemsJson.isNotEmpty() && itemsJson != "null") {
                            try {
                                val jsonArray = JSONArray(itemsJson)
                                val sb = StringBuilder()
                                for (i in 0 until jsonArray.length()) {
                                    val obj         = jsonArray.getJSONObject(i)
                                    val namaProduk  = obj.optString("nama", obj.optString("name", "Produk"))
                                    val qtyProduk   = obj.optInt("jumlah", obj.optInt("qty", 1))
                                    val hargaProduk = obj.optInt("harga", 0)
                                    if (hargaProduk > 0) {
                                        sb.append("• $namaProduk × $qtyProduk (Rp $hargaProduk)\n")
                                    } else {
                                        sb.append("• $namaProduk × $qtyProduk\n")
                                    }
                                }
                                tvProduk?.text = sb.toString().trim()
                            } catch (e: Exception) {
                                tvProduk?.text = "Detail produk tidak tersedia"
                            }
                        } else {
                            tvProduk?.text = "Tidak ada detail produk"
                        }

                        // ── Tanggal → format WIB ──────────────────────────────
                        val tvTanggal = itemView.findViewById<TextView>(R.id.tvAdminTransTanggal)
                        try {
                            val cleanDate = rawDate
                                .replace(Regex("\\.\\d{1,6}Z?$"), "")
                                .replace("T", " ")
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            inputFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID"))
                            outputFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                            val date = inputFormat.parse(cleanDate)
                            tvTanggal?.text = if (date != null) outputFormat.format(date) else rawDate
                        } catch (e: Exception) {
                            tvTanggal?.text = rawDate
                        }

                        // ── Sembunyikan tombol aksi (read-only) ───────────────
                        itemView.findViewById<View>(R.id.btnSelesaiPesanan)?.visibility = View.GONE
                        itemView.findViewById<View>(R.id.btnBatalkanPesanan)?.visibility = View.GONE
                        itemView.findViewById<View>(R.id.btnCetakStruk)?.visibility = View.GONE
                        itemView.findViewById<View>(R.id.llContainerImage)?.visibility = View.GONE

                        container.addView(itemView)
                    }

                    tvPendapatan?.text = "Rp $totalPendapatan"

                } catch (e: Exception) {
                    e.printStackTrace()
                    val tvError = TextView(this).apply {
                        text = "Error memproses data: ${e.message}"
                        setPadding(32, 32, 32, 32)
                    }
                    container.addView(tvError)
                }
            }
        }
    }
}