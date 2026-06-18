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
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
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
import java.util.*

class PembeliDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var gridLayoutProduk: GridLayout
    private lateinit var layoutHome: ScrollView
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var layoutProfile: ScrollView
    private lateinit var layoutLocation: FrameLayout
    private lateinit var toolbar: Toolbar
    private lateinit var acSearchProduk: AutoCompleteTextView

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

    // ── Navigasi ─────────────────────────────────────────────────────────────

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

    private fun showHistory() {
        layoutHome.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        layoutProfile.visibility = View.GONE
        layoutLocation.visibility = View.GONE
        toolbar.title = "Riwayat Pesanan"
        if (currentUserEmail != null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerPembeli, HistoryFragment.newInstance(currentUserEmail!!))
                .commit()
        }
    }

    private fun showProfile() {
        layoutHome.visibility = View.GONE
        fragmentContainer.visibility = View.GONE
        layoutProfile.visibility = View.VISIBLE
        layoutLocation.visibility = View.GONE
        toolbar.title = "Profil Saya"
    }

    // ── Map ──────────────────────────────────────────────────────────────────

    private fun setupOsmMap() {
        val map = mapOsm ?: return
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
                startActivity(Intent(Intent.ACTION_VIEW, gmapsUri))
                true
            }
            map.overlays.add(marker)

            locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
            locationOverlay?.enableMyLocation()
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

    // ── Katalog Produk ───────────────────────────────────────────────────────

    private fun loadKategori(onFinished: () -> Unit) {
        ApiClient.getKategori { categories ->
            listKategori.clear()
            listKategori.addAll(categories)
            runOnUiThread { onFinished() }
        }
    }

    private fun loadKatalogProduk() {
        ApiClient.getAllProducts { jsonStr ->
            if (jsonStr != null) {
                try {
                    val root = JSONObject(jsonStr)
                    val data = root.getJSONArray("data")
                    listProdukApi.clear()
                    listNamaProdukKatalog.clear()
                    for (i in 0 until data.length()) {
                        val p = data.getJSONObject(i)
                        listProdukApi.add(p)
                        listNamaProdukKatalog.add(p.getString("nama_produk"))
                    }
                    runOnUiThread {
                        displayProduk(listProdukApi)
                        setupSearch()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    private fun setupSearch() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listNamaProdukKatalog)
        acSearchProduk.setAdapter(adapter)
        acSearchProduk.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String
            val filtered = listProdukApi.filter { it.getString("nama_produk") == selectedName }
            displayProduk(filtered)
        }
    }

    private fun displayProduk(list: List<JSONObject>) {
        gridLayoutProduk.removeAllViews()
        for (produk in list) {
            val view = LayoutInflater.from(this)
                .inflate(R.layout.item_produk_pembeli, gridLayoutProduk, false)

            val iv       = view.findViewById<ImageView>(R.id.ivPembeliProdImage)
            val tvNama   = view.findViewById<TextView>(R.id.tvPembeliProdName)
            val tvHarga  = view.findViewById<TextView>(R.id.tvPembeliProdPrice)
            val btnPesan = view.findViewById<Button>(R.id.btnPesanKatalog)

            tvNama.text  = produk.getString("nama_produk")
            tvHarga.text = "Rp ${produk.getInt("harga")}"

            val imgPath = produk.optString("gambar")
            if (imgPath.isNotEmpty()) {
                Glide.with(this)
                    .load(ApiClient.IMAGE_BASE_URL + imgPath)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(iv)
            }

            btnPesan.text = "Pesan"
            btnPesan.setOnClickListener { showPesanDialog(produk) }
            gridLayoutProduk.addView(view)
        }
    }

    private fun showPesanDialog(produk: JSONObject) {
        val namaProduk = produk.optString("nama_produk", "")
        val hargaDasar = produk.optInt("harga", 0)
        val fotoBase64 = produk.optString("gambar", "")

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_pesan_produk)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvQty              = dialog.findViewById<TextView>(R.id.tvQty)
        val btnTambah          = dialog.findViewById<Button>(R.id.btnTambahKeranjang)
        val spWarna            = dialog.findViewById<Spinner>(R.id.spWarna)
        val rgKemasan          = dialog.findViewById<RadioGroup>(R.id.rgKemasan)
        val cbSablon           = dialog.findViewById<CheckBox>(R.id.cbSablon)
        val cbThanksCard       = dialog.findViewById<CheckBox>(R.id.cbThanksCard)
        val cbInvitedCard      = dialog.findViewById<CheckBox>(R.id.cbInvitedCard)
        val llContainerTanggal = dialog.findViewById<LinearLayout>(R.id.llContainerTanggalAcara)
        val btnPilihTanggal    = dialog.findViewById<Button>(R.id.btnPilihTanggal)
        val btnPilihWaktu      = dialog.findViewById<Button>(R.id.btnPilihWaktu)
        val tvWaktuTerpilih    = dialog.findViewById<TextView>(R.id.tvWaktuTerpilih)
        val etCatatan          = dialog.findViewById<EditText>(R.id.etCatatan)

        var selectedTanggal = ""
        var selectedWaktu   = ""
        var qty             = 1

        // 1) Fungsi-fungsi lokal didefinisikan PALING ATAS dulu,
        //    sebelum dipakai di listener manapun di bawah.

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
        val varianOptions = arrayOf(
            "Pilih varian...", "Warna Pastel", "Monokrom",
            "Aksen Emas", "Random", "Custom Desain Sendiri"
        )
        spWarna.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, varianOptions)

        spWarna.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { updateVisibility() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 3) Sekarang baru pasang semua listener yang memanggil updateHarga()/updateVisibility()
        //    — aman karena keduanya sudah didefinisikan di atas.

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
                selectedTanggal = "$day/${month + 1}/$year"
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
        dialog.findViewById<Button>(R.id.btnMinQty).setOnClickListener  { if (qty > 1) { qty--; tvQty.text = qty.toString(); updateHarga() } }

        dialog.findViewById<Button>(R.id.btnPilihFile).visibility = View.GONE
        dialog.findViewById<TextView>(R.id.btnTutupDialog).setOnClickListener { dialog.dismiss() }

        btnTambah.setOnClickListener {
            if (spWarna.selectedItemPosition == 0) {
                Toast.makeText(this, "Pilih varian terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val total = (hargaDasar + hitungFeeTambahan()) * qty
            val catatanExtra = if (llContainerTanggal.isVisible) "\nWaktu Acara: $selectedTanggal $selectedWaktu" else ""
            val finalCatatan = etCatatan.text.toString() + catatanExtra

            if (dbHelper.tambahKeKeranjang(namaProduk, qty, total, null, fotoBase64)) {
                Toast.makeText(this, "Ditambahkan ke keranjang!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                val intent = Intent(this, CartActivity::class.java)
                intent.putExtra("USER_EMAIL", currentUserEmail)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Gagal menambahkan ke keranjang!", Toast.LENGTH_SHORT).show()
            }
        }

        updateHarga()
        dialog.show()
    }
    // ── Settings ─────────────────────────────────────────────────────────────

    private fun showSettingDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_setting)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etFontSize   = dialog.findViewById<EditText>(R.id.etFontSize)
        val etButtonText = dialog.findViewById<EditText>(R.id.etButtonText)
        val etTvText     = dialog.findViewById<EditText>(R.id.etTextViewText)
        val spinner      = dialog.findViewById<Spinner>(R.id.spinnerBgColor)
        val btnSimpan    = dialog.findViewById<Button>(R.id.btnSimpanSetting)
        val btnBatal     = dialog.findViewById<Button>(R.id.btnBatalSetting)

        val colors = arrayOf("Default", "Abu-abu", "Merah Maroon", "Biru", "Hijau")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colors)

        val prefs = getSharedPreferences("SmoliePrefs", Context.MODE_PRIVATE)
        val savedColor = try { prefs.getString("bg_color", "Default") } catch (e: ClassCastException) { "Default" }
        val savedFont  = try { prefs.getString("font_size", "") }       catch (e: ClassCastException) { "" }
        val savedBtn   = try { prefs.getString("button_text", "") }     catch (e: ClassCastException) { "" }
        val savedTv    = try { prefs.getString("textview_text", "") }   catch (e: ClassCastException) { "" }

        spinner.setSelection(colors.indexOf(savedColor).coerceAtLeast(0))
        etFontSize.setText(savedFont)
        etButtonText.setText(savedBtn)
        etTvText.setText(savedTv)

        btnSimpan.setOnClickListener {
            prefs.edit().clear().apply()
            prefs.edit()
                .putString("bg_color",      spinner.selectedItem.toString())
                .putString("font_size",     etFontSize.text.toString())
                .putString("button_text",   etButtonText.text.toString())
                .putString("textview_text", etTvText.text.toString())
                .apply()
            applySettings()
            dialog.dismiss()
            Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show()
        }

        btnBatal.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun applySettings() {
        val prefs = getSharedPreferences("SmoliePrefs", Context.MODE_PRIVATE)

        val bgColor = try { prefs.getString("bg_color", "Default") }
        catch (e: ClassCastException) { prefs.edit().remove("bg_color").apply(); "Default" }
        val layout = findViewById<View>(R.id.layoutHomePembeli)
        when (bgColor) {
            "Abu-abu"      -> layout.setBackgroundColor(Color.LTGRAY)
            "Merah Maroon" -> layout.setBackgroundColor(Color.parseColor("#800000"))
            "Biru"         -> layout.setBackgroundColor(Color.parseColor("#ADD8E6"))
            "Hijau"        -> layout.setBackgroundColor(Color.parseColor("#90EE90"))
            else           -> layout.setBackgroundColor(Color.WHITE)
        }

        val buttonText = try { prefs.getString("button_text", "") }
        catch (e: ClassCastException) { prefs.edit().remove("button_text").apply(); "" }
        val btnKeranjang = findViewById<Button>(R.id.btnLihatKeranjang)
        btnKeranjang?.text = if (!buttonText.isNullOrEmpty()) buttonText else "Keranjang Belanja"

        val tvText = try { prefs.getString("textview_text", "") }
        catch (e: ClassCastException) { prefs.edit().remove("textview_text").apply(); "" }
        val tvHeader = findViewById<TextView>(R.id.tvHeaderTitle)
        tvHeader?.text = if (!tvText.isNullOrEmpty()) tvText else "Cari Souvenir Unik & Cantik?"

        val fontSizeStr = try { prefs.getString("font_size", "") }
        catch (e: ClassCastException) { prefs.edit().remove("font_size").apply(); "" }
        if (!fontSizeStr.isNullOrEmpty()) {
            val fontSize = fontSizeStr.toFloatOrNull()
            if (fontSize != null && fontSize in 8f..40f) tvHeader?.textSize = fontSize
        }
    }

    // ── Profile ──────────────────────────────────────────────────────────────

    private fun loadUserProfile(email: String) {
        val cursor = dbHelper.getUserByEmail(email)
        if (cursor != null && cursor.moveToFirst()) {
            val name     = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
            val phone    = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE))
            val username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME))
            val gender   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GENDER))
            val address  = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS))
            cursor.close()

            runOnUiThread {
                findViewById<TextView>(R.id.tvProfileName)?.text     = name
                findViewById<TextView>(R.id.tvProfileEmail)?.text    = email  // ← tambahkan ini
                findViewById<TextView>(R.id.tvProfilePhone)?.text    = "Telepon: $phone"
                findViewById<TextView>(R.id.tvProfileUsername)?.text = "Username: $username"
                findViewById<TextView>(R.id.tvProfileGender)?.text   = "Jenis Kelamin: $gender"
                findViewById<TextView>(R.id.tvProfileAddress)?.text  = "Alamat: $address"
            }
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            } finally { cursor?.close() }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) result = result?.substring(cut!! + 1)
        }
        return result ?: "unknown"
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.pembeli_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuSetting -> { showSettingDialog(); true }
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