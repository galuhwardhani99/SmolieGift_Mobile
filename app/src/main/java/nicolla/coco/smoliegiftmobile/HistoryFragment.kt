package nicolla.coco.smoliegiftmobile

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.smoliegift.database.DatabaseHelper
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HistoryFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var llDaftarHistory: LinearLayout
    private var currentUserEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        dbHelper = DatabaseHelper(requireContext())
        llDaftarHistory = view.findViewById(R.id.llDaftarHistoryPembeli)
        
        currentUserEmail = arguments?.getString("USER_EMAIL")
        
        loadOrderHistory()
        return view
    }

    private fun loadOrderHistory() {
        llDaftarHistory.removeAllViews()
        
        if (currentUserEmail == null) {
            val tvKosong = TextView(context)
            tvKosong.text = "Sesi tidak valid. Silakan login kembali."
            tvKosong.setPadding(16, 16, 16, 16)
            llDaftarHistory.addView(tvKosong)
            return
        }

        val db = dbHelper.readableDatabase
        val inflater = LayoutInflater.from(context)

        try {
            // Hanya menampilkan pesanan yang sudah dikonfirmasi (ada di Table History)
            val cursorHistory = db.rawQuery(
                "SELECT * FROM ${DatabaseHelper.TABLE_HISTORY} WHERE ${DatabaseHelper.COLUMN_CUSTOMER_NAME} = ? ORDER BY ${DatabaseHelper.COLUMN_TRANS_ID} DESC",
                arrayOf(currentUserEmail)
            )
            while (cursorHistory.moveToNext()) {
                addOrderToLayout(cursorHistory, "TERKONFIRMASI", "#2E7D32", inflater)
            }
            cursorHistory.close()
            
        } catch (e: Exception) {
            e.printStackTrace()
            val tvError = TextView(context)
            tvError.text = "Terjadi kesalahan saat memuat data."
            tvError.setPadding(16, 16, 16, 16)
            llDaftarHistory.addView(tvError)
        }

        if (llDaftarHistory.childCount == 0) {
            val tvKosong = TextView(context)
            tvKosong.text = "Belum ada riwayat pesanan yang terkonfirmasi.\nSilakan tunggu konfirmasi admin."
            tvKosong.setPadding(32, 32, 32, 32)
            tvKosong.gravity = android.view.Gravity.CENTER
            llDaftarHistory.addView(tvKosong)
        }
    }

    private fun addOrderToLayout(cursor: android.database.Cursor, status: String, statusColor: String, inflater: LayoutInflater) {
        val idIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRANS_ID)
        val totalIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_GRAND_TOTAL)
        val dateIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRANS_DATE)
        val eventIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_EVENT_INFO)
        val itemsIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_ITEMS_JSON)
        val imageIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_CUSTOM_IMAGE)

        val id = if (idIdx != -1) cursor.getInt(idIdx) else 0
        val total = if (totalIdx != -1) cursor.getInt(totalIdx) else 0
        val rawDate = if (dateIdx != -1) cursor.getString(dateIdx) ?: "" else ""
        val eventInfo = if (eventIdx != -1) cursor.getString(eventIdx) ?: "" else ""
        val itemsJson = if (itemsIdx != -1) cursor.getString(itemsIdx) ?: "" else ""
        val customImageBase64 = if (imageIdx != -1) cursor.getString(imageIdx) ?: "" else ""

        val itemView = inflater.inflate(R.layout.item_transaksi_admin, llDaftarHistory, false)
        
        itemView.findViewById<TextView>(R.id.tvAdminTransId)?.text = "#INV-0$id"
        
        itemView.findViewById<TextView>(R.id.tvAdminTransStatusLabel)?.apply {
            text = status
            setTextColor(Color.parseColor(statusColor))
        }
        
        itemView.findViewById<TextView>(R.id.tvAdminTransNama)?.text = "Pesanan Saya"
        itemView.findViewById<TextView>(R.id.tvAdminTransTotal)?.text = "Total: Rp $total"
        
        val tvProduk = itemView.findViewById<TextView>(R.id.tvAdminTransProduk)
        if (!itemsJson.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(itemsJson)
                val sb = StringBuilder("Daftar Produk:\n")
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    sb.append("- ${obj.optString("name", "Produk")} (${obj.optInt("qty", 1)} pcs)\n")
                }
                tvProduk?.text = sb.toString().trim()
            } catch (e: Exception) { 
                tvProduk?.text = "Detail produk tidak tersedia" 
            }
        }

        val tvCatatan = itemView.findViewById<TextView>(R.id.tvAdminTransCatatan)
        if (!eventInfo.isNullOrEmpty() && eventInfo != "Tanpa catatan") {
            tvCatatan?.text = "Catatan: $eventInfo"
            tvCatatan?.visibility = View.VISIBLE
        } else {
            tvCatatan?.visibility = View.GONE
        }

        val tvTanggal = itemView.findViewById<TextView>(R.id.tvAdminTransTanggal)
        if (!rawDate.isNullOrEmpty()) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val outputFormat = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm", Locale("id", "ID"))
                outputFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                
                val date = inputFormat.parse(rawDate)
                tvTanggal?.text = if (date != null) "Waktu: ${outputFormat.format(date)}" else "Waktu: $rawDate"
            } catch (e: Exception) {
                tvTanggal?.text = "Waktu: $rawDate"
            }
        }

        val ivCustomDesign = itemView.findViewById<ImageView>(R.id.ivCustomDesignAdmin)
        val llContainerImage = itemView.findViewById<LinearLayout>(R.id.llContainerImage)

        if (!customImageBase64.isNullOrEmpty()) {
            try {
                val decodedString = Base64.decode(customImageBase64, Base64.DEFAULT)
                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                ivCustomDesign?.setImageBitmap(decodedByte)
                llContainerImage?.visibility = View.VISIBLE
            } catch (e: Exception) {
                llContainerImage?.visibility = View.GONE
            }
        } else {
            llContainerImage?.visibility = View.GONE
        }

        // Sembunyikan tombol aksi admin di halaman riwayat user
        itemView.findViewById<View>(R.id.btnSelesaiPesanan)?.visibility = View.GONE
        itemView.findViewById<View>(R.id.btnCetakStruk)?.visibility = View.GONE
        itemView.findViewById<View>(R.id.tvAdminTransWa)?.visibility = View.GONE
        itemView.findViewById<View>(R.id.tvAdminTransMetode)?.visibility = View.GONE

        llDaftarHistory.addView(itemView)
    }

    companion object {
        fun newInstance(userEmail: String): HistoryFragment {
            val fragment = HistoryFragment()
            val args = Bundle()
            args.putString("USER_EMAIL", userEmail)
            fragment.arguments = args
            return fragment
        }
    }
}
