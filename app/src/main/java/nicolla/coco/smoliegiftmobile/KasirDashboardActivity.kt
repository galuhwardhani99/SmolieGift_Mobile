package nicolla.coco.smoliegiftmobile

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.smoliegift.database.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class KasirDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var layoutHome: ScrollView
    private lateinit var layoutProfile: LinearLayout
    private lateinit var toolbar: Toolbar
    private var currentUserEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kasir_dashboard)

        dbHelper = DatabaseHelper(this)
        currentUserEmail = intent.getStringExtra("USER_EMAIL") ?: "kasir@smolie.com"

        toolbar = findViewById(R.id.toolbarKasir)
        setSupportActionBar(toolbar)

        layoutHome = findViewById(R.id.layoutHomeKasir)
        layoutProfile = findViewById(R.id.layoutProfileKasir)

        val cvInputManual = findViewById<MaterialCardView>(R.id.cvInputManual)
        val cvDataTransaksi = findViewById<MaterialCardView>(R.id.cvDataTransaksi)
        val cvLaporanSelesai = findViewById<MaterialCardView>(R.id.cvLaporanSelesai)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavKasir)

        cvInputManual.setOnClickListener {
            val intent = Intent(this, PembeliDashboardActivity::class.java)
            intent.putExtra("IS_KASIR_MODE", true)
            intent.putExtra("USER_EMAIL", currentUserEmail)
            startActivity(intent)
        }

        cvDataTransaksi.setOnClickListener {
            startActivity(Intent(this, AdminTransaksiActivity::class.java))
        }

        cvLaporanSelesai.setOnClickListener {
            startActivity(Intent(this, AdminLaporanActivity::class.java))
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

        loadKasirProfile(currentUserEmail!!)
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
