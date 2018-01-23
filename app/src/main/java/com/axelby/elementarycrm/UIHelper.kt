package com.axelby.elementarycrm

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView

fun View.showPopup(text: String) {
    val textView = TextView(this.context)
    textView.setPadding(24, 24, 24, 24)
    textView.setBackgroundColor(this.context.resources.getColor(R.color.colorAccent, null))
    textView.text = text

    val popup = PopupWindow(textView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    popup.elevation = 5.0f
    // dismiss by touching outside
    popup.isFocusable = true
    popup.isOutsideTouchable = true
    popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    val rect = Rect()
    this.getGlobalVisibleRect(rect)
    popup.showAtLocation(
            this,
            Gravity.NO_GRAVITY,
            (rect.left + rect.right - textView.width) / 2,
            rect.top + this.height)
}

