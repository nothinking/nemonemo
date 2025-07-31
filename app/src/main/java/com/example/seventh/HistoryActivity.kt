package com.example.seventh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.seventh.data.AppDatabase
import com.example.seventh.data.ScanHistory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: HistoryPagerAdapter
    private lateinit var database: AppDatabase
    private lateinit var emptyText: TextView
    private lateinit var backButton: ImageButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        viewPager = findViewById(R.id.view_pager)
        emptyText = findViewById(R.id.empty_text)
        backButton = findViewById(R.id.back_button)
        database = AppDatabase.getDatabase(this)
        
        adapter = HistoryPagerAdapter()
        viewPager.adapter = adapter
        
        // 뒤로가기 버튼 클릭 이벤트 설정
        backButton.setOnClickListener {
            finish()
        }
        
        // 히스토리 데이터 로드
        loadHistoryData()
    }
    
    private fun loadHistoryData() {
        lifecycleScope.launch {
            database.scanHistoryDao().getAllScanHistory().collectLatest { historyList ->
                adapter.submitList(historyList)
                
                // 빈 상태 처리
                if (historyList.isEmpty()) {
                    viewPager.visibility = View.GONE
                    emptyText.visibility = View.VISIBLE
                } else {
                    viewPager.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE
                }
            }
        }
    }
}

class HistoryPagerAdapter : RecyclerView.Adapter<HistoryPagerAdapter.HistoryViewHolder>() {
    private var historyList: List<ScanHistory> = emptyList()
    
    fun submitList(newList: List<ScanHistory>) {
        historyList = newList
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(historyList[position])
    }
    
    override fun getItemCount(): Int = historyList.size
    
    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.history_image)
        private val tagsTextView: TextView = itemView.findViewById(R.id.history_tags)
        private val dateTextView: TextView = itemView.findViewById(R.id.history_date)
        
        fun bind(scanHistory: ScanHistory) {
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
        }
    }
} 