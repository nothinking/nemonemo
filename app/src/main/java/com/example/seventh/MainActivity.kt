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
import android.view.GestureDetector // GestureDetector 추가
import android.view.MotionEvent // MotionEvent 추가
//import androidx.compose.ui.graphics.vector.path
//import androidx.compose.ui.semantics.text
import androidx.core.view.GestureDetectorCompat // GestureDetectorCompat 추가
//import androidx.glance.visibility
import com.example.seventh.data.AppDatabase
import com.example.seventh.data.ScanHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var resultInfo: TextView
    private lateinit var firstPageView: ImageView
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
    private val TRIPLE_TAP_TIMEOUT = 500L // 밀리초 단위, 세 번의 탭 사이의 최대 시간 간격
    private var lastTapTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultInfo = findViewById(R.id.result_info)
        firstPageView = findViewById(R.id.first_page_view)
        saveToGalleryButton = findViewById(R.id.save_to_gallery_button)
        saveToHistoryButton = findViewById(R.id.save_to_history_button)
        historyButton = findViewById(R.id.history_button)
        database = AppDatabase.getDatabase(this)

        // 초기에는 resultInfo를 숨김
        resultInfo.visibility = View.GONE

        saveToGalleryButton.visibility = View.GONE
        saveToHistoryButton.visibility = View.GONE
        
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
                // onDown은 항상 true를 반환해야 이벤트를 계속 받을 수 있음
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < TRIPLE_TAP_TIMEOUT) {
                    tripleTapCounter++
                } else {
                    // 시간 간격이 너무 길면 카운터 초기화
                    tripleTapCounter = 1
                }
                lastTapTime = currentTime

                if (tripleTapCounter == 3) {
                    resultInfo.visibility = if (resultInfo.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    tripleTapCounter = 0 // 카운터 초기화
                    if (resultInfo.visibility == View.VISIBLE) {
                        Toast.makeText(this@MainActivity, "Result Info 표시", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Result Info 숨김", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }
        })

        // 최상위 뷰 또는 터치 이벤트를 감지할 뷰에 OnTouchListener 설정
        // 예를 들어, activity_main.xml의 루트 레이아웃 ID가 'root_layout'이라고 가정
        val rootView = findViewById<View>(android.R.id.content) // 또는 특정 레이아웃 ID
        rootView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            // true를 반환하면 다른 뷰로 터치 이벤트가 전달되지 않을 수 있으므로 주의
            // 여기서는 다른 뷰의 클릭 이벤트 등을 방해하지 않기 위해 false 또는 gestureDetector의 결과를 반환할 수 있음
            // 하지만 onDown에서 true를 반환했으므로, 여기서는 gestureDetector가 처리하도록 둡니다.
            true // 모든 터치 이벤트를 gestureDetector가 소모하도록 함
        }
    }

    // onScanButtonClicked, handleActivityResult, saveImageToGallery, TAG 등은 이전과 동일하게 유지

    @Suppress("UNUSED_PARAMETER")
    fun onScanButtonClicked(unused: View) {
        // resultInfo.text = null // resultInfo가 숨겨져 있을 수 있으므로, 텍스트 초기화는 필요에 따라 조정
        if (resultInfo.visibility == View.VISIBLE) { // 보이는 경우에만 텍스트 초기화
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
                if (resultInfo.visibility == View.VISIBLE) {
                    resultInfo.text = errorMessage
                } else {
                    // resultInfo가 숨겨져 있을 때는 Toast 등으로 오류를 알릴 수 있습니다.
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleActivityResult(activityResult: ActivityResult) {
        val resultCode = activityResult.resultCode
        val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)

        currentScannedImageUri = null
        saveToGalleryButton.visibility = View.GONE
        saveToHistoryButton.visibility = View.GONE
        Glide.with(this).clear(firstPageView)

        if (resultCode == Activity.RESULT_OK && result != null) {
            val scanResultText = getString(R.string.scan_result, result)
            if (resultInfo.visibility == View.VISIBLE) {
                resultInfo.text = scanResultText
            } else if (resultInfo.text == null) { // 처음 resultInfo가 숨겨진 상태에서 성공했을 때
                resultInfo.text = scanResultText // 내용을 설정해두고, 나중에 보이게 할 때 표시되도록
            }


            val pages = result.pages
            if (pages != null && pages.isNotEmpty()) {
                val imageUri = pages[0].imageUri
                currentScannedImageUri = imageUri
                Glide.with(this).load(imageUri).into(firstPageView)
                saveToGalleryButton.visibility = View.VISIBLE
                saveToHistoryButton.visibility = View.VISIBLE
            }

            result.pdf?.uri?.path?.let { path ->
                val externalUri = FileProvider.getUriForFile(this, packageName + ".provider", File(path))
                // PDF 공유 로직 (필요시 사용)
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            val cancelMessage = getString(R.string.error_scanner_cancelled)
            if (resultInfo.visibility == View.VISIBLE) {
                resultInfo.text = cancelMessage
            } else {
                Toast.makeText(this, cancelMessage, Toast.LENGTH_SHORT).show()
            }
        } else {
            val defaultErrorMessage = getString(R.string.error_default_message)
            if (resultInfo.visibility == View.VISIBLE) {
                resultInfo.text = defaultErrorMessage
            } else {
                Toast.makeText(this, defaultErrorMessage, Toast.LENGTH_SHORT).show()
            }
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
