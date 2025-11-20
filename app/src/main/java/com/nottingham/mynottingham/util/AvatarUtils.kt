package com.nottingham.mynottingham.util

import com.nottingham.mynottingham.R

object AvatarUtils {
    // 对应你 drawable 里的文件名: tx1.png ~ tx13.png
    val AVATAR_KEYS = listOf(
        "tx1", "tx2", "tx3", "tx4", "tx5", "tx6", "tx7", 
        "tx8", "tx9", "tx10", "tx11", "tx12", "tx13"
    )

    // 核心方法：把数据库存的字符串转成 Android 图片资源 ID
    fun getDrawableId(avatarKey: String?): Int {
        return when (avatarKey) {
            "tx1" -> R.drawable.tx1
            "tx2" -> R.drawable.tx2
            "tx3" -> R.drawable.tx3
            "tx4" -> R.drawable.tx4
            "tx5" -> R.drawable.tx5
            "tx6" -> R.drawable.tx6
            "tx7" -> R.drawable.tx7
            "tx8" -> R.drawable.tx8
            "tx9" -> R.drawable.tx9
            "tx10" -> R.drawable.tx10
            "tx11" -> R.drawable.tx11
            "tx12" -> R.drawable.tx12
            "tx13" -> R.drawable.tx13
            else -> R.drawable.tx1 // Default to tx1
        }
    }
}
