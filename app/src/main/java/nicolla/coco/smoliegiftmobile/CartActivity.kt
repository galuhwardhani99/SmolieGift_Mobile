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
import org.json.JSONArray
import org.json.JSONObject

class CartActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var imageBase64UntukPesanan: String? = null
    private var eventInfoUntukPesanan: String? = null
    private var waUser: String = "-"
    private var currentUserEmail: String? = null

    private var metodeDipilih = "Tunai"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        dbHelper = DatabaseHelper(this)
        currentUserEmail = intent.getStringExtra("USER_EMAIL")

        val llDaftar = findViewById<LinearLayout>(R.id.llDaftarKeranjang)
        val tvTotalAkhir = findViewById<TextView>(R.id.tvTotalBayarAkhir)
        val btnKonfirmasi = findViewById<Button>(R.id.btnKonfirmasi)
        val etNama = findViewById<EditText>(R.id.etNamaPemesan)

        // Tampilkan email pada input nama pemesan dan buat tidak bisa diedit
        etNama.setText(currentUserEmail)
        etNama.isEnabled = false
        etNama.setTextColor(Color.parseColor("#64748B"))
        
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
            val email = currentUserEmail ?: ""

            if (grandTotal <= 0) {
                Toast.makeText(this, "Keranjang Anda masih kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                Toast.makeText(this, "Sesi tidak valid, silakan login ulang.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cursorUser = dbHelper.readableDatabase.rawQuery(
                "SELECT ${DatabaseHelper.COLUMN_PHONE} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_EMAIL} = ?", 
                arrayOf(email)
            )
            if (cursorUser.moveToFirst()) {
                waUser = cursorUser.getString(0)
            }
            cursorUser.close()

            val itemsJson = getCartItemsAsJson()

            val sukses = dbHelper.buatPesanan(email, waUser, metodeDipilih, grandTotal, imageBase64UntukPesanan, eventInfoUntukPesanan, itemsJson)

            if (sukses) {
                kurangiStokDariKeranjang()
                
                dbHelper.kosongkanKeranjang()
                Toast.makeText(this, "Pesanan Berhasil!", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Gagal menyimpan pesanan.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun kurangiStokDariKeranjang() {
        val cursor = dbHelper.getSemuaKeranjang()
        while (cursor.moveToNext()) {
            val namaProd = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME))
            val qtyBought = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY))
            dbHelper.kurangiStokProduk(namaProd, qtyBought)
        }
        cursor.close()
    }

    private fun getCartItemsAsJson(): String {
        val cursor = dbHelper.getSemuaKeranjang()
        val jsonArray = JSONArray()
        while (cursor.moveToNext()) {
            val item = JSONObject()
            item.put("name", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME)))
            item.put("qty", cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY)))
            jsonArray.put(item)
        }
        cursor.close()
        return jsonArray.toString()
    }

    private fun tampilkanDataDanHitungTotal(container: LinearLayout): Int {
        val cursor: Cursor = dbHelper.getSemuaKeranjang()
        val inflater = LayoutInflater.from(this)
        var totalHargaSemua = 0

        container.removeAllViews()

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Keranjang Anda masih kosong."
            tvKosong.setPadding(32, 32, 32, 32)
            container.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val cartId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_ID))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME))
                val qty = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY))
                val hargaTotalItem = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_PRICE))
                val customImgBase64 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOM_IMAGE))
                val prodImgBase64 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_PRODUCT_IMAGE))

                totalHargaSemua += hargaTotalItem

                val itemView = inflater.inflate(R.layout.item_cart, container, false)
                itemView.findViewById<TextView>(R.id.tvItemName).text = nama
                itemView.findViewById<TextView>(R.id.tvItemQty).text = qty.toString()
                itemView.findViewById<TextView>(R.id.tvItemPrice).text = "Rp $hargaTotalItem"

                val ivPreview = itemView.findViewById<ImageView>(R.id.ivItemImage)
                val displayImgBase64 = if (!customImgBase64.isNullOrEmpty()) customImgBase64 else prodImgBase64
                
                if (!displayImgBase64.isNullOrEmpty()) {
                    val imageBytes = Base64.decode(displayImgBase64, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ivPreview.setImageBitmap(decodedImage)
                    if (!customImgBase64.isNullOrEmpty()) imageBase64UntukPesanan = customImgBase64
                }

                itemView.findViewById<Button>(R.id.btnHapusItemKeranjang).setOnClickListener {
                    if (dbHelper.hapusItemKeranjang(cartId)) {
                        Toast.makeText(this, "Item dihapus", Toast.LENGTH_SHORT).show()
                        recreate()
                    }
                }
                container.addView(itemView)
            }
        }
        cursor.close()
        return totalHargaSemua
    }
}