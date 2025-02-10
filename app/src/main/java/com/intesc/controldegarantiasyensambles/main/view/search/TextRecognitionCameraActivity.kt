package com.intesc.controldegarantiasyensambles.main.view.search

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.intesc.controldegarantiasyensambles.databinding.ActivityTextRecognitionCameraBinding
import timber.log.Timber
import java.io.File

class TextRecognitionCameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextRecognitionCameraBinding
    private val cameraPermissionCode = 93426
    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextRecognitionCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkCameraPermission()
        binding.fabTakePhoto.setOnClickListener { takePhoto() }
    }

    //Method for check camera permission
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                cameraPermissionCode
            )
        }
    }

    //Method for manage result from checkCameraPermission method
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                showToast("Debe permitir el acceso a la cámara")
                finish()
            }
        }
    }

    //method for staring camera
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.pvRecognitionText.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this as androidx.lifecycle.LifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(this))
    }

    //Method for take a photo using floating action button
    private fun takePhoto() {
        val photoFile = File(this.externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    runTextRecognition(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Timber.e("CameraTextRecognizer", "Error al capturar la imagen", exception)
                }
            })
    }

    //Method for text recognition in photography
    private fun runTextRecognition(imageFile: File) {
        val image = InputImage.fromFilePath(this, Uri.fromFile(imageFile))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val recognizedText = visionText.text
                if (recognizedText.isNotEmpty()) {
                    returnRecognizedTextToSearchActivity(recognizedText)
                } else {
                    showToast("Error al reconocer el texto")
                }
                deleteImage(imageFile)
            }
            .addOnFailureListener { e ->
                Timber.e("CameraTextRecognizer", "Error al reconocer el texto", e)
                showToast("Error al reconocer el texto")
                deleteImage(imageFile)
            }
    }

    //Method for delete image after text recognition
    private fun deleteImage(imageFile: File) {
        if (imageFile.exists()) {
            imageFile.delete()
        }
    }

    //Method to return the search activity and send the text
    private fun returnRecognizedTextToSearchActivity(recognizedText: String) {
        val resultIntent = Intent()
        resultIntent.putExtra("recognizedText", recognizedText)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    //Method for simplify Toast creation
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
