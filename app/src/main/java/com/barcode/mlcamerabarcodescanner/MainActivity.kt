package com.barcode.mlcamerabarcodescanner;

import android.app.Activity;
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

public class MainActivity : AppCompatActivity() {
    lateinit var scanButton:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scanButton = findViewById(R.id.scan_button)
        scanButton.setOnClickListener {
            val scannerIntent = Intent(this,ScannerActivity::class.java)
            startActivity(scannerIntent)
            finish()
        }
    }
}
