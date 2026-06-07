package nicolla.coco.smoliegiftmobile

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class KelolaKategoriActivity : AppCompatActivity() {

    private lateinit var lvDaftarKategori: ListView
    private var kategoriList = mutableListOf<Pair<Int, String>>()
    private lateinit var adapter: KategoriAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kelola_kategori)

        val toolbar = findViewById<Toolbar>(R.id.toolbarKategori)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        lvDaftarKategori = findViewById(R.id.lvDaftarKategori)
        val btnTambah = findViewById<Button>(R.id.btnTambahKategori)

        adapter = KategoriAdapter(this, kategoriList)
        lvDaftarKategori.adapter = adapter

        loadKategori()

        btnTambah.setOnClickListener { tampilkanDialogTambah() }
    }

    // ── Muat ulang list dari server ────────────────────────────────────────
    private fun loadKategori() {
        ApiClient.getKategori { list ->
            runOnUiThread {
                kategoriList.clear()
                kategoriList.addAll(list)
                adapter.notifyDataSetChanged()
            }
        }
    }

    // ── Dialog Tambah ──────────────────────────────────────────────────────
    private fun tampilkanDialogTambah() {
        val input = EditText(this).apply {
            hint = "Nama Kategori Baru"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Tambah Kategori")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = input.text.toString().trim()
                if (nama.isNotEmpty()) {
                    ApiClient.addKategori(nama) { berhasil ->
                        runOnUiThread {
                            if (berhasil) {
                                Toast.makeText(this, "Kategori berhasil ditambah.", Toast.LENGTH_SHORT).show()
                                loadKategori()
                            } else {
                                Toast.makeText(this, "Gagal menambah kategori!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ── Dialog Edit ────────────────────────────────────────────────────────
    private fun tampilkanDialogEdit(id: Int, namaLama: String) {
        val input = EditText(this).apply {
            setText(namaLama)
            setSelection(text.length)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Edit Kategori")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val namaBaru = input.text.toString().trim()
                if (namaBaru.isEmpty()) {
                    Toast.makeText(this, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (namaBaru == namaLama) {
                    Toast.makeText(this, "Tidak ada perubahan.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                ApiClient.editKategori(id, namaBaru) { berhasil ->
                    runOnUiThread {
                        if (berhasil) {
                            Toast.makeText(this, "Kategori berhasil diperbarui.", Toast.LENGTH_SHORT).show()
                            loadKategori()
                        } else {
                            Toast.makeText(this, "Gagal memperbarui kategori!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ── Dialog Hapus ───────────────────────────────────────────────────────
    private fun tampilkanDialogHapus(id: Int, nama: String, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Kategori")
            .setMessage("Yakin ingin menghapus kategori \"$nama\"?")
            .setPositiveButton("Hapus") { _, _ ->
                ApiClient.deleteKategori(id) { berhasil ->
                    runOnUiThread {
                        if (berhasil) {
                            kategoriList.removeAt(position)
                            adapter.notifyDataSetChanged()
                            Toast.makeText(this, "Kategori berhasil dihapus.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Gagal menghapus kategori!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ── Custom Adapter ─────────────────────────────────────────────────────
    inner class KategoriAdapter(
        context: Context,
        private val list: MutableList<Pair<Int, String>>
    ) : ArrayAdapter<Pair<Int, String>>(context, R.layout.item_kategori, list) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_kategori, parent, false)

            val (id, nama) = list[position]

            view.findViewById<TextView>(R.id.tvNamaKategori).text = nama

            view.findViewById<Button>(R.id.btnEditKategori).setOnClickListener {
                tampilkanDialogEdit(id, nama)
            }

            view.findViewById<Button>(R.id.btnHapusKategori).setOnClickListener {
                tampilkanDialogHapus(id, nama, position)
            }

            return view
        }
    }
}