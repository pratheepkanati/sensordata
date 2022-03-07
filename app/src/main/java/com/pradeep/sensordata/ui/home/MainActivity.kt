package com.pradeep.sensordata.ui.home

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.google.android.material.snackbar.Snackbar
import com.pradeep.sensordata.databinding.ActivityMainBinding
import com.pradeep.sensordata.services.SensorDataService
import com.pradeep.sensordata.utils.isMyServiceRunning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.lang.Exception
import android.widget.Toast

import java.io.File

import android.os.Environment
import androidx.core.content.FileProvider
import com.pradeep.sensordata.BuildConfig
import java.util.ArrayList





class MainActivity : AppCompatActivity() {


    lateinit var binding: ActivityMainBinding
    private var requestUri = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result != null && result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                intent.data?.let { fileUri ->
                  lifecycle.coroutineScope.launch {
                      writeFileContent(fileUri)
                  }
                } ?: run {
                    Snackbar.make(binding.root,"Failed to save csv", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initClickListeners()
        if(isMyServiceRunning(SensorDataService::class.java)){
            binding.btnStartService.setText("Stop Sensor Data Collection")
        }
    }

    private fun initClickListeners(){
        binding.btnStartService.setOnClickListener {
            if(!isMyServiceRunning(SensorDataService::class.java)){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startService(Intent(this,SensorDataService::class.java))
                }
                else{
                    applicationContext.startService(Intent(this,SensorDataService::class.java))
                }
                binding.btnStartService.setText("Stop Sensor Data Collection")
                Snackbar.make(binding.root,"Service Started", Snackbar.LENGTH_SHORT).show()
            }
            else{
                applicationContext.stopService(Intent(this,SensorDataService::class.java))
                binding.btnStartService.setText("Start Sensor Data Collection")
                Snackbar.make(binding.root,"Service Stopped", Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.btnClearCsv.setOnClickListener {
            if(isMyServiceRunning(SensorDataService::class.java)){
                binding.btnStartService.performClick()
            }
            val file = File(applicationContext.getFilesDir().toString() + "/CSVFiles/sensorsData.csv")
            if(file.exists()){
                file.delete()
            }
            Snackbar.make(binding.root,"CSV File Cleared", Snackbar.LENGTH_SHORT).show()
        }
        binding.btnDownloadCsv.setOnClickListener {
            val file = File(applicationContext.getFilesDir().toString() + "/CSVFiles/sensorsData.csv")
            if(file.exists()){
                createFile()
            }
            else{
                Snackbar.make(binding.root,"No csv file found collect sensor data first", Snackbar.LENGTH_SHORT).show()
            }

        }
        binding.btnShareCsv.setOnClickListener {
            val uris = ArrayList<Uri>()
            val file = File(applicationContext.getFilesDir().toString() + "/CSVFiles/sensorsData.csv")
            if(file.exists()){
                val uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
                )
                uris.add(uri)
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                //intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("email@example.com"))
                intent.putExtra(Intent.EXTRA_SUBJECT, "Sensor data csv file")
                intent.putExtra(Intent.EXTRA_TEXT, "Here you find the Accelerometer and Gyroscopic sensor data Csv File")
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(Intent.createChooser(intent, "Share File.."))
            }
            else{
                Snackbar.make(binding.root,"No csv file found collect sensor data first", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    private fun createFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "sensorsData.csv")

        }
        requestUri.launch(intent)
    }
    private suspend fun writeFileContent(uri: Uri) {
        val dialog = ProgressDialog(this)
        dialog.setMessage("Saving File..")
        dialog.setCancelable(false)
        withContext(Dispatchers.IO){
            val csvFile = File(applicationContext.getFilesDir().toString() + "/CSVFiles/sensorsData.csv")
            try {
                FileInputStream(csvFile).use { inputStream ->
                    contentResolver.openFileDescriptor(uri, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use { outputStream ->
                            val buf = ByteArray(1024)
                            var len: Int
                            while (inputStream.read(buf).also { len = it } > 0) {
                                outputStream.write(buf, 0, len)
                            }
                            inputStream.close()
                            outputStream.close()
                            withContext(Dispatchers.Main){
                                dialog.dismiss()
                                Snackbar.make(binding.root,"Saved Successfully", Snackbar.LENGTH_SHORT)
                                    .setAction("open"){
                                        val intent = Intent(Intent.ACTION_EDIT)
                                        intent.setDataAndType(uri, "text/plain")
                                        startActivity(intent)
                                    }
                                    .show()
                            }
                        }
                    }
                }
            }
            catch (ex:Exception){
                 ex.printStackTrace()
                withContext(Dispatchers.Main){
                    dialog.dismiss()
                    Snackbar.make(binding.root,"Failed to save", Snackbar.LENGTH_SHORT).show()
                }
            }
        }


    }
}