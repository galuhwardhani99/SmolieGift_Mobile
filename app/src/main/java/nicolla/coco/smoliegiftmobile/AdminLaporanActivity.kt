package nicolla.coco.smoliegiftmobile

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper

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
            container.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANS_ID))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_NAME))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GRAND_TOTAL))

                totalPendapatan += total

                val itemView = inflater.inflate(R.layout.item_transaksi_admin, container, false)

                itemView.findViewById<TextView>(R.id.tvAdminTransId).text = "SELESAI (INV-0$id)"
                itemView.findViewById<TextView>(R.id.tvAdminTransNama).text = "Pemesan: $nama"
                itemView.findViewById<TextView>(R.id.tvAdminTransTotal).text = "Rp $total"

                itemView.findViewById<Button>(R.id.btnSelesaiPesanan).visibility = View.GONE

                container.addView(itemView)
            }
        }
        cursor.close()
        return totalPendapatan
    }
}