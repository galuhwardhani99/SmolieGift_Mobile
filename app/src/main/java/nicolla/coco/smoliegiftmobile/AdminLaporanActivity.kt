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
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.Locale

class AdminLaporanActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_laporan)

        dbHelper = DatabaseHelper(this)
        val llDaftar = findViewById<LinearLayout>(R.id.llDaftarLaporan)
        val tvPendapatan = findViewById<TextView>(R.id.tvTotalPendapatanAdmin)

        val totalPendapatanGlobal = muatDataLaporan(llDaftar)
        tvPendapatan.text = "Rp $totalPendapatanGlobal"
    }

    private fun muatDataLaporan(container: LinearLayout): Int {
        val cursor: Cursor = dbHelper.getLaporanPenjualan()
        val inflater = LayoutInflater.from(this)
        var totalPendapatan = 0

        container.removeAllViews()

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Belum ada riwayat penjualan sukses."
            tvKosong.setPadding(32, 32, 32, 32)
            container.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANS_ID))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_NAME))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GRAND_TOTAL))
                val rawDate = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANS_DATE))
                val customImageBase64 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOM_IMAGE))
                val eventInfo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_INFO))

                totalPendapatan += total

                val itemView = inflater.inflate(R.layout.item_transaksi_admin, container, false)

                itemView.findViewById<TextView>(R.id.tvAdminTransId).text = "SELESAI (INV-0$id)"
                itemView.findViewById<TextView>(R.id.tvAdminTransNama).text = "Pemesan: $nama"
                itemView.findViewById<TextView>(R.id.tvAdminTransTotal).text = "Rp $total"

                // Menampilkan Info Tanggal/Waktu Acara jika ada (Input Pembeli)
                val tvTanggal = itemView.findViewById<TextView>(R.id.tvAdminTransTanggal)
                if (!eventInfo.isNullOrEmpty()) {
                    tvTanggal.text = "Waktu Acara: $eventInfo"
                    tvTanggal.setTextColor(Color.parseColor("#DD3827"))
                } else {
                    // Jika reguler, tampilkan waktu transaksi sistem
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm", Locale("id", "ID"))
                        val date = inputFormat.parse(rawDate)
                        if (date != null) {
                            tvTanggal.text = "Selesai: ${outputFormat.format(date)}"
                        } else {
                            tvTanggal.text = "Waktu: $rawDate"
                        }
                        tvTanggal.setTextColor(Color.parseColor("#64748B"))
                    } catch (e: Exception) {
                        tvTanggal.text = "Waktu: $rawDate"
                    }
                }

                // Menampilkan Gambar Custom (Kartu Ucapan) jika ada
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

                // Sembunyikan elemen yang tidak perlu di laporan
                itemView.findViewById<TextView>(R.id.tvAdminTransWa).visibility = View.GONE
                itemView.findViewById<Button>(R.id.btnSelesaiPesanan).visibility = View.GONE

                container.addView(itemView)
            }
        }
        cursor.close()
        return totalPendapatan
    }
}