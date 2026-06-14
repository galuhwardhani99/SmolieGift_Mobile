package com.example.smoliegift.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import nicolla.coco.smoliegiftmobile.User
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 23
        private const val DATABASE_NAME = "SmolieGift.db"

        const val TABLE_USERS = "users"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_GENDER = "gender"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_ADDRESS = "address"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_USERTYPE = "usertype"

        const val TABLE_CART = "cart"
        const val COLUMN_CART_ID = "cart_id"
        const val COLUMN_PRODUCT_NAME = "product_name"
        const val COLUMN_QTY = "qty"
        const val COLUMN_TOTAL_PRICE = "total_price"
        const val COLUMN_CUSTOM_IMAGE = "custom_image"
        const val COLUMN_CART_PRODUCT_IMAGE = "cart_product_image"

        const val TABLE_PRODUCTS = "products"
        const val COLUMN_PROD_ID = "prod_id"
        const val COLUMN_PROD_NAME = "prod_name"
        const val COLUMN_PROD_CAT = "prod_category"
        const val COLUMN_PROD_PRICE = "prod_price"
        const val COLUMN_PROD_STOCK = "prod_stock"
        const val COLUMN_PROD_IMAGE = "prod_image"

        const val TABLE_CATEGORIES = "categories"
        const val COLUMN_CAT_ID = "id"
        const val COLUMN_CAT_NAME = "name"

        const val TABLE_TRANSACTIONS = "transactions"
        const val COLUMN_TRANS_ID = "trans_id"
        const val COLUMN_CUSTOMER_NAME = "customer_name"
        const val COLUMN_CUSTOMER_WA = "customer_wa"
        const val COLUMN_PAYMENT_METHOD = "payment_method"
        const val COLUMN_GRAND_TOTAL = "grand_total"
        const val COLUMN_TRANS_DATE = "trans_date"
        const val COLUMN_EVENT_INFO = "event_info"
        const val COLUMN_ITEMS_JSON = "items_json"
        const val COLUMN_TRANS_CODE = "trans_code"

        const val TABLE_HISTORY = "history"
    }

    // ─────────────────────────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────────────────────────
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT,
                $COLUMN_EMAIL TEXT UNIQUE,
                $COLUMN_USERNAME TEXT,
                $COLUMN_GENDER TEXT,
                $COLUMN_PHONE TEXT,
                $COLUMN_ADDRESS TEXT,
                $COLUMN_PASSWORD TEXT,
                $COLUMN_USERTYPE TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_CART (
                $COLUMN_CART_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PRODUCT_NAME TEXT,
                $COLUMN_QTY INTEGER,
                $COLUMN_TOTAL_PRICE INTEGER,
                $COLUMN_CUSTOM_IMAGE TEXT,
                $COLUMN_CART_PRODUCT_IMAGE TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_PRODUCTS (
                $COLUMN_PROD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PROD_NAME TEXT,
                $COLUMN_PROD_CAT TEXT,
                $COLUMN_PROD_PRICE INTEGER,
                $COLUMN_PROD_STOCK INTEGER,
                $COLUMN_PROD_IMAGE TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_CATEGORIES (
                $COLUMN_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CAT_NAME TEXT UNIQUE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COLUMN_TRANS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CUSTOMER_NAME TEXT,
                $COLUMN_CUSTOMER_WA TEXT,
                $COLUMN_PAYMENT_METHOD TEXT,
                $COLUMN_GRAND_TOTAL INTEGER,
                $COLUMN_CUSTOM_IMAGE TEXT,
                $COLUMN_TRANS_DATE DATETIME DEFAULT (datetime('now','localtime')),
                $COLUMN_EVENT_INFO TEXT,
                $COLUMN_ITEMS_JSON TEXT,
                $COLUMN_TRANS_CODE TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_HISTORY (
                $COLUMN_TRANS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CUSTOMER_NAME TEXT,
                $COLUMN_CUSTOMER_WA TEXT,
                $COLUMN_PAYMENT_METHOD TEXT,
                $COLUMN_GRAND_TOTAL INTEGER,
                $COLUMN_CUSTOM_IMAGE TEXT,
                $COLUMN_TRANS_DATE DATETIME DEFAULT (datetime('now','localtime')),
                $COLUMN_EVENT_INFO TEXT,
                $COLUMN_ITEMS_JSON TEXT,
                $COLUMN_TRANS_CODE TEXT
            )
        """.trimIndent())

        // Seed default users
        db.execSQL("""
            INSERT INTO $TABLE_USERS 
            ($COLUMN_NAME, $COLUMN_EMAIL, $COLUMN_USERNAME, $COLUMN_PHONE, $COLUMN_GENDER, $COLUMN_ADDRESS, $COLUMN_PASSWORD, $COLUMN_USERTYPE) 
            VALUES 
            ('Admin Smolie', 'admin@smolie.com', 'admin', '0895395810940', 'perempuan', 'Jl. Pogot 5 no.77, RT.009/RW.005, Kel. Tanah Kali Kedinding, Kec. Kenjeran, Kota Surabaya, 60129', '1', 'admin')
        """.trimIndent())
        
        db.execSQL("INSERT INTO $TABLE_USERS ($COLUMN_NAME, $COLUMN_EMAIL, $COLUMN_USERNAME, $COLUMN_PASSWORD, $COLUMN_USERTYPE) VALUES ('Kasir Smolie', 'kasir@smolie.com', 'kasir', '1', 'kasir')")
    }

    // ─────────────────────────────────────────────────────────────
    // onUpgrade — migrasi tanpa hapus data
    // ─────────────────────────────────────────────────────────────
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 23) {
            try { db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COLUMN_TRANS_CODE TEXT") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_HISTORY ADD COLUMN $COLUMN_TRANS_CODE TEXT") } catch (_: Exception) {}
        }
    }

    // ─────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────
    fun registerUser(
        name: String, email: String, username: String,
        gender: String, phone: String, address: String, pass: String
    ): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_EMAIL, email)
            put(COLUMN_USERNAME, username)
            put(COLUMN_GENDER, gender)
            put(COLUMN_PHONE, phone)
            put(COLUMN_ADDRESS, address)
            put(COLUMN_PASSWORD, pass)
            put(COLUMN_USERTYPE, "pembeli")
        }
        val result = db.insert(TABLE_USERS, null, values)
        db.close()
        return result != -1L
    }

    fun checkLogin(email: String, pass: String): User? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(email, pass)
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id       = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name     = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                email    = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                usertype = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERTYPE))
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun getUserByEmail(email: String): Cursor? =
        this.readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?", arrayOf(email)
        )

    fun updateUser(email: String, name: String, username: String, phone: String, gender: String, address: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_USERNAME, username)
            put(COLUMN_PHONE, phone)
            put(COLUMN_GENDER, gender)
            put(COLUMN_ADDRESS, address)
        }
        val result = db.update(TABLE_USERS, values, "$COLUMN_EMAIL=?", arrayOf(email))
        db.close()
        return result > 0
    }

    // ─────────────────────────────────────────────────────────────
    // KATEGORI
    // ─────────────────────────────────────────────────────────────
    fun getSemuaKategori(): Cursor =
        this.readableDatabase.rawQuery(
            "SELECT $COLUMN_CAT_ID AS _id, * FROM $TABLE_CATEGORIES", null
        )

    fun tambahKategori(nama: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COLUMN_CAT_NAME, nama) }
        val result = db.insert(TABLE_CATEGORIES, null, values)
        db.close()
        return result != -1L
    }

    fun updateKategori(id: Int, nama: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COLUMN_CAT_NAME, nama) }
        val result = db.update(TABLE_CATEGORIES, values, "$COLUMN_CAT_ID=?", arrayOf(id.toString()))
        db.close()
        return result > 0
    }

    fun hapusKategori(id: Int): Boolean {
        val db = this.writableDatabase
        val result = db.delete(TABLE_CATEGORIES, "$COLUMN_CAT_ID=?", arrayOf(id.toString()))
        db.close()
        return result > 0
    }

    // ─────────────────────────────────────────────────────────────
    // PRODUK
    // ─────────────────────────────────────────────────────────────
    fun getSemuaProduk(): Cursor =
        this.readableDatabase.rawQuery(
            "SELECT $COLUMN_PROD_ID AS _id, * FROM $TABLE_PRODUCTS", null
        )

    fun tambahProduk(
        nama: String, kategori: String, harga: Int,
        stok: Int, imageBase64: String
    ): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PROD_NAME, nama)
            put(COLUMN_PROD_CAT, kategori)
            put(COLUMN_PROD_PRICE, harga)
            put(COLUMN_PROD_STOCK, stok)
            put(COLUMN_PROD_IMAGE, imageBase64)
        }
        val result = db.insert(TABLE_PRODUCTS, null, values)
        db.close()
        return result != -1L
    }

    // ✅ Bug fix: qty dulu baru nama — sesuai urutan ? di query
    fun kurangiStokProduk(nama: String, qty: Int) {
        this.writableDatabase.execSQL(
            "UPDATE $TABLE_PRODUCTS SET $COLUMN_PROD_STOCK = $COLUMN_PROD_STOCK - ? WHERE $COLUMN_PROD_NAME = ?",
            arrayOf(qty.toString(), nama)
        )
    }

    // ─────────────────────────────────────────────────────────────
    // KERANJANG
    // ─────────────────────────────────────────────────────────────
    fun getSemuaKeranjang(): Cursor =
        this.readableDatabase.rawQuery(
            "SELECT $COLUMN_CART_ID AS _id, * FROM $TABLE_CART", null
        )

    // Alias untuk kompatibilitas
    fun ambilKeranjang(): Cursor = getSemuaKeranjang()

    fun kosongkanKeranjang() {
        this.writableDatabase.execSQL("DELETE FROM $TABLE_CART")
    }

    fun tambahKeKeranjang(n: String, q: Int, t: Int, c: String?, p: String?): Boolean {
        val v = ContentValues().apply {
            put(COLUMN_PRODUCT_NAME, n)
            put(COLUMN_QTY, q)
            put(COLUMN_TOTAL_PRICE, t)
            put(COLUMN_CUSTOM_IMAGE, c)
            put(COLUMN_CART_PRODUCT_IMAGE, p)
        }
        return this.writableDatabase.insert(TABLE_CART, null, v) != -1L
    }

    fun hapusItemKeranjang(id: Int): Boolean =
        this.writableDatabase.delete(TABLE_CART, "$COLUMN_CART_ID=?", arrayOf(id.toString())) > 0

    // ─────────────────────────────────────────────────────────────
    // HELPER INTERNAL
    // ─────────────────────────────────────────────────────────────
    private fun generateTransactionCode(): String {
        val datePart   = SimpleDateFormat("yyMMddHHmm", Locale.getDefault()).format(Date())
        val randomPart = (10..99).random()
        return "TX$datePart$randomPart"
    }

    // ─────────────────────────────────────────────────────────────
    // TRANSAKSI
    // ─────────────────────────────────────────────────────────────

    /**
     * Buat pesanan baru (mode pembeli biasa).
     * Kode di-generate otomatis.
     */
    fun buatPesanan(
        nama: String, wa: String, metode: String, total: Int,
        image: String?, event: String?, items: String?
    ): Long {
        val db = this.writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_CUSTOMER_NAME,  nama)
            put(COLUMN_CUSTOMER_WA,    wa)
            put(COLUMN_PAYMENT_METHOD, metode)
            put(COLUMN_GRAND_TOTAL,    total)
            put(COLUMN_CUSTOM_IMAGE,   image)
            put(COLUMN_EVENT_INFO,     event)
            put(COLUMN_ITEMS_JSON,     items)
            put(COLUMN_TRANS_CODE,     generateTransactionCode())
        }
        return db.insert(TABLE_TRANSACTIONS, null, v)
    }

    /**
     * Simpan transaksi kasir dengan status pending.
     * Dipanggil saat kasir klik "Generate QR".
     * kode = kode_transaksi yang di-generate di CartActivity.
     */
    fun simpanTransaksiPending(
        nama: String, wa: String, metode: String, total: Int,
        image: String?, event: String?, items: String?, kode: String
    ): Long {
        val db = this.writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_CUSTOMER_NAME,  nama)
            put(COLUMN_CUSTOMER_WA,    wa)
            put(COLUMN_PAYMENT_METHOD, metode)
            put(COLUMN_GRAND_TOTAL,    total)
            put(COLUMN_CUSTOM_IMAGE,   image)
            put(COLUMN_EVENT_INFO,     event)
            put(COLUMN_ITEMS_JSON,     items)
            put(COLUMN_TRANS_CODE,     kode)
        }
        return db.insert(TABLE_TRANSACTIONS, null, v)
    }

    /**
     * Simpan transaksi langsung — dengan kode (8 parameter).
     * Dipakai KasirDashboardActivity yang sudah pass kode sendiri.
     */
    fun simpanTransaksiLangsung(
        nama: String, wa: String, metode: String, total: Int,
        image: String?, event: String?, items: String?, kode: String
    ): Long {
        val db = this.writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_CUSTOMER_NAME,  nama)
            put(COLUMN_CUSTOMER_WA,    wa)
            put(COLUMN_PAYMENT_METHOD, metode)
            put(COLUMN_GRAND_TOTAL,    total)
            put(COLUMN_CUSTOM_IMAGE,   image)
            put(COLUMN_EVENT_INFO,     event)
            put(COLUMN_ITEMS_JSON,     items)
            put(COLUMN_TRANS_CODE,     kode)
        }
        return db.insert(TABLE_TRANSACTIONS, null, v)
    }

    /**
     * Overload simpanTransaksiLangsung tanpa kode — 7 parameter.
     * Untuk KasirDashboardActivity yang pakai signature lama.
     * Kode di-generate otomatis.
     */
    fun simpanTransaksiLangsung(
        nama: String, wa: String, metode: String, total: Int,
        image: String?, event: String?, items: String?
    ): Long {
        return simpanTransaksiLangsung(
            nama, wa, metode, total, image, event, items,
            generateTransactionCode()
        )
    }

    /**
     * Hapus transaksi berdasarkan ID (Long).
     */
    fun hapusTransaksiById(id: Long): Boolean {
        val db = this.writableDatabase
        val result = db.delete(TABLE_TRANSACTIONS, "$COLUMN_TRANS_ID=?", arrayOf(id.toString()))
        db.close()
        return result > 0
    }

    /**
     * Update status transaksi berdasarkan kode:
     *   "selesai" → salin ke TABLE_HISTORY, hapus dari TABLE_TRANSACTIONS
     *   "batal"   → record tetap di TABLE_TRANSACTIONS (tidak dihapus, tidak dipindah)
     */
    fun updateStatusTransaksi(kode: String, status: String) {
        if (kode.isEmpty()) return

        when (status) {
            "selesai" -> {
                val db = this.writableDatabase
                val c  = db.rawQuery(
                    "SELECT * FROM $TABLE_TRANSACTIONS WHERE $COLUMN_TRANS_CODE = ?",
                    arrayOf(kode)
                )
                if (c.moveToFirst()) {
                    val v = ContentValues().apply {
                        put(COLUMN_CUSTOMER_NAME,  c.getString(c.getColumnIndexOrThrow(COLUMN_CUSTOMER_NAME)))
                        put(COLUMN_CUSTOMER_WA,    c.getString(c.getColumnIndexOrThrow(COLUMN_CUSTOMER_WA)))
                        put(COLUMN_PAYMENT_METHOD, c.getString(c.getColumnIndexOrThrow(COLUMN_PAYMENT_METHOD)))
                        put(COLUMN_GRAND_TOTAL,    c.getInt(c.getColumnIndexOrThrow(COLUMN_GRAND_TOTAL)))
                        put(COLUMN_CUSTOM_IMAGE,   c.getString(c.getColumnIndexOrThrow(COLUMN_CUSTOM_IMAGE)))
                        put(COLUMN_TRANS_DATE,     c.getString(c.getColumnIndexOrThrow(COLUMN_TRANS_DATE)))
                        put(COLUMN_EVENT_INFO,     c.getString(c.getColumnIndexOrThrow(COLUMN_EVENT_INFO)))
                        put(COLUMN_ITEMS_JSON,     c.getString(c.getColumnIndexOrThrow(COLUMN_ITEMS_JSON)))
                        put(COLUMN_TRANS_CODE,     c.getString(c.getColumnIndexOrThrow(COLUMN_TRANS_CODE)))
                    }
                    if (db.insert(TABLE_HISTORY, null, v) != -1L) {
                        db.delete(TABLE_TRANSACTIONS, "$COLUMN_TRANS_CODE=?", arrayOf(kode))
                    }
                }
                c.close()
            }
            "batal" -> {
                // Record tetap di TABLE_TRANSACTIONS, tidak dipindah ke history.
                // Sesuai desain: transaksi batal tetap tersimpan.
            }
        }
    }

    /**
     * Selesaikan pesanan berdasarkan ID (Int).
     * Pindahkan dari TABLE_TRANSACTIONS ke TABLE_HISTORY.
     */
    fun selesaikanPesanan(id: Int): Boolean {
        val db = this.writableDatabase
        val c  = db.rawQuery(
            "SELECT * FROM $TABLE_TRANSACTIONS WHERE $COLUMN_TRANS_ID = ?",
            arrayOf(id.toString())
        )
        var s = false
        if (c.moveToFirst()) {
            val v = ContentValues().apply {
                put(COLUMN_CUSTOMER_NAME,  c.getString(c.getColumnIndexOrThrow(COLUMN_CUSTOMER_NAME)))
                put(COLUMN_CUSTOMER_WA,    c.getString(c.getColumnIndexOrThrow(COLUMN_CUSTOMER_WA)))
                put(COLUMN_PAYMENT_METHOD, c.getString(c.getColumnIndexOrThrow(COLUMN_PAYMENT_METHOD)))
                put(COLUMN_GRAND_TOTAL,    c.getInt(c.getColumnIndexOrThrow(COLUMN_GRAND_TOTAL)))
                put(COLUMN_CUSTOM_IMAGE,   c.getString(c.getColumnIndexOrThrow(COLUMN_CUSTOM_IMAGE)))
                put(COLUMN_TRANS_DATE,     c.getString(c.getColumnIndexOrThrow(COLUMN_TRANS_DATE)))
                put(COLUMN_EVENT_INFO,     c.getString(c.getColumnIndexOrThrow(COLUMN_EVENT_INFO)))
                put(COLUMN_ITEMS_JSON,     c.getString(c.getColumnIndexOrThrow(COLUMN_ITEMS_JSON)))
                put(COLUMN_TRANS_CODE,     c.getString(c.getColumnIndexOrThrow(COLUMN_TRANS_CODE)))
            }
            if (db.insert(TABLE_HISTORY, null, v) != -1L) {
                db.delete(TABLE_TRANSACTIONS, "$COLUMN_TRANS_ID=?", arrayOf(id.toString()))
                s = true
            }
        }
        c.close()
        return s
    }

    // ─────────────────────────────────────────────────────────────
    // LAPORAN & QUERY
    // ─────────────────────────────────────────────────────────────

    fun getJumlahTransaksiAktif(): Int {
        val cursor = this.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TRANSACTIONS", null
        )
        var count = 0
        if (cursor.moveToFirst()) count = cursor.getInt(0)
        cursor.close()
        return count
    }

    fun getSeluruhTransaksi(): Cursor {
        val cols = """
            $COLUMN_TRANS_ID AS _id,
            $COLUMN_CUSTOMER_NAME,
            $COLUMN_CUSTOMER_WA,
            $COLUMN_PAYMENT_METHOD,
            $COLUMN_GRAND_TOTAL,
            $COLUMN_CUSTOM_IMAGE,
            $COLUMN_TRANS_DATE,
            $COLUMN_EVENT_INFO,
            $COLUMN_ITEMS_JSON,
            $COLUMN_TRANS_CODE
        """.trimIndent()
        val q = """
            SELECT $cols, 'AKTIF' as status FROM $TABLE_TRANSACTIONS
            UNION ALL
            SELECT $cols, 'SELESAI' as status FROM $TABLE_HISTORY
            ORDER BY $COLUMN_TRANS_DATE DESC
        """.trimIndent()
        return this.readableDatabase.rawQuery(q, null)
    }

    fun getLaporanPenjualan(): Cursor =
        this.readableDatabase.rawQuery(
            "SELECT $COLUMN_TRANS_ID AS _id, * FROM $TABLE_HISTORY ORDER BY $COLUMN_TRANS_DATE DESC",
            null
        )
}
