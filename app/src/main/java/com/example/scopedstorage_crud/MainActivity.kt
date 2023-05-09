package com.example.scopedstorage_crud

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.scopedstorage_crud.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val SDK_28_ABOVE = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q

    // Reqesut Codes
    private val READ_EXTERNAL_STORAGE_REQUEST_CODE = 1
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 2

    // Launcher variables
    private lateinit var pickImage: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Void?>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initLaunchers()

        binding.btnRead.setOnClickListener {
            if (checkReadStoragePermission()) {
                pickImageFromGallery()
            } else {
                // Permission not granted, request it
                getReadPermission()
            }
        }

        binding.btnWrite.setOnClickListener {
            if (checkWriteStoragePermission()) {
                 lauchCamera()
            } else {
                // Permission not granted, request it
                getWritePermission()
            }
        }

    }


    private fun initLaunchers() {
        // photo picker activity launcher
        pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

            if (uri != null) {
                binding.ivImage.setImageURI(uri)

            } else {
                Toast.makeText(this, "PhotoPicker , No media selected", Toast.LENGTH_SHORT).show()

            }
        }

        // Camera activity Launcher
        takePhotoLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
                if (bitmap != null) {
                    binding.ivImage.setImageBitmap(bitmap)
                    saveImageToExternalStorage(bitmap)

                }
            }

    }

    private fun lauchCamera() {
        takePhotoLauncher.launch(null)
    }
    private fun pickImageFromGallery() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    }

    //Save Image to External Storage
    private fun saveImageToExternalStorage(bitmap: Bitmap){
        val imageCollection = if (SDK_28_ABOVE) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val fileName = "my_image_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+ File.separator+"Test Storage")

        }
        try {
            contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if(!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                        throw IOException("Couldn't save bitmap")
                    }
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show()

        } catch(e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to Save Image", Toast.LENGTH_SHORT).show()
        }
    }





    private fun getReadPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            READ_EXTERNAL_STORAGE_REQUEST_CODE
        )
    }

    private fun getWritePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            WRITE_EXTERNAL_STORAGE_REQUEST_CODE
        )
    }


    private fun checkReadStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun checkWriteStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    }





    // Save the image to external storage
    private fun saveImageToExternalSt(bitmap: Bitmap) {
        // Get the directory for storing images
        val imagesDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        val fileName = "my_image_${System.currentTimeMillis()}.jpg"
        val imageFile = File(imagesDirectory, fileName)
        var outputStream: OutputStream? = null
        try {
            outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            // Flush the stream
            outputStream.flush()

        } catch (e: Exception) {
            e.printStackTrace()

        } finally {
            // Close the output stream
            outputStream?.close()
        }

        // Add the image to the gallery (optional)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+ File.separator+"Test Storage")
            }

          //  contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show()

        } else {
            // For devices below Android 10, manually scan the saved image file to make it available in the gallery
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(imageFile)
            mediaScanIntent.data = contentUri
            sendBroadcast(mediaScanIntent)
            Toast.makeText(this, "Image Saved old", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to pick image from gallery
                pickImageFromGallery()
            } else {
                Toast.makeText(this, "Unable to Load Image", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "Please Give Storage Persmission", Toast.LENGTH_SHORT).show()
            }
        }
    }


}
