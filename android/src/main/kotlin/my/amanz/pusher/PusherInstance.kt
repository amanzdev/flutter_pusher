package my.amanz.pusher

import android.os.Handler
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.Channel
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionStateChange
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import my.amanz.pusher.listeners.EventChannelListener
import my.amanz.pusher.listeners.PresenceChannelListener
import my.amanz.pusher.listeners.PrivateChannelListener
import org.json.JSONObject
import com.pusher.client.util.HttpAuthorizer
import com.pusher.client.util.UrlEncodedConnectionFactory
import com.pusher.client.util.ConnectionFactory
import java.lang.Exception
import android.os.Looper
import com.pusher.client.connection.ConnectionState
import my.amanz.pusher.PusherPlugin.Companion.eventSink


class PusherInstance(private var instanceID: String): MethodChannel.MethodCallHandler {
    private var pusher : Pusher? = null
    private var isLoggingEnabled : Boolean = false
    private val channels : MutableMap<String, Channel> = mutableMapOf()

    private lateinit var eventListener : EventChannelListener
    private lateinit var privateChannelListener : PrivateChannelListener
    private lateinit var presenceChannelListener : PresenceChannelListener

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> {
                init(call, result)
            }
            "connect" -> {
                connect(call, result)
            }
            "disconnect" -> {
                disconnect(call, result)
            }
            "subscribe" -> {
                subscribe(call, result)
            }
            "unsubscribe" -> {
                unsubscribe(call, result)
            }
            "bind" -> {
                bind(call, result)
            }
            "unbind" -> {
                unbind(call, result)
            }
            "trigger" -> {

            }
            "getSocketID" -> {
                getSocketId(call, result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun initListeners() {
        eventListener = EventChannelListener(instanceID, isLoggingEnabled)
        privateChannelListener = PrivateChannelListener(instanceID, isLoggingEnabled)
        presenceChannelListener = PresenceChannelListener(instanceID, isLoggingEnabled)
    }

    private fun init(call: MethodCall, result: MethodChannel.Result) {
        if(pusher != null) {
            for ((name) in channels) {
                pusher!!.unsubscribe(name)
                channels.remove(name)
            }
        }

        try {
            val json = JSONObject(call.arguments.toString())
            val options = json.getJSONObject("options")

            if(json.has("isLoggingEnabled")) {
                isLoggingEnabled = json.getBoolean("isLoggingEnabled")
            }

            val pusherOptions = PusherOptions()

            if(options.has("auth")) {
                val auth = options.getJSONObject("auth")
                val endpoint = auth.getString("endpoint")
                val mapType = object : TypeToken<Map<String, String>>() {}.type
                val headers = Gson().fromJson<Map<String, String>>(auth.get("headers").toString(), mapType)

                pusherOptions.authorizer = getAuthorizer(endpoint, headers)
            }

            if (options.has("activityTimeout")) {
                pusherOptions.activityTimeout = options.getInt("activityTimeout").toLong()
            }
            if (options.has("cluster")) {
                pusherOptions.setCluster(options.getString("cluster"))
            }
            if (options.has("host")) {
                pusherOptions.setHost(options.getString("host"))
            }

            // defaults to encrypted connection on port 443
            val port = if (options.has("port")) options.getInt("port") else 443
            val encrypted = !options.has("encrypted") || options.getBoolean("encrypted")

            if (encrypted) {
                pusherOptions.setWssPort(port)
            } else {
                pusherOptions.setWsPort(port)
            }
            pusherOptions.isUseTLS = encrypted

            // create client
            pusher = Pusher(json.getString("appKey"), pusherOptions)
            initListeners()

            if (isLoggingEnabled) {
                Log.d("PUSHER", "init")
            }

            result.success(null)
        } catch (e: Exception) {
            if (isLoggingEnabled) {
                Log.d("PUSHER", "init error: " + e.message)
                e.printStackTrace()
            }
        }
    }

    private fun connect(call: MethodCall, result: MethodChannel.Result) {
        pusher!!.connect(object : ConnectionEventListener {
            override fun onConnectionStateChange(change: ConnectionStateChange?) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        val eventStreamMessageJson = JSONObject()
                        val connectionStateChangeJson = JSONObject()
                        connectionStateChangeJson.put("currentState", change!!.currentState.toString())
                        connectionStateChangeJson.put("previousState", change.previousState.toString())
                        eventStreamMessageJson.put("connectionStateChange", connectionStateChangeJson)
                        eventStreamMessageJson.put("instanceId", instanceID)

                        eventSink?.success(eventStreamMessageJson.toString())

                    } catch (e: Exception) {
                        if (isLoggingEnabled) {
                            Log.d("PUSHER", "onConnectionStateChange error: " + e.message)
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onError(message: String?, code: String?, e: Exception?) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        val exMessage: String? = e?.message
                        val eventStreamMessageJson = JSONObject()
                        val connectionErrorJson = JSONObject()
                        connectionErrorJson.put("instanceId", instanceID)
                        connectionErrorJson.put("message", message)
                        connectionErrorJson.put("code", code)
                        connectionErrorJson.put("exception", exMessage)
                        eventStreamMessageJson.put("connectionError", connectionErrorJson)
                        eventStreamMessageJson.put("instanceId", instanceID)

                        eventSink!!.success(eventStreamMessageJson.toString())

                    } catch (e: Exception) {
                        if (isLoggingEnabled) {
                            Log.d("PUSHER", "onError exception: " + e.message)
                            e.printStackTrace()
                        }
                    }
                }
            }
        }, ConnectionState.ALL)

        if (isLoggingEnabled) {
            Log.d("PUSHER", "connect")
        }

        result.success(null)
    }

    private fun getSocketId(call: MethodCall, result: MethodChannel.Result) {
        result.success(pusher!!.connection.socketId)
    }

    private fun disconnect(call: MethodCall, result: MethodChannel.Result) {
        pusher!!.disconnect()
        if (isLoggingEnabled) {
            Log.d("PUSHER", "disconnect")
        }
        result.success(null)
    }

    private fun subscribe(call: MethodCall, result: MethodChannel.Result) {
        try {
            val json = JSONObject(call.arguments.toString())
            val channelName = json.getString("channelName")
            val channelType = channelName.split("-").toTypedArray()[0]
            var channel = channels[channelName]

            if (channel != null && channel.isSubscribed) {
                if (isLoggingEnabled) {
                    Log.d("PUSHER", "Already subscribed, ignoring ...")
                }
                result.success(null)
                return
            }

            when (channelType) {
                "private" -> {
                    channel = pusher!!.subscribePrivate(channelName, privateChannelListener)
                    if (isLoggingEnabled) {
                        Log.d("PUSHER", "subscribe (private)")
                    }
                }
                "presence" -> {
                    channel = pusher!!.subscribePresence(channelName, presenceChannelListener)
                    if (isLoggingEnabled) {
                        Log.d("PUSHER", "subscribe (presence)")
                    }
                }
                else -> {
                    channel = pusher!!.subscribe(channelName, eventListener)
                    if (isLoggingEnabled) {
                        Log.d("PUSHER", "subscribe")
                    }
                }
            }
            channels[channelName] = channel!!
            result.success(null)
        } catch (e: Exception) {
            if (isLoggingEnabled) {
                Log.d("PUSHER", "subscribe error: " + e.message)
                e.printStackTrace()
            }
        }
    }

    private fun unsubscribe(call: MethodCall, result: MethodChannel.Result) {
        try {
            val json = JSONObject(call.arguments.toString())
            val channelName = json.getString("channelName")
            pusher!!.unsubscribe(channelName)
            channels.remove(channelName)
            if (isLoggingEnabled) {
                Log.d("PUSHER", String.format("unsubscribe (%s)", channelName))
            }
            result.success(null)
        } catch (e: Exception) {
            if (isLoggingEnabled) {
                Log.d("PUSHER", "unsubscribe error: " + e.message)
                e.printStackTrace()
            }
        }
    }

    private fun bind(call: MethodCall, result: MethodChannel.Result) {
        try {
            val json = JSONObject(call.arguments.toString())
            val channelName = json.getString("channelName")
            val channelType = channelName.split("-").toTypedArray()[0]
            val eventName = json.getString("eventName")
            val channel = channels[channelName]
            when (channelType) {
                "private" -> channel!!.bind(eventName, privateChannelListener)
                "presence" -> channel!!.bind(eventName, presenceChannelListener)
                else -> channel!!.bind(eventName, eventListener)
            }
            if (isLoggingEnabled) {
                Log.d("PUSHER", String.format("bind (%s)", eventName))
            }
            result.success(null)
        } catch (e: Exception) {
            if (isLoggingEnabled) {
                Log.d("PUSHER", String.format("bind exception: %s", e.message))
                e.printStackTrace()
            }
        }
    }

    private fun unbind(call: MethodCall, result: MethodChannel.Result) {
        try {
            val json = JSONObject(call.arguments.toString())
            val channelName = json.getString("channelName")
            val channelType = channelName.split("-").toTypedArray()[0]
            val eventName = json.getString("eventName")
            val channel = channels[channelName]
            when (channelType) {
                "private" -> channel!!.unbind(eventName, privateChannelListener)
                "presence" -> channel!!.unbind(eventName, presenceChannelListener)
                else -> channel!!.unbind(eventName, eventListener)
            }
            if (isLoggingEnabled) {
                Log.d("PUSHER", String.format("unbind (%s)", eventName))
            }
            result.success(null)
        } catch (e: Exception) {
            if (isLoggingEnabled) {
                Log.d("PUSHER", String.format("unbind exception: %s", e.message))
                e.printStackTrace()
            }
        }
    }

    private fun getAuthorizer(endpoint: String, headers: Map<String, String>): HttpAuthorizer {
        val connection: ConnectionFactory = if (headers.containsValue("application/json")) JsonEncodedConnectionFactory() else UrlEncodedConnectionFactory()
        val authorizer = HttpAuthorizer(endpoint, connection)
        authorizer.setHeaders(headers)
        return authorizer
    }
}