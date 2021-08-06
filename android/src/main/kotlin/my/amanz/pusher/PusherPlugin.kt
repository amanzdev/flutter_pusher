package my.amanz.pusher

import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import my.amanz.pusher.platform_messages.InstanceMessage
import java.lang.IllegalArgumentException

/** PusherPlugin */
class PusherPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var eventStream : EventChannel
  private val instanceMap : MutableMap<String, PusherInstance> = mutableMapOf()

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "pusher")
    eventStream = EventChannel(flutterPluginBinding.binaryMessenger, "pusherStream")

    channel.setMethodCallHandler(this)
    eventStream.setStreamHandler(object : EventChannel.StreamHandler {
      override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
      }

      override fun onCancel(arguments: Any?) {
        Log.d("PUSHER", String.format("onCancel args: %s", arguments?.toString() ?: "null"))
      }
    })
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    val type = object : TypeToken<InstanceMessage>() {}.type
    val instanceMessage = Gson().fromJson<InstanceMessage>(call.arguments.toString(), type)
    val instanceID = instanceMessage.getInstanceID()
    val instance = getPusherInstance(instanceID)

    if(instance == null) {
      val message = String.format("Pusher instance with id %s is not found", instanceID)
      throw IllegalArgumentException(message)
    }

    instance.onMethodCall(call, result)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private fun getPusherInstance(instanceID : String) : PusherInstance? {
    if(!instanceMap.containsKey(instanceID)) {
      instanceMap[instanceID] = PusherInstance(instanceID)
    }

    return instanceMap[instanceID]
  }

  companion object {
    var eventSink : EventChannel.EventSink? = null
  }
}
