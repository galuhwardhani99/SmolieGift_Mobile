package nicolla.coco.smoliegiftmobile

import android.app.AlertDialog
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper

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
        llDaftar.removeAllViews() // Bersihkan layar sebelum memuat ulang
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_TRANSACTIONS} ORDER BY ${DatabaseHelper.COLUMN_TRANS_ID} DESC",
            null
        )
        val inflater = LayoutInflater.from(this)

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Belum ada pesanan yang masuk atau semua pesanan sudah diselesaikan."
            tvKosong.setPadding(16, 16, 16, 16)
            llDaftar.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANS_ID))
                val nama =
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_NAME))
                val metode =
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAYMENT_METHOD))
                val total =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GRAND_TOTAL))

                val itemView = inflater.inflate(R.layout.item_transaksi_admin, llDaftar, false)

                itemView.findViewById<TextView>(R.id.tvAdminTransId).text = "#INV-0$id"
                itemView.findViewById<TextView>(R.id.tvAdminTransNama).text = "Pemesan: $nama"
                itemView.findViewById<TextView>(R.id.tvAdminTransMetode).text = "Metode: $metode"
                itemView.findViewById<TextView>(R.id.tvAdminTransTotal).text = "Rp $total"

                // --- LOGIKA TOMBOL ---
                val btnSelesai = itemView.findViewById<Button>(R.id.btnSelesaiPesanan)

                // Logika Selesaikan Pesanan (Pindah ke DB Laporan)
                btnSelesai.setOnClickListener {
                    konfirmasiSelesai(id, nama)
                }

                llDaftar.addView(itemView)
            }
        }
        cursor.close()
        db.close()
    }

    private fun konfirmasiSelesai(id: Int, nama: String) {
        AlertDialog.Builder(this)
            .setTitle("Pesanan Selesai?")
            .setMessage("Pesanan atas nama $nama sudah selesai diproses? Data ini akan dipindahkan ke Laporan Penjualan.")
            .setPositiveButton("Ya, Sudah") { _, _ ->
                // PANGGIL FUNGSI PINDAH KE HISTORY
                val sukses = dbHelper.selesaikanPesanan(id)

                if (sukses) {
                    Toast.makeText(this, "Berhasil masuk ke Laporan!", Toast.LENGTH_SHORT).show()
                    muatDataPesanan() // Refresh halaman
                }
            }
            .setNegativeButton("Belum", null)
            .show()
    }
}