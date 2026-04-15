package nicolla.coco.smoliegiftmobile

import android.database.Cursor
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper

class CartActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var imageBase64UntukPesanan: String? = null // Penampung gambar untuk dikirim ke Transaksi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        dbHelper = DatabaseHelper(this)

        val llDaftar = findViewById<LinearLayout>(R.id.llDaftarKeranjang)
        val tvTotalAkhir = findViewById<TextView>(R.id.tvTotalBayarAkhir)
        val btnKonfirmasi = findViewById<Button>(R.id.btnKonfirmasi)
        val etNama = findViewById<EditText>(R.id.etNamaPemesan)
        val etWa = findViewById<EditText>(R.id.etNoWhatsapp)

        val grandTotal = tampilkanDataDanHitungTotal(llDaftar)
        tvTotalAkhir.text = "Rp $grandTotal"

        btnKonfirmasi.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val wa = etWa.text.toString().trim()

            if (grandTotal <= 0) {
                Toast.makeText(this, "Keranjang Anda masih kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nama.isEmpty() || wa.isEmpty()) {
                Toast.makeText(this, "Harap isi Nama dan Nomor WhatsApp!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // KOREKSI: Kirim 5 parameter (termasuk imageBase64UntukPesanan)
            val sukses = dbHelper.buatPesanan(nama, wa, "Tunai", grandTotal, imageBase64UntukPesanan)

            if (sukses) {
                dbHelper.kosongkanKeranjang()
                Toast.makeText(this, "Pesanan Berhasil! Terima kasih, $nama.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Gagal menyimpan pesanan.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun tampilkanDataDanHitungTotal(container: LinearLayout): Int {
        val cursor: Cursor = dbHelper.getSemuaKeranjang()
        val inflater = LayoutInflater.from(this)
        var totalHargaSemua = 0

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Keranjang Anda masih kosong."
            tvKosong.setPadding(32, 32, 32, 32)
            container.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME))
                val qty = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY))
                val hargaTotalItem = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_PRICE))

                // KOREKSI: Ambil data gambar kustom dari tiap item keranjang
                val imgBase64 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOM_IMAGE))

                totalHargaSemua += hargaTotalItem

                val itemView = inflater.inflate(R.layout.item_cart, container, false)
                itemView.findViewById<TextView>(R.id.tvItemName).text = nama
                itemView.findViewById<TextView>(R.id.tvItemQty).text = qty.toString()
                itemView.findViewById<TextView>(R.id.tvItemPrice).text = "Rp $hargaTotalItem"

                // KOREKSI: Tampilkan gambar kustom jika ada
                val ivPreview = itemView.findViewById<ImageView>(R.id.ivItemImage)
                if (!imgBase64.isNullOrEmpty()) {
                    val imageBytes = Base64.decode(imgBase64, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ivPreview.setImageBitmap(decodedImage)

                    // Simpan gambar terakhir yang ditemukan untuk dimasukkan ke data Transaksi
                    imageBase64UntukPesanan = imgBase64
                }

                container.addView(itemView)
            }
        }
        cursor.close()
        return totalHargaSemua
    }
}