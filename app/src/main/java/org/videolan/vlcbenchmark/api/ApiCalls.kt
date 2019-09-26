package org.videolan.vlcbenchmark.api

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import org.videolan.vlcbenchmark.R
import org.videolan.vlcbenchmark.ResultPage
import org.videolan.vlcbenchmark.tools.DialogInstance
import org.videolan.vlcbenchmark.tools.FileHandler
import org.videolan.vlcbenchmark.tools.TestInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.ArrayList

class ApiCalls {

    companion object {

        @Suppress("UNUSED")
        private val TAG = this::class.java.name

        private fun displayProgressDialog(context: Context, call: Call<out Any>): Dialog {
            val cancelCallback = DialogInterface.OnClickListener { dialog, _ ->
                call.cancel()
                dialog.dismiss()
            }
            val view = LayoutInflater.from(context).inflate(R.layout.layout_upoad_progress_dialog, null)
            view.keepScreenOn = true
            return AlertDialog.Builder(context)
                    .setView(view)
                    .setNegativeButton(R.string.dialog_btn_cancel, cancelCallback)
                    .show()
        }

        private fun getUploadCallback(context: Context, progressDialog: Dialog): Callback<Void> {
            return object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.code() == 200) {
                        val websiteLink = DialogInterface.OnClickListener { _, _ ->
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bench.videolabs.io"))
                            (context as ResultPage).startActivity(browserIntent)
                        }
                        AlertDialog.Builder(context)
                                .setTitle(R.string.dialog_title_success)
                                .setMessage(R.string.dialog_text_upload_success)
                                .setNeutralButton(R.string.dialog_btn_visit, websiteLink)
                                .setNegativeButton(R.string.dialog_btn_continue, null)
                                .show()
                        FileHandler.deleteScreenshots()
                    }
                    progressDialog.dismiss()
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e(TAG, t.toString())
                    DialogInstance(R.string.dialog_title_error, R.string.dialog_text_err_upload).display(context)
                    FileHandler.deleteScreenshots()
                    progressDialog.dismiss()
                }
            }
        }

        @JvmStatic
        fun uploadBenchmark(context: Context, res: JSONObject) {
            val retrofit = RetrofitInstance.retrofit
            if (retrofit != null) {
                val apiInterface = retrofit.create(ApiInterface::class.java)
                val call = apiInterface.uploadBenchmark(
                        RequestBody.create(
                                MediaType.parse("application/json; charset=utf-8"),
                                res.toString()
                        )
                )
                val progressDialog = displayProgressDialog(context, call)
                val callback = getUploadCallback(context, progressDialog)
                call.enqueue(callback)
            } else {
                Log.e(TAG, "uploadBenchmark: Retrofit is null")
            }
        }

        @JvmStatic
        fun uploadBenchmarkWithScreenshots(context: Context, jsonObject: JSONObject, results: ArrayList<TestInfo>) {
            val retrofit = RetrofitInstance.retrofit
            if (retrofit != null) {
                val apiInterface = retrofit.create(ApiInterface::class.java)
                val builder = MultipartBody.Builder()
                builder.setType(MultipartBody.FORM)
                builder.addFormDataPart("data", jsonObject.toString())

                val screenshotFolder = FileHandler.getFolderStr(FileHandler.screenshotFolder)
                for (test in results) {
                    for (screen in test.screenshots) {
                        val file = File(screenshotFolder, screen)
                        builder.addFormDataPart(
                                screen,
                                screen,
                                RequestBody.create(
                                        MediaType.parse("multipart/form-data"),
                                        file
                                )
                        )
                    }
                }

                val requestBody = builder.build()
                val call = apiInterface.uploadBenchmarkWithScreenshots(requestBody)
                val progressDialog = displayProgressDialog(context, call)
                val callback = getUploadCallback(context, progressDialog)
                call.enqueue(callback)
            } else {
                Log.e(TAG, "uploadBenchmarkWithScreenshots: Retrofit is null")
            }
        }
    }
}