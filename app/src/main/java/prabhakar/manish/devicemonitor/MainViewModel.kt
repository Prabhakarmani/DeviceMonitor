package prabhakar.manish.devicemonitor

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application)
{

    private val _showPopup = MutableLiveData<Boolean>()
    val showPopup: LiveData<Boolean> = _showPopup

    val numberInput = MutableLiveData<String>().apply {
        value = "15" // Set the default value here
    }

    fun showPopup() {
        _showPopup.value = true
    }

    private val _timestamp = MutableLiveData<String>()
    private val _captureCount = MutableLiveData<Int>()
    val captureCount: LiveData<Int> = _captureCount

    private val _isConnected = MutableLiveData<Boolean>()
    private val _isCharging = MutableLiveData<Boolean>()
    private var batteryStatusReceiver1: BroadcastReceiver? = null
    private val _batteryPercentage = MutableLiveData<String>()
    private val context = application.applicationContext
    private val batteryStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateBatteryPercentage(intent)
        }
    }

    private val _locationData = MutableLiveData<Pair<Double, Double>>()
    val locationData: LiveData<Pair<Double, Double>> = _locationData

    val timestamp: LiveData<String>
        get() = _timestamp

    val isConnected: LiveData<Boolean>
        get() = _isConnected

    val isCharging: LiveData<Boolean>
        get() = _isCharging

    val batteryPercentage: LiveData<String>
        get() = _batteryPercentage

    init {
        updateTimestamp()
        _captureCount.value = 0
        _isConnected.value = isInternetConnected()
        _isCharging.value = false
        initBatteryStatusReceiver()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryStatusReceiver, filter)
    }

    fun updateTimestamp() {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        _timestamp.value = currentTime
    }

    fun incrementCaptureCount() {
        val currentCount = _captureCount.value ?: 0
        _captureCount.value = currentCount + 1
    }

    private fun isInternetConnected(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

    private fun initBatteryStatusReceiver() {
        batteryStatusReceiver1 = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val status = intent.getIntExtra("status", -1)
                    val isCharging = status == 2 || status == 5
                    _isCharging.value = isCharging
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        getApplication<Application>().registerReceiver(batteryStatusReceiver1, filter)
    }

    private fun updateBatteryPercentage(intent: Intent) {
        val level = intent.getIntExtra("level", 0)
        val scale = intent.getIntExtra("scale", 100)
        val percentage = (level.toFloat() / scale * 100).toInt()
        _batteryPercentage.value = context.getString(R.string.battery_percentage, percentage)
    }

    fun setLocation(latitude: Double, longitude: Double) {
        _locationData.value = Pair(latitude, longitude)
    }

    fun updateConnectivityStatus() {
        _isConnected.value = isInternetConnected()
    }
    fun updateBatteryChargingStatus() {
        val batteryStatusIntent = getApplication<Application>().registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val status = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        _isCharging.value = isCharging
    }

    fun updateBatteryPercentage() {
        val batteryStatusIntent = getApplication<Application>().registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        if (level != null && scale != null && level != -1 && scale != -1) {
            val percentage = (level.toFloat() / scale.toFloat() * 100).toInt()
            _batteryPercentage.value = "$percentage%"
        } else {
            _batteryPercentage.value = "N/A"
        }
    }

    fun refreshbutton() {
        incrementCaptureCount()
        updateConnectivityStatus()
        updateBatteryChargingStatus()
        updateBatteryPercentage()
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(batteryStatusReceiver1)
        context.unregisterReceiver(batteryStatusReceiver)
    }
}