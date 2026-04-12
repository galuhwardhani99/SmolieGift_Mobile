package nicolla.coco.smoliegiftmobile

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smoliegift.database.DatabaseHelper

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DatabaseHelper(this)

        // Binding ID dari XML
        val etNama = findViewById<EditText>(R.id.etRegNama)
        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val etUsername = findViewById<EditText>(R.id.etRegUsername)
        val spGender = findViewById<Spinner>(R.id.spRegGender)
        val etPhone = findViewById<EditText>(R.id.etRegPhone)
        val etAddress = findViewById<EditText>(R.id.etRegAddress)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val etConfirmPass = findViewById<EditText>(R.id.etRegConfirmPass)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvGoToLogin)

        // Setup Spinner Jenis Kelamin
        val genderOptions = arrayOf("Pilih...", "Laki-laki", "Perempuan")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderOptions)
        spGender.adapter = adapter

        btnRegister.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val gender = spGender.selectedItem.toString()
            val phone = etPhone.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val pass = etPassword.text.toString()
            val confirmPass = etConfirmPass.text.toString()

            // 1. Validasi Input Kosong
            if (nama.isEmpty() || email.isEmpty() || pass.isEmpty() || gender == "Pilih...") {
                Toast.makeText(this, "Harap lengkapi semua data wajib!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validasi Password Sama
            if (pass != confirmPass) {
                Toast.makeText(this, "Konfirmasi password tidak cocok!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Simpan ke Database
            val isSuccess = dbHelper.registerUser(nama, email, username, gender, phone, address, pass)
            if (isSuccess) {
                Toast.makeText(this, "Registrasi Berhasil! Silakan Login.", Toast.LENGTH_LONG).show()
                finish() // Menutup halaman register dan otomatis kembali ke halaman Login
            } else {
                Toast.makeText(this, "Gagal. Email mungkin sudah terdaftar.", Toast.LENGTH_SHORT).show()
            }
        }

        // Tombol kembali ke Login
        tvLogin.setOnClickListener {
            finish()
        }
    }
}