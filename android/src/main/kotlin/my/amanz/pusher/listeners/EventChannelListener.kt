package my.amanz.pusher.listeners

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pusher.client.channel.ChannelEventListener
import com.pusher.client.channel.PusherEvent
import my.amanz.pusher.PusherPlugin.Companion.eventSink
import java.lang.Exception
import org.json.JSONObject

open class EventChannelListener(private val instanceID: String, private val isLoggingEnabled: Boolean = false) : ChannelEventListener {

    override fun onEvent(pusherEvent: PusherEvent?) {
        Handler(Looper.getMainLooper()).post {
            try {
                val eventStreamMessageJson = JSONObject()
                val eventJson = JSONObject()
                val channel: String = pusherEvent!!.channelName
                val event: String = pusherEvent.eventName
                val data: String = pusherEvent.data

                eventJson.put("channel", channel)
                eventJson.put("event", event)
                eventJson.put("data", data)
                eventStreamMessageJson.put("isEvent", true)
                eventStreamMessageJson.put("event", eventJson)
                eventStreamMessageJson.put("instanceId", instanceID)

                eventSink?.success(eventStreamMessageJson.toString())

                if (isLoggingEnabled) {
                    Log.d("PUSHER", String.format("onEvent: \nCHANNEL: %s \nEVENT: %s \nDATA: %s", channel, event, data))
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    override fun onSubscriptionSucceeded(channelName: String?) {
        this.onEvent(toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT, null, null))
    }

    fun onError(e: Exception?) {
        Handler(Looper.getMainLooper()).post {
            try {
                val eventStreamMessageJson = JSONObject()
                val connectionErrorJson = JSONObject()
                connectionErrorJson.put("message", e?.message)
                connectionErrorJson.put("code", "Channel error")
                connectionErrorJson.put("exception", e)
                eventStreamMessageJson.put("connectionError", connectionErrorJson)
                eventStreamMessageJson.put("instanceId", instanceID)

                eventSink!!.success(eventStreamMessageJson.toString())

                if (isLoggingEnabled) {
                    Log.d("PUSHER", "onError : " + e?.message)
                    e?.printStackTrace()
                }

            } catch (ex: Exception) {
                if (isLoggingEnabled) {
                    Log.d("PUSHER", "onError exception: " + e?.message)
                    ex.printStackTrace()
                }
            }
        }
    }

    companion object {
        const val SUBSCRIPTION_SUCCESS_EVENT = "pusher:subscription_succeeded"
        const val MEMBER_ADDED_EVENT = "pusher:member_added"
        const val MEMBER_REMOVED_EVENT = "pusher:member_removed"

        fun toPusherEvent(channel: String?, event: String, userId: String?, data: String?): PusherEvent {
            val eventData : MutableMap<String, Any?> = mutableMapOf()

            eventData["channel"] = channel
            eventData["event"] = event
            eventData["data"] = data ?: ""

            if(userId != null) {
                eventData["user_id"] = userId
            }

            return PusherEvent(eventData)
        }
    }
}
