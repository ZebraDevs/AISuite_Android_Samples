package com.zebra.aidatacapturedemo.ui.view

import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.style.BulletSpan
import org.xml.sax.XMLReader

class BulletHandler : Html.TagHandler {
    class Bullet

    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        if (tag == "li" && opening) {
            output.setSpan(Bullet(), output.length, output.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
        if (tag == "li" && !opening) {
            output.append("\n\n")
            val lastMark = output.getSpans(0, output.length, Bullet::class.java).lastOrNull()
            lastMark?.let {
                val start = output.getSpanStart(it)
                output.removeSpan(it)
                if (start != output.length) {
                    output.setSpan(BulletSpan(), start, output.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }
}