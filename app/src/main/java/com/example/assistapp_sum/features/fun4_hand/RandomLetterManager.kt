package com.example.assistapp_sum.features.fun4_hand

import kotlin.random.Random

class RandomLetterManager(private val level: Int) {

    private val level1 = listOf("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅎ", "ㅏ", "ㅣ")
    private val level2 = listOf("ㄲ", "ㄸ", "ㅃ", "ㅆ", "ㅉ", "가", "나", "다", "라", "마")
    private val level3 = listOf("가", "나", "다", "라", "마", "바", "사", "아", "자", "차")
    private val level4 = listOf("까", "따", "빠", "싸", "짜", "꺼", "떠", "뻐", "쌍", "뿌")

    private var currentList: MutableList<String> = mutableListOf()
    private var index = 0

    init { shuffleLetters() }

    private fun shuffleLetters() {
        currentList = when (level) {
            1 -> level1.shuffled(Random(System.currentTimeMillis())).toMutableList()
            2 -> level2.shuffled(Random(System.currentTimeMillis())).toMutableList()
            3 -> level3.shuffled(Random(System.currentTimeMillis())).toMutableList()
            else -> level4.shuffled(Random(System.currentTimeMillis())).toMutableList()
        }
        index = 0
    }

    fun next(): String {
        if (index >= currentList.size) shuffleLetters()
        return currentList[index++]
    }
}
