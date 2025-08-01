package com.example.seventh

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.IOException
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import com.example.seventh.data.AppDatabase
import com.example.seventh.data.ScanHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : AppCompatActivity() {
    private lateinit var resultInfo: TextView
    private lateinit var firstPageView: ImageView
    private lateinit var resultCard: MaterialCardView
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var enableGalleryImport = true

    private lateinit var saveToGalleryButton: Button
    private lateinit var saveToHistoryButton: Button
    private lateinit var historyButton: Button
    private var currentScannedImageUri: Uri? = null
    private lateinit var database: AppDatabase

    // 트리플 탭 감지를 위한 변수
    private lateinit var gestureDetector: GestureDetectorCompat
    private var tripleTapCounter = 0
    private val TRIPLE_TAP_TIMEOUT = 500L
    private var lastTapTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Material Toolbar 설정
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        resultInfo = findViewById(R.id.result_info)
        firstPageView = findViewById(R.id.first_page_view)
        resultCard = findViewById(R.id.result_card)
        saveToGalleryButton = findViewById(R.id.save_to_gallery_button)
        saveToHistoryButton = findViewById(R.id.save_to_history_button)
        historyButton = findViewById(R.id.history_button)
        database = AppDatabase.getDatabase(this)

        // 초기에는 resultCard를 숨김
        resultCard.visibility = View.GONE

        saveToGalleryButton.setOnClickListener {
            currentScannedImageUri?.let { uri ->
                try {
                    saveImageToGallery(uri)
                    Toast.makeText(this, "이미지가 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    saveToGalleryButton.visibility = View.GONE
                } catch (e: IOException) {
                    resultInfo.append("\n${getString(R.string.error_saving_image, e.message)}")
                    Toast.makeText(this, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        saveToHistoryButton.setOnClickListener {
            showTagInputDialog()
        }
        
        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            handleActivityResult(result)
        }

        // GestureDetector 초기화
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < TRIPLE_TAP_TIMEOUT) {
                    tripleTapCounter++
                } else {
                    tripleTapCounter = 1
                }
                lastTapTime = currentTime

                if (tripleTapCounter == 3) {
                    resultCard.visibility = if (resultCard.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    tripleTapCounter = 0
                    if (resultCard.visibility == View.VISIBLE) {
                        Toast.makeText(this@MainActivity, "스캔 결과 표시", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "스캔 결과 숨김", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }
        })

        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onScanButtonClicked(unused: View) {
        if (resultCard.visibility == View.VISIBLE) {
            resultInfo.text = null
        }
        Glide.with(this).clear(firstPageView)
        currentScannedImageUri = null
        saveToGalleryButton.visibility = View.GONE
        saveToHistoryButton.visibility = View.GONE

        val options =
            GmsDocumentScannerOptions.Builder()
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setGalleryImportAllowed(enableGalleryImport)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .setPageLimit(1)

        GmsDocumentScanning.getClient(options.build())
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender: IntentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener() { e: Exception ->
                val errorMessage = getString(R.string.error_default_message, e.message)
                if (resultCard.visibility == View.VISIBLE) {
                    resultInfo.text = errorMessage
                } else {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    // 테스트용 메서드 - 스캔 결과 시뮬레이션
    @Suppress("UNUSED_PARAMETER")
    fun onTestScanClicked(unused: View) {
        Log.d(TAG, "Test scan clicked - simulating scan result")
        
        // 테스트용 이미지 URI 생성 (실제로는 존재하지 않는 URI)
        val testImageUri = Uri.parse("content://com.example.seventh.test/scanned_image.jpg")
        currentScannedImageUri = testImageUri
        
        // 테스트용 이미지 로드 (기본 이미지 사용)
        Glide.with(this)
            .load(R.drawable.ic_launcher_foreground)
            .into(firstPageView)
        
        // 결과 정보 설정
        resultInfo.text = "테스트 스캔 완료 - 스캔된 페이지: 1개"
        
        // 버튼들 표시
        saveToGalleryButton.visibility = View.VISIBLE
        saveToHistoryButton.visibility = View.VISIBLE
        resultCard.visibility = View.VISIBLE
        
        Log.d(TAG, "Test scan result displayed successfully")
        Toast.makeText(this, "테스트 스캔 결과가 표시되었습니다", Toast.LENGTH_SHORT).show()
    }

    private fun handleActivityResult(activityResult: ActivityResult) {
        val resultCode = activityResult.resultCode
        val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)

        Log.d(TAG, "handleActivityResult: resultCode=$resultCode, result=$result")

        currentScannedImageUri = null
        saveToGalleryButton.visibility = View.GONE
        saveToHistoryButton.visibility = View.GONE
        resultCard.visibility = View.GONE
        Glide.with(this).clear(firstPageView)

        if (resultCode == Activity.RESULT_OK && result != null) {
            Log.d(TAG, "Scan successful, processing result")
            val scanResultText = getString(R.string.scan_result, result)
            resultInfo.text = scanResultText

            val pages = result.pages
            Log.d(TAG, "Pages: $pages, size: ${pages?.size}")
            
            if (pages != null && pages.isNotEmpty()) {
                val imageUri = pages[0].imageUri
                Log.d(TAG, "Image URI: $imageUri")
                currentScannedImageUri = imageUri
                Glide.with(this).load(imageUri).into(firstPageView)
                saveToGalleryButton.visibility = View.VISIBLE
                saveToHistoryButton.visibility = View.VISIBLE
                resultCard.visibility = View.VISIBLE
                Log.d(TAG, "Result card and buttons made visible")
            } else {
                Log.w(TAG, "No pages found in scan result")
            }

            result.pdf?.uri?.path?.let { path ->
                val externalUri = FileProvider.getUriForFile(this, packageName + ".provider", File(path))
                Log.d(TAG, "PDF path: $path")
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d(TAG, "Scan cancelled by user")
            val cancelMessage = getString(R.string.error_scanner_cancelled)
            Toast.makeText(this, cancelMessage, Toast.LENGTH_SHORT).show()
        } else {
            Log.w(TAG, "Scan failed with resultCode: $resultCode")
            val defaultErrorMessage = getString(R.string.error_default_message)
            Toast.makeText(this, defaultErrorMessage, Toast.LENGTH_SHORT).show()
        }
    }


    @Throws(IOException::class)
    private fun saveImageToGallery(imageUri: Uri) {
        val contentResolver = contentResolver
        val displayName = "scanned_image_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyScannedImages")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageOutUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageOutUri?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, imageUri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
        } ?: throw IOException("Failed to create new MediaStore record for image.")
    }

    private fun showTagInputDialog() {
        val dialog = TagInputDialog()
        dialog.setOnTagsConfirmedListener { tags ->
            currentScannedImageUri?.let { uri ->
                saveToHistory(uri.toString(), tags)
            }
        }
        dialog.show(supportFragmentManager, "TagInputDialog")
    }
    
    private fun saveToHistory(imageUri: String, tags: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val scanHistory = ScanHistory(
                imageUri = imageUri,
                tags = tags
            )
            database.scanHistoryDao().insertScanHistory(scanHistory)
            
            runOnUiThread {
                Toast.makeText(this@MainActivity, "히스토리에 저장되었습니다.", Toast.LENGTH_SHORT).show()
                saveToHistoryButton.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
