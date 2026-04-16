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

class HistoryFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var llDaftarHistory: LinearLayout
    private var currentUserName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        dbHelper = DatabaseHelper(requireContext())
        llDaftarHistory = view.findViewById(R.id.llDaftarHistoryPembeli)
        
        currentUserName = arguments?.getString("USER_NAME")
        
        loadOrderHistory()
        return view
    }

    private fun loadOrderHistory() {
        llDaftarHistory.removeAllViews()
        
        if (currentUserName == null) {
            val tvKosong = TextView(context)
            tvKosong.text = "Gagal memuat riwayat. User tidak dikenal."
            tvKosong.setPadding(16, 16, 16, 16)
            llDaftarHistory.addView(tvKosong)
            return
        }

        val db = dbHelper.readableDatabase
        val inflater = LayoutInflater.from(context)

        // 1. Load Pending Orders (DIPROSES)
        val cursorPending = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_CUSTOMER_NAME} = ? ORDER BY ${DatabaseHelper.COLUMN_TRANS_ID} DESC",
            arrayOf(currentUserName)
        )
        
        while (cursorPending.moveToNext()) {
            addOrderToLayout(cursorPending, "DIPROSES", "#F4511E", inflater)
        }
        cursorPending.close()

        // 2. Load Completed Orders (SELESAI)
        val cursorHistory = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_HISTORY} WHERE ${DatabaseHelper.COLUMN_CUSTOMER_NAME} = ? ORDER BY ${DatabaseHelper.COLUMN_TRANS_ID} DESC",
            arrayOf(currentUserName)
        )

        while (cursorHistory.moveToNext()) {
            addOrderToLayout(cursorHistory, "SELESAI", "#2E7D32", inflater)
        }
        cursorHistory.close()

        if (llDaftarHistory.childCount == 0) {
            val tvKosong = TextView(context)
            tvKosong.text = "Belum ada riwayat pesanan."
            tvKosong.setPadding(16, 16, 16, 16)
            tvKosong.gravity = android.view.Gravity.CENTER
            llDaftarHistory.addView(tvKosong)
        }
    }

    private fun addOrderToLayout(cursor: android.database.Cursor, status: String, statusColor: String, inflater: LayoutInflater) {
        val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANS_ID))
        val total = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GRAND_TOTAL))
        val date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANS_DATE))
        val itemsJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEMS_JSON))
        val customImageBase64 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CUSTOM_IMAGE))

        val itemView = inflater.inflate(R.layout.item_transaksi_admin, llDaftarHistory, false)
        
        itemView.findViewById<TextView>(R.id.tvAdminTransId).text = "INV-0$id"
        
        val tvStatus = itemView.findViewById<TextView>(R.id.tvAdminTransStatusLabel)
        tvStatus.text = status
        tvStatus.setTextColor(Color.parseColor(statusColor))
        
        itemView.findViewById<TextView>(R.id.tvAdminTransNama).text = "Pemesan: $currentUserName"
        itemView.findViewById<TextView>(R.id.tvAdminTransTotal).text = "Rp $total"
        itemView.findViewById<TextView>(R.id.tvAdminTransTanggal).text = "Waktu: $date"
        
        val tvProduk = itemView.findViewById<TextView>(R.id.tvAdminTransProduk)
        if (!itemsJson.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(itemsJson)
                val sb = StringBuilder("Produk:\n")
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    sb.append("- ${obj.getString("name")} (${obj.getInt("qty")} pcs)\n")
                }
                tvProduk.text = sb.toString().trim()
            } catch (e: Exception) { tvProduk.text = "Produk: -" }
        }

        val ivCustomDesign = itemView.findViewById<ImageView>(R.id.ivCustomDesignAdmin)
        val llContainerImage = itemView.findViewById<LinearLayout>(R.id.llContainerImage)

        if (!customImageBase64.isNullOrEmpty()) {
            try {
                val decodedString = Base64.decode(customImageBase64, Base64.DEFAULT)
                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                ivCustomDesign.setImageBitmap(decodedByte)
                llContainerImage.visibility = View.VISIBLE
            } catch (e: Exception) {
                llContainerImage.visibility = View.GONE
            }
        } else {
            llContainerImage.visibility = View.GONE
        }

        itemView.findViewById<Button>(R.id.btnSelesaiPesanan).visibility = View.GONE
        itemView.findViewById<TextView>(R.id.tvAdminTransWa).visibility = View.GONE
        itemView.findViewById<TextView>(R.id.tvAdminTransMetode).visibility = View.GONE

        llDaftarHistory.addView(itemView)
    }

    companion object {
        fun newInstance(userName: String): HistoryFragment {
            val fragment = HistoryFragment()
            val args = Bundle()
            args.putString("USER_NAME", userName)
            fragment.arguments = args
            return fragment
        }
    }
}