package com.yung_coder.oluwole.imagecompressor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.tonyodev.fetch.Fetch
import com.tonyodev.fetch.request.Request
import kotlinx.android.synthetic.main.activity_download.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Download : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        button_download.setOnClickListener {
            val url = url_download.text.toString()
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(this, "Provide a valid URl to download Image from", Toast.LENGTH_LONG).show()
            } else {
                button_download.text = "Please Wait"
                button_download.isClickable = false

                download_progress.visibility = View.VISIBLE

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss",
                        Locale.getDefault()).format(Date())
                val imageFileName = "JPEG_" + timeStamp + "_.jpg"

                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "Image")

                val file = File("$dir/$imageFileName")

                val fetch = Fetch.newInstance(this)
                val request = Request(url, dir.toString(), imageFileName)
                val download_id = fetch.enqueue(request)

                fetch.addFetchListener { id, status, progress, _, _, error ->
                    if (download_id == id && status == Fetch.STATUS_DOWNLOADING) {
                        download_progress.progress = progress
                    } else if (error != Fetch.NO_ERROR) {
                        Toast.makeText(this, "There seems to be a network Connectivity Issue. Try to download the image" +
                                "again", Toast.LENGTH_LONG).show()
                    }

                    if (status == Fetch.STATUS_DONE && error == Fetch.NO_ERROR) {
                        if (file.exists() && file.length() > 0) {
                            Toast.makeText(this, "Image Download Complete....Beginning Compression", Toast.LENGTH_LONG).show()
                            val intent = Intent()
                            intent.putExtra("file_path", file.toString())
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        } else {
                            Toast.makeText(this, "An error occurred during download. Please try again.", Toast.LENGTH_LONG).show()
                            button_download.isClickable = true
                            button_download.text = "Download"
                        }
                    }
                }
            }
        }
    }
}
