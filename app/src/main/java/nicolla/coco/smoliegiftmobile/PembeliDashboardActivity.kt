package nicolla.coco.smoliegiftmobile

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.addTextChangedListener
import com.example.smoliegift.database.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.InputStream
import java.util.Calendar

class PembeliDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var gridLayoutProduk: GridLayout
    private lateinit var layoutHome: ScrollView
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var layoutProfile: ScrollView
    private lateinit var toolbar: Toolbar
    private lateinit var acSearchProduk: AutoCompleteTextView
    private var isAdminView: Boolean = false
    private var isKasirMode: Boolean = false
    private var currentUserEmail: String? = null

    private var currentCustomImageBase64: String? = null
    private var btnPilihFileRef: Button? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val fileName = getFileName(it)
                val inputStream: InputStream? = contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    currentCustomImageBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                    btnPilihFileRef?.text = "✅ $fileName"
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pembeli_dashboard)

        dbHelper = DatabaseHelper(this)
        isAdminView = intent.getBooleanExtra("IS_ADMIN_VIEW", false)
        isKasirMode = intent.getBooleanExtra("IS_KASIR_MODE", false)
        currentUserEmail = intent.getStringExtra("USER_EMAIL")

        toolbar = findViewById(R.id.toolbarPembeli)
        setSupportActionBar(toolbar)

        if (isAdminView || isKasirMode) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setNavigationOnClickListener { finish() }
        }

        gridLayoutProduk = findViewById(R.id.glDaftarProdukPembeli)
        layoutHome = findViewById(R.id.layoutHomePembeli)
        fragmentContainer = findViewById(R.id.fragmentContainerPembeli)
        layoutProfile = findViewById(R.id.layoutProfilePembeli)
        acSearchProduk = findViewById(R.id.acSearchProduk)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavPembeli)
        val btnLihatKeranjang = findViewById<Button>(R.id.btnLihatKeranjang)

        if (isAdminView) {
            btnLihatKeranjang.visibility = View.GONE
            bottomNav.visibility = View.GONE
        }
        
        if (isKasirMode) {
            bottomNav.visibility = View.GONE
            btnLihatKeranjang.text = "Keranjang Penjualan Toko"
            btnLihatKeranjang.setBackgroundColor(Color.parseColor("#DD3827"))
            btnLihatKeranjang.setTextColor(Color.WHITE)
        }

        btnLihatKeranjang.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            intent.putExtra("USER_EMAIL", currentUserEmail)
            intent.putExtra("IS_KASIR_MODE", isKasirMode)
            startActivity(intent)
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> { showHome(); true }
                R.id.navigation_history -> { showHistory(); true }
                R.id.navigation_profile -> { showProfile(); true }
                else -> false
            }
        }

        if (currentUserEmail != null) loadUserProfile(currentUserEmail!!)
        loadKatalogProduk()
        setupSearchAutoComplete()
    }

    private fun setupSearchAutoComplete() {
        val cursor: Cursor = dbHelper.getSemuaProduk()
        val listNamaProduk = mutableListOf<String>()
        while (cursor.moveToNext()) {
            listNamaProduk.add(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_NAME)))
        }
        cursor.close()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listNamaProduk)
        acSearchProduk.setAdapter(adapter)
        acSearchProduk.setOnItemClickListener { parent, _, position, _ ->
            filterKatalogProduk(parent.getItemAtPosition(position) as String)
        }
        acSearchProduk.addTextChangedListener { if (it.isNullOrEmpty()) loadKatalogProduk() }
    }

    private fun filterKatalogProduk(query: String) {
        gridLayoutProduk.removeAllViews()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_PRODUCTS} WHERE ${DatabaseHelper.COLUMN_PROD_NAME} = ?", arrayOf(query))
        tampilkanDataKatalog(cursor)
    }

    private fun showHome() {
        layoutHome.visibility = View.VISIBLE
        fragmentContainer.visibility = View.GONE
        layoutProfile.visibility = View.GONE
        toolbar.title = if (isAdminView) "Katalog Produk (Admin)" else if (isKasirMode) "Input Manual Toko" else "Smolie Gift"
    }

    private fun showHistory() {
        layoutHome.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        layoutProfile.visibility = View.GONE
        toolbar.title = "Riwayat Pesanan"
        if (currentUserEmail != null) {
            supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerPembeli, HistoryFragment.newInstance(currentUserEmail!!)).commit()
        }
    }

    private fun showProfile() {
        layoutHome.visibility = View.GONE
        fragmentContainer.visibility = View.GONE
        layoutProfile.visibility = View.VISIBLE
        toolbar.title = "Profil Saya"
    }

    private fun loadUserProfile(email: String) {
        val cursor = dbHelper.getUserByEmail(email)
        if (cursor != null && cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
            findViewById<TextView>(R.id.tvProfileName).text = name
            findViewById<TextView>(R.id.tvProfileEmail).text = email
            findViewById<TextView>(R.id.tvProfileUsername).text = "Username: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME))
            findViewById<TextView>(R.id.tvProfilePhone).text = "Telepon: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE))
            cursor.close()
        }
    }

    private fun loadKatalogProduk() {
        gridLayoutProduk.removeAllViews()
        tampilkanDataKatalog(dbHelper.getSemuaProduk())
    }

    private fun tampilkanDataKatalog(cursor: Cursor) {
        val inflater = LayoutInflater.from(this)
        val itemWidth = (resources.displayMetrics.widthPixels / 2) - 48
        if (cursor.count == 0) {
            val tv = TextView(this)
            tv.text = "Tidak ada produk."; tv.setPadding(32, 32, 32, 32)
            gridLayoutProduk.addView(tv)
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

                if (!fotoBase64.isNullOrEmpty()) {
                    val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                    itemView.findViewById<ImageView>(R.id.ivPembeliProdImage).setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                }

                val btnPesan = itemView.findViewById<Button>(R.id.btnPesanKatalog)
                if (isAdminView) btnPesan.visibility = View.GONE
                else btnPesan.setOnClickListener { tampilkanDialogPesanan(nama, harga, fotoBase64) }

                val params = GridLayout.LayoutParams()
                params.width = itemWidth; params.setMargins(12, 16, 12, 16)
                itemView.layoutParams = params
                gridLayoutProduk.addView(itemView)
            }
        }
        cursor.close()
    }

    private fun tampilkanDialogPesanan(namaProduk: String, hargaDasar: Int, fotoProdukBase64: String?) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_pesan_produk)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvQty = dialog.findViewById<TextView>(R.id.tvQty)
        val btnTambah = dialog.findViewById<Button>(R.id.btnTambahKeranjang)
        var qty = 1

        fun updateHarga() {
            val total = hargaDasar * qty
            btnTambah.text = "Tambah — Rp $total"
        }

        dialog.findViewById<TextView>(R.id.tvDialogJudul).text = namaProduk
        dialog.findViewById<Button>(R.id.btnPlusQty).setOnClickListener { qty++; tvQty.text = qty.toString(); updateHarga() }
        dialog.findViewById<Button>(R.id.btnMinQty).setOnClickListener { if(qty>1) {qty--; tvQty.text = qty.toString(); updateHarga()} }
        btnPilihFileRef = dialog.findViewById(R.id.btnPilihFile)
        btnPilihFileRef?.setOnClickListener { pickImageLauncher.launch("image/*") }

        btnTambah.setOnClickListener {
            val totalText = btnTambah.text.toString().replace("Tambah — Rp ", "").replace(".", "").trim()
            val total = totalText.toIntOrNull() ?: (hargaDasar * qty)
            if (dbHelper.tambahKeKeranjang(namaProduk, qty, total, currentCustomImageBase64, fotoProdukBase64)) {
                Toast.makeText(this, "Berhasil!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        updateHarga(); dialog.show()
    }

    private fun getFileName(uri: Uri): String {
        var res: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use { if (it.moveToFirst()) res = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) }
        }
        return res ?: uri.path?.substringAfterLast('/') ?: "image"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.pembeli_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuLogoutPembeli) {
            startActivity(Intent(this, LoginActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
            finish(); return true
        }
        return super.onOptionsItemSelected(item)
    }
}
