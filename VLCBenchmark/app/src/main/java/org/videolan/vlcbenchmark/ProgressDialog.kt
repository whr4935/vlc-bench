package org.videolan.vlcbenchmark

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView

class ProgressDialog : DialogFragment() {

    companion object {
        @Suppress("UNUSED")
        private val TAG = this::class.java.name
    }

    var mTitleId: Int = 0
    lateinit var mTitle: TextView
    lateinit var mText: TextView
    lateinit var mCurrentSample: TextView
    lateinit var mProgressBar: ProgressBar
    lateinit var cancel: () -> Unit

    init {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.progress_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mTitle = view.findViewById(R.id.progress_dialog_title)
        mText = view.findViewById(R.id.progress_dialog_text)
        mCurrentSample = view.findViewById(R.id.progress_dialog_current_sample)
        mProgressBar = view.findViewById(R.id.progress_dialog_bar)

        mTitle.setText(mTitleId)
        mProgressBar.progress = 0
        mProgressBar.max = 100
        mText.setText(R.string.default_percent_value)

        val button: Button = view.findViewById(R.id.progress_dialog_cancel)
        button.setOnClickListener {
            cancel()
            this.dismiss()
        }
    }

    fun setTitle(titleId: Int) {
        mTitleId = titleId
    }

    fun setCancelCallback(cancel: () -> Unit) {
        this.cancel = cancel
    }


    fun updateProgress(value: Double, text: String, sample: String) {
        mProgressBar.progress = value.toInt()
        mText.text = text
        mCurrentSample.text = sample
    }

}