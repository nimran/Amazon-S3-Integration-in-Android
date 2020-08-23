package study.amazons3integration

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import study.amazons3integration.aws.S3Uploader
import study.amazons3integration.aws.S3Uploader.S3UploadInterface
import study.amazons3integration.aws.S3Utils.generates3ShareUrl
import java.io.FileNotFoundException
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private var ivSelectedImage: ImageView? = null
    private var tvStatus: TextView? = null
    private val SELECT_PICTURE = 2
    private var imageUri: Uri? = null
    private var s3uploaderObj: S3Uploader? = null
    private var urlFromS3: String? = null
    private var progressDialog: ProgressDialog? = null
    private val TAG = MainActivity::class.java.canonicalName
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        s3uploaderObj = S3Uploader(this)
        progressDialog = ProgressDialog(this)
    }

    private fun bindViews() {
        ivSelectedImage = findViewById<View>(R.id.image_selected) as ImageView
        tvStatus = findViewById<View>(R.id.status) as TextView
        findViewById<View>(R.id.choose_file).setOnClickListener { isStoragePermissionGranted }
        findViewById<View>(R.id.upload_file).setOnClickListener {
            if (imageUri != null) {
                uploadImageTos3(imageUri!!)
            }
        }
    }

    //permission is automatically granted on sdk<23 upon installation
    private val isStoragePermissionGranted: Boolean
        get() = if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                chooseImage()
                true
            } else {
                Log.v(TAG, "Permission is revoked")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted")
            chooseImage()
            true
        }

    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            chooseImage()
            Log.e(TAG, "Permission: " + permissions[0] + "was " + grantResults[0])
        } else {
            Log.e(TAG, "Please click again and select allow to choose profile picture")
        }
    }

    fun onPictureSelect(data: Intent?) {
        imageUri = data!!.data
        var imageStream: InputStream? = null
        try {
            imageStream = contentResolver.openInputStream(imageUri!!)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        if (imageStream != null) {
            ivSelectedImage!!.setImageBitmap(BitmapFactory.decodeStream(imageStream))
        }
    }

    private fun getFilePathfromURI(selectedImageUri: Uri): String {
        var filePath = ""
        val wholeID = DocumentsContract.getDocumentId(selectedImageUri)

        // Split at colon, use second item in the array
        val id = wholeID.split(":".toRegex()).toTypedArray()[1]
        val column = arrayOf(MediaStore.Images.Media.DATA)

        // where id is equal to
        val sel = MediaStore.Images.Media._ID + "=?"
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, arrayOf(id), null)
        val columnIndex = cursor!!.getColumnIndex(column[0])
        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex)
        }
        cursor.close()
        return filePath
    }

    private fun uploadImageTos3(imageUri: Uri) {
        val path = getFilePathfromURI(imageUri)
        if (path != null) {
            showLoading("Uploading details !!")
            s3uploaderObj!!.initUpload(path)
            s3uploaderObj!!.setOns3UploadDone(object : S3UploadInterface {
                override fun onUploadSuccess(response: String?) {
                    if (response.equals("Success", ignoreCase = true)) {
                        hideLoading()
                        urlFromS3 = generates3ShareUrl(applicationContext, path)
                        if (!TextUtils.isEmpty(urlFromS3)) {
                            tvStatus!!.text = "Uploaded : $urlFromS3"
                        }
                    }
                }

                override fun onUploadError(response: String?) {
                    hideLoading()
                    tvStatus!!.text = "Error : $response"
                    Log.e(TAG, "Error Uploading")
                }
            })
        }
    }

    private fun showLoading(message: String) {
        if (progressDialog != null && !progressDialog!!.isShowing) {
            progressDialog!!.setMessage(message)
            progressDialog!!.setCancelable(false)
            progressDialog!!.show()
        }
    }

    private fun hideLoading() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                onPictureSelect(data)
            }
        }
    }
}