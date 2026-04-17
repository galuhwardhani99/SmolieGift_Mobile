package nicolla.coco.smoliegiftmobile

import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
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
import com.example.smoliegift.database.DatabaseHelper
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AdminTransaksiActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var llDaftar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_transaksi)

        dbHelper = DatabaseHelper(this)
        llDaftar = findViewById(R.id.llDaftarTransaksi)

        muatDataPesanan()
    }

    private fun muatDataPesanan() {
        llDaftar.removeAllViews()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_TRANSACTIONS} ORDER BY ${DatabaseHelper.COLUMN_TRANS_ID} DESC",
            null
        )
        val inflater = LayoutInflater.from(this)

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Belum ada pesanan yang masuk."
            tvKosong.setPadding(32, 32, 32, 32)
            tvKosong.gravity = android.view.Gravity.CENTER
            llDaftar.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANS_ID))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_NAME))
                val wa = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_WA))
                val metode = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAYMENT_METHOD))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GRAND_TOTAL))
                val customImageBase64 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOM_IMAGE))
                val rawDate = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANS_DATE))
                val eventInfo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_INFO))
                val itemsJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEMS_JSON))

                val itemView = inflater.inflate(R.layout.item_transaksi_admin, llDaftar, false)

                itemView.findViewById<TextView>(R.id.tvAdminTransId).text = "#INV-0$id"
                itemView.findViewById<TextView>(R.id.tvAdminTransNama).text = "Pemesan: $nama"
                itemView.findViewById<TextView>(R.id.tvAdminTransWa).text = "WA: $wa"
                itemView.findViewById<TextView>(R.id.tvAdminTransMetode).text = "Metode: $metode"
                itemView.findViewById<TextView>(R.id.tvAdminTransTotal).text = "Rp $total"

                val tvProduk = itemView.findViewById<TextView>(R.id.tvAdminTransProduk)
                if (!itemsJson.isNullOrEmpty()) {
                    try {
                        val jsonArray = JSONArray(itemsJson)
                        val sb = StringBuilder("Produk:\n")
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val pName = obj.getString("name")
                            val pQty = obj.getInt("qty")
                            sb.append("- $pName ($pQty pcs)\n")
                        }
                        tvProduk.text = sb.toString().trim()
                    } catch (e: Exception) {
                        tvProduk.text = "Produk: Error memuat data"
                    }
                } else {
                    tvProduk.text = "Produk: Tidak ada detail"
                }

                val tvTanggal = itemView.findViewById<TextView>(R.id.tvAdminTransTanggal)
                if (!eventInfo.isNullOrEmpty()) {
                    tvTanggal.text = "Info Acara: $eventInfo"
                    tvTanggal.setTextColor(Color.parseColor("#DD3827"))
                } else {
                    try {
                        // Konversi UTC dari DB ke WIB (Asia/Jakarta)
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                        
                        val outputFormat = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm 'WIB'", Locale("id", "ID"))
                        outputFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                        
                        val date = inputFormat.parse(rawDate)
                        if (date != null) {
                            tvTanggal.text = "Waktu Pesan: ${outputFormat.format(date)}"
                        } else {
                            tvTanggal.text = "Waktu: $rawDate"
                        }
                        tvTanggal.setTextColor(Color.parseColor("#64748B"))
                    } catch (e: Exception) {
                        tvTanggal.text = "Waktu: $rawDate"
                    }
                }

                val ivCustomDesign = itemView.findViewById<ImageView>(R.id.ivCustomDesignAdmin)
                val llContainerImage = itemView.findViewById<LinearLayout>(R.id.llContainerImage)

                if (!customImageBase64.isNullOrEmpty()) {
                    try {
                        val decodedString = Base64.decode(customImageBase64, Base64.DEFAULT)
                        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        ivCustomDesign.setImageBitmap(decodedByte)
                        llContainerImage.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        llContainerImage.visibility = View.GONE
                    }
                } else {
                    llContainerImage.visibility = View.GONE
                }

                val btnSelesai = itemView.findViewById<Button>(R.id.btnSelesaiPesanan)
                btnSelesai.setOnClickListener { konfirmasiSelesai(id, nama) }

                llDaftar.addView(itemView)
            }
        }
        cursor.close()
        db.close()
    }

    private fun konfirmasiSelesai(id: Int, nama: String) {
        AlertDialog.Builder(this)
            .setTitle("Pesanan Selesai?")
            .setMessage("Pesanan atas nama $nama sudah selesai? Data akan dipindahkan ke Laporan.")
            .setPositiveButton("Ya, Selesai") { _, _ ->
                val sukses = dbHelper.selesaikanPesanan(id)
                if (sukses) {
                    Toast.makeText(this, "Berhasil masuk ke Laporan!", Toast.LENGTH_SHORT).show()
                    muatDataPesanan()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
