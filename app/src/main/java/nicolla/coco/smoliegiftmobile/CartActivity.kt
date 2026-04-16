package nicolla.coco.smoliegiftmobile

import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper

class CartActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var imageBase64UntukPesanan: String? = null

    private var metodeDipilih = "Tunai"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        dbHelper = DatabaseHelper(this)

        val llDaftar = findViewById<LinearLayout>(R.id.llDaftarKeranjang)
        val tvTotalAkhir = findViewById<TextView>(R.id.tvTotalBayarAkhir)
        val btnKonfirmasi = findViewById<Button>(R.id.btnKonfirmasi)
        val etNama = findViewById<EditText>(R.id.etNamaPemesan)

        val btnMetodeTunai = findViewById<Button>(R.id.btnMetodeTunai)
        val btnMetodeQris = findViewById<Button>(R.id.btnMetodeQris)
        val llContainerQris = findViewById<LinearLayout>(R.id.llContainerQris)

        val grandTotal = tampilkanDataDanHitungTotal(llDaftar)
        tvTotalAkhir.text = "Rp $grandTotal"

        btnMetodeTunai.setOnClickListener {
            metodeDipilih = "Tunai"

            btnMetodeTunai.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0D6EFD"))
            btnMetodeTunai.setTextColor(Color.parseColor("#FFFFFF"))

            btnMetodeQris.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5F6F8"))
            btnMetodeQris.setTextColor(Color.parseColor("#2D3142"))


            llContainerQris.visibility = View.GONE
        }

        btnMetodeQris.setOnClickListener {
            metodeDipilih = "QRIS"

            btnMetodeQris.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0D6EFD"))
            btnMetodeQris.setTextColor(Color.parseColor("#FFFFFF"))

            btnMetodeTunai.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5F6F8"))
            btnMetodeTunai.setTextColor(Color.parseColor("#2D3142"))


            llContainerQris.visibility = View.VISIBLE
        }

        btnKonfirmasi.setOnClickListener {
            val nama = etNama.text.toString().trim()

            if (grandTotal <= 0) {
                Toast.makeText(this, "Keranjang Anda masih kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nama.isEmpty()) {
                Toast.makeText(this, "Harap isi Nama Pemesan!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val statusPesanan = if (metodeDipilih == "QRIS") "Lunas" else "Pending"


            val sukses = dbHelper.buatPesanan(nama, "-", metodeDipilih, grandTotal, imageBase64UntukPesanan, statusPesanan)

            if (sukses) {
                dbHelper.kosongkanKeranjang()
                Toast.makeText(this, "Pesanan Berhasil! Status: $statusPesanan", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Gagal menyimpan pesanan.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun tampilkanDataDanHitungTotal(container: LinearLayout): Int {
        val cursor: Cursor = dbHelper.getSemuaKeranjang()
        val inflater = LayoutInflater.from(this)
        var totalHargaSemua = 0

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Keranjang Anda masih kosong."
            tvKosong.setPadding(32, 32, 32, 32)
            container.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME))
                val qty = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY))
                val hargaTotalItem = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_PRICE))
                val imgBase64 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOM_IMAGE))

                totalHargaSemua += hargaTotalItem

                val itemView = inflater.inflate(R.layout.item_cart, container, false)
                itemView.findViewById<TextView>(R.id.tvItemName).text = nama
                itemView.findViewById<TextView>(R.id.tvItemQty).text = qty.toString()
                itemView.findViewById<TextView>(R.id.tvItemPrice).text = "Rp $hargaTotalItem"

                val ivPreview = itemView.findViewById<ImageView>(R.id.ivItemImage)
                if (!imgBase64.isNullOrEmpty()) {
                    val imageBytes = Base64.decode(imgBase64, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ivPreview.setImageBitmap(decodedImage)
                    imageBase64UntukPesanan = imgBase64
                }

                container.addView(itemView)
            }
        }
        cursor.close()
        return totalHargaSemua
    }
}