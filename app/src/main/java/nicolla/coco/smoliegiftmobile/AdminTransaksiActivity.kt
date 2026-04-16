package nicolla.coco.smoliegiftmobile

import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
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

                val itemView = inflater.inflate(R.layout.item_transaksi_admin, llDaftar, false)

                itemView.findViewById<TextView>(R.id.tvAdminTransId).text = "#INV-0$id"
                itemView.findViewById<TextView>(R.id.tvAdminTransNama).text = "Pemesan: $nama"
                itemView.findViewById<TextView>(R.id.tvAdminTransWa).text = "WA: $wa"
                itemView.findViewById<TextView>(R.id.tvAdminTransMetode).text = "Metode: $metode"
                itemView.findViewById<TextView>(R.id.tvAdminTransTotal).text = "Rp $total"

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