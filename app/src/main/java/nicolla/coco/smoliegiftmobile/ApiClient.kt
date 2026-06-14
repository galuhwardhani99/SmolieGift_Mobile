package nicolla.coco.smoliegiftmobile

import android.graphics.Bitmap
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

object ApiClient {
    private val client = OkHttpClient()
    private const val BASE_URL_PRODUK = "http://192.168.1.29/toko-smolie/public/api/produk"
    private const val UPLOAD_URL      = "http://192.168.1.29/toko-smolie/public/api/produk/upload-image"
    private const val KATEGORI_URL    = "http://192.168.1.29/toko-smolie/public/api/kategori"
    private const val TRANSAKSI_URL   = "http://192.168.1.29/toko-smolie/public/api/transaksi"
    private const val BASE_URL_API    = "http://192.168.1.29/toko-smolie/public/api/"
    const val IMAGE_BASE_URL          = "http://192.168.1.29/toko-smolie/public/img/produk/"

    // Generic GET
    fun get(endpoint: String, callback: (String?) -> Unit) {
        val request = Request.Builder().url(BASE_URL_API + endpoint).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) { callback(response.body?.string()) }
        })
    }

    // Generic POST
    fun post(endpoint: String, body: JSONObject, callback: (String?) -> Unit) {
        val requestBody = body.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(BASE_URL_API + endpoint).post(requestBody).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) { callback(response.body?.string()) }
        })
    }

    // Generic PATCH
    fun patch(endpoint: String, body: JSONObject, callback: (String?) -> Unit) {
        val requestBody = body.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(BASE_URL_API + endpoint).patch(requestBody).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) { callback(response.body?.string()) }
        })
    }

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
                    callback(res.getString("status") == "success")
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
                    callback(res.getString("status") == "success")
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
                    val status = res.getString("status")
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
            .addFormDataPart("image", "produk_${System.currentTimeMillis()}.jpg",
                byteArray.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val request = Request.Builder().url(UPLOAD_URL).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    if (json.getString("status") == "success") callback(json.getString("filename"))
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
                    callback(res.getString("status") == "success")
                } catch (e: Exception) { callback(false) }
            }
        })
    }

    fun editKategori(id: Int, namaBaru: String, callback: (Boolean) -> Unit) {
        val json = JSONObject().apply { put("nama_kategori", namaBaru) }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$KATEGORI_URL/$id").put(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "")
                    callback(res.getString("status") == "success")
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
                    callback(res.getString("status") == "success")
                } catch (e: Exception) { callback(false) }
            }
        })
    }

    // ===================== TRANSAKSI =====================

    fun buatTransaksi(
        namaPembeli: String,
        noHp: String,
        metodePembayaran: String,
        jenisPesanan: String,
        kodeTransaksi: String,
        totalHarga: Int,
        callback: (Boolean) -> Unit
    ) {
        val json = JSONObject().apply {
            put("nama_pembeli",      namaPembeli)
            put("no_hp",             noHp)
            put("metode_pembayaran", metodePembayaran)
            put("jenis_pesanan",     jenisPesanan)
            put("kode_transaksi",    kodeTransaksi)
            put("total_harga",       totalHarga)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(TRANSAKSI_URL).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "")
                    callback(res.getString("status") == "success")
                } catch (e: Exception) { callback(false) }
            }
        })
    }

    fun getAllTransaksi(callback: (String?) -> Unit) {
        val request = Request.Builder().url("$TRANSAKSI_URL/all").get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) { callback(response.body?.string()) }
        })
    }

    fun getTransaksiPending(callback: (String?) -> Unit) {
        val request = Request.Builder().url(TRANSAKSI_URL).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) { callback(response.body?.string()) }
        })
    }

    fun konfirmasiTransaksi(id: Int, callback: (Boolean) -> Unit) {
        val body = "{}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$TRANSAKSI_URL/$id/confirm").put(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "")
                    callback(res.getString("status") == "success")
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
}