package com.pradeep.sensordata.utils

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

object Utils {
 fun  getCurrentDateTime():String{
     val simpleDateFormat =  SimpleDateFormat("yyyy.MM.dd.HH.mm.ss",Locale.ENGLISH)
     simpleDateFormat.timeZone= TimeZone.getTimeZone("UTC")
     return  simpleDateFormat.format(Date())
 }

}

//Kotlin timer extension
fun CoroutineScope.launchPeriodicAsync(
    repeatMillis: Long,
    action: () -> Unit
) = this.async {
    if (repeatMillis > 0) {
        while (isActive) {
            action()
            delay(repeatMillis)
        }
    } else {
        action()
    }
}

fun Context.isMyServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return manager.getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == serviceClass.name }
}