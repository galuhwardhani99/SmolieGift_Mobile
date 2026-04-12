package nicolla.coco.smoliegiftmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val btnLogout = findViewById<Button>(R.id.btnLogoutAdmin)
        val btnModeKasir = findViewById<Button>(R.id.btnModeKasir)

        // 1. Kenalkan ID CardView dari XML ke Kotlin
        val cvMenuKategori = findViewById<CardView>(R.id.cvMenuKategori)
        val cvMenuProduk = findViewById<CardView>(R.id.cvMenuProduk)
        val cvMenuTransaksi = findViewById<CardView>(R.id.cvMenuTransaksi)
        val cvMenuLaporan = findViewById<CardView>(R.id.cvMenuLaporan)

        btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnModeKasir.setOnClickListener {
            Toast.makeText(this, "Membuka Mode Kasir...", Toast.LENGTH_SHORT).show()
        }

        // 2. Berikan aksi klik untuk pindah ke Kelola Kategori
        cvMenuKategori.setOnClickListener {
            val intent = Intent(this, KelolaKategoriActivity::class.java)
            startActivity(intent)
        }

        // 3. Berikan aksi klik untuk pindah ke Kelola Produk
        cvMenuProduk.setOnClickListener {
            val intent = Intent(this, KelolaProdukActivity::class.java)
            startActivity(intent)
        }

        // 4. Berikan aksi klik untuk pindah ke Daftar Transaksi
        cvMenuTransaksi.setOnClickListener {
            val intent = Intent(this, AdminTransaksiActivity::class.java)
            startActivity(intent)
        }
        
        // 5. Berikan aksi klik untuk pindah ke Laporan
        cvMenuLaporan.setOnClickListener {
            startActivity(Intent(this, AdminLaporanActivity::class.java))
        }
    }
}