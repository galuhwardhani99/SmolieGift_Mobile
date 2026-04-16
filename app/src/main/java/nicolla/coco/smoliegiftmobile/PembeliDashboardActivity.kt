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
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.smoliegift.database.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.InputStream
import java.util.Calendar

class PembeliDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var gridLayoutProduk: GridLayout
    private lateinit var layoutHome: ScrollView
    private lateinit var layoutProfile: ScrollView
    private lateinit var toolbar: Toolbar

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
                    Toast.makeText(this, "Gambar kustom berhasil dimuat!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Gagal membaca file gambar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Terjadi kesalahan saat memproses gambar", Toast.LENGTH_SHORT).show()
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

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavPembeli)

        findViewById<Button>(R.id.btnLihatKeranjang).setOnClickListener {
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
        val itemWidth = (displayMetrics.widthPixels / 2) - 48

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

        val tvJudul = dialog.findViewById<TextView>(R.id.tvDialogJudul)
        val tvQty = dialog.findViewById<TextView>(R.id.tvQty)
        val btnTambah = dialog.findViewById<Button>(R.id.btnTambahKeranjang)
        val btnUpload = dialog.findViewById<Button>(R.id.btnPilihFile)
        val spWarna = dialog.findViewById<Spinner>(R.id.spWarna)
        val rgKemasan = dialog.findViewById<RadioGroup>(R.id.rgKemasan)
        val cbSablon = dialog.findViewById<CheckBox>(R.id.cbSablon)
        val cbThanks = dialog.findViewById<CheckBox>(R.id.cbThanksCard)
        val btnMin = dialog.findViewById<Button>(R.id.btnMinQty)
        val btnPlus = dialog.findViewById<Button>(R.id.btnPlusQty)
        val etCatatan = dialog.findViewById<EditText>(R.id.etCatatan)

        val cbInvitedCard = dialog.findViewById<CheckBox>(R.id.cbInvitedCard)
        val llContainerTanggalAcara = dialog.findViewById<LinearLayout>(R.id.llContainerTanggalAcara)
        val btnPilihTanggal = dialog.findViewById<Button>(R.id.btnPilihTanggal)
        val btnPilihWaktu = dialog.findViewById<Button>(R.id.btnPilihWaktu)

        var tanggalAcaraTerpilih = ""
        var waktuAcaraTerpilih = ""

        btnPilihFileRef = btnUpload
        tvJudul.text = namaProduk
        var qtySaatIni = 1

        val listWarna = arrayOf("Original", "Pastel Pink", "Sky Blue", "Lilac", "Emerald Green", "Custom (Tulis di Catatan)")
        val adapterWarna = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listWarna)
        spWarna.adapter = adapterWarna

        fun updateHargaTotal() {
            var tambahanHarga = 0
            when (rgKemasan.checkedRadioButtonId) {
                R.id.rbTile -> tambahanHarga += 1000
                R.id.rbBox -> tambahanHarga += 2500
            }
            if (cbSablon.isChecked) tambahanHarga += 500
            if (cbThanks.isChecked) tambahanHarga += 300
            if (cbInvitedCard.isChecked) tambahanHarga += 400 // Harga tambahan Invited Card

            val totalPerItem = hargaDasar + tambahanHarga
            val totalFinal = totalPerItem * qtySaatIni
            btnTambah.text = "Tambah — Rp $totalFinal"
        }

        rgKemasan.setOnCheckedChangeListener { _, _ -> updateHargaTotal() }
        cbSablon.setOnCheckedChangeListener { _, _ -> updateHargaTotal() }
        cbThanks.setOnCheckedChangeListener { _, _ -> updateHargaTotal() }

        cbInvitedCard.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                llContainerTanggalAcara.visibility = View.VISIBLE
            } else {
                llContainerTanggalAcara.visibility = View.GONE
                btnPilihTanggal.text = "Pilih Tanggal"
                btnPilihWaktu.text = "Pilih Waktu"
                btnPilihTanggal.setTextColor(Color.parseColor("#64748B"))
                btnPilihWaktu.setTextColor(Color.parseColor("#64748B"))
                tanggalAcaraTerpilih = ""
                waktuAcaraTerpilih = ""
            }
            updateHargaTotal()
        }

        btnPilihTanggal.setOnClickListener {
            val kalender = Calendar.getInstance()
            val tahun = kalender.get(Calendar.YEAR)
            val bulan = kalender.get(Calendar.MONTH)
            val hari = kalender.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                tanggalAcaraTerpilih = "$dayOfMonth/${month + 1}/$year"
                btnPilihTanggal.text = tanggalAcaraTerpilih
                btnPilihTanggal.setTextColor(Color.parseColor("#DD3827"))
            }, tahun, bulan, hari)
            datePicker.show()
        }

        btnPilihWaktu.setOnClickListener {
            val kalender = Calendar.getInstance()
            val jam = kalender.get(Calendar.HOUR_OF_DAY)
            val menit = kalender.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
                val jamFormat = String.format("%02d:%02d", hourOfDay, minute)
                waktuAcaraTerpilih = jamFormat
                btnPilihWaktu.text = jamFormat
                btnPilihWaktu.setTextColor(Color.parseColor("#DD3827"))
            }, jam, menit, true) // true untuk format 24 jam
            timePicker.show()
        }

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

        btnUpload?.setOnClickListener { pickImageLauncher.launch("image/*") }

        dialog.findViewById<TextView>(R.id.btnTutupDialog).setOnClickListener { dialog.dismiss() }

        btnTambah.setOnClickListener {
            val totalText = btnTambah.text.toString()
                .replace("Tambah — Rp ", "")
                .replace(".", "")
                .trim()

            val totalHargaFix = try { totalText.toInt() } catch (e: Exception) { (hargaDasar * qtySaatIni) }

            var infoTambahan = ""
            if (cbInvitedCard.isChecked && (tanggalAcaraTerpilih.isNotEmpty() || waktuAcaraTerpilih.isNotEmpty())) {
                infoTambahan = " (Invited Card: $tanggalAcaraTerpilih $waktuAcaraTerpilih)"
            }

            val namaProdukFinal = namaProduk + infoTambahan

            val berhasil = dbHelper.tambahKeKeranjang(namaProdukFinal, qtySaatIni, totalHargaFix, currentCustomImageBase64)
            if (berhasil) {
                Toast.makeText(this, "Berhasil masuk keranjang!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
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