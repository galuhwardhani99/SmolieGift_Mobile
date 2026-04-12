package nicolla.coco.smoliegiftmobile

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper

class CartActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        dbHelper = DatabaseHelper(this)

        val llDaftar = findViewById<LinearLayout>(R.id.llDaftarKeranjang)
        val tvTotalAkhir = findViewById<TextView>(R.id.tvTotalBayarAkhir)
        val btnKonfirmasi = findViewById<Button>(R.id.btnKonfirmasi)

        // KOREKSI: Tambahkan binding untuk EditText Nama & WhatsApp
        val etNama = findViewById<EditText>(R.id.etNamaPemesan)
        val etWa = findViewById<EditText>(R.id.etNoWhatsapp)

        // Panggil fungsi untuk menampilkan data dan menghitung total
        val grandTotal = tampilkanDataDanHitungTotal(llDaftar)

        // Tampilkan total ke layar
        tvTotalAkhir.text = "Rp $grandTotal"

        btnKonfirmasi.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val wa = etWa.text.toString().trim()

            // 1. Validasi: Jangan biarkan checkout jika keranjang kosong
            if (grandTotal <= 0) {
                Toast.makeText(this, "Keranjang Anda masih kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validasi: Pastikan Nama dan WA sudah diisi
            if (nama.isEmpty() || wa.isEmpty()) {
                Toast.makeText(this, "Harap isi Nama dan Nomor WhatsApp Anda!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Simpan ke Database (Tabel Transactions)
            val sukses = dbHelper.buatPesanan(nama, wa, "Tunai", grandTotal)

            if (sukses) {
                // 4. Kosongkan Keranjang SQLite setelah pesanan sukses
                dbHelper.kosongkanKeranjang()

                Toast.makeText(this, "Pesanan Berhasil! Terima kasih, $nama.", Toast.LENGTH_LONG).show()

                // Tutup halaman ini dan kembali ke Dashboard
                finish()
            } else {
                Toast.makeText(this, "Gagal menyimpan pesanan. Coba lagi.", Toast.LENGTH_SHORT).show()
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
            tvKosong.setPadding(16, 16, 16, 16)
            container.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME))
                val qty = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY))
                val hargaTotalItem = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_PRICE))

                totalHargaSemua += hargaTotalItem

                val itemView = inflater.inflate(R.layout.item_cart, container, false)
                val tvNama = itemView.findViewById<TextView>(R.id.tvItemName)
                val tvHarga = itemView.findViewById<TextView>(R.id.tvItemPrice)
                val tvQty = itemView.findViewById<TextView>(R.id.tvItemQty)

                tvNama.text = nama
                tvQty.text = qty.toString()
                tvHarga.text = "Rp $hargaTotalItem"

                container.addView(itemView)
            }
        }
        cursor.close()
        return totalHargaSemua
    }
}