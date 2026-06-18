package nicolla.coco.smoliegiftmobile

import android.Manifest
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smoliegift.database.DatabaseHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CartActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var imageBase64UntukPesanan: String? = null
    private var eventInfoUntukPesanan: String? = null
    private var currentUserEmail: String? = null
    private var isKasirMode: Boolean = false

    private var metodeDipilih = "Tunai"
    private var grandTotal = 0

    private var currentKodeTransaksi: String? = null
    private var currentTransaksiId: Long = -1L

    private var btSocket: BluetoothSocket? = null
    private var btOutputStream: OutputStream? = null

    // Menyimpan teks struk yang sedang menunggu izin Bluetooth diberikan
    private var pendingStrukText: String? = null

    companion object {
        const val BASE_URL = "http://192.168.1.28/toko-smolie/public"
        const val REQUEST_BLUETOOTH_PERMISSION = 101
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

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
        val btnMetodeTunai = findViewById<Button>(R.id.btnMetodeTunai)
        val btnMetodeQris = findViewById<Button>(R.id.btnMetodeQris)
        val llContainerQris = findViewById<LinearLayout>(R.id.llContainerQris)

        if (isKasirMode) {
            toolbar.title = "Kasir - Pembayaran"
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

            btnKonfirmasi.text = "Generate QR untuk Pelanggan"
            btnKonfirmasi.setOnClickListener {
                val namaPembeli = etNama.text.toString().trim()
                if (namaPembeli.isEmpty()) {
                    Toast.makeText(this, "Nama pembeli tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (grandTotal <= 0) {
                    Toast.makeText(this, "Keranjang kosong!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                prosesGenerateQR(namaPembeli)
            }

        } else {
            etNama.setText(currentUserEmail)
            etNama.isEnabled = false

            btnMetodeTunai.setOnClickListener {
                metodeDipilih = "Tunai"
                btnMetodeTunai.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D6EFD")))
                btnMetodeTunai.setTextColor(Color.WHITE)
                btnMetodeQris.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F6F8")))
                btnMetodeQris.setTextColor(Color.parseColor("#2D3142"))
                llContainerQris.visibility = View.GONE
            }

            btnMetodeQris.setOnClickListener {
                metodeDipilih = "QRIS"
                btnMetodeQris.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D6EFD")))
                btnMetodeQris.setTextColor(Color.WHITE)
                btnMetodeTunai.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F6F8")))
                btnMetodeTunai.setTextColor(Color.parseColor("#2D3142"))
                llContainerQris.visibility = View.VISIBLE
            }

            // ✅ UPDATED: Kirim ke Laravel API
            // Ambil no_hp dari database sebelum kirim transaksi
            btnKonfirmasi.setOnClickListener {
                val namaPembeli = etNama.text.toString().trim()
                if (grandTotal <= 0 || namaPembeli.isEmpty()) return@setOnClickListener

                // Ambil no_hp dari DB
                val noHp = try {
                    val cursor = dbHelper.getUserByEmail(currentUserEmail ?: "")
                    if (cursor != null && cursor.moveToFirst()) {
                        val hp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE)) ?: "-"
                        cursor.close()
                        hp
                    } else "-"
                } catch (e: Exception) { "-" }

                val kode = generateKodeTransaksi()
                btnKonfirmasi.isEnabled = false
                btnKonfirmasi.text = "Mengirim pesanan..."

                // Kirim items keranjang juga
                val itemsJson = getCartItemsAsJson()

                ApiClient.buatTransaksi(
                    namaPembeli      = namaPembeli,
                    noHp             = noHp,        // ← sudah ada no_hp
                    metodePembayaran = metodeDipilih,
                    jenisPesanan     = "Online",
                    kodeTransaksi    = kode,
                    totalHarga       = grandTotal,
                    itemsJson        = itemsJson    // ← tambah parameter ini
                ) { berhasil ->
                    runOnUiThread {
                        if (berhasil) {
                            dbHelper.kosongkanKeranjang()
                            Toast.makeText(this, "Pesanan berhasil dikirim!", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            btnKonfirmasi.isEnabled = true
                            btnKonfirmasi.text = "Konfirmasi Pesanan"
                            Toast.makeText(this, "Gagal mengirim pesanan. Coba lagi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        grandTotal = tampilkanDataDanHitungTotal(llDaftar)
        tvTotalAkhir.text = formatRupiah(grandTotal)
    }

    private fun prosesGenerateQR(namaPembeli: String) {
        val itemsJson = getCartItemsAsJson()
        val kode = generateKodeTransaksi()
        currentKodeTransaksi = kode

        currentTransaksiId = dbHelper.simpanTransaksiPending(
            namaPembeli, "Beli di Toko", "Tunai", grandTotal,
            imageBase64UntukPesanan, "Penjualan Toko", itemsJson, kode
        )

        if (currentTransaksiId == -1L) {
            Toast.makeText(this, "Gagal menyimpan transaksi!", Toast.LENGTH_SHORT).show()
            return
        }

        // Juga kirim ke API agar admin bisa lihat (jenisPesanan harus konsisten
        // dengan pengecekan "offline"/"Beli di Toko" di AdminTransaksiActivity
        // & AdminLaporanActivity, supaya tampil sebagai "Pesanan Offline")
        ApiClient.buatTransaksi(
            namaPembeli      = namaPembeli,
            noHp             = "-",
            metodePembayaran = "Tunai",
            jenisPesanan     = "Beli di Toko",
            kodeTransaksi    = kode,
            totalHarga       = grandTotal,
            itemsJson        = itemsJson
        ) { _ -> }

        val rincianStruk = buildStrukText(namaPembeli, itemsJson, grandTotal, 0, 0)
        val qrBitmap = generateQRBitmap(rincianStruk, 600)

        runOnUiThread {
            if (qrBitmap != null) {
                tampilkanDialogQR(qrBitmap, rincianStruk, namaPembeli, itemsJson)
            } else {
                Toast.makeText(this, "Gagal membuat QR Code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun tampilkanDialogQR(qrBitmap: Bitmap, isiStruk: String, namaPembeli: String, itemsJson: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_qr_kasir)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)

        val ivQR = dialog.findViewById<ImageView>(R.id.ivQRCode)
        val tvStatusQR = dialog.findViewById<TextView>(R.id.tvStatusQR)
        val tvUrlStruk = dialog.findViewById<TextView>(R.id.tvUrlStruk)
        val btnBatalQR = dialog.findViewById<Button>(R.id.btnBatalQR)
        val btnKonfirmasiBayar = dialog.findViewById<Button>(R.id.btnKonfirmasiBayar)

        ivQR.setImageBitmap(qrBitmap)
        tvUrlStruk.text = "Scan untuk rincian belanja"
        btnKonfirmasiBayar.visibility = View.VISIBLE
        tvStatusQR.text = "QR Ready untuk di-scan"

        btnBatalQR.setOnClickListener {
            if (currentTransaksiId != -1L) dbHelper.hapusTransaksiById(currentTransaksiId)
            dialog.dismiss()
        }

        btnKonfirmasiBayar.setOnClickListener {
            dialog.dismiss()
            val etUang = findViewById<EditText>(R.id.etUangDiterima)
            val uangDiterima = etUang.text.toString().toIntOrNull() ?: grandTotal
            tampilkanDialogKonfirmasiBayar(namaPembeli, itemsJson, grandTotal, uangDiterima)
        }
        dialog.show()
    }

    private fun tampilkanDialogKonfirmasiBayar(pembeli: String, itemsJson: String, total: Int, diterima: Int) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_struk)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)

        val kembalian = diterima - total
        val sb = buildStrukText(pembeli, itemsJson, total, diterima, kembalian)
        dialog.findViewById<TextView>(R.id.tvIsiStruk).text = sb

        dialog.findViewById<Button>(R.id.btnPrintStruk).setOnClickListener { printViaBluetoothAtauPilih(sb) }
        dialog.findViewById<Button>(R.id.btnSimpanPdf).setOnClickListener { simpanSebagaiPdf(sb) }
        dialog.findViewById<Button>(R.id.btnTutupStruk).setOnClickListener {
            dbHelper.updateStatusTransaksi(currentKodeTransaksi ?: "", "selesai")
            if (currentTransaksiId != -1L) {
                kurangiStokDariKeranjang()
                dbHelper.kosongkanKeranjang()
            }
            dialog.dismiss()
            finish()
        }
        dialog.show()
    }

    // ===================== BLUETOOTH PRINT =====================

    private fun printViaBluetoothAtauPilih(strukteks: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Simpan teks struk supaya bisa dilanjutkan otomatis
                // setelah user memberi izin di onRequestPermissionsResult
                pendingStrukText = strukteks
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_PERMISSION)
                return
            }
        }
        lanjutkanProsesPrint(strukteks)
    }

    private fun lanjutkanProsesPrint(strukteks: String) {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null || !btAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth tidak aktif!", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin Bluetooth diperlukan untuk mencetak.", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice> = btAdapter.bondedDevices
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "Tidak ada printer Bluetooth!", Toast.LENGTH_LONG).show()
            return
        }
        val deviceNames = pairedDevices.map { it.name }.toTypedArray()
        val deviceList = pairedDevices.toList()
        AlertDialog.Builder(this)
            .setTitle("Pilih Printer")
            .setItems(deviceNames) { _, index -> koneksiDanPrint(deviceList[index], strukteks) }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diberikan → lanjutkan proses print yang tertunda
                pendingStrukText?.let { lanjutkanProsesPrint(it) }
            } else {
                Toast.makeText(this, "Izin Bluetooth ditolak. Tidak bisa mencetak.", Toast.LENGTH_SHORT).show()
            }
            pendingStrukText = null
        }
    }

    private fun koneksiDanPrint(device: BluetoothDevice, strukteks: String) {
        Thread {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return@Thread
                btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                btSocket?.connect()
                btOutputStream = btSocket?.outputStream
                btOutputStream?.write(buildEscPosData(strukteks))
                btOutputStream?.flush()
                runOnUiThread { Toast.makeText(this, "Berhasil dicetak!", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Gagal print: ${e.message}", Toast.LENGTH_LONG).show() }
            } finally {
                try { btOutputStream?.close(); btSocket?.close() } catch (_: Exception) {}
            }
        }.start()
    }

    private fun simpanSebagaiPdf(strukteks: String) {
        val htmlContent = "<html><body style='font-family:monospace; white-space:pre;'>$strukteks</body></html>"
        val webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = getSystemService(PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Struk_SmolieGift")
                printManager.print("Struk_SmolieGift", printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun buildStrukText(pembeli: String, itemsJson: String, total: Int, diterima: Int, kembalian: Int): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date())
        val sb = StringBuilder()
        sb.append("      SMOLIE GIFT      \n")
        sb.append("   Jl. Contoh No. 123  \n")
        sb.append("------------------------------\n")
        sb.append("Tgl: $dateStr\n")
        sb.append("Pembeli: $pembeli\n")
        sb.append("------------------------------\n")
        try {
            val arr = JSONArray(itemsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val nama = obj.getString("nama")
                val qty = obj.getInt("jumlah")
                val harga = obj.getInt("harga")
                sb.append("$nama\n")
                sb.append("  $qty x ${formatRupiah(harga)} = ${formatRupiah(qty * harga)}\n")
            }
        } catch (e: Exception) {
            sb.append("Gagal memuat rincian item\n")
        }
        sb.append("------------------------------\n")
        sb.append("TOTAL:     ${formatRupiah(total)}\n")
        if (diterima > 0) {
            sb.append("DITERIMA:  ${formatRupiah(diterima)}\n")
            sb.append("KEMBALI:   ${formatRupiah(kembalian)}\n")
        }
        sb.append("------------------------------\n")
        sb.append("   Terima Kasih Atas   \n")
        sb.append("    Kunjungan Anda     \n")
        return sb.toString()
    }

    private fun buildEscPosData(text: String): ByteArray {
        val result = mutableListOf<Byte>()
        result.add(27.toByte()); result.add(64.toByte())
        result.add(27.toByte()); result.add(97.toByte()); result.add(0.toByte())
        result.addAll(text.toByteArray(Charsets.US_ASCII).toList())
        result.add(10.toByte()); result.add(10.toByte()); result.add(10.toByte())
        return result.toByteArray()
    }

    private fun generateKodeTransaksi(): String {
        val datePart = SimpleDateFormat("yyMMddHHmm", Locale.getDefault()).format(Date())
        val randomPart = (10..99).random()
        return "TX$datePart$randomPart"
    }

    private fun generateQRBitmap(content: String, size: Int): Bitmap? {
        return try {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) for (y in 0 until size)
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            bitmap
        } catch (e: WriterException) { null }
    }

    private fun getCartItemsAsJson(): String {
        val cursor = dbHelper.getSemuaKeranjang()
        val jsonArray = JSONArray()
        if (cursor.moveToFirst()) {
            do {
                val obj = JSONObject()
                obj.put("nama", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME)))
                obj.put("harga", cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_PRICE)))
                obj.put("jumlah", cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY)))
                jsonArray.put(obj)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return jsonArray.toString()
    }

    private fun kurangiStokDariKeranjang() {
        val cursor = dbHelper.getSemuaKeranjang()
        if (cursor.moveToFirst()) {
            do {
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME))
                val jumlah = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY))
                dbHelper.kurangiStokProduk(nama, jumlah)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    private fun tampilkanDataDanHitungTotal(container: LinearLayout): Int {
        container.removeAllViews()
        val cursor = dbHelper.getSemuaKeranjang()
        var total = 0
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                val nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCT_NAME))
                val harga = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_PRICE))
                val jumlah = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QTY))
                total += (harga * jumlah)

                val view = LayoutInflater.from(this).inflate(R.layout.item_cart, container, false)
                view.findViewById<TextView>(R.id.tvItemName).text = nama
                view.findViewById<TextView>(R.id.tvItemPrice).text = formatRupiah(harga)
                view.findViewById<TextView>(R.id.tvItemQty).text = "$jumlah"
                view.findViewById<Button>(R.id.btnHapusItemKeranjang).setOnClickListener {
                    dbHelper.hapusItemKeranjang(id)
                    grandTotal = tampilkanDataDanHitungTotal(container)
                    findViewById<TextView>(R.id.tvTotalBayarAkhir).text = formatRupiah(grandTotal)
                }
                container.addView(view)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return total
    }

    private fun formatRupiah(number: Int): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(number.toLong()).replace(",00", "")
    }
}