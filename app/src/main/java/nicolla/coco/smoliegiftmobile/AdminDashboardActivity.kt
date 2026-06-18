package nicolla.coco.smoliegiftmobile

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.example.smoliegift.database.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var layoutHome: ScrollView
    private lateinit var layoutProfile: ScrollView
    private lateinit var toolbar: Toolbar
    private lateinit var ivAdminProfile: ImageView
    private var currentUserEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        dbHelper = DatabaseHelper(this)
        currentUserEmail = intent.getStringExtra("USER_EMAIL") ?: "admin@smolie.com"

        toolbar = findViewById(R.id.toolbarAdmin)
        setSupportActionBar(toolbar)

        layoutHome = findViewById(R.id.layoutHome)
        layoutProfile = findViewById(R.id.layoutProfile)

        ivAdminProfile = findViewById(R.id.ivAdminProfile)
        registerForContextMenu(ivAdminProfile)

        val btnPilihMenu = findViewById<Button>(R.id.btnPilihMenu)
        val cvMenuKategori = findViewById<CardView>(R.id.cvMenuKategori)
        val cvMenuProduk = findViewById<CardView>(R.id.cvMenuProduk)
        val cvMenuTransaksi = findViewById<CardView>(R.id.cvMenuTransaksi)
        val cvMenuLaporan = findViewById<CardView>(R.id.cvMenuLaporan)
        val cvMenuReview = findViewById<CardView>(R.id.cvMenuReview)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfileAdmin)

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

        btnPilihMenu.setOnClickListener { view ->
            val popup = PopupMenu(this@AdminDashboardActivity, view)
            popup.menu.add(0, 1, 0, "Lihat Transaksi")
            popup.menu.add(0, 2, 0, "Lihat Katalog Produk")
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        startActivity(Intent(this@AdminDashboardActivity, AdminTransaksiActivity::class.java))
                        true
                    }
                    2 -> {
                        // Diarahkan ke AdminKatalogViewActivity (Hanya View, Tanpa CRUD)
                        startActivity(Intent(this@AdminDashboardActivity, AdminKatalogViewActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        cvMenuKategori.setOnClickListener {
            startActivity(Intent(this@AdminDashboardActivity, KelolaKategoriActivity::class.java))
        }

        cvMenuProduk.setOnClickListener {
            startActivity(Intent(this@AdminDashboardActivity, KelolaProdukActivity::class.java))
        }

        cvMenuTransaksi.setOnClickListener {
            startActivity(Intent(this@AdminDashboardActivity, AdminTransaksiActivity::class.java))
        }
        
        cvMenuLaporan.setOnClickListener {
            startActivity(Intent(this@AdminDashboardActivity, AdminLaporanActivity::class.java))
        }

        cvMenuReview.setOnClickListener {
            startActivity(Intent(this@AdminDashboardActivity, AdminReviewActivity::class.java))
        }

        btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        if (currentUserEmail != null) loadAdminProfile(currentUserEmail!!)
    }

    private fun showHome() {
        layoutHome.visibility = View.VISIBLE
        layoutProfile.visibility = View.GONE
        toolbar.title = "Dashboard Admin"
    }

    private fun showProfile() {
        layoutHome.visibility = View.GONE
        layoutProfile.visibility = View.VISIBLE
        toolbar.title = "Profil Admin"
    }

    private fun loadAdminProfile(email: String) {
        val cursor = dbHelper.getUserByEmail(email)
        if (cursor != null && cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
            val username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME))
            val phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE))
            val gender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GENDER))
            val address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS))

            findViewById<TextView>(R.id.tvAdminProfileName).text = name ?: "Administrator"
            findViewById<TextView>(R.id.tvAdminProfileEmail).text = email
            findViewById<TextView>(R.id.tvAdminProfileUsername).text = "Username: ${username ?: "-"}"
            findViewById<TextView>(R.id.tvAdminProfilePhone).text = "Telepon: ${phone ?: "-"}"
            findViewById<TextView>(R.id.tvAdminProfileGender).text = "Jenis Kelamin: ${gender ?: "-"}"
            findViewById<TextView>(R.id.tvAdminProfileAddress).text = "Alamat: ${address ?: "-"}"
            
            cursor.close()
        }
    }

    private fun showEditProfileDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_profile)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

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

            if (name.isEmpty() || user.isEmpty()) {
                Toast.makeText(this, "Nama dan Username wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = dbHelper.updateUser(currentUserEmail!!, name, user, phone, gender, addr)
            if (success) {
                Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                loadAdminProfile(currentUserEmail!!)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show()
            }
        }

        btnBatal.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v?.id == R.id.ivAdminProfile) {
            menu?.setHeaderTitle("Opsi Profil")
            menu?.add(0, 1, 0, "Kunjungi Instagram Smolie Gift")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/smolie.gift/"))
                val browserIntent = Intent.createChooser(intent, "Buka dengan")
                startActivity(browserIntent)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuLogout -> {
                val intent = Intent(this@AdminDashboardActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
