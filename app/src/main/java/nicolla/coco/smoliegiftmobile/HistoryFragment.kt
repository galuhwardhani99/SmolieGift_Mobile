package nicolla.coco.smoliegiftmobile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HistoryFragment : Fragment() {

    private lateinit var llDaftarHistory: LinearLayout
    private var currentUserEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        llDaftarHistory = view.findViewById(R.id.llDaftarHistoryPembeli)
        currentUserEmail = arguments?.getString("USER_EMAIL")
        loadOrderHistory()
        return view
    }

    private fun loadOrderHistory() {
        llDaftarHistory.removeAllViews()

        if (currentUserEmail == null) {
            val tv = TextView(context)
            tv.text = "Sesi tidak valid. Silakan login kembali."
            tv.setPadding(16, 16, 16, 16)
            llDaftarHistory.addView(tv)
            return
        }

        val tvLoading = TextView(context)
        tvLoading.text = "Memuat riwayat pesanan..."
        tvLoading.setPadding(32, 32, 32, 32)
        tvLoading.gravity = android.view.Gravity.CENTER
        llDaftarHistory.addView(tvLoading)

        ApiClient.getRiwayatTransaksi(currentUserEmail!!) { response ->
            activity?.runOnUiThread {
                llDaftarHistory.removeAllViews()

                if (response == null) {
                    val tv = TextView(context)
                    tv.text = "Gagal memuat riwayat. Periksa koneksi."
                    tv.setPadding(32, 32, 32, 32)
                    tv.gravity = android.view.Gravity.CENTER
                    llDaftarHistory.addView(tv)
                    return@runOnUiThread
                }

                try {
                    val json = JSONObject(response)
                    val data = json.getJSONArray("data")
                    val inflater = LayoutInflater.from(context)

                    if (data.length() == 0) {
                        val tv = TextView(context)
                        tv.text = "Belum ada riwayat pesanan yang terkonfirmasi.\nSilakan tunggu konfirmasi admin."
                        tv.setPadding(32, 32, 32, 32)
                        tv.gravity = android.view.Gravity.CENTER
                        llDaftarHistory.addView(tv)
                        return@runOnUiThread
                    }

                    for (i in 0 until data.length()) {
                        val item    = data.getJSONObject(i)
                        val id      = item.getInt("id")
                        val total   = item.optInt("total_harga", 0)
                        val kode    = item.optString("kode_transaksi", "-")
                        val rawDate = item.optString("created_at", "-")
                        val metode  = item.optString("metode_pembayaran", "-")
                        val itemsJson = item.optString("items_json", "")

                        val itemView = inflater.inflate(R.layout.item_transaksi_admin, llDaftarHistory, false)

                        itemView.findViewById<TextView>(R.id.tvAdminTransId)?.text = "#$kode"
                        itemView.findViewById<TextView>(R.id.tvAdminTransNama)?.text = "Pesanan Saya"
                        itemView.findViewById<TextView>(R.id.tvAdminTransTotal)?.text = "Total: Rp $total"
                        itemView.findViewById<TextView>(R.id.tvAdminTransMetode)?.text = "Bayar: $metode"

                        val tvTanggal = itemView.findViewById<TextView>(R.id.tvAdminTransTanggal)
                        try {
                            val cleanDate = rawDate
                                .replace(Regex("\\.\\d{1,6}Z?$"), "")
                                .replace("T", " ")

                            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            inputFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")

                            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID"))
                            outputFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")

                            val date = inputFormat.parse(cleanDate)
                            tvTanggal?.text = "Waktu: " + if (date != null) outputFormat.format(date) else rawDate
                        } catch (e: Exception) {
                            tvTanggal?.text = "Waktu: $rawDate"
                        }

                        // Daftar produk
                        val tvProduk = itemView.findViewById<TextView>(R.id.tvAdminTransProduk)
                        if (itemsJson.isNotEmpty() && itemsJson != "null") {
                            try {
                                val jsonArray = JSONArray(itemsJson)
                                val sb = StringBuilder()
                                for (j in 0 until jsonArray.length()) {
                                    val obj = jsonArray.getJSONObject(j)
                                    val namaProduk  = obj.optString("nama", obj.optString("name", "Produk"))
                                    val qtyProduk   = obj.optInt("jumlah", obj.optInt("qty", 1))
                                    val hargaProduk = obj.optInt("harga", 0)
                                    if (hargaProduk > 0) {
                                        sb.append("• $namaProduk × $qtyProduk (Rp $hargaProduk)\n")
                                    } else {
                                        sb.append("• $namaProduk × $qtyProduk\n")
                                    }
                                }
                                tvProduk?.text = sb.toString().trim()
                            } catch (e: Exception) {
                                tvProduk?.text = "Detail produk tidak tersedia"
                            }
                        } else {
                            tvProduk?.text = "Tidak ada detail produk"
                        }

                        itemView.findViewById<TextView>(R.id.tvAdminTransStatusLabel)?.apply {
                            text = "TERKONFIRMASI"
                            setTextColor(Color.parseColor("#2E7D32"))
                        }

                        itemView.findViewById<View>(R.id.btnSelesaiPesanan)?.visibility = View.GONE
                        itemView.findViewById<View>(R.id.btnCetakStruk)?.visibility = View.GONE
                        itemView.findViewById<View>(R.id.tvAdminTransWa)?.visibility = View.GONE

                        // Tombol ulasan
                        val btnUlasan = itemView.findViewById<Button>(R.id.btnUlasan)
                        btnUlasan?.visibility = View.VISIBLE
                        btnUlasan?.setOnClickListener {
                            val intent = Intent(context, ReviewActivity::class.java)
                            intent.putExtra("TRANSAKSI_ID", id)
                            intent.putExtra("KODE_TRANSAKSI", kode)
                            startActivity(intent)
                        }

                        llDaftarHistory.addView(itemView)
                    }
                } catch (e: Exception) {
                    val tv = TextView(context)
                    tv.text = "Error: ${e.message}"
                    tv.setPadding(32, 32, 32, 32)
                    llDaftarHistory.addView(tv)
                }
            }
        }
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