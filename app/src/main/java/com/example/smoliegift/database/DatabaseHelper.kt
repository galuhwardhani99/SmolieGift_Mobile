package com.example.smoliegift.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.smoliegift.models.User

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 7
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

        const val TABLE_HISTORY = "history"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_USERS ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_NAME TEXT, $COLUMN_EMAIL TEXT UNIQUE, $COLUMN_USERNAME TEXT, $COLUMN_GENDER TEXT, $COLUMN_PHONE TEXT, $COLUMN_ADDRESS TEXT, $COLUMN_PASSWORD TEXT, $COLUMN_USERTYPE TEXT)")
        db.execSQL("CREATE TABLE $TABLE_CART ($COLUMN_CART_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_PRODUCT_NAME TEXT, $COLUMN_QTY INTEGER, $COLUMN_TOTAL_PRICE INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_PRODUCTS ($COLUMN_PROD_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_PROD_NAME TEXT, $COLUMN_PROD_CAT TEXT, $COLUMN_PROD_PRICE INTEGER, $COLUMN_PROD_STOCK INTEGER, $COLUMN_PROD_IMAGE TEXT)")
        db.execSQL("CREATE TABLE $TABLE_CATEGORIES ($COLUMN_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_CAT_NAME TEXT UNIQUE)")
        db.execSQL("CREATE TABLE $TABLE_TRANSACTIONS ($COLUMN_TRANS_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_CUSTOMER_NAME TEXT, $COLUMN_CUSTOMER_WA TEXT, $COLUMN_PAYMENT_METHOD TEXT, $COLUMN_GRAND_TOTAL INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_HISTORY ($COLUMN_TRANS_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_CUSTOMER_NAME TEXT, $COLUMN_CUSTOMER_WA TEXT, $COLUMN_PAYMENT_METHOD TEXT, $COLUMN_GRAND_TOTAL INTEGER)")

        val adminValues = ContentValues().apply {
            put(COLUMN_NAME, "Admin Smolie")
            put(COLUMN_EMAIL, "admin@smolie.com")
            put(COLUMN_USERNAME, "adminsmolie")
            put(COLUMN_GENDER, "Laki-laki")
            put(COLUMN_PHONE, "081234567890")
            put(COLUMN_ADDRESS, "Kantor Pusat")
            put(COLUMN_PASSWORD, "1")
            put(COLUMN_USERTYPE, "admin")
        }
        db.insert(TABLE_USERS, null, adminValues)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CART")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    // --- USER & LOGIN ---
    fun registerUser(name: String, email: String, username: String, gender: String, phone: String, address: String, pass: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COLUMN_NAME, name); put(COLUMN_EMAIL, email); put(COLUMN_USERNAME, username); put(COLUMN_GENDER, gender); put(COLUMN_PHONE, phone); put(COLUMN_ADDRESS, address); put(COLUMN_PASSWORD, pass); put(COLUMN_USERTYPE, "pembeli") }
        val result = db.insert(TABLE_USERS, null, values)
        db.close()
        return result != -1L
    }

    fun checkLogin(email: String, pass: String): User? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?", arrayOf(email, pass))
        var user: User? = null
        if (cursor.moveToFirst()) { user = User(id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)), name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)), email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)), usertype = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERTYPE))) }
        cursor.close(); db.close()
        return user
    }

    // --- KERANJANG ---
    fun tambahKeKeranjang(namaProduk: String, qty: Int, totalHarga: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COLUMN_PRODUCT_NAME, namaProduk); put(COLUMN_QTY, qty); put(COLUMN_TOTAL_PRICE, totalHarga) }
        val result = db.insert(TABLE_CART, null, values)
        db.close()
        return result != -1L
    }

    fun getSemuaKeranjang(): Cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_CART", null)
    fun kosongkanKeranjang() { val db = this.writableDatabase; db.execSQL("DELETE FROM $TABLE_CART"); db.close() }

    // --- PRODUK ---
    fun tambahProduk(nama: String, kategori: String, harga: Int, stok: Int, imageBase64: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PROD_NAME, nama); put(COLUMN_PROD_CAT, kategori); put(COLUMN_PROD_PRICE, harga); put(COLUMN_PROD_STOCK, stok); put(COLUMN_PROD_IMAGE, imageBase64)
        }
        val result = db.insert(TABLE_PRODUCTS, null, values)
        db.close()
        return result != -1L
    }

    fun updateProduk(id: Int, nama: String, kategori: String, harga: Int, stok: Int, imageBase64: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PROD_NAME, nama); put(COLUMN_PROD_CAT, kategori); put(COLUMN_PROD_PRICE, harga); put(COLUMN_PROD_STOCK, stok)
            if (imageBase64.isNotEmpty()) put(COLUMN_PROD_IMAGE, imageBase64)
        }
        val result = db.update(TABLE_PRODUCTS, values, "$COLUMN_PROD_ID=?", arrayOf(id.toString()))
        db.close()
        return result > 0
    }

    fun getSemuaProduk(): Cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_PRODUCTS", null)
    fun hapusProduk(idProduk: Int): Boolean {
        val db = this.writableDatabase; val result = db.delete(TABLE_PRODUCTS, "$COLUMN_PROD_ID=?", arrayOf(idProduk.toString())); db.close()
        return result > 0
    }

    // --- KATEGORI ---
    fun getSemuaKategori(): Cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_CATEGORIES", null)
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

    // --- TRANSAKSI & LAPORAN ---
    fun buatPesanan(namaPemesan: String, noWa: String, metodeBayar: String, total: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COLUMN_CUSTOMER_NAME, namaPemesan); put(COLUMN_CUSTOMER_WA, noWa); put(COLUMN_PAYMENT_METHOD, metodeBayar); put(COLUMN_GRAND_TOTAL, total) }
        val result = db.insert(TABLE_TRANSACTIONS, null, values); db.close()
        return result != -1L
    }

    fun selesaikanPesanan(idPesanan: Int): Boolean {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TRANSACTIONS WHERE $COLUMN_TRANS_ID = ?", arrayOf(idPesanan.toString()))
        if (cursor.moveToFirst()) {
            val values = ContentValues().apply {
                put(COLUMN_CUSTOMER_NAME, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUSTOMER_NAME)))
                put(COLUMN_CUSTOMER_WA, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUSTOMER_WA)))
                put(COLUMN_PAYMENT_METHOD, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAYMENT_METHOD)))
                put(COLUMN_GRAND_TOTAL, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GRAND_TOTAL)))
            }
            db.insert(TABLE_HISTORY, null, values)
            db.delete(TABLE_TRANSACTIONS, "$COLUMN_TRANS_ID=?", arrayOf(idPesanan.toString()))
            cursor.close(); db.close()
            return true
        }
        cursor.close(); db.close()
        return false
    }

    fun getLaporanPenjualan(): Cursor = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_HISTORY ORDER BY $COLUMN_TRANS_ID DESC", null)
}