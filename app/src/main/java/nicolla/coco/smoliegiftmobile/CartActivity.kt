package nicolla.coco.smoliegiftmobile

import android.app.Dialog
import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.smoliegift.database.DatabaseHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CartActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var imageBase64UntukPesanan: String? = null
    private var eventInfoUntukPesanan: String? = null
    private var waUser: String = "-"
    private var currentUserEmail: String? = null
    private var isKasirMode: Boolean = false

    private var metodeDipilih = "Tunai"
    private var grandTotal = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        val toolbar = findViewById<Toolbar>(R.id.toolbarCart)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        dbHelper = DatabaseHelper(this)
        currentUserEmail = intent.getStringExtra("USER_EMAIL")
        isKasirMode = intent.getBooleanExtra("IS_KASIR_MODE", false)

        val llDaftar = findViewById<LinearLayout>(R.id.llDaftarKeranjang)
        val tvTotalAkhir = findViewById<TextView>(R.id.tvTotalBayarAkhir)
        val btnKonfirmasi = findViewById<Button>(R.id.btnKonfirmasi)
        val etNama = findViewById<EditText>(R.id.etNamaPemesan)
        val tvLabelNama = findViewById<TextView>(R.id.tvLabelNamaPemesan)

        val llKasirPaymentSection = findViewById<LinearLayout>(R.id.llKasirPaymentSection)
        val etUangDiterima = findViewById<EditText>(R.id.etUangDiterima)
        val tvKembalian = findViewById<TextView>(R.id.tvKembalian)

        if (isKasirMode) {
            toolbar.title = "Kasir - Pembayaran Tunai"
            tvLabelNama.text = "NAMA PEMBELI"
            etNama.hint = "Masukkan Nama Pembeli"
            etNama.isEnabled = true
            etNama.setText("")
            
            findViewById<LinearLayout>(R.id.llPaymentMethods).visibility = View.GONE
            metodeDipilih = "Tunai"
            llKasirPaymentSection.visibility = View.VISIBLE
            
            etUangDiterima.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val input = s.toString().toIntOrNull() ?: 0
                    val kembalian = input - grandTotal
                    tvKembalian.text = "Rp $kembalian"
                    tvKembalian.setTextColor(if (kembalian >= 0) Color.parseColor("#2E7D32") else Color.RED)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        } else {
            etNama.setText(currentUserEmail)
            etNama.isEnabled = false
        }
        
        grandTotal = tampilkanDataDanHitungTotal(llDaftar)
        tvTotalAkhir.text = "Rp $grandTotal"

        btnKonfirmasi.setOnClickListener {
            val namaPembeli = etNama.text.toString().trim()
            val uangDiterimaStr = etUangDiterima.text.toString()
            val uangDiterima = uangDiterimaStr.toIntOrNull() ?: 0

            if (grandTotal <= 0) return@setOnClickListener
            if (namaPembeli.isEmpty()) {
                Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isKasirMode && (uangDiterimaStr.isEmpty() || uangDiterima < grandTotal)) {
                Toast.makeText(this, "Pembayaran kurang!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val itemsJson = getCartItemsAsJson()
            
            val resultId = if (isKasirMode) {
                // KASIR: Langsung ke Riwayat
                dbHelper.simpanTransaksiLangsung(namaPembeli, "Beli di Toko", "Tunai", grandTotal, imageBase64UntukPesanan, "Penjualan Toko", itemsJson)
            } else {
                // PEMBELI: Masuk Antrean Pesanan
                dbHelper.buatPesanan(namaPembeli, "-", metodeDipilih, grandTotal, imageBase64UntukPesanan, eventInfoUntukPesanan, itemsJson)
            }

            if (resultId != -1L) {
                kurangiStokDariKeranjang()
                if (isKasirMode) {
                    tampilkanDialogStruk(namaPembeli, itemsJson, grandTotal, uangDiterima)
                } else {
                    dbHelper.kosongkanKeranjang()
                    Toast.makeText(this, "Berhasil!", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun tampilkanDialogStruk(pembeli: String, itemsJson: String, total: Int, diterima: Int) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_struk)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
        
        val sb = StringBuilder()
        sb.append("      SMOLIE GIFT SHOP\n")
        sb.append("   Jl. Raya No. 123 Surabaya\n")
        sb.append("--------------------------------\n")
        sb.append("Plgn: $pembeli\n")
        sb.append("--------------------------------\n")
        val items = JSONArray(itemsJson)
        for (i in 0 until items.length()) {
            val obj = items.getJSONObject(i)
            sb.append("${obj.getString("name")} x${obj.getInt("qty")}\n")
        }
        sb.append("--------------------------------\n")
        sb.append("TOTAL   : Rp $total\n")
        sb.append("BAYAR   : Rp $diterima\n")
        sb.append("KEMBALI : Rp ${diterima - total}\n")
        
        dialog.findViewById<TextView>(R.id.tvIsiStruk).text = sb.toString()
        dialog.findViewById<Button>(R.id.btnTutupStruk).setOnClickListener {
            dbHelper.kosongkanKeranjang()
            dialog.dismiss()
            finish()
        }
        dialog.show()
    }

    private fun kurangiStokDariKeranjang() {
        val c = dbHelper.getSemuaKeranjang()
        while (c.moveToNext()) {
            dbHelper.kurangiStokProduk(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME)), c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY)))
        }
        c.close()
    }

    private fun getCartItemsAsJson(): String {
        val c = dbHelper.getSemuaKeranjang()
        val array = JSONArray()
        while (c.moveToNext()) {
            array.put(JSONObject().apply {
                put("name", c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME)))
                put("qty", c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY)))
            })
        }
        c.close()
        return array.toString()
    }

    private fun tampilkanDataDanHitungTotal(container: LinearLayout): Int {
        val cursor: Cursor = dbHelper.getSemuaKeranjang()
        val inflater = LayoutInflater.from(this)
        var total = 0
        container.removeAllViews()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CART_ID))
            val p = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_PRICE))
            total += p
            val v = inflater.inflate(R.layout.item_cart, container, false)
            v.findViewById<TextView>(R.id.tvItemName).text = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME))
            v.findViewById<TextView>(R.id.tvItemQty).text = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY)).toString()
            v.findViewById<TextView>(R.id.tvItemPrice).text = "Rp $p"
            v.findViewById<Button>(R.id.btnHapusItemKeranjang).setOnClickListener { if (dbHelper.hapusItemKeranjang(id)) recreate() }
            container.addView(v)
        }
        cursor.close()
        return total
    }
}
