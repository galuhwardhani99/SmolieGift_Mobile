package nicolla.coco.smoliegiftmobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.example.smoliegift.database.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var layoutHome: ScrollView
    private lateinit var layoutProfile: LinearLayout
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
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

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
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Lihat Transaksi")
            popup.menu.add(0, 2, 0, "Lihat Katalog Produk")
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        startActivity(Intent(this, AdminTransaksiActivity::class.java))
                        true
                    }
                    2 -> {
                        val intent = Intent(this, PembeliDashboardActivity::class.java)
                        intent.putExtra("IS_ADMIN_VIEW", true)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        cvMenuKategori.setOnClickListener {
            startActivity(Intent(this, KelolaKategoriActivity::class.java))
        }

        cvMenuProduk.setOnClickListener {
            startActivity(Intent(this, KelolaProdukActivity::class.java))
        }

        cvMenuTransaksi.setOnClickListener {
            startActivity(Intent(this, AdminTransaksiActivity::class.java))
        }
        
        cvMenuLaporan.setOnClickListener {
            startActivity(Intent(this, AdminLaporanActivity::class.java))
        }

        loadAdminProfile(currentUserEmail!!)
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
            findViewById<TextView>(R.id.tvAdminProfileName).text = 
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
            findViewById<TextView>(R.id.tvAdminProfileEmail).text = email
            findViewById<TextView>(R.id.tvAdminProfileUsername).text = "Username: " + 
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME))
            findViewById<TextView>(R.id.tvAdminProfilePhone).text = "Telepon: " + 
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE))
            findViewById<TextView>(R.id.tvAdminProfileGender).text = "Gender: " + 
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GENDER))
            findViewById<TextView>(R.id.tvAdminProfileAddress).text = "Alamat: " + 
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS))
            cursor.close()
        }
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
