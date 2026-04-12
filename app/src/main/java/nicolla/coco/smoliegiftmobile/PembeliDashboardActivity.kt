package nicolla.coco.smoliegiftmobile

import android.app.Dialog
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper

class PembeliDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var gridLayoutProduk: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pembeli_dashboard)

        dbHelper = DatabaseHelper(this)
        gridLayoutProduk = findViewById(R.id.glDaftarProdukPembeli)

        val btnLogout = findViewById<Button>(R.id.btnLogoutPembeli)
        val btnKeranjang = findViewById<Button>(R.id.btnLihatKeranjang)

        btnLogout.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnKeranjang.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Panggil fungsi untuk memuat data dari database
        loadKatalogProduk()
    }

    private fun loadKatalogProduk() {
        gridLayoutProduk.removeAllViews()
        val cursor: Cursor = dbHelper.getSemuaProduk()
        val inflater = LayoutInflater.from(this)

        // MENGHITUNG LEBAR LAYAR HP SECARA DINAMIS
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        // Lebar layar dibagi 2 kolom, dikurangi sedikit ruang untuk margin (jarak antar kotak)
        val itemWidth = (screenWidth / 2) - 48

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Belum ada produk dari admin."
            gridLayoutProduk.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_NAME))
                val kategori = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_CAT))
                val harga = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_PRICE))
                val fotoBase64 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_IMAGE))

                val itemView = inflater.inflate(R.layout.item_produk_pembeli, gridLayoutProduk, false)

                itemView.findViewById<TextView>(R.id.tvPembeliProdName).text = nama
                itemView.findViewById<TextView>(R.id.tvPembeliProdCat).text = kategori
                itemView.findViewById<TextView>(R.id.tvPembeliProdPrice).text = "Rp $harga"

                // Logika Merakit Teks Base64 Menjadi Gambar
                val ivGambar = itemView.findViewById<android.widget.ImageView>(R.id.ivPembeliProdImage)
                if (!fotoBase64.isNullOrEmpty()) {
                    try {
                        val bytes = android.util.Base64.decode(fotoBase64, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ivGambar.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val btnPesan = itemView.findViewById<Button>(R.id.btnPesanKatalog)
                btnPesan.setOnClickListener {
                    tampilkanDialogPesanan(nama, harga)
                }

                // MENERAPKAN UKURAN LEBAR YANG SUDAH DIHITUNG KE MASING-MASING KARTU
                val params = android.widget.GridLayout.LayoutParams()
                params.width = itemWidth // Atur lebar mengikuti hitungan responsif
                params.setMargins(12, 16, 12, 16)
                itemView.layoutParams = params

                gridLayoutProduk.addView(itemView)
            }
        }
        cursor.close()
    }

    private fun tampilkanDialogPesanan(namaProduk: String, hargaDasar: Int) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_pesan_produk)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvJudul = dialog.findViewById<TextView>(R.id.tvDialogJudul)
        val btnTutup = dialog.findViewById<TextView>(R.id.btnTutupDialog)
        val spWarna = dialog.findViewById<Spinner>(R.id.spWarna)
        val tvQty = dialog.findViewById<TextView>(R.id.tvQty)
        val btnMin = dialog.findViewById<Button>(R.id.btnMinQty)
        val btnPlus = dialog.findViewById<Button>(R.id.btnPlusQty)
        val btnTambah = dialog.findViewById<Button>(R.id.btnTambahKeranjang)

        tvJudul.text = namaProduk
        var qtySaatIni = 1

        btnTambah.text = "Tambah - Rp ${hargaDasar * qtySaatIni}"

        val varianOptions = arrayOf("Pilih varian...", "Warna Pastel", "Monokrom", "Custom")
        spWarna.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, varianOptions)

        btnMin.setOnClickListener {
            if (qtySaatIni > 1) {
                qtySaatIni--
                tvQty.text = qtySaatIni.toString()
                btnTambah.text = "Tambah - Rp ${hargaDasar * qtySaatIni}"
            }
        }

        btnPlus.setOnClickListener {
            qtySaatIni++
            tvQty.text = qtySaatIni.toString()
            btnTambah.text = "Tambah - Rp ${hargaDasar * qtySaatIni}"
        }

        btnTutup.setOnClickListener { dialog.dismiss() }

        btnTambah.setOnClickListener {
            val qty = tvQty.text.toString().toInt()
            val totalHarga = hargaDasar * qty

            val berhasil = dbHelper.tambahKeKeranjang(namaProduk, qty, totalHarga)
            if (berhasil) {
                Toast.makeText(this, "Berhasil masuk keranjang!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Gagal menambahkan", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        dialog.show()
    }
}