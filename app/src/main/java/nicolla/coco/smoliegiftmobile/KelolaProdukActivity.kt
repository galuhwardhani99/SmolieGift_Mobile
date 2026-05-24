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
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.smoliegift.database.DatabaseHelper
import java.io.ByteArrayOutputStream

class KelolaProdukActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var lvDaftarProduk: ListView

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

        // Setup Toolbar sebagai tombol kembali
        val toolbar = findViewById<Toolbar>(R.id.toolbarProduk)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        dbHelper = DatabaseHelper(this)
        lvDaftarProduk = findViewById(R.id.lvDaftarProdukAdmin)
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
        val cursor: Cursor = dbHelper.getSemuaProduk()
        val adapter = object : CursorAdapter(this, cursor, 0) {
            override fun newView(context: android.content.Context?, cursor: Cursor?, parent: ViewGroup?): View {
                return LayoutInflater.from(context).inflate(R.layout.item_produk_admin, parent, false)
            }

            override fun bindView(view: View?, context: android.content.Context?, cursor: Cursor?) {
                if (view == null || cursor == null) return

                val id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_NAME)) ?: ""
                val kategori = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_CAT)) ?: "Umum"
                val harga = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_PRICE))
                val stok = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_STOCK))
                val image = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_IMAGE)) ?: ""

                view.findViewById<TextView>(R.id.tvAdminProdName).text = nama
                view.findViewById<TextView>(R.id.tvAdminProdCat).text = kategori
                view.findViewById<TextView>(R.id.tvAdminProdPrice).text = "Rp $harga"
                view.findViewById<TextView>(R.id.tvAdminProdStock).text = "Stok: $stok"

                view.findViewById<Button>(R.id.btnAdminEditProd).setOnClickListener {
                    tampilkanFormEdit(id, nama, kategori, harga, stok, image)
                }

                view.findViewById<Button>(R.id.btnAdminHapusProd).setOnClickListener {
                    konfirmasiHapus(id, nama)
                }
            }
        }
        lvDaftarProduk.adapter = adapter
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

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val inputNama = EditText(this).apply { hint = "Nama Produk" }
        val spinnerKategori = Spinner(this)
        val kategoriAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, getKategoriList())
        kategoriAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = kategoriAdapter

        val inputHarga = EditText(this).apply { hint = "Harga"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val inputStok = EditText(this).apply { hint = "Stok"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val btnPilihFoto = Button(this).apply { text = "Pilih Foto Produk" }

        btnPilihFoto.setOnClickListener { imagePickerLauncher.launch("image/*") }

        layout.addView(inputNama)
        layout.addView(TextView(this).apply { text = "Kategori:" })
        layout.addView(spinnerKategori)
        layout.addView(inputHarga)
        layout.addView(inputStok)
        layout.addView(btnPilihFoto)

        builder.setView(layout)
        builder.setPositiveButton("Simpan") { _, _ ->
            val hrg = inputHarga.text.toString().toIntOrNull() ?: 0
            val stk = inputStok.text.toString().toIntOrNull() ?: 0
            if (inputNama.text.isNotEmpty() && hrg > 0) {
                dbHelper.tambahProduk(inputNama.text.toString(), spinnerKategori.selectedItem.toString(), hrg, stk, sImage)
                loadDataProduk()
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun tampilkanFormEdit(id: Int, nama: String, kategoriLama: String, harga: Int, stok: Int, image: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Produk")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val inputNama = EditText(this).apply { setText(nama) }
        val spinnerKategori = Spinner(this)
        val listKat = getKategoriList()
        spinnerKategori.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listKat)
        spinnerKategori.setSelection(listKat.indexOf(kategoriLama))

        val inputHarga = EditText(this).apply { setText(harga.toString()); inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val inputStok = EditText(this).apply { setText(stok.toString()); inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        sImage = image

        val btnPilihFoto = Button(this).apply { text = "Ubah Foto (Opsional)" }
        btnPilihFoto.setOnClickListener { imagePickerLauncher.launch("image/*") }

        layout.addView(inputNama)
        layout.addView(spinnerKategori)
        layout.addView(inputHarga)
        layout.addView(inputStok)
        layout.addView(btnPilihFoto)

        builder.setView(layout)
        builder.setPositiveButton("Update") { _, _ ->
            dbHelper.updateProduk(id, inputNama.text.toString(), spinnerKategori.selectedItem.toString(), inputHarga.text.toString().toIntOrNull() ?: 0, inputStok.text.toString().toIntOrNull() ?: 0, sImage)
            loadDataProduk()
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun konfirmasiHapus(id: Int, nama: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus")
            .setMessage("Hapus $nama?")
            .setPositiveButton("Ya") { _, _ -> dbHelper.hapusProduk(id); loadDataProduk() }
            .setNegativeButton("Batal", null)
            .show()
    }
}
