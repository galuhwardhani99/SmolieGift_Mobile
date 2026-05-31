package nicolla.coco.smoliegiftmobile

import android.database.Cursor
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.smoliegift.database.DatabaseHelper
import org.json.JSONArray
import org.json.JSONObject

class AdminTransaksiActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var llDaftar: LinearLayout
    
    // Data sementara untuk PDF
    private var pdfDataId: Int = 0
    private var pdfDataNama: String = ""
    private var pdfDataTotal: Int = 0
    private var pdfDataMetode: String = ""
    private var pdfDataTanggal: String = ""
    private var pdfDataItems: String? = null

    private val createPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        uri?.let {
            generateAndSavePdf(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_transaksi)

        val toolbar = findViewById<Toolbar>(R.id.toolbarTransaksi)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        dbHelper = DatabaseHelper(this)
        llDaftar = findViewById(R.id.llDaftarTransaksi)

        muatSeluruhTransaksi()
    }

    private fun muatSeluruhTransaksi() {
        llDaftar.removeAllViews()
        val cursor: Cursor = dbHelper.getSeluruhTransaksi()
        val inflater = LayoutInflater.from(this)

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Belum ada riwayat transaksi."
            tvKosong.setPadding(32, 32, 32, 32)
            tvKosong.gravity = android.view.Gravity.CENTER
            llDaftar.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_NAME))
                val wa = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_WA))
                val metode = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAYMENT_METHOD))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GRAND_TOTAL))
                val rawDate = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANS_DATE))
                val itemsJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEMS_JSON))
                val eventInfo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_INFO))
                val status = cursor.getString(cursor.getColumnIndexOrThrow("status"))

                val itemView = inflater.inflate(R.layout.item_transaksi_admin, llDaftar, false)

                val tvStatus = itemView.findViewById<TextView>(R.id.tvAdminTransStatusLabel)
                val btnSelesai = itemView.findViewById<Button>(R.id.btnSelesaiPesanan)
                val btnCetak = itemView.findViewById<Button>(R.id.btnCetakStruk)
                val tvCatatan = itemView.findViewById<TextView>(R.id.tvAdminTransCatatan)

                itemView.findViewById<TextView>(R.id.tvAdminTransId).text = "#INV-0$id"
                itemView.findViewById<TextView>(R.id.tvAdminTransNama).text = nama
                itemView.findViewById<TextView>(R.id.tvAdminTransWa).text = "WhatsApp: $wa"
                itemView.findViewById<TextView>(R.id.tvAdminTransMetode).text = "Bayar: $metode"
                itemView.findViewById<TextView>(R.id.tvAdminTransTotal).text = "Rp $total"
                itemView.findViewById<TextView>(R.id.tvAdminTransTanggal).text = "Tgl: $rawDate"
                
                tvCatatan.text = "Catatan: ${eventInfo ?: "-"}"

                if (status == "SELESAI") {
                    tvStatus.text = "SELESAI"
                    tvStatus.setTextColor(Color.parseColor("#10B981"))
                    btnSelesai.visibility = View.GONE
                } else {
                    tvStatus.text = "AKTIF"
                    tvStatus.setTextColor(Color.parseColor("#EF4444"))
                    btnSelesai.visibility = View.VISIBLE
                    btnSelesai.setOnClickListener { konfirmasiSelesai(id, nama) }
                }

                val tvProduk = itemView.findViewById<TextView>(R.id.tvAdminTransProduk)
                if (!itemsJson.isNullOrEmpty()) {
                    try {
                        val jsonArray = JSONArray(itemsJson)
                        val sb = StringBuilder()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            sb.append("• ${obj.getString("name")} (${obj.getInt("qty")}x)\n")
                        }
                        tvProduk.text = sb.toString().trim()
                    } catch (e: Exception) { tvProduk.text = "Detail tidak tersedia" }
                }

                btnCetak.setOnClickListener {
                    pdfDataId = id
                    pdfDataNama = nama
                    pdfDataTotal = total
                    pdfDataMetode = metode
                    pdfDataTanggal = rawDate
                    pdfDataItems = itemsJson
                    
                    val fileName = "Struk_Smolie_#INV-0$id.pdf"
                    createPdfLauncher.launch(fileName)
                }

                llDaftar.addView(itemView)
            }
        }
        cursor.close()
    }

    private fun generateAndSavePdf(uri: Uri) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        
        // Ukuran struk mini (lebar 300, tinggi menyesuaikan items)
        val pageInfo = PdfDocument.PageInfo.Builder(300, 500, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Header Toko
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 18f
        canvas.drawText("SMOLIE GIFT", 150f, 40f, titlePaint)
        
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Surabaya, Jawa Timur", 150f, 55f, paint)
        canvas.drawLine(20f, 70f, 280f, 70f, paint)

        // Detail Transaksi
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 9f
        canvas.drawText("No. Inv  : #INV-0$pdfDataId", 20f, 90f, paint)
        canvas.drawText("Tgl      : $pdfDataTanggal", 20f, 105f, paint)
        canvas.drawText("Kasir    : Smolie Admin", 20f, 120f, paint)
        canvas.drawText("Customer : $pdfDataNama", 20f, 135f, paint)
        
        canvas.drawLine(20f, 150f, 280f, 150f, paint)
        
        // List Item
        var yPos = 170f
        if (!pdfDataItems.isNullOrEmpty()) {
            val jsonArray = JSONArray(pdfDataItems)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.getString("name")
                val qty = obj.getInt("qty")
                
                canvas.drawText(name, 20f, yPos, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("x$qty", 280f, yPos, paint)
                
                paint.textAlign = Paint.Align.LEFT
                yPos += 15f
            }
        }
        
        canvas.drawLine(20f, yPos + 10f, 280f, yPos + 10f, paint)
        
        // Total
        yPos += 35f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("METODE : $pdfDataMetode", 20f, yPos, paint)
        paint.textSize = 14f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("TOTAL : Rp $pdfDataTotal", 280f, yPos, paint)
        
        // Footer
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        canvas.drawText("Terima kasih telah berbelanja!", 150f, yPos + 40f, paint)
        canvas.drawText("~ Smolie Gift Mobile ~", 150f, yPos + 55f, paint)

        pdfDocument.finishPage(page)

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            Toast.makeText(this, "Struk PDF Berhasil Disimpan!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal: " + e.message, Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun konfirmasiSelesai(id: Int, nama: String) {
        AlertDialog.Builder(this)
            .setTitle("Pesanan Selesai?")
            .setMessage("Tandai pesanan $nama sebagai selesai?")
            .setPositiveButton("Ya") { _, _ ->
                if (dbHelper.selesaikanPesanan(id)) {
                    Toast.makeText(this, "Berhasil!", Toast.LENGTH_SHORT).show()
                    muatSeluruhTransaksi()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
