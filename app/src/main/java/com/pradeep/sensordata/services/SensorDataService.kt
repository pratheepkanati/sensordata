package com.pradeep.sensordata.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.github.doyaaaaaken.kotlincsv.client.CsvFileWriter
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.pradeep.sensordata.R
import com.pradeep.sensordata.utils.Utils
import com.pradeep.sensordata.utils.launchPeriodicAsync
import kotlinx.coroutines.*
import java.io.File

//A started service can use the startForeground API to put the service in a foreground state, where the system considers it to be something the user is actively aware of and thus not a candidate for killing when low on memory. By default services are background, meaning that if the system needs to kill them to reclaim more memory (such as to display a large page in a web browser), they can be killed without too much harm
class SensorDataService : Service(), SensorEventListener {
    val SENSOR_SERVICE = "sensor_service"
    val SERVICE_ID=10
    lateinit var  sensorManager: SensorManager
     val accelerometerValues = FloatArray(3)
     val gyroScopeValues = FloatArray(3)
    lateinit var timer:Job
    lateinit var csvFileWriter: CsvFileWriter
    // Binder given to clients
    private val binder = LocalBinder()
    var callback: ((FloatArray, FloatArray) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        // for long running service  w
         createNotification()
        //SensorManager lets you access the device's sensors.
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        //Registering to Gyroscopic sensor with fastest interval
        // Measures a device's rate of rotation in rad/s around each of the three physical axes (x, y, and z).
        sensorManager.registerListener(this, sensorManager.getDefaultSensor
                (Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);


        //Registering to accelerometer sensor with fastest interval
        //Measures the acceleration force in m/s2 that is applied to a device on all three physical axes (x, y, and z), including the force of gravity.
        sensorManager.registerListener(this, sensorManager.getDefaultSensor
                (Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST)

        timer = CoroutineScope(Dispatchers.IO).launchPeriodicAsync(1000) {
            Log.v("SensorData",gyroScopeValues.joinToString())
            Log.v("SensorData",accelerometerValues.joinToString())
            if(!(::csvFileWriter.isInitialized)){
                val file = File(applicationContext.getFilesDir().toString() + "/CSVFiles/sensorsData.csv")
                var newFile=false
                if(!file.exists()){
                   file.parentFile?.mkdirs()
                   file.createNewFile()
                   newFile=true
                }
                @OptIn(KotlinCsvExperimental::class)
                csvFileWriter = csvWriter().openAndGetRawWriter(file)
                if(newFile){
                    //CSV Header
                    val header = listOf("datetime","acc_X","acc_Y", "acc_Z","gyro_X","gyro_Y","gyro_Z")
                    // adding data to file
                    csvFileWriter.writeRow(header)
                }

            }
            appendDataToCSV()
        }


    }
     private fun appendDataToCSV(){
         val row1 = listOf(Utils.getCurrentDateTime(),accelerometerValues[0],accelerometerValues[1], accelerometerValues[2],gyroScopeValues[0],gyroScopeValues[1], gyroScopeValues[2])
         // adding data to csv file
         csvFileWriter.writeRow(row1)
         callback?.let { it(accelerometerValues,gyroScopeValues) }
     }




    override fun onBind(intent: Intent): IBinder {
        return  binder
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        if((::csvFileWriter.isInitialized)){
            csvFileWriter.close()
        }
        sensorManager.unregisterListener(this)

    }

    override fun onSensorChanged(event: SensorEvent) {

        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER)
        {
           accelerometerValues[0]=event.values[0]
           accelerometerValues[1]=event.values[1]
           accelerometerValues[2]=event.values[2]
        }
        else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE)
        {
            gyroScopeValues[0]=event.values[0]
            gyroScopeValues[1]=event.values[1]
            gyroScopeValues[2]=event.values[2]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do something here if sensor accuracy changes.

    }






    private fun createNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            creteNotificationChannel()
        }
        val  builder = getNotification()

        showNotification(builder)
    }

    private fun showNotification(builder: NotificationCompat.Builder){
       startForeground(SERVICE_ID, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun creteNotificationChannel() {
        val chan = NotificationChannel(SENSOR_SERVICE, "Sensor Service", NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
    }

    private fun getNotification(): NotificationCompat.Builder {
        val  builder = NotificationCompat.Builder(this, SENSOR_SERVICE)
            .setContentTitle("Sensor Service")
            .setContentText("Collecting...")
            .setSmallIcon(R.drawable.ic_baseline_edgesensor_high_24)
        return builder
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): SensorDataService = this@SensorDataService
    }


}
