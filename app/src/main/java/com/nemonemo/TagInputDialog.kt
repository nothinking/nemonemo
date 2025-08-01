package com.nemonemo

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton

class TagInputDialog : DialogFragment() {
    private var onTagsConfirmed: ((String) -> Unit)? = null
    
    fun setOnTagsConfirmedListener(listener: (String) -> Unit) {
        onTagsConfirmed = listener
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_tag_input, null)
        
        val tagEditText = view.findViewById<EditText>(R.id.tag_edit_text)
        val confirmButton = view.findViewById<MaterialButton>(R.id.confirm_button)
        val cancelButton = view.findViewById<MaterialButton>(R.id.cancel_button)
        
        return AlertDialog.Builder(requireContext())
            .setTitle("함께 찍은 사람 이름")
            .setView(view)
            .setCancelable(false)
            .create().apply {
                
                confirmButton.setOnClickListener {
                    val tags = tagEditText.text.toString().trim()
                    if (tags.isNotEmpty()) {
                        onTagsConfirmed?.invoke(tags)
                        dismiss()
                    } else {
                        tagEditText.error = "함께 찍은 사람의 이름을 입력해주세요"
                    }
                }
                
                cancelButton.setOnClickListener {
                    dismiss()
                }
            }
    }
} 