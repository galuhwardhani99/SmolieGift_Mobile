package nicolla.coco.smoliegiftmobile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper

class KelolaKategoriActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var llDaftarKategori: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kelola_kategori)

        dbHelper = DatabaseHelper(this)
        llDaftarKategori = findViewById(R.id.llDaftarKategori)
        val btnTambah = findViewById<Button>(R.id.btnTambahKategori)

        loadKategori()

        btnTambah.setOnClickListener {
            tampilkanDialogTambah()
        }
    }

    private fun loadKategori() {
        llDaftarKategori.removeAllViews()
        val cursor = dbHelper.getSemuaKategori()
        val inflater = LayoutInflater.from(this)

        if (cursor.count == 0) {
            val tv = TextView(this)
            tv.text = "Belum ada kategori."
            llDaftarKategori.addView(tv)
        } else {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CAT_ID))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CAT_NAME))

                val itemView = inflater.inflate(R.layout.item_kategori, llDaftarKategori, false)
                itemView.findViewById<TextView>(R.id.tvNamaKategori).text = nama
                
                itemView.findViewById<Button>(R.id.btnEditKategori).setOnClickListener {
                    tampilkanDialogEdit(id, nama)
                }
                
                itemView.findViewById<Button>(R.id.btnHapusKategori).setOnClickListener {
                    konfirmasiHapus(id, nama)
                }
                llDaftarKategori.addView(itemView)
            }
        }
        cursor.close()
    }

    private fun tampilkanDialogTambah() {
        val input = EditText(this)
        input.hint = "Nama Kategori Baru"
        
        AlertDialog.Builder(this)
            .setTitle("Tambah Kategori")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = input.text.toString().trim()
                if (nama.isNotEmpty()) {
                    val berhasil = dbHelper.tambahKategori(nama)
                    if (berhasil) {
                        Toast.makeText(this, "Kategori Berhasil Ditambah", Toast.LENGTH_SHORT).show()
                        loadKategori()
                    } else {
                        Toast.makeText(this, "Gagal menambah kategori!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun tampilkanDialogEdit(id: Int, namaLama: String) {
        val input = EditText(this)
        input.setText(namaLama)
        input.hint = "Nama Kategori"
        
        AlertDialog.Builder(this)
            .setTitle("Edit Kategori")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val namaBaru = input.text.toString().trim()
                if (namaBaru.isNotEmpty()) {
                    val berhasil = dbHelper.updateKategori(id, namaBaru)
                    if (berhasil) {
                        Toast.makeText(this, "Kategori Berhasil Diupdate", Toast.LENGTH_SHORT).show()
                        loadKategori()
                    } else {
                        Toast.makeText(this, "Gagal mengupdate kategori!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun konfirmasiHapus(id: Int, nama: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Kategori")
            .setMessage("Yakin ingin menghapus kategori '$nama'?")
            .setPositiveButton("Ya") { _, _ ->
                val berhasil = dbHelper.hapusKategori(id)
                if (berhasil) {
                    Toast.makeText(this, "Kategori Dihapus", Toast.LENGTH_SHORT).show()
                    loadKategori()
                } else {
                    Toast.makeText(this, "Gagal menghapus kategori!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}