package nicolla.coco.smoliegiftmobile

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
import org.json.JSONArray
import org.json.JSONObject

class AdminTransaksiActivity : AppCompatActivity() {

    private var llDaftar: LinearLayout? = null

    private var pdfDataId: Int = 0
    private var pdfDataNama: String = ""
    private var pdfDataTotal: Int = 0
    private var pdfDataMetode: String = ""
    private var pdfDataTanggal: String = ""
    private var pdfDataItems: String? = null

    private val createPdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? -> uri?.let { generateAndSavePdf(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_transaksi)

        val toolbar = findViewById<Toolbar>(R.id.toolbarTransaksi)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        llDaftar = findViewById(R.id.llDaftarTransaksi)
        muatSeluruhTransaksi()
    }

    private fun muatSeluruhTransaksi() {
        val container = llDaftar ?: return
        container.removeAllViews()

        val tvLoading = TextView(this)
        tvLoading.text = "Memuat data transaksi..."
        tvLoading.setPadding(32, 32, 32, 32)
        tvLoading.gravity = android.view.Gravity.CENTER
        container.addView(tvLoading)

        ApiClient.getAllTransaksi { response ->
            runOnUiThread {
                container.removeAllViews()

                if (response == null) {
                    val tv = TextView(this)
                    tv.text = "Gagal memuat data. Periksa koneksi internet."
                    tv.setPadding(32, 32, 32, 32)
                    tv.gravity = android.view.Gravity.CENTER
                    container.addView(tv)
                    return@runOnUiThread
                }

                try {
                    val json = JSONObject(response)
                    val data = json.getJSONArray("data")
                    val inflater = LayoutInflater.from(this)

                    if (data.length() == 0) {
                        val tv = TextView(this)
                        tv.text = "Belum ada transaksi."
                        tv.setPadding(32, 32, 32, 32)
                        tv.gravity = android.view.Gravity.CENTER
                        container.addView(tv)
                        return@runOnUiThread
                    }

                    for (i in 0 until data.length()) {
                        val item     = data.getJSONObject(i)
                        val id       = item.getInt("id")
                        val nama     = item.optString("nama_pembeli", "-")
                        val noHp     = item.optString("no_hp", "-")
                        val metode   = item.optString("metode_pembayaran", "-")
                        val total    = item.optInt("total_harga", 0)
                        val kode     = item.optString("kode_transaksi", "-")
                        val status   = item.optString("status", "pending")
                        val tanggal  = item.optString("created_at", "-")

                        val itemView = inflater.inflate(R.layout.item_transaksi_admin, container, false)

                        itemView.findViewById<TextView>(R.id.tvAdminTransId)?.text = "#$kode"
                        itemView.findViewById<TextView>(R.id.tvAdminTransNama)?.text = nama
                        itemView.findViewById<TextView>(R.id.tvAdminTransWa)?.text = "HP: $noHp"
                        itemView.findViewById<TextView>(R.id.tvAdminTransMetode)?.text = "Bayar: $metode"
                        itemView.findViewById<TextView>(R.id.tvAdminTransTotal)?.text = "Rp $total"
                        itemView.findViewById<TextView>(R.id.tvAdminTransTanggal)?.text = "Tgl: $tanggal"
                        itemView.findViewById<TextView>(R.id.tvAdminTransCatatan)?.text = "Pesanan Online"

                        val tvProdukTrans = itemView.findViewById<TextView>(R.id.tvAdminTransProduk)
                        val itemsJsonTrans = item.optString("items_json", "")
                        if (itemsJsonTrans.isNotEmpty() && itemsJsonTrans != "null") {
                            try {
                                val arr = org.json.JSONArray(itemsJsonTrans)
                                val sb = StringBuilder()
                                for (j in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(j)
                                    val nm = obj.optString("nama", obj.optString("name", "Produk"))
                                    val qt = obj.optInt("jumlah", obj.optInt("qty", 1))
                                    sb.append("• $nm × $qt\n")
                                }
                                tvProdukTrans?.text = sb.toString().trim()
                            } catch (_: Exception) {
                                tvProdukTrans?.text = "Pesanan Online"
                            }
                        } else {
                            tvProdukTrans?.text = "Pesanan Online"
                        }

                        val tvStatus   = itemView.findViewById<TextView>(R.id.tvAdminTransStatusLabel)
                        val btnSelesai = itemView.findViewById<Button>(R.id.btnSelesaiPesanan)
                        val btnCetak   = itemView.findViewById<Button>(R.id.btnCetakStruk)

                        if (status == "selesai") {
                            tvStatus?.text = "SELESAI"
                            tvStatus?.setTextColor(Color.parseColor("#10B981"))
                            btnSelesai?.visibility = View.GONE
                        } else {
                            tvStatus?.text = "PENDING"
                            tvStatus?.setTextColor(Color.parseColor("#EF4444"))
                            btnSelesai?.visibility = View.VISIBLE
                            btnSelesai?.setOnClickListener { konfirmasiSelesai(id, nama) }
                        }

                        btnCetak?.setOnClickListener {
                            pdfDataId      = id
                            pdfDataNama    = nama
                            pdfDataTotal   = total
                            pdfDataMetode  = metode
                            pdfDataTanggal = tanggal
                            pdfDataItems   = null
                            try {
                                createPdfLauncher.launch("Struk_Smolie_$kode.pdf")
                            } catch (e: Exception) {
                                Toast.makeText(this, "Gagal mencetak: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        container.addView(itemView)
                    }
                } catch (e: Exception) {
                    val tv = TextView(this)
                    tv.text = "Error: ${e.message}"
                    tv.setPadding(32, 32, 32, 32)
                    container.addView(tv)
                }
            }
        }
    }

    private fun konfirmasiSelesai(id: Int, nama: String) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Pesanan")
            .setMessage("Tandai pesanan dari $nama sebagai selesai?")
            .setPositiveButton("Ya") { _, _ ->
                ApiClient.konfirmasiTransaksi(id) { berhasil ->
                    runOnUiThread {
                        if (berhasil) {
                            Toast.makeText(this, "Pesanan dikonfirmasi!", Toast.LENGTH_SHORT).show()
                            muatSeluruhTransaksi()
                        } else {
                            Toast.makeText(this, "Gagal konfirmasi. Coba lagi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun generateAndSavePdf(uri: Uri) {
        val pdfDocument = PdfDocument()
        try {
            val paint = Paint()
            val titlePaint = Paint()
            val pageInfo = PdfDocument.PageInfo.Builder(300, 500, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            titlePaint.textAlign = Paint.Align.CENTER
            titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            titlePaint.textSize = 18f
            canvas.drawText("SMOLIE GIFT", 150f, 40f, titlePaint)

            paint.textSize = 10f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Surabaya, Jawa Timur", 150f, 55f, paint)
            canvas.drawLine(20f, 70f, 280f, 70f, paint)

            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 9f
            canvas.drawText("No. Inv  : #INV-0$pdfDataId", 20f, 90f, paint)
            canvas.drawText("Tgl      : $pdfDataTanggal", 20f, 105f, paint)
            canvas.drawText("Kasir    : Smolie Admin", 20f, 120f, paint)
            canvas.drawText("Customer : $pdfDataNama", 20f, 135f, paint)

            canvas.drawLine(20f, 150f, 280f, 150f, paint)

            var yPos = 170f
            if (!pdfDataItems.isNullOrEmpty()) {
                try {
                    val jsonArray = JSONArray(pdfDataItems)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.optString("nama", "Produk")
                        val qty = obj.optInt("jumlah", 1)
                        canvas.drawText(name, 20f, yPos, paint)
                        paint.textAlign = Paint.Align.RIGHT
                        canvas.drawText("x$qty", 280f, yPos, paint)
                        paint.textAlign = Paint.Align.LEFT
                        yPos += 15f
                    }
                } catch (e: Exception) {
                    canvas.drawText("- Detail tidak tersedia -", 20f, yPos, paint)
                    yPos += 15f
                }
            }

            canvas.drawLine(20f, yPos + 10f, 280f, yPos + 10f, paint)
            yPos += 35f

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("METODE : $pdfDataMetode", 20f, yPos, paint)
            paint.textSize = 14f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("TOTAL : Rp $pdfDataTotal", 280f, yPos, paint)

            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f
            canvas.drawText("Terima kasih telah berbelanja!", 150f, yPos + 40f, paint)
            canvas.drawText("~ Smolie Gift Mobile ~", 150f, yPos + 55f, paint)

            pdfDocument.finishPage(page)
            contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
            Toast.makeText(this, "Struk PDF Berhasil Disimpan!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
        }
    }
}