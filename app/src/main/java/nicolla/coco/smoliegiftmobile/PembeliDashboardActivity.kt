package nicolla.coco.smoliegiftmobile

import android.Manifest
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
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
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.example.smoliegift.database.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.InputStream
import java.util.Calendar

class PembeliDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var gridLayoutProduk: GridLayout
    private lateinit var layoutHome: ScrollView
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var layoutProfile: ScrollView
    private lateinit var layoutLocation: FrameLayout
    private lateinit var toolbar: Toolbar
    private lateinit var acSearchProduk: AutoCompleteTextView

    // ✅ OSMDroid MapView
    private var mapOsm: MapView? = null
    private var mapSudahDiset = false
    private var locationOverlay: MyLocationNewOverlay? = null
    private lateinit var fabMyLocation: FloatingActionButton

    private var isAdminView: Boolean = false
    private var isKasirMode: Boolean = false
    private var currentUserEmail: String? = null

    private var currentCustomImageBase64: String? = null
    private var btnPilihFileRef: Button? = null

    private val listKategori = mutableListOf<Pair<Int, String>>()
    private var listProdukApi = mutableListOf<JSONObject>()
    private var listNamaProdukKatalog = mutableListOf<String>()

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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            locationOverlay?.enableMyLocation()
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk fitur ini", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Wajib untuk OSMDroid
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))

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
        layoutLocation = findViewById(R.id.layoutLocationPembeli)
        acSearchProduk = findViewById(R.id.acSearchProduk)
        mapOsm = findViewById(R.id.mapOsm)
        fabMyLocation = findViewById(R.id.fabMyLocation)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavPembeli)
        val btnLihatKeranjang = findViewById<Button>(R.id.btnLihatKeranjang)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> { showHome(); true }
                R.id.navigation_location -> { showLocation(); true }
                R.id.navigation_history -> { showHistory(); true }
                R.id.navigation_profile -> { showProfile(); true }
                else -> false
            }
        }

        btnLihatKeranjang.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            intent.putExtra("USER_EMAIL", currentUserEmail)
            startActivity(intent)
        }

        fabMyLocation.setOnClickListener {
            val myLoc = locationOverlay?.myLocation
            if (myLoc != null) {
                mapOsm?.controller?.animateTo(myLoc)
                mapOsm?.controller?.setZoom(18.0)
            } else {
                Toast.makeText(this, "Mencari lokasi...", Toast.LENGTH_SHORT).show()
            }
        }

        if (currentUserEmail != null) loadUserProfile(currentUserEmail!!)
        loadKategori { loadKatalogProduk() }
        applySettings()
    }

    private fun showHome() {
        layoutHome.visibility = View.VISIBLE
        fragmentContainer.visibility = View.GONE
        layoutProfile.visibility = View.GONE
        layoutLocation.visibility = View.GONE
        toolbar.title = "Smolie Gift"
    }

    private fun showLocation() {
        layoutHome.visibility = View.GONE
        fragmentContainer.visibility = View.GONE
        layoutProfile.visibility = View.GONE
        layoutLocation.visibility = View.VISIBLE
        toolbar.title = "Lokasi Toko"

        setupOsmMap()
    }

    private fun setupOsmMap() {
        val map = mapOsm ?: return

        // ✅ Koordinat Smolie Gift Surabaya (Pogot 9 No.60)
        val smolieGiftLoc = GeoPoint(-7.228519, 112.768652)

        if (!mapSudahDiset) {
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)
            map.controller.setZoom(15.0)
            map.controller.setCenter(smolieGiftLoc)

            val marker = Marker(map)
            marker.position = smolieGiftLoc
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Smolie Gift Surabaya"
            marker.snippet = "Jl. Pogot 9 No.60, Tanah Kali Kedinding, Surabaya\nKetuk untuk buka di Google Maps"

            marker.setOnMarkerClickListener { _, _ ->
                val gmapsUri = Uri.parse("https://maps.app.goo.gl/vxLNPpnuToAu2NS5A")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmapsUri)
                startActivity(mapIntent)
                true
            }
            map.overlays.add(marker)

            locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
            locationOverlay?.enableMyLocation()
            // Kita tidak memanggil enableFollowLocation() agar di awal fokus tetap ke Toko
            map.overlays.add(locationOverlay)

            mapSudahDiset = true
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            locationOverlay?.enableMyLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        mapOsm?.onResume()
        locationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        mapOsm?.onPause()
        locationOverlay?.disableMyLocation()
    }

    private fun showHistory() {
        layoutHome.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        layoutProfile.visibility = View.GONE
        layoutLocation.visibility = View.GONE
        toolbar.title = "Riwayat Pesanan"
        if (currentUserEmail != null) {
            supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerPembeli, HistoryFragment.newInstance(currentUserEmail!!)).commit()
        }
    }

    private fun showProfile() {
        layoutHome.visibility = View.GONE
        fragmentContainer.visibility = View.GONE
        layoutProfile.visibility = View.VISIBLE
        layoutLocation.visibility = View.GONE
        toolbar.title = "Profil Saya"
    }

    private fun showSettingDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_setting)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etFont = dialog.findViewById<EditText>(R.id.etFontSize)
        val etBtn = dialog.findViewById<EditText>(R.id.etButtonText)
        val etTv = dialog.findViewById<EditText>(R.id.etTextViewText)
        val spinner = dialog.findViewById<Spinner>(R.id.spinnerBgColor)
        val btnSimpan = dialog.findViewById<Button>(R.id.btnSimpanSetting)
        val btnBatal = dialog.findViewById<Button>(R.id.btnBatalSetting)

        val colors = arrayOf("Default", "Abu-abu", "Merah Maroon", "Biru", "Hijau")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colors)
        spinner.adapter = adapter

        val prefs = getSharedPreferences("SmoliePrefs", Context.MODE_PRIVATE)
        spinner.setSelection(colors.indexOf(prefs.getString("bg_color", "Default")))

        btnSimpan.setOnClickListener {
            val editor = prefs.edit()
            editor.putString("bg_color", spinner.selectedItem.toString())
            editor.apply()
            applySettings()
            dialog.dismiss()
            Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show()
        }

        btnBatal.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showPesanDialog(produk: JSONObject) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_pesan_produk)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvJudul    = dialog.findViewById<TextView>(R.id.tvDialogJudul)
        val btnTutup   = dialog.findViewById<TextView>(R.id.btnTutupDialog)
        val spWarna    = dialog.findViewById<Spinner>(R.id.spWarna)
        val rgKemasan  = dialog.findViewById<RadioGroup>(R.id.rgKemasan)
        val cbSablon   = dialog.findViewById<CheckBox>(R.id.cbSablon)
        val cbThanks   = dialog.findViewById<CheckBox>(R.id.cbThanksCard)
        val cbInvited  = dialog.findViewById<CheckBox>(R.id.cbInvitedCard)
        val btnPilihFile = dialog.findViewById<Button>(R.id.btnPilihFile)
        val etCatatan  = dialog.findViewById<EditText>(R.id.etCatatan)
        val btnMin     = dialog.findViewById<Button>(R.id.btnMinQty)
        val btnPlus    = dialog.findViewById<Button>(R.id.btnPlusQty)
        val tvQty      = dialog.findViewById<TextView>(R.id.tvQty)
        val btnTambah  = dialog.findViewById<Button>(R.id.btnTambahKeranjang)

        // Container tanggal acara
        val llTanggal  = dialog.findViewById<LinearLayout>(R.id.llContainerTanggalAcara)
        val btnTanggal = dialog.findViewById<Button>(R.id.btnPilihTanggal)
        val btnWaktu   = dialog.findViewById<Button>(R.id.btnPilihWaktu)
        val tvWaktu    = dialog.findViewById<TextView>(R.id.tvWaktuTerpilih)

        val namaProd  = produk.getString("nama_produk")
        val hargaBase = produk.getInt("harga")
        val prodImage = produk.optString("prod_image", null)

        tvJudul.text = namaProd
        btnPilihFileRef = btnPilihFile
        currentCustomImageBase64 = null

        // Spinner Warna
        val listWarna = arrayOf("Random / Mix", "Merah", "Biru", "Hijau", "Kuning", "Pink", "Ungu")
        spWarna.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listWarna)

        var qty = 1
        var tglAcara = ""
        var jamAcara = ""

        fun updateHarga() {
            var hargaTambahan = 0
            when (rgKemasan.checkedRadioButtonId) {
                R.id.rbTile -> hargaTambahan += 1000
                R.id.rbBox  -> hargaTambahan += 2500
            }
            if (cbSablon.isChecked)  hargaTambahan += 500
            if (cbThanks.isChecked)  hargaTambahan += 300
            if (cbInvited.isChecked) hargaTambahan += 400

            val totalBayar = (hargaBase + hargaTambahan) * qty
            btnTambah.text = "Tambah — Rp $totalBayar"
        }

        rgKemasan.setOnCheckedChangeListener { _, _ -> updateHarga() }
        cbSablon.setOnCheckedChangeListener  { _, _ -> updateHarga() }
        cbThanks.setOnCheckedChangeListener  { _, _ -> updateHarga() }

        // ✅ FIX: cbInvited hanya di-set SEKALI — handle visibility DAN updateHarga sekaligus
        cbInvited.setOnCheckedChangeListener { _, isChecked ->
            llTanggal.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                // Reset waktu jika unchecked
                tglAcara = ""
                jamAcara = ""
                tvWaktu.text = "Belum memilih waktu"
                btnTanggal.text = "Pilih Tanggal"
                btnWaktu.text = "Pilih Waktu"
            }
            updateHarga()
        }

        // ✅ Date Picker
        btnTanggal.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    tglAcara = String.format("%02d/%02d/%04d", d, m + 1, y)
                    btnTanggal.text = tglAcara
                    tvWaktu.text = "$tglAcara $jamAcara".trim()
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // ✅ Time Picker
        btnWaktu.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, h, m ->
                    jamAcara = String.format("%02d:%02d", h, m)
                    btnWaktu.text = jamAcara
                    tvWaktu.text = "$tglAcara $jamAcara".trim()
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
            ).show()
        }

        btnMin.setOnClickListener {
            if (qty > 1) { qty--; tvQty.text = qty.toString(); updateHarga() }
        }
        btnPlus.setOnClickListener {
            qty++; tvQty.text = qty.toString(); updateHarga()
        }

        btnPilihFile.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnTutup.setOnClickListener { dialog.dismiss() }

        btnTambah.setOnClickListener {
            val kemasan = when (rgKemasan.checkedRadioButtonId) {
                R.id.rbTile -> "Tile"
                R.id.rbBox  -> "Box"
                else        -> "Plastik"
            }
            val catatan = etCatatan.text.toString()
            val waktuAcara = if (cbInvited.isChecked && tglAcara.isNotEmpty()) "$tglAcara $jamAcara".trim() else ""
            val fullEventInfo = buildString {
                append("Warna: ${spWarna.selectedItem}, Kemasan: $kemasan")
                val extras = mutableListOf<String>()
                if (cbSablon.isChecked)  extras.add("Sablon")
                if (cbThanks.isChecked)  extras.add("Thanks Card")
                if (cbInvited.isChecked) extras.add("Invited Card${if (waktuAcara.isNotEmpty()) " ($waktuAcara)" else ""}")
                if (extras.isNotEmpty()) append(", Extras: ${extras.joinToString(", ")}")
                if (catatan.isNotEmpty()) append("\nCatatan: $catatan")
            }

            // Ambil harga per item dari tombol (sudah termasuk tambahan)
            val totalText = btnTambah.text.toString()
            val totalBayar = totalText.substringAfter("Rp ").replace(".", "").replace(",", "").trim().toIntOrNull() ?: (hargaBase * qty)
            val itemPrice = totalBayar / qty

            val success = dbHelper.tambahKeKeranjang(namaProd, qty, itemPrice, currentCustomImageBase64, prodImage)
            if (success) {
                Toast.makeText(this, "✓ Berhasil masuk keranjang", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Gagal menambahkan ke keranjang", Toast.LENGTH_SHORT).show()
            }
        }

        updateHarga()
        dialog.show()
    }

    private fun applySettings() {
        val prefs = getSharedPreferences("SmoliePrefs", Context.MODE_PRIVATE)
        val selectedColor = prefs.getString("bg_color", "Default")
        val rootLayout = findViewById<View>(R.id.layoutHomePembeli)
        when (selectedColor) {
            "Abu-abu" -> rootLayout.setBackgroundColor(Color.LTGRAY)
            "Merah Maroon" -> rootLayout.setBackgroundColor(Color.parseColor("#800000"))
            "Biru" -> rootLayout.setBackgroundColor(Color.parseColor("#E3F2FD"))
            "Hijau" -> rootLayout.setBackgroundColor(Color.parseColor("#E8F5E9"))
            else -> rootLayout.setBackgroundColor(Color.parseColor("#F8F9FA"))
        }
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
                        for (i in 0 until data.length()) { listProdukApi.add(data.getJSONObject(i)) }
                        tampilkanDataKatalogApi(listProdukApi)
                    } catch (e: Exception) { tampilkanDataKatalogLocal(dbHelper.getSemuaProduk()) }
                } else { tampilkanDataKatalogLocal(dbHelper.getSemuaProduk()) }
            }
        }
    }

    private fun tampilkanDataKatalogApi(data: List<JSONObject>) {
        val inflater = LayoutInflater.from(this)
        val itemWidth = (resources.displayMetrics.widthPixels / 2) - 48
        for (produk in data) {
            val itemView = inflater.inflate(R.layout.item_produk_pembeli, gridLayoutProduk, false)
            itemView.findViewById<TextView>(R.id.tvPembeliProdName).text = produk.getString("nama_produk")
            itemView.findViewById<TextView>(R.id.tvPembeliProdPrice).text = "Rp ${produk.getInt("harga")}"

            itemView.findViewById<Button>(R.id.btnPesanKatalog).setOnClickListener {
                showPesanDialog(produk)
            }

            val params = GridLayout.LayoutParams(); params.width = itemWidth; params.setMargins(12, 16, 12, 16)
            itemView.layoutParams = params; gridLayoutProduk.addView(itemView)
        }
    }

    private fun tampilkanDataKatalogLocal(cursor: Cursor) {
        val inflater = LayoutInflater.from(this)
        val itemWidth = (resources.displayMetrics.widthPixels / 2) - 48
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_NAME))
            val price = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_PRICE))
            val category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_CAT))
            val image = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROD_IMAGE))

            val itemView = inflater.inflate(R.layout.item_produk_pembeli, gridLayoutProduk, false)
            itemView.findViewById<TextView>(R.id.tvPembeliProdName).text = name
            itemView.findViewById<TextView>(R.id.tvPembeliProdPrice).text = "Rp $price"

            itemView.findViewById<Button>(R.id.btnPesanKatalog).setOnClickListener {
                val obj = JSONObject()
                obj.put("nama_produk", name)
                obj.put("harga", price)
                obj.put("prod_image", image)
                showPesanDialog(obj)
            }

            val params = GridLayout.LayoutParams(); params.width = itemWidth; params.setMargins(12, 16, 12, 16)
            itemView.layoutParams = params; gridLayoutProduk.addView(itemView)
        }
        cursor.close()
    }

    private fun loadKategori(onDone: () -> Unit) {
        ApiClient.getKategori { list ->
            runOnUiThread { listKategori.clear(); listKategori.addAll(list); onDone() }
        }
    }

    private fun loadUserProfile(email: String) {
        val cursor = dbHelper.getUserByEmail(email)
        if (cursor != null && cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
            val username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME))
            val gender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GENDER))
            val phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE))
            val address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS))

            findViewById<TextView>(R.id.tvProfileName).text = name
            findViewById<TextView>(R.id.tvProfileEmail).text = email
            findViewById<TextView>(R.id.tvProfileUsername).text = "Username: ${username ?: "-"}"
            findViewById<TextView>(R.id.tvProfileGender).text = "Jenis Kelamin: ${gender ?: "-"}"
            findViewById<TextView>(R.id.tvProfilePhone).text = "Telepon: ${phone ?: "-"}"
            findViewById<TextView>(R.id.tvProfileAddress).text = "Alamat: ${address ?: "-"}"
            
            cursor.close()
        }
    }

    private fun getFileName(uri: Uri): String {
        var res: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use { if (it.moveToFirst()) res = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) }
        }
        return res ?: uri.path?.substringAfterLast('/') ?: "image"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean { menuInflater.inflate(R.menu.pembeli_menu, menu); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuLogoutPembeli -> {
                startActivity(Intent(this, LoginActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }); finish()
                return true
            }
            R.id.menuSetting -> {
                showSettingDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
