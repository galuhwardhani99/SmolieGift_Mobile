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
    private const val BASE_URL   = "http://192.168.43.121/toko-smolie/public/api/produk"
    private const val UPLOAD_URL = "http://192.168.43.121/toko-smolie/public/api/produk/upload-image"
    const val IMAGE_BASE_URL     = "http://192.168.43.121/toko-smolie/public/img/produk/"

    // GET semua produk
    fun getAllProducts(callback: (String?) -> Unit) {
        val request = Request.Builder().url(BASE_URL).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) {
                callback(response.body?.string())
            }
        })
    }

    // POST tambah produk
    fun addProduct(name: String, category: String, price: Int, stock: Int, image: String, callback: (Boolean) -> Unit) {
        val json = JSONObject().apply {
            put("nama_produk", name)
            put("kategori_id", category)
            put("harga",       price)
            put("stock",       stock)
            put("gambar",      image)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(BASE_URL).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                val responseStr = response.body?.string() ?: ""
                android.util.Log.d("ApiClient", "addProduct response: $responseStr") // ← tambah ini
                val res = JSONObject(responseStr)
                callback(res.getString("status") == "success")
            }
        })
    }

    // PUT update produk
    fun updateProduct(id: Int, name: String, category: String, price: Int, stock: Int, image: String, callback: (Boolean) -> Unit) {
        val json = JSONObject().apply {
            put("nama_produk", name)
            put("kategori_id", category)
            put("harga",       price)
            put("stock",       stock)
            put("gambar",      image)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        // ← URL pakai id di path, bukan di body
        val request = Request.Builder().url("$BASE_URL/$id").put(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                val res = JSONObject(response.body?.string() ?: "")
                callback(res.getString("status") == "success")
            }
        })
    }

    // DELETE hapus produk
    fun deleteProduct(id: Int, callback: (Boolean) -> Unit) {
        // ← URL pakai id di path, bukan di body
        val request = Request.Builder().url("$BASE_URL/$id").delete().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                val res = JSONObject(response.body?.string() ?: "")
                callback(res.getString("status") == "success")
            }
        })
    }

    // UPLOAD gambar ke server → kembalikan nama file
    fun uploadImage(bitmap: Bitmap, callback: (String?) -> Unit) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "produk_${System.currentTimeMillis()}.jpg",
                byteArray.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder().url(UPLOAD_URL).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    if (json.getString("status") == "success") {
                        callback(json.getString("filename"))
                    } else {
                        callback(null)
                    }
                } catch (e: Exception) {
                    callback(null)
                }
            }
        })
    }
    // GET semua kategori dari Laravel
    fun getKategori(callback: (List<Pair<Int, String>>) -> Unit) {
        val url = "http://192.168.1.8/toko-smolie/public/api/kategori"
        val request = Request.Builder().url(url).get().build()
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
                } catch (e: Exception) {
                    callback(emptyList())
                }
            }
        })
    }
}