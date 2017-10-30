package com.yung_coder.oluwole.imagecompressor

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_compare.*
import java.io.File
import java.text.DecimalFormat

class Compare : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare)

        val extra_bundle = intent.getBundleExtra("extra")
        val first_path = File(extra_bundle.getString("first_image"))
        val second_path = File(extra_bundle.getString("second_image"))
        val initial_size = extra_bundle.getString("initial_size")
        val final_size = extra_bundle.getString("final_size")

        Picasso.with(this@Compare).load(first_path).into(first_image)
        Picasso.with(this@Compare).load(second_path).into(second_image)


        val df = DecimalFormat("####0.00")
        var data_message = "Initial Image Size:  ${df.format((initial_size.toFloat() / (1024 * 1024)))} MB\n"
        data_message += "Final Image Size: ${df.format((final_size.toFloat() / (1024 * 1024)))} MB"

        text_data.text = data_message
    }
}
