package com.example.assistapp_sum.features.feature_manager

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.assistapp_sum.R

class FeatureManagerActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_manager)
        prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val chk1 = findViewById<CheckBox>(R.id.chkFeature1)
        val chk2 = findViewById<CheckBox>(R.id.chkFeature2)
        val chk3 = findViewById<CheckBox>(R.id.chkFeature3)
        val codeInput = findViewById<EditText>(R.id.etCode)
        val saveBtn = findViewById<Button>(R.id.btnSave)

        // 기존 활성화 상태 불러오기
        val active = prefs.getStringSet("activeFeatures", setOf("1", "2", "3"))!!
        chk1.isChecked = "1" in active
        chk2.isChecked = "2" in active
        chk3.isChecked = "3" in active

        // 기존 저장된 코드 불러오기
        codeInput.setText(prefs.getString("generated_key", ""))

        saveBtn.setOnClickListener {
            val newSet = mutableSetOf<String>()
            if (chk1.isChecked) newSet.add("1")
            if (chk2.isChecked) newSet.add("2")
            if (chk3.isChecked) newSet.add("3")
            prefs.edit().apply {
                putStringSet("activeFeatures", newSet)
                putString("generated_key", codeInput.text.toString().trim())
                apply()
            }
            finish()
        }
    }
}
