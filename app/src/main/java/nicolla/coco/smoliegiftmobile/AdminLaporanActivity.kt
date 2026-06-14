package nicolla.coco.smoliegiftmobile

import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.smoliegift.database.DatabaseHelper
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AdminLaporanActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
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

            dbHelper = DatabaseHelper(this)
            llDaftar = findViewById(R.id.llDaftarLaporan)
            tvPendapatan = findViewById(R.id.tvTotalPendapatanAdmin)

            val total = muatDataLaporan()
            tvPendapatan?.text = "Rp $total"
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal memuat Laporan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun muatDataLaporan(): Int {
        val container = llDaftar ?: return 0
        container.removeAllViews()
        
        var totalPendapatan = 0
        val cursor: Cursor = try {
            dbHelper.getLaporanPenjualan()
        } catch (e: Exception) {
            val tvError = TextView(this)
            tvError.text = "Error Database: ${e.message}"
            container.addView(tvError)
            return 0
        }

        val inflater = LayoutInflater.from(this)

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Belum ada riwayat penjualan sukses."
            tvKosong.setPadding(32, 32, 32, 32)
            tvKosong.gravity = android.view.Gravity.CENTER
            container.addView(tvKosong)
        } else {
            // Index kolom yang aman
            val idIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRANS_ID)
            val nameIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_CUSTOMER_NAME)
            val totalIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_GRAND_TOTAL)
            val dateIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRANS_DATE)
            val imageIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_CUSTOM_IMAGE)
            val eventIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_EVENT_INFO)
            val itemsIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_ITEMS_JSON)
            val waIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_CUSTOMER_WA)

            while (cursor.moveToNext()) {
                try {
                    val id = if (idIdx != -1) cursor.getInt(idIdx) else 0
                    val nama = if (nameIdx != -1) cursor.getString(nameIdx) ?: "-" else "-"
                    val totalItem = if (totalIdx != -1) cursor.getInt(totalIdx) else 0
                    val rawDate = if (dateIdx != -1) cursor.getString(dateIdx) ?: "-" else "-"
                    val customImageBase64 = if (imageIdx != -1) cursor.getString(imageIdx) else null
                    val eventInfo = if (eventIdx != -1) cursor.getString(eventIdx) else null
                    val itemsJson = if (itemsIdx != -1) cursor.getString(itemsIdx) else null
                    val wa = if (waIdx != -1) cursor.getString(waIdx) ?: "-" else "-"

                    totalPendapatan += totalItem

                    val itemView = inflater.inflate(R.layout.item_transaksi_admin, container, false)

                    itemView.findViewById<TextView>(R.id.tvAdminTransId)?.text = "#INV-0$id"
                    
                    val tvStatus = itemView.findViewById<TextView>(R.id.tvAdminTransStatusLabel)
                    tvStatus?.text = "SELESAI"
                    tvStatus?.setTextColor(Color.parseColor("#2E7D32"))
                    
                    itemView.findViewById<TextView>(R.id.tvAdminTransNama)?.text = "Pemesan: $nama"
                    itemView.findViewById<TextView>(R.id.tvAdminTransTotal)?.text = "Rp $totalItem"
                    itemView.findViewById<TextView>(R.id.tvAdminTransWa)?.text = "WA: $wa"

                    val tvProduk = itemView.findViewById<TextView>(R.id.tvAdminTransProduk)
                    if (!itemsJson.isNullOrEmpty()) {
                        try {
                            val jsonArray = JSONArray(itemsJson)
                            val sb = StringBuilder()
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                // ✅ Support key "nama" (baru) dan "name" (lama)
                                val namaProduk = obj.optString("nama",
                                    obj.optString("name", "Produk"))
                                // ✅ Support key "jumlah" (baru) dan "qty" (lama)
                                val qtyProduk = obj.optInt("jumlah",
                                    obj.optInt("qty", 1))
                                // ✅ Tampilkan harga jika ada
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
                    val tvTanggal = itemView.findViewById<TextView>(R.id.tvAdminTransTanggal)
                    if (!eventInfo.isNullOrEmpty() && eventInfo != "Tanpa catatan") {
                        tvTanggal?.text = "Info: $eventInfo"
                    } else {
                        try {
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID"))
                            outputFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                            val date = inputFormat.parse(rawDate)
                            tvTanggal?.text = if (date != null) "Selesai: ${outputFormat.format(date)}" else "Tgl: $rawDate"
                        } catch (e: Exception) {
                            tvTanggal?.text = "Tgl: $rawDate"
                        }
                    }

                    val ivCustomDesign = itemView.findViewById<ImageView>(R.id.ivCustomDesignAdmin)
                    val llContainerImage = itemView.findViewById<LinearLayout>(R.id.llContainerImage)

                    if (!customImageBase64.isNullOrEmpty()) {
                        try {
                            val decodedString = Base64.decode(customImageBase64, Base64.DEFAULT)
                            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            ivCustomDesign?.setImageBitmap(decodedByte)
                            llContainerImage?.visibility = View.VISIBLE
                        } catch (e: Exception) {
                            llContainerImage?.visibility = View.GONE
                        }
                    } else {
                        llContainerImage?.visibility = View.GONE
                    }

                    // Sembunyikan tombol aksi
                    itemView.findViewById<View>(R.id.btnSelesaiPesanan)?.visibility = View.GONE
                    itemView.findViewById<View>(R.id.btnCetakStruk)?.visibility = View.GONE

                    container.addView(itemView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        cursor.close()
        return totalPendapatan
    }
}
