package nicolla.coco.smoliegiftmobile

import android.graphics.Bitmap
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL_API    = "http://192.168.1.28/toko-smolie/public/api/"
    private const val BASE_URL_PRODUK = "${BASE_URL_API}produk"
    private const val UPLOAD_URL      = "${BASE_URL_PRODUK}/upload-image"
    private const val KATEGORI_URL    = "${BASE_URL_API}kategori"
    private const val TRANSAKSI_URL   = "${BASE_URL_API}transaksi"
    const val IMAGE_BASE_URL          = "http://192.168.1.28/toko-smolie/public/img/produk/"

    // ===================== PRODUK =====================

    fun getAllProducts(callback: (String?) -> Unit) {
        val request = Request.Builder().url(BASE_URL_PRODUK).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) { callback(response.body?.string()) }
        })
    }

    fun addProduct(name: String, category: String, price: Int, stock: Int, image: String, callback: (Boolean) -> Unit) {
        val json = JSONObject().apply {
            put("nama_produk", name)
            put("kategori_id", category)
            put("harga",       price)
            put("stock",       stock)
            put("gambar",      image)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(BASE_URL_PRODUK).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "")
                    callback(res.optString("status") == "success")
                } catch (e: Exception) { callback(false) }
            }
        })
    }

    fun updateProduct(id: Int, name: String, category: String, price: Int, stock: Int, image: String, callback: (Boolean) -> Unit) {
        val json = JSONObject().apply {
            put("nama_produk", name)
            put("kategori_id", category)
            put("harga",       price)
            put("stock",       stock)
            put("gambar",      image)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$BASE_URL_PRODUK/$id").put(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "")
                    callback(res.optString("status") == "success")
                } catch (e: Exception) { callback(false) }
            }
        })
    }

    fun deleteProduct(id: Int, callback: (Boolean) -> Unit) {
        val request = Request.Builder().url("$BASE_URL_PRODUK/$id").delete().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "")
                    val status = res.optString("status")
                    callback(status == "success" || status == "archived")
                } catch (e: Exception) { callback(false) }
            }
        })
    }

    fun uploadImage(bitmap: Bitmap, callback: (String?) -> Unit) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "upload.jpg", byteArray.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val request = Request.Builder().url(UPLOAD_URL).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "")
                    if (res.optString("status") == "success") callback(res.optString("filename"))
                    else callback(null)
                } catch (e: Exception) { callback(null) }
            }
        })
    }

    // ===================== KATEGORI =====================

    fun getKategori(callback: (List<Pair<Int, String>>) -> Unit) {
        val request = Request.Builder().url(KATEGORI_URL).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(emptyList()) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    val data = json.getJSONArray("data")
                    val list = mutableListOf<Pair<Int, String>>()
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        list.add(Pair(item.getInt("id"), item.getString("nama_kategori")))
                    }
                    callback(list)
                } catch (e: Exception) { callback(emptyList()) }
            }
        })
    }

    fun addKategori(nama: String, callback: (Boolean) -> Unit) {
        val json = JSONObject().apply { put("nama_kategori", nama) }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(KATEGORI_URL).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "")
                    callback(res.optString("status") == "success")
                } catch (e: Exception) { callback(false) }
            }
        })
    }

    fun editKategori(id: Int, nama: String, callback: (Boolean) -> Unit) {
        val json = JSONObject().apply { put("nama_kategori", nama) }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$KATEGORI_URL/$id").put(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "")
                    callback(res.optString("status") == "success")
                } catch (e: Exception) { callback(false) }
            }
        })
    }

    fun deleteKategori(id: Int, callback: (Boolean) -> Unit) {
        val request = Request.Builder().url("$KATEGORI_URL/$id").delete().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "")
                    callback(res.optString("status") == "success")
                } catch (e: Exception) { callback(false) }
            }
        })
    }

    // ===================== TRANSAKSI =====================

    fun getAllTransaksi(callback: (String?) -> Unit) {
        val request = Request.Builder()
            .url("$TRANSAKSI_URL/all")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.body?.string())
            }
        })
    }

    fun konfirmasiTransaksi(id: Int, callback: (Boolean) -> Unit) {
        val json = JSONObject().apply {
            put("status", "selesai")
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$TRANSAKSI_URL/$id/confirm")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .put(body)  // ← ganti POST jadi PUT
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                android.util.Log.d("KONFIRMASI", "Code: ${response.code}, Body: $responseBody")
                try {
                    val res = JSONObject(responseBody)
                    callback(
                        res.optString("status") == "success" ||
                                res.optString("message").contains("berhasil", ignoreCase = true) ||
                                response.isSuccessful
                    )
                } catch (e: Exception) {
                    callback(response.isSuccessful)
                }
            }
        })
    }

    fun buatTransaksi(
        namaPembeli: String,
        noHp: String,
        metodePembayaran: String,
        jenisPesanan: String,
        kodeTransaksi: String,
        totalHarga: Int,
        itemsJson: String = "",   // ← tambah parameter ini
        callback: (Boolean) -> Unit
    ) {
        val json = JSONObject().apply {
            put("nama_pembeli",      namaPembeli)
            put("no_hp",             noHp)
            put("metode_pembayaran", metodePembayaran)
            put("jenis_pesanan",     jenisPesanan)
            put("kode_transaksi",    kodeTransaksi)
            put("total_harga",       totalHarga)
            put("items_json",        itemsJson)   // ← kirim items
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(TRANSAKSI_URL).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "")
                    callback(res.optString("status") == "success")
                } catch (e: Exception) { callback(false) }
            }
        })
    }

    fun getRiwayatTransaksi(email: String, callback: (String?) -> Unit) {
        val request = Request.Builder().url("$TRANSAKSI_URL/history/$email").get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) { callback(response.body?.string()) }
        })
    }

    // ===================== ULASAN (REVIEW) =====================

    fun submitReview(transaksiId: Int, kodeTransaksi: String, ulasan: String, rating: Int, foto: String?, video: String?, callback: (Boolean, String?) -> Unit) {
        val json = JSONObject().apply {
            put("transaksi_id", transaksiId)
            put("kode_transaksi", kodeTransaksi)
            put("ulasan", ulasan)
            put("rating", rating)
            put("foto", foto ?: "")
            put("video", video ?: "")
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${BASE_URL_API}reviews")
            .header("Accept", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "Koneksi gagal: " + e.message)
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                try {
                    if (!response.isSuccessful) {
                        val errorJson = JSONObject(responseBody)
                        callback(false, errorJson.optString("message", "Error " + response.code))
                        return
                    }
                    val res = JSONObject(responseBody)
                    if (res.optString("status") == "success" || res.optString("message").contains("berhasil", ignoreCase = true)) {
                        callback(true, null)
                    } else {
                        callback(false, res.optString("message", "Gagal menyimpan ulasan"))
                    }
                } catch (e: Exception) {
                    callback(false, "Respon server: $responseBody")
                }
            }
        })
    }

    fun batalkanTransaksi(id: Int, callback: (Boolean) -> Unit) {
        val json = JSONObject().apply {
            put("status", "dibatalkan")
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$TRANSAKSI_URL/$id/cancel")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .put(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                android.util.Log.d("BATALKAN", "Code: ${response.code}, Body: $responseBody")
                try {
                    val res = JSONObject(responseBody)
                    callback(res.optString("status") == "success" || response.isSuccessful)
                } catch (e: Exception) {
                    callback(response.isSuccessful)
                }
            }
        })
    }

    fun getAllTransaksiLengkap(callback: (String?) -> Unit) {
        val request = Request.Builder().url("$TRANSAKSI_URL/all").get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) { callback(response.body?.string()) }
        })
    }

    fun getAllReviews(callback: (String?) -> Unit) {
        val request = Request.Builder()
            .url("${BASE_URL_API}reviews")
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) {
                callback(response.body?.string())
            }
        })
    }

}
