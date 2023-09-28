package com.barcode.mlcamerabarcodescanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val CAMERA_PERMISSION_REQUEST_CODE = 1
class ScannerActivity : AppCompatActivity() {
    private lateinit var previewView:PreviewView
    private lateinit var barCodeResult:TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeBoxView: BarcodeBoxView
    private var lastScannedBarcodeValue: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        previewView = findViewById(R.id.previewView)
       barCodeResult = findViewById(R.id.scannerBarCode)
        if (hasCameraPermission()) bindCameraUseCases()
        else requestPermission()

    }

    private suspend fun pauseScanning(delayMillis: Long) {
        delay(delayMillis)
    }
    // checking to see whether user has already granted permission
    private fun hasCameraPermission() =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission(){
        // opening up dialog to ask for camera permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // user granted permissions - we can set up our scanner
            bindCameraUseCases()
        } else {
            // user did not grant permissions - we can't use the camera
            Toast.makeText(this,
                "Camera permission required",
                Toast.LENGTH_LONG
            ).show()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // setting up the preview use case
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            val options = BarcodeScannerOptions.Builder()
                .enableAllPotentialBarcodes()
                .build()
            val scanner = BarcodeScanning.getClient(options)

            // setting up the analysis use case
            val analysisUseCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

           val cameraExecutor =  Executors.newSingleThreadExecutor()
            analysisUseCase.setAnalyzer(
                Executors.newSingleThreadExecutor()
            ) { imageProxy ->
                processImageProxy(scanner, imageProxy)
            }

            // configure to use the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    previewUseCase,
                    analysisUseCase)
            } catch (illegalStateException: IllegalStateException) {
                // If the use case has already been bound to another lifecycle or method is not called on main thread.
                Log.e("MainActivity", illegalStateException.message.orEmpty())
            } catch (illegalArgumentException: IllegalArgumentException) {
                // If the provided camera selector is unable to resolve a camera to be used for the given use cases.
                Log.e("MainActivity", illegalArgumentException.message.orEmpty())
            }
        }, ContextCompat.getMainExecutor(this))

        }


    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {

        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodeList ->
                 //   processBarcode(barcodeList,barcodeScanner)
                   val barcode = barcodeList.getOrNull(0)

                    // `rawValue` is the decoded value of the barcode
                    if (lastScannedBarcodeValue != barcode?.rawValue) {
                        barcode?.rawValue?.let { value ->
                            barCodeResult.text = value
                            lastScannedBarcodeValue = value
                       //     navigateToMain(barcodeScanner)
                        }
                }


                }
                .addOnFailureListener {
                    // This failure will happen if the barcode scanning model
                    // fails to download from Google Play Services

                    Log.e("MainActivity", it.message.orEmpty())
                }.addOnCompleteListener {
                    // When the image is from CameraX analysis use case, must
                    // call image.close() on received images when finished
                    // using them. Otherwise, new images may not be received
                    // or the camera may stall.

                    imageProxy.image?.close()
                    imageProxy.close()
                }
        }
    }


    private fun navigateToMain( barcodeScanner: BarcodeScanner) {
        //barcodeScanner.close()
        Handler(Looper.getMainLooper()).postDelayed({
            barcodeScanner.close()
            val mainIntent = Intent(this,MainActivity::class.java)
            startActivity(mainIntent)
            finish()
        }, 5000)
    }


    private fun processBarcode(barcodeList: List<Barcode>, scanner: BarcodeScanner) {
        if (barcodeList.isNotEmpty()) {
            with (barcodeList.first()) {
                println("Barcode value is $rawValue")
                val rawValue = this.rawValue.toString()
                val format = this.format.toString()
                barCodeResult.text = rawValue
              //  scanner.close()
            }

            Handler(Looper.getMainLooper()).postDelayed({
                val mainIntent = Intent(applicationContext,MainActivity::class.java)
                startActivity(mainIntent)
                finish()
            }, 5000)
        }
    }
}