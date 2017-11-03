package com.yung_coder.oluwole.imagecompressor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import java.io.File
import java.io.IOException
import java.text.DecimalFormat


@Suppress("DEPRECATION")
class Home : AppCompatActivity() {

    companion object {
        val REQUEST_STORAGE_PERMISSION = 1
        val REQUEST_IMAGE_CAPTURE = 1
        val REQUEST_DOWNLOAD_IMAGE = 3
        val FILE_PROVIDER_AUTHORITY = "com.yung_coder.oluwole.fileprovider"
        var mTempPhotoPath = ""
        var initial_size = 0f
        var final_size = 0f
        var savedImagePath = ""
        val PICKFILE_RESULT_CODE = 2
        var self_path: File? = null
        var initial_path: File? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)

        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        button_compare.visibility = View.GONE

        button_compare.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("first_image", initial_path.toString())
            bundle.putString("second_image", self_path.toString())
            bundle.putString("initial_size", initial_size.toString())
            bundle.putString("final_size", final_size.toString())
            val intent = Intent(this@Home, Compare::class.java)
            intent.putExtra("extra", bundle)
            startActivity(intent)
        }

        menu_camera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_STORAGE_PERMISSION)
            } else {
                launchCamera()
            }
        }

        menu_open_file.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_STORAGE_PERMISSION)
            } else {
                val file_intent = Intent(Intent.ACTION_GET_CONTENT)
                file_intent.type = "image/*"
                try {
                    startActivityForResult(Intent.createChooser(file_intent, "Select Picture"), PICKFILE_RESULT_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("EXCEPTION: ", e.message)
                }
            }
        }

        menu_open_download.setOnClickListener {
            startActivityForResult(Intent(this, Download::class.java), REQUEST_DOWNLOAD_IMAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, launch the camera
                    launchCamera()
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = BitmapUtils.createTempImageFile(this)
            } catch(e: Exception) {
                e.printStackTrace()
                Log.e("Exception: ", e.message)
            }

            if (photoFile != null) {
                mTempPhotoPath = photoFile.absolutePath
                val photoUri: Uri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun getPath(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = managedQuery(uri, projection, null, null, null)
        return if (cursor != null) {
            val column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(column_index)
        } else
            null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE ->
                if (resultCode == Activity.RESULT_OK) {
                    processAndSetImage(mTempPhotoPath)
                } else {
                    BitmapUtils.deleteImageFile(this, mTempPhotoPath)
                }
            PICKFILE_RESULT_CODE ->
                if (resultCode == Activity.RESULT_OK) {
                    val image_uri = data?.data
                    val file_manager_path = image_uri!!.path
                    val media_gallery_path = getPath(image_uri)
                    if (media_gallery_path != null) {
                        processAndSetImage(media_gallery_path)
                    } else {
                        processAndSetImage(file_manager_path)
                    }
                }
            REQUEST_DOWNLOAD_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val image_uri = data?.getStringExtra("file_path")
                    if (image_uri != null || image_uri != "") {
                        processAndSetImage(image_uri.toString())
                    }
                }
            }
        }
    }

    private fun getCompressionRate(): Float {
        return ((initial_size - final_size) / initial_size) * 100
    }

    private fun processAndSetImage(path: String) {
        menu.close(true)

        val df = DecimalFormat("####0.00")

        initial_size = BitmapUtils.getSize(path)

        if (initial_size > (1024 * 1024)) {
            compressImage(path)
            BitmapUtils.getSize(savedImagePath)
            self_path = File(savedImagePath)
        } else {
            final_size = initial_size
            self_path = File(path)
        }

        Picasso.with(this).load(self_path).into(img)
        image_final_size.text = getString(R.string.final_size, df.format((final_size / (1024 * 1024))))
        image_initial_size.text = getString(R.string.initial_size, df.format((initial_size / (1024 * 1024))))
        image_name.text = getString(R.string.image_name, savedImagePath.substring(savedImagePath.lastIndexOf("/") + 1))
        image_compress_rate.text = getString(R.string.compression_rate, df.format(getCompressionRate()), "%")
        btnSendBluetooth.visibility = View.VISIBLE

        btnSendBluetooth.setOnClickListener {
            sendBluetooth(self_path)
        }

        initial_path = File(path)
//        BitmapUtils.deleteImageFile(this, path)

        button_compare.visibility = View.VISIBLE
    }


    private fun sendBluetooth(data: File?) {

        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.type = "audio/"
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(data))
        // appContext?.startActivity(intent)

        val pm: PackageManager? = packageManager
        val appList = pm?.queryIntentActivities(intent, 0)
        if (appList?.count()!! >= 0) {
            var packageName: String? = null
            var className: String? = null
            var found = false

            for (info in appList) {
                packageName = info.activityInfo.packageName
                if (packageName == "com.android.bluetooth") {
                    className = info.activityInfo.name
                    found = true
                    break
                }
            }

            if (!found) {
                Toast.makeText(applicationContext, "Bluetooth not found on Device", Toast.LENGTH_LONG).show()
            } else {
                intent.setClassName(packageName, className)
                startActivity(intent)
            }
        }
    }

    private fun compressImage(path: String) {

        var scaledBitmap: Bitmap? = null
        val options: BitmapFactory.Options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var bmp = BitmapFactory.decodeFile(path, options)
        var actualHeight = options.outHeight
        var actualWidth = options.outWidth

        val maxHeight = 816.0f
        val maxWidth = 612.0f
        var imgRatio = (actualWidth / actualHeight).toFloat()
        val maxRatio = (maxWidth / maxHeight)

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            when {
                imgRatio < maxRatio -> {
                    imgRatio = maxHeight / actualHeight
                    actualWidth = (imgRatio * actualWidth).toInt()
                    actualHeight = maxHeight.toInt()
                }
                imgRatio > maxRatio -> {
                    imgRatio = maxWidth / actualWidth
                    actualHeight = (imgRatio * actualHeight).toInt()
                    actualWidth = maxWidth.toInt()
                }
                else -> {
                    actualHeight = maxHeight.toInt()
                    actualWidth = maxWidth.toInt()

                }
            }
        }

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true
        options.inInputShareable = true
        options.inTempStorage = ByteArray(16 * 1024)

        try {
            //          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(path, options)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()

        }

        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }


        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()
        val middleX = actualWidth / 2.0f
        val middleY = actualHeight / 2.0f

        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)

        val canvas = Canvas(scaledBitmap)
        canvas.matrix = scaleMatrix
        canvas.drawBitmap(bmp, middleX - bmp.width / 2, middleY - bmp.height / 2, Paint(Paint.FILTER_BITMAP_FLAG))

//      check the rotation of the image and display it properly

        try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)

            Log.d("EXIF", "Exif: " + orientation)
            val matrix = Matrix()
            when (orientation) {
                6 -> {
                    matrix.postRotate(90f)
                    Log.d("EXIF", "Exif: " + orientation)
                }
                3 -> {
                    matrix.postRotate(180f)
                    Log.d("EXIF", "Exif: " + orientation)
                }
                8 -> {
                    matrix.postRotate(270f)
                    Log.d("EXIF", "Exif: " + orientation)
                }
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap!!.width, scaledBitmap.height, matrix,
                    true)
            savedImagePath = BitmapUtils.saveImage(this, scaledBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        val totalPixels = (width * height).toFloat()
        val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }

        return inSampleSize
    }

}
