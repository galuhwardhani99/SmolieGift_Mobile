package nicolla.coco.smoliegiftmobile

import android.app.AlertDialog
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper
import java.io.ByteArrayOutputStream

class KelolaProdukActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var llDaftarProduk: LinearLayout

    private var sImage: String = ""

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    sImage = encodeImage(bitmap)
                    Toast.makeText(this, "Foto berhasil dipilih!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memuat foto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kelola_produk)

        dbHelper = DatabaseHelper(this)
        llDaftarProduk = findViewById(R.id.llDaftarProdukAdmin)
        val btnTambah = findViewById<Button>(R.id.btnTambahProdukBaru)

        loadDataProduk()

        btnTambah.setOnClickListener {
            sImage = ""
            tampilkanFormTambah()
        }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun loadDataProduk() {
        llDaftarProduk.removeAllViews()
        val cursor: Cursor = dbHelper.getSemuaProduk()
        val inflater = LayoutInflater.from(this)

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Belum ada produk. Silakan tambah menu."
            llDaftarProduk.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_ID))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_NAME))
                val kategori = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_CAT))
                val harga = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_PRICE))
                val stok = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_STOCK))
                val image = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_IMAGE))

                val itemView = inflater.inflate(R.layout.item_produk_admin, llDaftarProduk, false)

                itemView.findViewById<TextView>(R.id.tvAdminProdName).text = nama
                itemView.findViewById<TextView>(R.id.tvAdminProdCat).text = kategori
                itemView.findViewById<TextView>(R.id.tvAdminProdPrice).text = "Rp $harga"
                itemView.findViewById<TextView>(R.id.tvAdminProdStock).text = "Stok: $stok"

                val btnEdit = itemView.findViewById<Button>(R.id.btnAdminEditProd)
                val btnHapus = itemView.findViewById<Button>(R.id.btnAdminHapusProd)

                btnEdit.setOnClickListener {
                    tampilkanFormEdit(id, nama, kategori, harga, stok, image)
                }

                btnHapus.setOnClickListener {
                    konfirmasiHapus(id, nama)
                }

                llDaftarProduk.addView(itemView)
            }
        }
        cursor.close()
    }

    private fun getKategoriList(): List<String> {
        val list = mutableListOf<String>()
        val cursor = dbHelper.getSemuaKategori()
        while (cursor.moveToNext()) {
            list.add(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CAT_NAME)))
        }
        cursor.close()
        if (list.isEmpty()) list.add("Umum")
        return list
    }

    private fun tampilkanFormTambah() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tambah Produk Baru")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 20)

        val inputNama = EditText(this).apply { hint = "Nama Produk" }
        val spinnerKategori = Spinner(this)
        val kategoriAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, getKategoriList())
        kategoriAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = kategoriAdapter

        val inputHarga = EditText(this).apply { hint = "Harga (Angka saja)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val inputStok = EditText(this).apply { hint = "Stok"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }

        val btnPilihFoto = Button(this).apply {
            text = "Pilih Foto Produk"
            setBackgroundColor(Color.LTGRAY)
            setTextColor(Color.BLACK)
        }

        btnPilihFoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        layout.addView(inputNama)
        layout.addView(TextView(this).apply { text = "Pilih Kategori:"; setPadding(0, 10, 0, 0) })
        layout.addView(spinnerKategori)
        layout.addView(inputHarga)
        layout.addView(inputStok)
        layout.addView(btnPilihFoto)

        builder.setView(layout)

        builder.setPositiveButton("Simpan") { dialog, _ ->
            val nama = inputNama.text.toString()
            val kat = spinnerKategori.selectedItem.toString()
            val hrg = inputHarga.text.toString().toIntOrNull() ?: 0
            val stk = inputStok.text.toString().toIntOrNull() ?: 0

            if (nama.isNotEmpty() && hrg > 0 && sImage.isNotEmpty()) {
                dbHelper.tambahProduk(nama, kat, hrg, stk, sImage)
                Toast.makeText(this, "Produk Disimpan!", Toast.LENGTH_SHORT).show()
                loadDataProduk()
            } else {
                Toast.makeText(this, "Gagal! Lengkapi data dan pilih foto.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun tampilkanFormEdit(id: Int, nama: String, kategoriLama: String, harga: Int, stok: Int, image: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Produk")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 20)

        val inputNama = EditText(this).apply { 
            hint = "Nama Produk"
            setText(nama)
        }

        val spinnerKategori = Spinner(this)
        val listKategori = getKategoriList()
        val kategoriAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listKategori)
        kategoriAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = kategoriAdapter

        val pos = listKategori.indexOf(kategoriLama)
        if (pos >= 0) spinnerKategori.setSelection(pos)

        val inputHarga = EditText(this).apply { 
            hint = "Harga"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(harga.toString())
        }
        val inputStok = EditText(this).apply { 
            hint = "Stok"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(stok.toString())
        }

        sImage = image

        val btnPilihFoto = Button(this).apply {
            text = "Ubah Foto (Opsional)"
            setBackgroundColor(Color.LTGRAY)
            setTextColor(Color.BLACK)
        }

        btnPilihFoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        layout.addView(inputNama)
        layout.addView(TextView(this).apply { text = "Pilih Kategori:"; setPadding(0, 10, 0, 0) })
        layout.addView(spinnerKategori)
        layout.addView(inputHarga)
        layout.addView(inputStok)
        layout.addView(btnPilihFoto)

        builder.setView(layout)

        builder.setPositiveButton("Update") { dialog, _ ->
            val newNama = inputNama.text.toString()
            val newKat = spinnerKategori.selectedItem.toString()
            val newHrg = inputHarga.text.toString().toIntOrNull() ?: 0
            val newStk = inputStok.text.toString().toIntOrNull() ?: 0

            if (newNama.isNotEmpty() && newHrg > 0) {
                dbHelper.updateProduk(id, newNama, newKat, newHrg, newStk, sImage)
                Toast.makeText(this, "Produk Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                loadDataProduk()
            } else {
                Toast.makeText(this, "Gagal! Nama dan Harga tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun konfirmasiHapus(id: Int, nama: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Produk")
            .setMessage("Yakin ingin menghapus $nama?")
            .setPositiveButton("Ya") { _, _ ->
                dbHelper.hapusProduk(id)
                loadDataProduk()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}