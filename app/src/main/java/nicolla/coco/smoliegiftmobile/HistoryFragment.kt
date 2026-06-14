package nicolla.coco.smoliegiftmobile

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import org.json.JSONObject

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
                        val tanggal = item.optString("created_at", "-")
                        val metode  = item.optString("metode_pembayaran", "-")

                        val itemView = inflater.inflate(R.layout.item_transaksi_admin, llDaftarHistory, false)

                        itemView.findViewById<TextView>(R.id.tvAdminTransId)?.text = "#$kode"
                        itemView.findViewById<TextView>(R.id.tvAdminTransNama)?.text = "Pesanan Saya"
                        itemView.findViewById<TextView>(R.id.tvAdminTransTotal)?.text = "Total: Rp $total"
                        itemView.findViewById<TextView>(R.id.tvAdminTransTanggal)?.text = "Waktu: $tanggal"
                        itemView.findViewById<TextView>(R.id.tvAdminTransMetode)?.text = "Bayar: $metode"

                        itemView.findViewById<TextView>(R.id.tvAdminTransStatusLabel)?.apply {
                            text = "TERKONFIRMASI"
                            setTextColor(Color.parseColor("#2E7D32"))
                        }

                        // Sembunyikan tombol aksi admin
                        itemView.findViewById<View>(R.id.btnSelesaiPesanan)?.visibility = View.GONE
                        itemView.findViewById<View>(R.id.btnCetakStruk)?.visibility = View.GONE
                        itemView.findViewById<View>(R.id.tvAdminTransWa)?.visibility = View.GONE

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