package com.example.scopedstorage_crud

import android.Manifest
import android.annotation.SuppressLint
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.scopedstorage_crud.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val SDK_28_ABOVE = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q

    // Reqesut Codes
    private val READ_EXTERNAL_STORAGE_REQUEST_CODE = 1
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 2
    private val DELETE_EXTERNAL_STORAGE_REQUEST_CODE = 3

    private val PERMISSION_REQUEST_CODE = 201

    // Launcher variables
    private lateinit var pickImage: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickImageAndDelete: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Void?>

    private var permissionsFlag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initLaunchers()

        //
        if(checkPermissions()){
            permissionsFlag = true
        }
        else{
            getPermissions()
        }

        binding.btnRead.setOnClickListener {
            eventLaunch(this::pickImageFromGallery)
           // eventLaunch { pickImageFromGallery() }
        }

        binding.btnWrite.setOnClickListener {
            eventLaunch(this::lauchCamera)
        }

        binding.btnDelete.setOnClickListener {
            eventLaunch(this::pickImageForDeletion)
        }
    }
    private fun eventLaunch(event:() -> Unit){
        if (permissionsFlag) {
            event()
        }
        else{
            getPermissions()
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
        pickImageAndDelete =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                   deleteImageFromExternalStorage(uri)

                } else {
                    Toast.makeText(this, "PhotoPicker , No media selected", Toast.LENGTH_SHORT)
                        .show()

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

    private fun pickImageFromGallery() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    }
    private fun lauchCamera() {
        takePhotoLauncher.launch(null)
    }
    private fun pickImageForDeletion() {
        pickImageAndDelete.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    //Save Image to External Storage
    private fun saveImageToExternalStorage(bitmap: Bitmap) {
        if(SDK_28_ABOVE) {
            val imageCollection =
                if (SDK_28_ABOVE) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val fileName = "my_image_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + File.separator + "Test Storage"
                )

            }
            try {
                contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                    contentResolver.openOutputStream(uri).use { outputStream ->
                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                            throw IOException("Couldn't save bitmap")
                        }
                    }
                } ?: throw IOException("Couldn't create MediaStore entry")
                Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show()

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Unable to Save Image", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            saveImageToStorageForOldVersion(bitmap)
        }
    }
    @SuppressLint("Range")
    private fun deleteImageFromExternalStorage(uri: Uri){
            val cursor = this.contentResolver.query(uri,null,null,null,null)
            try {
                if(cursor!=null && cursor.moveToFirst()){
                    DocumentsContract.deleteDocument(this.contentResolver,uri)
                }
                Toast.makeText(this,"Image Deleted Successfully",Toast.LENGTH_SHORT).show()
            }catch (e:Exception){
                e.printStackTrace()
                Toast.makeText(this,"Unable to Delete Image",Toast.LENGTH_SHORT).show()
            }finally {
                cursor?.close()
            }
    }

    // Permissions Methods
    private fun getPermissions(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }
    private fun checkPermissions(): Boolean {
        val readPermission =  ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val writePermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val check = readPermission && writePermission
       return check
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST_CODE){
            if (grantResults.size > 1 ) {
                val READ_EXTERNAL_STORAGE = grantResults[0] === PackageManager.PERMISSION_GRANTED
                val WRITE_EXTERNAL_STORAGE = grantResults[1] === PackageManager.PERMISSION_GRANTED
                if(READ_EXTERNAL_STORAGE && WRITE_EXTERNAL_STORAGE){
                    permissionsFlag = true
                }
                else {
                    Toast.makeText(this, "Please Give Storage Persmission", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "size is smaller than 1", Toast.LENGTH_SHORT).show()
            }
        }

    }
    private fun saveImageToStorageForOldVersion(bitmap: Bitmap){
        // If sdk is below 28
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString()
        val myDir = File("$root/saved_images")
        myDir.mkdirs()
        val generator = Random()
        var n = 10000
        n = generator.nextInt(n)
        val fname = "Image-$n.jpg"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        MediaScannerConnection.scanFile(
            this, arrayOf<String>(file.toString()), null
        ) { path, uri ->
            Log.i("ExternalStorage", "Scanned $path:")
            Log.i("ExternalStorage", "-> uri=$uri")
        }
    }

}
