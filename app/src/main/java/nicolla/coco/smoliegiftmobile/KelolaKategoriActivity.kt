package nicolla.coco.smoliegiftmobile

import android.app.AlertDialog
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper

class KelolaKategoriActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var lvDaftarKategori: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kelola_kategori)

        dbHelper = DatabaseHelper(this)
        lvDaftarKategori = findViewById(R.id.lvDaftarKategori)
        val btnTambah = findViewById<Button>(R.id.btnTambahKategori)

        loadKategori()

        btnTambah.setOnClickListener {
            tampilkanDialogTambah()
        }
    }

    private fun loadKategori() {
        val cursor: Cursor = dbHelper.getSemuaKategori()
        val adapter = object : CursorAdapter(this, cursor, 0) {
            override fun newView(context: android.content.Context?, cursor: Cursor?, parent: ViewGroup?): View {
                return LayoutInflater.from(context).inflate(R.layout.item_kategori, parent, false)
            }

            override fun bindView(view: View?, context: android.content.Context?, cursor: Cursor?) {
                if (view == null || cursor == null) return

                // Menggunakan "_id" karena di DatabaseHelper query sudah di-alias: COLUMN_CAT_ID AS _id
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CAT_NAME))

                view.findViewById<TextView>(R.id.tvNamaKategori).text = nama
                
                view.findViewById<Button>(R.id.btnEditKategori).setOnClickListener {
                    tampilkanDialogEdit(id, nama)
                }
                
                view.findViewById<Button>(R.id.btnHapusKategori).setOnClickListener {
                    konfirmasiHapus(id, nama)
                }
            }
        }
        lvDaftarKategori.adapter = adapter
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