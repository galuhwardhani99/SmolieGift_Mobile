package nicolla.coco.smoliegiftmobile

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.example.smoliegift.database.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class KasirDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var layoutHome: ScrollView
    private lateinit var layoutProfile: ScrollView
    private lateinit var toolbar: Toolbar
    private lateinit var gridLayoutProduk: GridLayout
    private lateinit var acSearchProduk: AutoCompleteTextView
    private lateinit var ivKasirProfile: ImageView
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

        ivKasirProfile = findViewById(R.id.ivKasirProfile)

        val cvMenuKatalog = findViewById<MaterialCardView>(R.id.cvMenuKatalog)
        val cvMenuTransaksi = findViewById<MaterialCardView>(R.id.cvMenuTransaksi)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavKasir)
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfileKasir)

        cvMenuTransaksi.setOnClickListener {
            val intent = Intent(this, AdminTransaksiActivity::class.java)
            intent.putExtra("IS_KASIR_MODE", true)
            startActivity(intent)
        }
        cvMenuKatalog.setOnClickListener {
            tampilkanDialogInputManual()
        }

        btnEditProfile.setOnClickListener {
            showEditProfileDialog()
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
        val spWarna = dialog.findViewById<Spinner>(R.id.spWarna)
        val rgKemasan = dialog.findViewById<RadioGroup>(R.id.rgKemasan)
        val cbSablon = dialog.findViewById<CheckBox>(R.id.cbSablon)
        val cbThanksCard = dialog.findViewById<CheckBox>(R.id.cbThanksCard)
        val cbInvitedCard = dialog.findViewById<CheckBox>(R.id.cbInvitedCard)
        val llContainerTanggal = dialog.findViewById<LinearLayout>(R.id.llContainerTanggalAcara)
        val btnPilihTanggal = dialog.findViewById<Button>(R.id.btnPilihTanggal)
        val btnPilihWaktu = dialog.findViewById<Button>(R.id.btnPilihWaktu)
        val tvWaktuTerpilih = dialog.findViewById<TextView>(R.id.tvWaktuTerpilih)
        val etCatatan = dialog.findViewById<EditText>(R.id.etCatatan)

        var selectedTanggal = ""
        var selectedWaktu = ""
        var qty = 1


        fun hitungFeeTambahan(): Int {
            var fee = 0
            fee += when (rgKemasan.checkedRadioButtonId) {
                R.id.rbTile -> 1000
                R.id.rbBox  -> 2500
                else        -> 0
            }
            if (cbSablon.isChecked)      fee += 500
            if (cbThanksCard.isChecked)  fee += 300
            if (cbInvitedCard.isChecked) fee += 400
            return fee
        }

        fun updateHarga() {
            val totalHarga = (hargaDasar + hitungFeeTambahan()) * qty
            btnTambah.text = String.format(Locale.getDefault(), "Tambah — Rp %d", totalHarga)
        }

        fun updateVisibility() {
            val varian = spWarna.selectedItem.toString()
            llContainerTanggal.isVisible = (varian == "Custom Desain Sendiri" || cbInvitedCard.isChecked)
        }

        // 2) Setup Spinner Varian
        val varianOptions = arrayOf("Pilih varian...", "Warna Pastel", "Monokrom", "Aksen Emas", "Random", "Custom Desain Sendiri")
        val adapterVarian = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, varianOptions)
        spWarna.adapter = adapterVarian

        spWarna.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { updateVisibility() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        rgKemasan.setOnCheckedChangeListener { _, _ -> updateHarga() }
        cbSablon.setOnCheckedChangeListener { _, _ -> updateHarga() }
        cbThanksCard.setOnCheckedChangeListener { _, _ -> updateHarga() }
        cbInvitedCard.setOnCheckedChangeListener { _, _ ->
            updateVisibility()
            updateHarga()
        }

        btnPilihTanggal.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                selectedTanggal = "$day/${month+1}/$year"
                tvWaktuTerpilih.text = String.format(Locale.getDefault(), "Waktu: %s %s", selectedTanggal, selectedWaktu)
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnPilihWaktu.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                selectedWaktu = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                tvWaktuTerpilih.text = String.format(Locale.getDefault(), "Waktu: %s %s", selectedTanggal, selectedWaktu)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        dialog.findViewById<TextView>(R.id.tvDialogJudul).text = namaProduk
        dialog.findViewById<Button>(R.id.btnPlusQty).setOnClickListener { qty++; tvQty.text = qty.toString(); updateHarga() }
        dialog.findViewById<Button>(R.id.btnMinQty).setOnClickListener { if(qty>1) {qty--; tvQty.text = qty.toString(); updateHarga()} }

        dialog.findViewById<Button>(R.id.btnPilihFile).visibility = View.GONE
        dialog.findViewById<TextView>(R.id.btnTutupDialog).setOnClickListener { dialog.dismiss() }

        btnTambah.setOnClickListener {
            val total = (hargaDasar + hitungFeeTambahan()) * qty
            val catatanExtra = if(llContainerTanggal.isVisible) "\nWaktu Acara: $selectedTanggal $selectedWaktu" else ""
            val finalCatatan = etCatatan.text.toString() + catatanExtra

            if (dbHelper.tambahKeKeranjang(namaProduk, qty, total, null, fotoProdukBase64)) {
                Toast.makeText(this, "Ditambahkan ke keranjang kasir!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                val intent = Intent(this, CartActivity::class.java)
                intent.putExtra("USER_EMAIL", currentUserEmail)
                intent.putExtra("IS_KASIR_MODE", true)
                startActivity(intent)
            }
        }
        updateHarga()
        dialog.show()
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
        val btnGenerateQR = dialog.findViewById<Button>(R.id.btnGenerateQR)
        val ivQR = dialog.findViewById<ImageView>(R.id.ivManualQR)
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

        btnGenerateQR.setOnClickListener {
            val produk = etNamaProduk.text.toString().trim()
            val hargaStr = etHarga.text.toString().trim()
            val qtyStr = etQty.text.toString().trim()

            if (produk.isEmpty() || hargaStr.isEmpty() || qtyStr.isEmpty()) {
                Toast.makeText(this, "Lengkapi data produk untuk generate QR!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val total = hargaStr.toLong() * qtyStr.toInt()
            val qrContent = "Produk: $produk\nHarga: Rp $hargaStr\nQty: $qtyStr\nTotal: Rp $total"
            
            val bitmap = generateQRCode(qrContent)
            if (bitmap != null) {
                ivQR.setImageBitmap(bitmap)
                ivQR.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Gagal generate QR Code", Toast.LENGTH_SHORT).show()
            }
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
                if (catatan.isEmpty()) "Penjualan Toko" else catatan, itemsArray.toString(), generateKodeTransaksi()
            )

            if (newId != -1L) {
                Toast.makeText(this, "Transaksi Berhasil!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun generateKodeTransaksi(): String {
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "SMG-$timestamp-$random"
    }

    private fun generateQRCode(text: String): Bitmap? {
        return try {
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
            val username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME))
            val phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE))
            val gender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GENDER))
            val address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS))

            findViewById<TextView>(R.id.tvKasirProfileName).text = name ?: "Kasir Smolie"
            findViewById<TextView>(R.id.tvKasirProfileEmail).text = email
            findViewById<TextView>(R.id.tvKasirProfileUsername).text = "Username: ${username ?: "-"}"
            findViewById<TextView>(R.id.tvKasirProfilePhone).text = "Telepon: ${phone ?: "-"}"
            findViewById<TextView>(R.id.tvKasirProfileGender).text = "Jenis Kelamin: ${gender ?: "-"}"
            findViewById<TextView>(R.id.tvKasirProfileAddress).text = "Alamat: ${address ?: "-"}"
            
            cursor.close()
        }
    }

    private fun showEditProfileDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_profile)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<TextView>(android.R.id.title)?.text = "Edit Profil Kasir"
        // Update header text in layout if possible, otherwise it will say "Edit Profil Admin"
        val tvHeader = dialog.findViewById<TextView>(R.id.tvDialogJudul) ?: dialog.findViewById<LinearLayout>(0)?.getChildAt(0) as? TextView
        tvHeader?.text = "Edit Profil Kasir"

        val etNama = dialog.findViewById<TextInputEditText>(R.id.etEditAdminNama)
        val etUsername = dialog.findViewById<TextInputEditText>(R.id.etEditAdminUsername)
        val etPhone = dialog.findViewById<TextInputEditText>(R.id.etEditAdminPhone)
        val spGender = dialog.findViewById<Spinner>(R.id.spEditAdminGender)
        val etAddress = dialog.findViewById<TextInputEditText>(R.id.etEditAdminAddress)
        val btnSimpan = dialog.findViewById<Button>(R.id.btnSimpanEditAdmin)
        val btnBatal = dialog.findViewById<Button>(R.id.btnBatalEditAdmin)

        val genderOptions = arrayOf("Laki-laki", "Perempuan")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderOptions)
        spGender.adapter = adapter

        // Fill current data
        val cursor = dbHelper.getUserByEmail(currentUserEmail!!)
        if (cursor != null && cursor.moveToFirst()) {
            etNama.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)))
            etUsername.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME)))
            etPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE)))
            etAddress.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS)))
            
            val gender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GENDER))
            val selection = genderOptions.indexOf(gender)
            if (selection >= 0) spGender.setSelection(selection)
            
            cursor.close()
        }

        btnSimpan.setOnClickListener {
            val name = etNama.text.toString().trim()
            val user = etUsername.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val gender = spGender.selectedItem.toString()
            val addr = etAddress.text.toString().trim()

            if (name.isEmpty() || user.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Nama, Username, dan Telepon wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = dbHelper.updateUser(currentUserEmail!!, name, user, phone, gender, addr)
            if (success) {
                Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                loadKasirProfile(currentUserEmail!!)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show()
            }
        }

        btnBatal.setOnClickListener { dialog.dismiss() }
        dialog.show()
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
