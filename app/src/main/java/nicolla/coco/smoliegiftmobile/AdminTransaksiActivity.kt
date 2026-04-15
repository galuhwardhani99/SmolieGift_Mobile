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
                // KOREKSI: Tambahkan pengambilan data nomor WA dari database
                val wa = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_WA))
                val metode = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAYMENT_METHOD))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GRAND_TOTAL))

                val itemView = inflater.inflate(R.layout.item_transaksi_admin, llDaftar, false)

                itemView.findViewById<TextView>(R.id.tvAdminTransId).text = "#INV-0$id"
                itemView.findViewById<TextView>(R.id.tvAdminTransNama).text = "Pemesan: $nama"

                // KOREKSI: Tampilkan nomor WA pelanggan ke TextView (Data real)
                itemView.findViewById<TextView>(R.id.tvAdminTransWa).text = "WA: $wa"

                itemView.findViewById<TextView>(R.id.tvAdminTransMetode).text = "Metode: $metode"
                itemView.findViewById<TextView>(R.id.tvAdminTransTotal).text = "Rp $total"

                // --- LOGIKA TOMBOL ---
                val btnSelesai = itemView.findViewById<Button>(R.id.btnSelesaiPesanan)

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