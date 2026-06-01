package nicolla.coco.smoliegiftmobile

import android.app.Dialog
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.addTextChangedListener
import com.example.smoliegift.database.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import org.json.JSONArray
import org.json.JSONObject

class KasirDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var layoutHome: ScrollView
    private lateinit var layoutProfile: LinearLayout
    private lateinit var toolbar: Toolbar
    private lateinit var gridLayoutProduk: GridLayout
    private lateinit var acSearchProduk: AutoCompleteTextView
    private var currentUserEmail: String? = null

    private val listNamaProdukKatalog = mutableListOf<String>()
    private var listProdukApi = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kasir_dashboard)

        dbHelper = DatabaseHelper(this)
        currentUserEmail = intent.getStringExtra("USER_EMAIL") ?: "kasir@smolie.com"

        toolbar = findViewById(R.id.toolbarKasir)
        setSupportActionBar(toolbar)

        layoutHome = findViewById(R.id.layoutHomeKasir)
        layoutProfile = findViewById(R.id.layoutProfileKasir)
        gridLayoutProduk = findViewById(R.id.glDaftarProdukKasir)
        acSearchProduk = findViewById(R.id.acSearchProdukKasir)

        val cvMenuKatalog = findViewById<MaterialCardView>(R.id.cvMenuKatalog)
        val cvMenuTransaksi = findViewById<MaterialCardView>(R.id.cvMenuTransaksi)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavKasir)

        cvMenuKatalog.setOnClickListener {
            // Input Manual Dialog (Reuse logic from PembeliDashboard if possible or implement here)
            tampilkanDialogInputManual()
        }

        cvMenuTransaksi.setOnClickListener {
            startActivity(Intent(this, AdminTransaksiActivity::class.java))
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> { showHome(); true }
                R.id.navigation_profile -> { showProfile(); true }
                else -> false
            }
        }

        loadKasirProfile(currentUserEmail!!)
        loadKatalogProduk()
    }

    private fun loadKatalogProduk() {
        gridLayoutProduk.removeAllViews()
        ApiClient.getAllProducts { response ->
            runOnUiThread {
                if (response != null) {
                    try {
                        val json = JSONObject(response)
                        val data = json.getJSONArray("data")
                        listProdukApi.clear()
                        for (i in 0 until data.length()) {
                            listProdukApi.add(data.getJSONObject(i))
                        }
                        tampilkanDataKatalogApi(listProdukApi)
                        setupSearchAutoComplete()
                    } catch (e: Exception) { 
                        tampilkanDataKatalogLocal(dbHelper.getSemuaProduk())
                        setupSearchAutoComplete() 
                    }
                } else { 
                    tampilkanDataKatalogLocal(dbHelper.getSemuaProduk())
                    setupSearchAutoComplete() 
                }
            }
        }
    }

    private fun tampilkanDataKatalogApi(data: List<JSONObject>) {
        val inflater = LayoutInflater.from(this)
        val itemWidth = (resources.displayMetrics.widthPixels / 2) - 48
        
        for (produk in data) {
            val nama = produk.getString("nama_produk")
            val harga = produk.optInt("harga", 0)
            val image = produk.optString("gambar", "")
            
            val itemView = inflater.inflate(R.layout.item_produk_pembeli, gridLayoutProduk, false)
            itemView.findViewById<TextView>(R.id.tvPembeliProdName).text = nama
            itemView.findViewById<TextView>(R.id.tvPembeliProdPrice).text = "Rp $harga"
            itemView.findViewById<TextView>(R.id.tvPembeliProdCat).visibility = View.GONE

            val ivProduk = itemView.findViewById<ImageView>(R.id.ivPembeliProdImage)
            if (image.isNotEmpty()) {
                Thread {
                    try {
                        val url = java.net.URL(ApiClient.IMAGE_BASE_URL + image)
                        val bmp = BitmapFactory.decodeStream(url.openStream())
                        runOnUiThread { ivProduk.setImageBitmap(bmp) }
                    } catch (e: Exception) { runOnUiThread { ivProduk.setImageResource(android.R.drawable.ic_menu_gallery) } }
                }.start()
            }

            itemView.findViewById<Button>(R.id.btnPesanKatalog).setOnClickListener {
                tampilkanDialogPesanan(nama, harga, null)
            }

            val params = GridLayout.LayoutParams()
            params.width = itemWidth; params.setMargins(12, 16, 12, 16)
            itemView.layoutParams = params
            gridLayoutProduk.addView(itemView)
        }
    }

    private fun tampilkanDataKatalogLocal(cursor: Cursor) {
        val inflater = LayoutInflater.from(this)
        val itemWidth = (resources.displayMetrics.widthPixels / 2) - 48
        while (cursor.moveToNext()) {
            val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_NAME))
            val harga = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_PRICE))
            val fotoBase64 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_IMAGE))

            val itemView = inflater.inflate(R.layout.item_produk_pembeli, gridLayoutProduk, false)
            itemView.findViewById<TextView>(R.id.tvPembeliProdName).text = nama
            itemView.findViewById<TextView>(R.id.tvPembeliProdPrice).text = "Rp $harga"
            itemView.findViewById<TextView>(R.id.tvPembeliProdCat).visibility = View.GONE

            if (!fotoBase64.isNullOrEmpty()) {
                val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                itemView.findViewById<ImageView>(R.id.ivPembeliProdImage).setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            }

            itemView.findViewById<Button>(R.id.btnPesanKatalog).setOnClickListener {
                tampilkanDialogPesanan(nama, harga, fotoBase64)
            }

            val params = GridLayout.LayoutParams()
            params.width = itemWidth; params.setMargins(12, 16, 12, 16)
            itemView.layoutParams = params
            gridLayoutProduk.addView(itemView)
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

        fun updateHarga() { btnTambah.text = "Tambah — Rp ${hargaDasar * qty}" }

        dialog.findViewById<TextView>(R.id.tvDialogJudul).text = namaProduk
        dialog.findViewById<Button>(R.id.btnPlusQty).setOnClickListener { qty++; tvQty.text = qty.toString(); updateHarga() }
        dialog.findViewById<Button>(R.id.btnMinQty).setOnClickListener { if(qty>1) {qty--; tvQty.text = qty.toString(); updateHarga()} }
        
        // Kasir tidak perlu upload gambar custom biasanya, tapi kita biarkan saja fiturnya
        dialog.findViewById<Button>(R.id.btnPilihFile).visibility = View.GONE

        btnTambah.setOnClickListener {
            val total = hargaDasar * qty
            if (dbHelper.tambahKeKeranjang(namaProduk, qty, total, null, fotoProdukBase64)) {
                Toast.makeText(this, "Ditambahkan ke keranjang kasir!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                
                // Buka CartActivity dalam mode kasir
                val intent = Intent(this, CartActivity::class.java)
                intent.putExtra("USER_EMAIL", currentUserEmail)
                intent.putExtra("IS_KASIR_MODE", true)
                startActivity(intent)
            }
        }
        updateHarga(); dialog.show()
    }

    private fun tampilkanDialogInputManual() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_input_manual)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etNamaPembeli = dialog.findViewById<EditText>(R.id.etManualNamaPembeli)
        val actvProdukKatalog = dialog.findViewById<AutoCompleteTextView>(R.id.actvManualProduk)
        val etNamaProduk = dialog.findViewById<EditText>(R.id.etManualNamaProduk)
        val etHarga = dialog.findViewById<EditText>(R.id.etManualHarga)
        val etQty = dialog.findViewById<EditText>(R.id.etManualQty)
        val etCatatan = dialog.findViewById<EditText>(R.id.etManualCatatan)
        val btnSimpan = dialog.findViewById<Button>(R.id.btnSimpanTransaksiManual)
        val btnBatal = dialog.findViewById<Button>(R.id.btnManualBatal)

        val adapterKatalog = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listNamaProdukKatalog)
        actvProdukKatalog.setAdapter(adapterKatalog)
        actvProdukKatalog.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as String
            etNamaProduk.setText(selected)
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_PROD_PRICE} FROM ${DatabaseHelper.TABLE_PRODUCTS} WHERE ${DatabaseHelper.COLUMN_PROD_NAME} = ?", arrayOf(selected))
            if (cursor.moveToFirst()) {
                etHarga.setText(cursor.getInt(0).toString())
            }
            cursor.close()
        }

        btnBatal.setOnClickListener { dialog.dismiss() }

        btnSimpan.setOnClickListener {
            val pembeli = etNamaPembeli.text.toString().trim()
            val produk = etNamaProduk.text.toString().trim()
            val hargaStr = etHarga.text.toString().trim()
            val qtyStr = etQty.text.toString().trim()
            val catatan = etCatatan.text.toString().trim()

            if (pembeli.isEmpty() || produk.isEmpty() || hargaStr.isEmpty() || qtyStr.isEmpty()) {
                Toast.makeText(this, "Lengkapi data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val total = hargaStr.toInt() * qtyStr.toInt()
            val itemsArray = JSONArray().put(JSONObject().apply {
                put("name", produk)
                put("qty", qtyStr.toInt())
            })

            val newId = dbHelper.simpanTransaksiLangsung(
                pembeli, "Beli di Toko", "Tunai", total, null, 
                if (catatan.isEmpty()) "Penjualan Toko" else catatan, itemsArray.toString()
            )

            if (newId != -1L) {
                Toast.makeText(this, "Transaksi Berhasil!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun setupSearchAutoComplete() {
        val cursor: Cursor = dbHelper.getSemuaProduk()
        listNamaProdukKatalog.clear()
        while (cursor.moveToNext()) {
            listNamaProdukKatalog.add(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_NAME)))
        }
        cursor.close()
        for (produk in listProdukApi) {
            val nama = produk.optString("nama_produk")
            if (nama.isNotEmpty() && !listNamaProdukKatalog.contains(nama)) { listNamaProdukKatalog.add(nama) }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listNamaProdukKatalog)
        acSearchProduk.setAdapter(adapter)
        acSearchProduk.setOnItemClickListener { parent, _, position, _ -> filterKatalogProduk(parent.getItemAtPosition(position) as String) }
        acSearchProduk.addTextChangedListener { if (it.isNullOrEmpty()) loadKatalogProduk() }
    }

    private fun filterKatalogProduk(query: String) {
        gridLayoutProduk.removeAllViews()
        val filteredApi = listProdukApi.filter { it.optString("nama_produk").contains(query, ignoreCase = true) }
        if (filteredApi.isNotEmpty()) tampilkanDataKatalogApi(filteredApi)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_PRODUCTS} WHERE ${DatabaseHelper.COLUMN_PROD_NAME} LIKE ?", arrayOf("%$query%"))
        tampilkanDataKatalogLocal(cursor)
    }

    private fun showHome() {
        layoutHome.visibility = View.VISIBLE
        layoutProfile.visibility = View.GONE
        toolbar.title = "Dashboard Kasir"
    }

    private fun showProfile() {
        layoutHome.visibility = View.GONE
        layoutProfile.visibility = View.VISIBLE
        toolbar.title = "Profil Kasir"
    }

    private fun loadKasirProfile(email: String) {
        val cursor = dbHelper.getUserByEmail(email)
        if (cursor != null && cursor.moveToFirst()) {
            findViewById<TextView>(R.id.tvKasirProfileName).text = 
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
            findViewById<TextView>(R.id.tvKasirProfileEmail).text = email
            findViewById<TextView>(R.id.tvKasirProfileUsername).text = "Username: " + 
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME))
            findViewById<TextView>(R.id.tvKasirProfilePhone).text = "Telepon: " + 
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE))
            cursor.close()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.kasir_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuLogoutKasir -> {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
