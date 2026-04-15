package nicolla.coco.smoliegiftmobile

import android.app.Dialog
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper
import java.io.InputStream

class PembeliDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var gridLayoutProduk: GridLayout

    private var currentCustomImageBase64: String? = null
    private var btnPilihFileRef: Button? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                currentCustomImageBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                btnPilihFileRef?.text = "✅ Gambar Terpilih"
                Toast.makeText(this, "Gambar kustom berhasil dimuat!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pembeli_dashboard)

        dbHelper = DatabaseHelper(this)
        gridLayoutProduk = findViewById(R.id.glDaftarProdukPembeli)

        findViewById<Button>(R.id.btnLogoutPembeli).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnLihatKeranjang).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        loadKatalogProduk()
    }

    private fun loadKatalogProduk() {
        gridLayoutProduk.removeAllViews()
        val cursor: Cursor = dbHelper.getSemuaProduk()
        val inflater = LayoutInflater.from(this)

        val displayMetrics = resources.displayMetrics
        val itemWidth = (displayMetrics.widthPixels / 2) - 48

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Belum ada produk."
            tvKosong.setPadding(32, 32, 32, 32)
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

                val ivGambar = itemView.findViewById<ImageView>(R.id.ivPembeliProdImage)
                if (!fotoBase64.isNullOrEmpty()) {
                    val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ivGambar.setImageBitmap(bitmap)
                }

                itemView.findViewById<Button>(R.id.btnPesanKatalog).setOnClickListener {
                    tampilkanDialogPesanan(nama, harga)
                }

                val params = GridLayout.LayoutParams()
                params.width = itemWidth
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
        currentCustomImageBase64 = null

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // BINDING KOMPONEN
        val tvJudul = dialog.findViewById<TextView>(R.id.tvDialogJudul)
        val tvQty = dialog.findViewById<TextView>(R.id.tvQty)
        val btnTambah = dialog.findViewById<Button>(R.id.btnTambahKeranjang)
        val btnUpload = dialog.findViewById<Button>(R.id.btnPilihFile)
        val spWarna = dialog.findViewById<Spinner>(R.id.spWarna)
        val rgKemasan = dialog.findViewById<RadioGroup>(R.id.rgKemasan)
        val cbSablon = dialog.findViewById<CheckBox>(R.id.cbSablon)
        val cbThanks = dialog.findViewById<CheckBox>(R.id.cbThanksCard)

        btnPilihFileRef = btnUpload
        tvJudul.text = namaProduk
        var qtySaatIni = 1

        // --- KOREKSI: MENGISI DATA SPINNER (Agar bisa diklik) ---
        val listWarna = arrayOf("Original", "Pastel Pink", "Sky Blue", "Lilac", "Emerald Green", "Custom (Tulis di Catatan)")
        val adapterWarna = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listWarna)
        spWarna.adapter = adapterWarna

        // --- FUNGSI UPDATE HARGA TOTAL ---
        fun updateHargaTotal() {
            var tambahanHarga = 0
            when (rgKemasan.checkedRadioButtonId) {
                R.id.rbTile -> tambahanHarga += 1000
                R.id.rbBox -> tambahanHarga += 2500
            }
            if (cbSablon.isChecked) tambahanHarga += 500
            if (cbThanks.isChecked) tambahanHarga += 300

            val totalPerItem = hargaDasar + tambahanHarga
            val totalFinal = totalPerItem * qtySaatIni
            btnTambah.text = "Tambah — Rp $totalFinal"
        }

        // LISTENERS
        rgKemasan.setOnCheckedChangeListener { _, _ -> updateHargaTotal() }
        cbSablon.setOnCheckedChangeListener { _, _ -> updateHargaTotal() }
        cbThanks.setOnCheckedChangeListener { _, _ -> updateHargaTotal() }

        dialog.findViewById<Button>(R.id.btnMinQty).setOnClickListener {
            if (qtySaatIni > 1) {
                qtySaatIni--
                tvQty.text = qtySaatIni.toString()
                updateHargaTotal()
            }
        }

        dialog.findViewById<Button>(R.id.btnPlusQty).setOnClickListener {
            qtySaatIni++
            tvQty.text = qtySaatIni.toString()
            updateHargaTotal()
        }

        btnUpload?.setOnClickListener { pickImageLauncher.launch("image/*") }
        dialog.findViewById<TextView>(R.id.btnTutupDialog).setOnClickListener { dialog.dismiss() }

        btnTambah.setOnClickListener {
            // KOREKSI: Logic parsing harga yang lebih aman
            val totalText = btnTambah.text.toString()
                .replace("Tambah — Rp ", "")
                .replace(".", "")
                .trim()

            val totalHargaFix = try { totalText.toInt() } catch (e: Exception) { (hargaDasar * qtySaatIni) }

            val berhasil = dbHelper.tambahKeKeranjang(namaProduk, qtySaatIni, totalHargaFix, currentCustomImageBase64)
            if (berhasil) {
                Toast.makeText(this, "Berhasil masuk keranjang!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        updateHargaTotal()
        dialog.show()
    }
}