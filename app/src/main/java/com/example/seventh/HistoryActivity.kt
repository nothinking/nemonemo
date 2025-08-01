package com.example.seventh

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.seventh.data.AppDatabase
import com.example.seventh.data.ScanHistory
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: HistoryPagerAdapter
    private lateinit var database: AppDatabase
    private lateinit var emptyCard: MaterialCardView
    private lateinit var emptyText: TextView
    private lateinit var tagChipGroup: ChipGroup
    private var dataLoadJob: Job? = null
    private var tagLoadJob: Job? = null
    private var currentFilterTag: String? = null
    
    companion object {
        private const val TAG = "HistoryActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        try {
            // Material Toolbar 설정
            val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
            setSupportActionBar(toolbar)
            
            // 뒤로가기 버튼 설정
            toolbar.setNavigationOnClickListener {
                finish()
            }
            
            viewPager = findViewById(R.id.view_pager)
            emptyCard = findViewById(R.id.empty_card)
            emptyText = findViewById(R.id.empty_text)
            tagChipGroup = findViewById(R.id.tag_chip_group)
            database = AppDatabase.getDatabase(this)
            
            adapter = HistoryPagerAdapter()
            viewPager.adapter = adapter
            
            // 태그 필터 설정
            setupTagFilter()
            
            // 히스토리 데이터 로드
            loadHistoryData()
            
            Log.d(TAG, "HistoryActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "히스토리 화면을 불러오는 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 데이터 로드 작업을 안전하게 취소
        dataLoadJob?.cancel()
        tagLoadJob?.cancel()
        Log.d(TAG, "HistoryActivity onDestroy - all jobs cancelled")
    }
    
    private fun setupTagFilter() {
        // 이전 태그 로드 작업이 있다면 취소
        tagLoadJob?.cancel()
        
        tagLoadJob = lifecycleScope.launch {
            try {
                Log.d(TAG, "Setting up tag filter")
                database.scanHistoryDao().getAllTags().collect { tagsList ->
                    Log.d(TAG, "Received tags: $tagsList")
                    
                    tagChipGroup.removeAllViews()
                    
                    // 중복 제거를 위한 Set 사용
                    val uniqueTags = mutableSetOf<String>()
                    
                    // 각 태그별로 Chip 생성
                    tagsList.forEach { tags ->
                        if (tags.isNotEmpty()) {
                            val tagArray = tags.split(",").map { it.trim() }
                            tagArray.forEach { tag ->
                                if (tag.isNotEmpty()) {
                                    uniqueTags.add(tag)
                                }
                            }
                        }
                    }
                    
                    Log.d(TAG, "Unique tags: $uniqueTags")
                    
                    // "전체" 태그 추가 (첫 번째로 추가하여 기본 선택)
                    val allChip = createTagChip("전체", null)
                    tagChipGroup.addView(allChip)
                    
                    // 고유한 태그들로 Chip 생성
                    uniqueTags.sorted().forEach { tag ->
                        val chip = createTagChip(tag, tag)
                        tagChipGroup.addView(chip)
                    }
                    
                    // 초기 상태에서 "전체" Chip 선택
                    if (currentFilterTag == null) {
                        allChip.isChecked = true
                        Log.d(TAG, "Set '전체' chip as checked")
                    }
                }
            } catch (e: Exception) {
                // CancellationException은 정상적인 취소이므로 무시
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "Tag loading job was cancelled normally: ${e.message}")
                    return@launch
                }
                
                // 액티비티가 종료 중이면 오류 메시지를 표시하지 않음
                if (!isFinishing && !isDestroyed) {
                    Log.e(TAG, "Error loading tags: ${e.message}", e)
                    Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                    Toast.makeText(this@HistoryActivity, "태그를 불러오는 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "Activity is finishing, ignoring tag loading error: ${e.message}")
                }
            }
        }
    }
    
    private fun createTagChip(text: String, filterTag: String?): Chip {
        return Chip(this).apply {
            this.text = text
            isCheckable = true
            isChecked = filterTag == currentFilterTag
            
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Log.d(TAG, "Tag chip clicked: $filterTag")
                    currentFilterTag = filterTag
                    loadHistoryData()
                }
            }
        }
    }
    
    private fun loadHistoryData() {
        // 이전 작업이 있다면 취소
        dataLoadJob?.cancel()
        
        dataLoadJob = lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading history data with filter: $currentFilterTag")
                
                val filterTag = currentFilterTag
                val flow = if (filterTag != null && filterTag.isNotEmpty()) {
                    database.scanHistoryDao().getScanHistoryByTag(filterTag)
                } else {
                    database.scanHistoryDao().getAllScanHistory()
                }
                
                flow.collect { historyList ->
                    // 액티비티가 종료되었는지 확인
                    if (isFinishing || isDestroyed) {
                        Log.d(TAG, "Activity is finishing or destroyed, stopping data collection")
                        return@collect
                    }
                    
                    Log.d(TAG, "Loaded ${historyList.size} history items with filter: $filterTag")
                    adapter.submitList(historyList)
                    
                    // 빈 상태 처리
                    if (historyList.isEmpty()) {
                        viewPager.visibility = View.GONE
                        emptyCard.visibility = View.VISIBLE
                        
                                            // 필터링된 결과가 없을 때 메시지 변경
                    val message = if (filterTag != null && filterTag.isNotEmpty()) {
                        "'$filterTag'와 함께 찍은 포토부스 사진이 없습니다."
                    } else {
                        "포토부스 추억이 없습니다."
                    }
                        emptyText.text = message
                        
                        Log.d(TAG, "Showing empty state with message: $message")
                    } else {
                        viewPager.visibility = View.VISIBLE
                        emptyCard.visibility = View.GONE
                        Log.d(TAG, "Showing history items")
                    }
                }
            } catch (e: Exception) {
                // CancellationException은 정상적인 취소이므로 무시
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "Job was cancelled normally: ${e.message}")
                    return@launch
                }
                
                // 액티비티가 종료 중이면 오류 메시지를 표시하지 않음
                if (!isFinishing && !isDestroyed) {
                    Log.e(TAG, "Error loading history data: ${e.message}", e)
                    Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                    Toast.makeText(this@HistoryActivity, "히스토리 데이터를 불러오는 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                } else {
                    Log.d(TAG, "Activity is finishing, ignoring error: ${e.message}")
                }
            }
        }
    }
}

class HistoryPagerAdapter : RecyclerView.Adapter<HistoryPagerAdapter.HistoryViewHolder>() {
    private var historyList: List<ScanHistory> = emptyList()
    
    companion object {
        private const val TAG = "HistoryPagerAdapter"
    }
    
    fun submitList(newList: List<ScanHistory>) {
        try {
            historyList = newList
            notifyDataSetChanged()
            Log.d(TAG, "Adapter updated with ${newList.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Error in submitList: ${e.message}", e)
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return HistoryViewHolder(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ViewHolder: ${e.message}", e)
            throw e
        }
    }
    
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        try {
            holder.bind(historyList[position])
        } catch (e: Exception) {
            Log.e(TAG, "Error binding ViewHolder at position $position: ${e.message}", e)
        }
    }
    
    override fun getItemCount(): Int = historyList.size
    
    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.history_image)
        private val tagChipGroup: ChipGroup = itemView.findViewById(R.id.tag_chip_group)
        private val dateTextView: TextView = itemView.findViewById(R.id.history_date)
        
        fun bind(scanHistory: ScanHistory) {
            try {
                // 이미지 로드
                Glide.with(itemView.context)
                    .load(scanHistory.imageUri)
                    .into(imageView)
                
                // 태그 표시
                tagChipGroup.removeAllViews()
                val tags = scanHistory.tags.split(",").map { it.trim() }
                tags.forEach { tag ->
                    if (tag.isNotEmpty()) {
                        val chip = Chip(itemView.context).apply {
                            text = tag
                            isCheckable = false
                        }
                        tagChipGroup.addView(chip)
                    }
                }
                
                // 날짜 표시
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val date = Date(scanHistory.timestamp)
                dateTextView.text = "촬영 날짜: ${dateFormat.format(date)}"
                
                Log.d(TAG, "Successfully bound history item: ${scanHistory.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error binding history item: ${e.message}", e)
            }
        }
    }
} 