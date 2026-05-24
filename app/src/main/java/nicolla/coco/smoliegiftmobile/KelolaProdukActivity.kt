package nicolla.coco.smoliegiftmobile

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.smoliegift.database.DatabaseHelper
import org.json.JSONArray
import org.json.JSONObject

class KelolaProdukActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var lvDaftarProduk: ListView

    // Ganti sImage (Base64) → selectedBitmap (file asli untuk diupload)
    private var selectedBitmap: Bitmap? = null

    private val listProduk = mutableListOf<JSONObject>()

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    selectedBitmap = bitmap // ← simpan bitmap, bukan Base64
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

        val toolbar = findViewById<Toolbar>(R.id.toolbarProduk)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        dbHelper = DatabaseHelper(this)
        lvDaftarProduk = findViewById(R.id.lvDaftarProdukAdmin)
        val btnTambah = findViewById<Button>(R.id.btnTambahProdukBaru)

        loadDataProduk()

        btnTambah.setOnClickListener {
            selectedBitmap = null
            tampilkanFormTambah()
        }
    }

    // LOAD DATA DARI API
    private fun loadDataProduk() {
        ApiClient.getAllProducts { response ->
            runOnUiThread {
                if (response == null) {
                    Toast.makeText(this, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                try {
                    val json = JSONObject(response)
                    val data: JSONArray = json.getJSONArray("data")

                    listProduk.clear()
                    for (i in 0 until data.length()) {
                        listProduk.add(data.getJSONObject(i))
                    }

                    val adapter = object : BaseAdapter() {
                        override fun getCount() = listProduk.size
                        override fun getItem(pos: Int) = listProduk[pos]
                        override fun getItemId(pos: Int) = pos.toLong()

                        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
                            val view = convertView ?: LayoutInflater.from(this@KelolaProdukActivity)
                                .inflate(R.layout.item_produk_admin, parent, false)

                            val produk   = listProduk[pos]
                            val id       = produk.getInt("prod_id")
                            val nama     = produk.getString("prod_name")
                            val kategori = produk.optString("prod_category", "Umum")
                            val harga    = produk.getInt("prod_price")
                            val stok     = produk.getInt("prod_stock")
                            val image    = produk.optString("prod_image", "")

                            view.findViewById<TextView>(R.id.tvAdminProdName).text  = nama
                            view.findViewById<TextView>(R.id.tvAdminProdCat).text   = kategori
                            view.findViewById<TextView>(R.id.tvAdminProdPrice).text = "Rp $harga"
                            view.findViewById<TextView>(R.id.tvAdminProdStock).text = "Stok: $stok"

                            // Tampilkan gambar dari URL server
                            val ivProduk = view.findViewById<ImageView>(R.id.ivAdminProdImage)
                            if (ivProduk != null && image.isNotEmpty()) {
                                Thread {
                                    try {
                                        val url = java.net.URL(ApiClient.IMAGE_BASE_URL + image)
                                        val bmp = BitmapFactory.decodeStream(url.openStream())
                                        runOnUiThread { ivProduk.setImageBitmap(bmp) }
                                    } catch (e: Exception) {
                                        runOnUiThread { ivProduk.setImageResource(android.R.drawable.ic_menu_gallery) }
                                    }
                                }.start()
                            }

                            view.findViewById<Button>(R.id.btnAdminEditProd).setOnClickListener {
                                selectedBitmap = null
                                tampilkanFormEdit(id, nama, kategori, harga, stok, image)
                            }
                            view.findViewById<Button>(R.id.btnAdminHapusProd).setOnClickListener {
                                konfirmasiHapus(id, nama)
                            }

                            return view
                        }
                    }
                    lvDaftarProduk.adapter = adapter

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

    // FORM TAMBAH
    private fun tampilkanFormTambah() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tambah Produk Baru")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val inputNama       = EditText(this).apply { hint = "Nama Produk" }
        val spinnerKategori = Spinner(this)
        val kategoriAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, getKategoriList())
        kategoriAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = kategoriAdapter

        val inputHarga   = EditText(this).apply { hint = "Harga"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val inputStok    = EditText(this).apply { hint = "Stok";  inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val tvFotoStatus = TextView(this).apply { text = "Belum ada foto dipilih"; textSize = 12f }
        val btnPilihFoto = Button(this).apply { text = "Pilih Foto Produk" }

        btnPilihFoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
            // Update status teks setelah dipilih (via observer di imagePickerLauncher)
            tvFotoStatus.text = "Foto dipilih ✓"
        }

        layout.addView(inputNama)
        layout.addView(TextView(this).apply { text = "Kategori:"; setPadding(0, 12, 0, 4) })
        layout.addView(spinnerKategori)
        layout.addView(inputHarga)
        layout.addView(inputStok)
        layout.addView(btnPilihFoto)
        layout.addView(tvFotoStatus)

        builder.setView(layout)
        builder.setPositiveButton("Simpan") { _, _ ->
            val nama = inputNama.text.toString()
            val kat  = spinnerKategori.selectedItem.toString()
            val hrg  = inputHarga.text.toString().toIntOrNull() ?: 0
            val stk  = inputStok.text.toString().toIntOrNull() ?: 0

            if (nama.isEmpty() || hrg <= 0) {
                Toast.makeText(this, "Nama dan harga wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (selectedBitmap != null) {
                // Ada gambar → upload dulu, baru simpan produk
                Toast.makeText(this, "Mengupload gambar...", Toast.LENGTH_SHORT).show()
                ApiClient.uploadImage(selectedBitmap!!) { filename ->
                    ApiClient.addProduct(nama, kat, hrg, stk, filename ?: "") { success ->
                        runOnUiThread {
                            if (success) {
                                Toast.makeText(this, "Produk berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                loadDataProduk()
                            } else {
                                Toast.makeText(this, "Gagal menambahkan produk", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                // Tidak ada gambar → langsung simpan
                ApiClient.addProduct(nama, kat, hrg, stk, "") { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Produk berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                            loadDataProduk()
                        } else {
                            Toast.makeText(this, "Gagal menambahkan produk", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    // FORM EDIT
    private fun tampilkanFormEdit(id: Int, nama: String, kategoriLama: String, harga: Int, stok: Int, imageLama: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Produk")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val inputNama       = EditText(this).apply { setText(nama) }
        val spinnerKategori = Spinner(this)
        val listKat         = getKategoriList()
        spinnerKategori.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listKat)
        val idxKat = listKat.indexOf(kategoriLama)
        if (idxKat >= 0) spinnerKategori.setSelection(idxKat)

        val inputHarga   = EditText(this).apply { setText(harga.toString()); inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val inputStok    = EditText(this).apply { setText(stok.toString());  inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val tvFotoStatus = TextView(this).apply {
            text = if (imageLama.isNotEmpty()) "Foto saat ini: $imageLama" else "Belum ada foto"
            textSize = 12f
        }
        val btnPilihFoto = Button(this).apply { text = "Ubah Foto (Opsional)" }
        btnPilihFoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
            tvFotoStatus.text = "Foto baru dipilih ✓"
        }

        layout.addView(inputNama)
        layout.addView(spinnerKategori)
        layout.addView(inputHarga)
        layout.addView(inputStok)
        layout.addView(btnPilihFoto)
        layout.addView(tvFotoStatus)

        builder.setView(layout)
        builder.setPositiveButton("Update") { _, _ ->
            val namaBaru = inputNama.text.toString()
            val katBaru  = spinnerKategori.selectedItem.toString()
            val hrgBaru  = inputHarga.text.toString().toIntOrNull() ?: 0
            val stkBaru  = inputStok.text.toString().toIntOrNull() ?: 0

            if (selectedBitmap != null) {
                // Ada gambar baru → upload dulu
                Toast.makeText(this, "Mengupload gambar...", Toast.LENGTH_SHORT).show()
                ApiClient.uploadImage(selectedBitmap!!) { filename ->
                    ApiClient.updateProduct(id, namaBaru, katBaru, hrgBaru, stkBaru, filename ?: imageLama) { success ->
                        runOnUiThread {
                            if (success) {
                                Toast.makeText(this, "Produk berhasil diupdate!", Toast.LENGTH_SHORT).show()
                                loadDataProduk()
                            } else {
                                Toast.makeText(this, "Gagal update produk", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                // Tidak ganti gambar → pakai nama file lama
                ApiClient.updateProduct(id, namaBaru, katBaru, hrgBaru, stkBaru, imageLama) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Produk berhasil diupdate!", Toast.LENGTH_SHORT).show()
                            loadDataProduk()
                        } else {
                            Toast.makeText(this, "Gagal update produk", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    // HAPUS
    private fun konfirmasiHapus(id: Int, nama: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus")
            .setMessage("Hapus $nama?")
            .setPositiveButton("Ya") { _, _ ->
                ApiClient.deleteProduct(id) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Produk dihapus!", Toast.LENGTH_SHORT).show()
                            loadDataProduk()
                        } else {
                            Toast.makeText(this, "Gagal menghapus produk", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}