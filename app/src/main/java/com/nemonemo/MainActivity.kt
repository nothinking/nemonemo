package com.nemonemo

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.nemonemo.data.AppDatabase
import com.nemonemo.data.ScanHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : AppCompatActivity() {
    private lateinit var firstPageView: ImageView
    private lateinit var resultCard: MaterialCardView
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var enableGalleryImport = true

    private lateinit var saveToGalleryButton: Button
    private lateinit var saveToHistoryButton: Button
    private lateinit var cancelButton: Button
    private lateinit var historyButton: Button
    private var currentScannedImageUri: Uri? = null
    private var isFromGallery: Boolean = false
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 시스템 UI 설정
        setupSystemUI()

        // Material Toolbar 설정
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        
        // 홈화면이므로 뒤로가기 버튼은 표시하지 않음
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        firstPageView = findViewById(R.id.first_page_view)
        resultCard = findViewById(R.id.result_card)
        saveToGalleryButton = findViewById(R.id.save_to_gallery_button)
        saveToHistoryButton = findViewById(R.id.save_to_history_button)
        cancelButton = findViewById(R.id.cancel_button)
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
                    Toast.makeText(this, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        saveToHistoryButton.setOnClickListener {
            showTagInputDialog()
        }
        
        cancelButton.setOnClickListener {
            resetImageArea()
        }
        
        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            handleActivityResult(result)
        }

        // 갤러리 이미지 선택을 위한 ActivityResultLauncher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            handleGalleryResult(uri)
        }
    }

    private fun setupSystemUI() {
        // Edge-to-edge 디스플레이 설정
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 시스템 UI 컨트롤러 설정
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false
        windowInsetsController.isAppearanceLightNavigationBars = false
        
        // WindowInsets 리스너 설정
        window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insets = WindowInsetsCompat.toWindowInsetsCompat(windowInsets)
            val navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            
            // 하단 패딩을 동적으로 조정
            val nestedScrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.nested_scroll_view)
            nestedScrollView?.setPadding(0, 0, 0, navigationBarHeight + 16)
            
            // FAB 위치 조정
            val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_scan)
            fab?.let {
                val layoutParams = it.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
                layoutParams.bottomMargin = navigationBarHeight + 16
                it.layoutParams = layoutParams
            }
            
            view.onApplyWindowInsets(windowInsets)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onScanButtonClicked(unused: View) {
        Glide.with(this).clear(firstPageView)
        currentScannedImageUri = null
        isFromGallery = false
        saveToGalleryButton.visibility = View.GONE
        saveToHistoryButton.visibility = View.GONE
        cancelButton.visibility = View.GONE
        resultCard.visibility = View.GONE
        
        // save_to_history_button의 패딩을 원래대로 복원
        val layoutParams = saveToHistoryButton.layoutParams as android.widget.LinearLayout.LayoutParams
        layoutParams.marginStart = resources.getDimensionPixelSize(R.dimen.button_margin_start)
        saveToHistoryButton.layoutParams = layoutParams

        val options =
            GmsDocumentScannerOptions.Builder()
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setGalleryImportAllowed(enableGalleryImport)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
                .setPageLimit(1)

        GmsDocumentScanning.getClient(options.build())
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender: IntentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener() { e: Exception ->
                val errorMessage = getString(R.string.error_default_message, e.message)
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onGalleryButtonClicked(unused: View) {
        // 갤러리에서 이미지 선택
        galleryLauncher.launch("image/*")
    }

    // 테스트용 메서드 - 스캔 결과 시뮬레이션
    @Suppress("UNUSED_PARAMETER")
    fun onTestScanClicked(unused: View) {
        Log.d(TAG, "Test scan clicked - simulating scan result")
        
        // 테스트용 이미지 URI 생성 (실제로는 존재하지 않는 URI)
        val testImageUri = Uri.parse("content://com.nemonemo.test/scanned_image.jpg")
        currentScannedImageUri = testImageUri
        isFromGallery = false
        
        // 테스트용 이미지 로드 (기본 이미지 사용)
        Glide.with(this)
            .load(R.drawable.ic_launcher_foreground)
            .into(firstPageView)
        
        // 버튼들 표시
        saveToGalleryButton.visibility = View.VISIBLE
        saveToHistoryButton.visibility = View.VISIBLE
        cancelButton.visibility = View.VISIBLE
        resultCard.visibility = View.VISIBLE
        
        Log.d(TAG, "Test scan result displayed successfully")
        Toast.makeText(this, "테스트 포토부스 사진이 표시되었습니다", Toast.LENGTH_SHORT).show()
    }

    private fun handleActivityResult(activityResult: ActivityResult) {
        val resultCode = activityResult.resultCode
        val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)

        Log.d(TAG, "handleActivityResult: resultCode=$resultCode, result=$result")

        currentScannedImageUri = null
        isFromGallery = false
        saveToGalleryButton.visibility = View.GONE
        saveToHistoryButton.visibility = View.GONE
        cancelButton.visibility = View.GONE
        resultCard.visibility = View.GONE
        Glide.with(this).clear(firstPageView)

        if (resultCode == Activity.RESULT_OK && result != null) {
            Log.d(TAG, "Scan successful, processing result")

            val pages = result.pages
            Log.d(TAG, "Pages: $pages, size: ${pages?.size}")
            
            if (pages != null && pages.isNotEmpty()) {
                val imageUri = pages[0].imageUri
                Log.d(TAG, "Image URI: $imageUri")
                currentScannedImageUri = imageUri
                isFromGallery = false
                Glide.with(this).load(imageUri).into(firstPageView)
                saveToGalleryButton.visibility = View.VISIBLE
                saveToHistoryButton.visibility = View.VISIBLE
                cancelButton.visibility = View.VISIBLE
                resultCard.visibility = View.VISIBLE
                
                // 포토부스에서 찍은 경우 save_to_history_button의 왼쪽 패딩 복원
                val layoutParams = saveToHistoryButton.layoutParams as android.widget.LinearLayout.LayoutParams
                layoutParams.marginStart = resources.getDimensionPixelSize(R.dimen.button_margin_start)
                saveToHistoryButton.layoutParams = layoutParams
                
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

    private fun handleGalleryResult(uri: Uri?) {
        uri?.let {
            currentScannedImageUri = it
            isFromGallery = true
            Glide.with(this).load(it).into(firstPageView)
            // 갤러리에서 불러온 이미지는 갤러리에 저장 버튼 숨김
            saveToGalleryButton.visibility = View.GONE
            saveToHistoryButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            resultCard.visibility = View.VISIBLE
            
            // 갤러리에서 가져온 경우 save_to_history_button의 왼쪽 패딩 제거
            val layoutParams = saveToHistoryButton.layoutParams as android.widget.LinearLayout.LayoutParams
            layoutParams.marginStart = 0
            saveToHistoryButton.layoutParams = layoutParams
            
            Toast.makeText(this, "갤러리에서 이미지를 불러왔습니다.", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "갤러리에서 이미지를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
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
                tags = tags,
                isFromGallery = isFromGallery
            )
            database.scanHistoryDao().insertScanHistory(scanHistory)
            
            runOnUiThread {
                val message = if (isFromGallery) "갤러리 이미지가 추억에 저장되었습니다." else "포토부스 추억에 저장되었습니다."
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                
                // 이미지 영역 리셋
                resetImageArea()
                
                // 추억보기 페이지로 이동
                val intent = Intent(this@MainActivity, HistoryActivity::class.java)
                startActivity(intent)
            }
        }
    }
    
    private fun resetImageArea() {
        // 이미지 뷰 초기화
        Glide.with(this).clear(firstPageView)
        firstPageView.setImageDrawable(null)
        
        // 현재 스캔된 이미지 URI 초기화
        currentScannedImageUri = null
        isFromGallery = false
        
        // 버튼들 숨기기
        saveToGalleryButton.visibility = View.GONE
        saveToHistoryButton.visibility = View.GONE
        cancelButton.visibility = View.GONE
        
        // 결과 카드 숨기기
        resultCard.visibility = View.GONE
        
        // save_to_history_button의 패딩을 원래대로 복원
        val layoutParams = saveToHistoryButton.layoutParams as android.widget.LinearLayout.LayoutParams
        layoutParams.marginStart = resources.getDimensionPixelSize(R.dimen.button_margin_start)
        saveToHistoryButton.layoutParams = layoutParams
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
