package my.amanz.pusher

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.pusher.client.util.ConnectionFactory

class JsonEncodedConnectionFactory : ConnectionFactory() {
    override fun getBody(): String {
        val payload = JsonObject()
        payload.add("channel_name", JsonPrimitive(channelName))
        payload.add("socket_id", JsonPrimitive(socketId))

        return payload.toString()
    }

    override fun getCharset(): String {
        return "UTF-8"
    }

    override fun getContentType(): String {
        return "application/json"
    }

}
