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
    private var dataLoadJob: Job? = null
    
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
            database = AppDatabase.getDatabase(this)
            
            adapter = HistoryPagerAdapter()
            viewPager.adapter = adapter
            
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
        Log.d(TAG, "HistoryActivity onDestroy - dataLoadJob cancelled")
    }
    
    private fun loadHistoryData() {
        // 이전 작업이 있다면 취소
        dataLoadJob?.cancel()
        
        dataLoadJob = lifecycleScope.launch {
            try {
                database.scanHistoryDao().getAllScanHistory().collect { historyList ->
                    // 액티비티가 종료되었는지 확인
                    if (isFinishing || isDestroyed) {
                        Log.d(TAG, "Activity is finishing or destroyed, stopping data collection")
                        return@collect
                    }
                    
                    Log.d(TAG, "Loaded ${historyList.size} history items")
                    adapter.submitList(historyList)
                    
                    // 빈 상태 처리
                    if (historyList.isEmpty()) {
                        viewPager.visibility = View.GONE
                        emptyCard.visibility = View.VISIBLE
                        Log.d(TAG, "Showing empty state")
                    } else {
                        viewPager.visibility = View.VISIBLE
                        emptyCard.visibility = View.GONE
                        Log.d(TAG, "Showing history items")
                    }
                }
            } catch (e: Exception) {
                // 액티비티가 종료 중이면 오류 메시지를 표시하지 않음
                if (!isFinishing && !isDestroyed) {
                    Log.e(TAG, "Error loading history data: ${e.message}", e)
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
        private val tagsTextView: TextView = itemView.findViewById(R.id.history_tags)
        private val dateTextView: TextView = itemView.findViewById(R.id.history_date)
        
        fun bind(scanHistory: ScanHistory) {
            try {
                // 이미지 로드
                Glide.with(itemView.context)
                    .load(scanHistory.imageUri)
                    .into(imageView)
                
                // 태그 표시
                tagsTextView.text = "태그: ${scanHistory.tags}"
                
                // 날짜 표시
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val date = Date(scanHistory.timestamp)
                dateTextView.text = "스캔 날짜: ${dateFormat.format(date)}"
                
                Log.d(TAG, "Successfully bound history item: ${scanHistory.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error binding history item: ${e.message}", e)
            }
        }
    }
} 