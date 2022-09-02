package kr.hyosang.drivediary.client.adapter

import android.widget.ImageView
import androidx.databinding.BindingAdapter

class ImageViewBindingAdapter {
    companion object {
        @JvmStatic
        @BindingAdapter("imgSrc")
        fun imgSrc(imageView: ImageView, resId: Int) {
            imageView.setImageResource(resId)
        }
    }
}