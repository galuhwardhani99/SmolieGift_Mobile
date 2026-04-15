package nicolla.coco.smoliegiftmobile

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.smoliegift.database.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class PembeliDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var gridLayoutProduk: GridLayout
    private lateinit var layoutHome: ScrollView
    private lateinit var layoutProfile: ScrollView
    private lateinit var toolbar: Toolbar
    
    private var tvNamaFileSelected: TextView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val uri = data?.data
            if (uri != null) {
                val fileName = getFileName(uri)
                tvNamaFileSelected?.text = fileName
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pembeli_dashboard)

        dbHelper = DatabaseHelper(this)
        
        toolbar = findViewById(R.id.toolbarPembeli)
        setSupportActionBar(toolbar)

        gridLayoutProduk = findViewById(R.id.glDaftarProdukPembeli)
        layoutHome = findViewById(R.id.layoutHomePembeli)
        layoutProfile = findViewById(R.id.layoutProfilePembeli)

        val btnKeranjang = findViewById<Button>(R.id.btnLihatKeranjang)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavPembeli)

        btnKeranjang.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    showHome()
                    true
                }
                R.id.navigation_profile -> {
                    showProfile()
                    true
                }
                else -> false
            }
        }

        val userEmail = intent.getStringExtra("USER_EMAIL")
        if (userEmail != null) {
            loadUserProfile(userEmail)
        }

        loadKatalogProduk()
    }

    private fun showHome() {
        layoutHome.visibility = View.VISIBLE
        layoutProfile.visibility = View.GONE
        toolbar.title = "Smolie Gift"
    }

    private fun showProfile() {
        layoutHome.visibility = View.GONE
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
            findViewById<TextView>(R.id.tvProfileAddress).text = "Alamat: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS))
            findViewById<TextView>(R.id.tvProfileGender).text = "Gender: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GENDER))
            cursor.close()
        }
    }

    private fun loadKatalogProduk() {
        gridLayoutProduk.removeAllViews()
        val cursor: Cursor = dbHelper.getSemuaProduk()
        val inflater = LayoutInflater.from(this)

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val itemWidth = (screenWidth / 2) - 48

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Belum ada produk."
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

                val params = android.widget.GridLayout.LayoutParams()
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

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvJudul = dialog.findViewById<TextView>(R.id.tvDialogJudul)
        val btnTutup = dialog.findViewById<TextView>(R.id.btnTutupDialog)
        val spWarna = dialog.findViewById<Spinner>(R.id.spWarna)
        val rgKemasan = dialog.findViewById<RadioGroup>(R.id.rgKemasan)
        val cbSablon = dialog.findViewById<CheckBox>(R.id.cbExtraSablon)
        val cbThanksCard = dialog.findViewById<CheckBox>(R.id.cbExtraThanksCard)
        val btnPilihFile = dialog.findViewById<Button>(R.id.btnPilihFile)
        tvNamaFileSelected = dialog.findViewById<TextView>(R.id.tvNamaFile)
        val etCatatan = dialog.findViewById<EditText>(R.id.etCatatan)
        
        val tvQty = dialog.findViewById<TextView>(R.id.tvQty)
        val btnMin = dialog.findViewById<Button>(R.id.btnMinQty)
        val btnPlus = dialog.findViewById<Button>(R.id.btnPlusQty)
        val btnTambah = dialog.findViewById<Button>(R.id.btnTambahKeranjang)

        tvJudul.text = namaProduk
        var qtySaatIni = 1
        var hargaTambahanKemasan = 0
        var hargaTambahanExtra = 0

        fun updateHargaTotal() {
            hargaTambahanExtra = 0
            if (cbSablon.isChecked) hargaTambahanExtra += 500
            if (cbThanksCard.isChecked) hargaTambahanExtra += 300
            
            val totalPerItem = hargaDasar + hargaTambahanKemasan + hargaTambahanExtra
            val totalSemua = totalPerItem * qtySaatIni
            btnTambah.text = "Tambah — Rp $totalSemua"
        }

        val varianOptions = arrayOf("Pilih varian...", "Warna Pastel", "Monokrom", "Custom")
        spWarna.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, varianOptions)

        rgKemasan.setOnCheckedChangeListener { _, checkedId ->
            hargaTambahanKemasan = when (checkedId) {
                R.id.rbKemasanTile -> 1000
                R.id.rbKemasanBox -> 2500
                else -> 0
            }
            updateHargaTotal()
        }

        cbSablon.setOnCheckedChangeListener { _, _ -> updateHargaTotal() }
        cbThanksCard.setOnCheckedChangeListener { _, _ -> updateHargaTotal() }

        btnMin.setOnClickListener {
            if (qtySaatIni > 1) {
                qtySaatIni--
                tvQty.text = qtySaatIni.toString()
                updateHargaTotal()
            }
        }

        btnPlus.setOnClickListener {
            qtySaatIni++
            tvQty.text = qtySaatIni.toString()
            updateHargaTotal()
        }

        btnPilihFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        btnTutup.setOnClickListener { dialog.dismiss() }

        btnTambah.setOnClickListener {
            val qty = tvQty.text.toString().toInt()
            val totalPerItem = hargaDasar + hargaTambahanKemasan + hargaTambahanExtra
            val totalHarga = totalPerItem * qty

            val berhasil = dbHelper.tambahKeKeranjang(namaProduk, qty, totalHarga)
            if (berhasil) {
                Toast.makeText(this, "Berhasil masuk keranjang!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Gagal menambahkan", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        
        updateHargaTotal()
        dialog.show()
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                result = result.substring(cut + 1)
            }
        }
        return result ?: "file_gambar"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.pembeli_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuLogoutPembeli -> {
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