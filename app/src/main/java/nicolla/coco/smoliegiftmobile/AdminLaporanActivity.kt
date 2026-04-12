package nicolla.coco.smoliegiftmobile

import android.content.ContentValues
import android.database.Cursor
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminLaporanActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var totalPendapatanGlobal = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_laporan)

        dbHelper = DatabaseHelper(this)
        val llDaftar = findViewById<LinearLayout>(R.id.llDaftarLaporan)
        val tvPendapatan = findViewById<TextView>(R.id.tvTotalPendapatanAdmin)
        val btnCetak = findViewById<Button>(R.id.btnCetakPdf)

        totalPendapatanGlobal = muatDataLaporan(llDaftar)
        tvPendapatan.text = "Rp $totalPendapatanGlobal"

        btnCetak.setOnClickListener {
            cetakLaporanKePdf()
        }
    }

    private fun muatDataLaporan(container: LinearLayout): Int {
        val cursor: Cursor = dbHelper.getLaporanPenjualan()
        val inflater = LayoutInflater.from(this)
        var totalPendapatan = 0

        container.removeAllViews()

        if (cursor.count == 0) {
            val tvKosong = TextView(this)
            tvKosong.text = "Belum ada riwayat penjualan sukses."
            container.addView(tvKosong)
        } else {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANS_ID))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_NAME))
                val wa = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_WA))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GRAND_TOTAL))

                totalPendapatan += total

                val itemView = inflater.inflate(R.layout.item_transaksi_admin, container, false)

                itemView.findViewById<TextView>(R.id.tvAdminTransId).text = "SELESAI (INV-0$id)"
                itemView.findViewById<TextView>(R.id.tvAdminTransNama).text = "Pemesan: $nama"
                itemView.findViewById<TextView>(R.id.tvAdminTransWa).text = "WA: $wa"
                itemView.findViewById<TextView>(R.id.tvAdminTransTotal).text = "Rp $total"

                itemView.findViewById<Button>(R.id.btnHubungiWa).visibility = View.GONE
                itemView.findViewById<Button>(R.id.btnSelesaiPesanan).visibility = View.GONE

                container.addView(itemView)
            }
        }
        cursor.close()
        return totalPendapatan
    }

    private fun cetakLaporanKePdf() {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.textSize = 18f
        titlePaint.isFakeBoldText = true
        canvas.drawText("LAPORAN PENJUALAN SMOLIE GIFT", 297f, 50f, titlePaint)

        titlePaint.textSize = 12f
        titlePaint.isFakeBoldText = false
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Dicetak pada: $date", 297f, 70f, titlePaint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(50f, 100f, 545f, 120f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("ID", 60f, 114f, paint)
        canvas.drawText("Nama Pemesan", 100f, 114f, paint)
        canvas.drawText("Metode", 300f, 114f, paint)
        canvas.drawText("Total Harga", 450f, 114f, paint)

        var yPos = 140f
        paint.isFakeBoldText = false
        val cursor = dbHelper.getLaporanPenjualan()

        if (cursor.count == 0) {
            canvas.drawText("Tidak ada data transaksi.", 60f, yPos, paint)
        } else {
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANS_ID))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOMER_NAME))
                val metode = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAYMENT_METHOD))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GRAND_TOTAL))

                canvas.drawText("INV-0$id", 60f, yPos, paint)
                canvas.drawText(nama, 100f, yPos, paint)
                canvas.drawText(metode, 300f, yPos, paint)
                canvas.drawText("Rp $total", 450f, yPos, paint)

                yPos += 20f
                if (yPos > 750f) break
            }
        }
        cursor.close()

        canvas.drawLine(50f, yPos, 545f, yPos, paint)
        yPos += 30f

        titlePaint.textAlign = Paint.Align.RIGHT
        titlePaint.textSize = 14f
        titlePaint.isFakeBoldText = true
        canvas.drawText("TOTAL PENDAPATAN: Rp $totalPendapatanGlobal", 545f, yPos, titlePaint)

        pdfDocument.finishPage(page)

        // Simpan File ke folder DOWNLOAD agar mudah ditemukan
        val fileName = "Laporan_Smolie_${System.currentTimeMillis()}.pdf"
        var outputStream: OutputStream? = null

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = resolver.openOutputStream(uri)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
            }

            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                Toast.makeText(this, "PDF Berhasil disimpan di folder DOWNLOAD", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal mencetak PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
            try { outputStream?.close() } catch (e: IOException) {}
        }
    }
}