package com.example.seventh

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar

class FullscreenImageActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: FullscreenImageAdapter
    private var imageUris: ArrayList<String> = ArrayList()
    private var initialPosition: Int = 0
    
    companion object {
        const val EXTRA_IMAGE_URIS = "extra_image_uris"
        const val EXTRA_INITIAL_POSITION = "extra_initial_position"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)
        
        // 시스템 UI 설정
        setupSystemUI()
        
        // Material Toolbar 설정
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // 뒤로가기 버튼 클릭 시 액티비티 종료
        toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // 인텐트에서 데이터 받기
        imageUris = intent.getStringArrayListExtra(EXTRA_IMAGE_URIS) ?: ArrayList()
        initialPosition = intent.getIntExtra(EXTRA_INITIAL_POSITION, 0)
        
        viewPager = findViewById(R.id.fullscreen_view_pager)
        adapter = FullscreenImageAdapter(imageUris)
        viewPager.adapter = adapter
        
        // 초기 위치 설정
        viewPager.setCurrentItem(initialPosition, false)
        
        // 페이지 변경 리스너
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateToolbarTitle(position)
            }
        })
        
        updateToolbarTitle(initialPosition)
    }

    private fun setupSystemUI() {
        // Edge-to-edge 디스플레이 설정
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 시스템 UI 컨트롤러 설정 - 전체화면 모드
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false
        windowInsetsController.isAppearanceLightNavigationBars = false
        
        // 상태바와 네비게이션바 숨기기
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }
    
    private fun updateToolbarTitle(position: Int) {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.title = "${position + 1} / ${imageUris.size}"
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class FullscreenImageAdapter(private val imageUris: List<String>) : 
    androidx.recyclerview.widget.RecyclerView.Adapter<FullscreenImageAdapter.FullscreenImageViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullscreenImageViewHolder {
        val imageView = ZoomableImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return FullscreenImageViewHolder(imageView)
    }
    
    override fun onBindViewHolder(holder: FullscreenImageViewHolder, position: Int) {
        holder.bind(imageUris[position])
    }
    
    override fun getItemCount(): Int = imageUris.size
    
    class FullscreenImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(imageUri: String) {
            Glide.with(itemView.context)
                .load(imageUri)
                .into(itemView as ImageView)
        }
    }
}

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {
    
    private var scaleGestureDetector: ScaleGestureDetector
    private var gestureDetector: GestureDetector
    private var scaleFactor = 1.0f
    private val minScale = 0.5f
    private val maxScale = 3.0f
    
    init {
        scaleType = ScaleType.FIT_CENTER
        
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
            scaleX = scaleFactor
            scaleY = scaleFactor
            return true
        }
    }
    
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (scaleFactor > 1.0f) {
                // 원래 크기로 복원
                scaleFactor = 1.0f
                scaleX = scaleFactor
                scaleY = scaleFactor
            } else {
                // 확대
                scaleFactor = 2.0f
                scaleX = scaleFactor
                scaleY = scaleFactor
            }
            return true
        }
        
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // 툴바 토글 기능 (필요시 구현)
            return true
        }
    }
} 