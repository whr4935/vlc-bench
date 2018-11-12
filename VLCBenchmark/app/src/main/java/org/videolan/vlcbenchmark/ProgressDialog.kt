package org.videolan.vlcbenchmark

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView

class ProgressDialog : DialogFragment() {

    lateinit var mTitle: TextView
    lateinit var mText: TextView
    lateinit var mCurrentSample: TextView
    lateinit var mProgressBar: ProgressBar

    init {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.progress_dialog, container, false)

        mTitle = view.findViewById(R.id.progress_dialog_title)
        mText = view.findViewById(R.id.progress_dialog_text)
        mCurrentSample = view.findViewById(R.id.progress_dialog_current_sample)
        mProgressBar = view.findViewById(R.id.progress_dialog_bar)

        mProgressBar.progress = 0
        mProgressBar.max = 100
        mText.setText(R.string.default_percent_value)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    fun setTitle(titleId: Int) {
        mTitle.setText(titleId)
    }

    fun updateProgress(value: Double, text: String, sample: String) {
        mProgressBar.progress = value.toInt()
        mText.text = text
        mCurrentSample.text = sample
    }

}