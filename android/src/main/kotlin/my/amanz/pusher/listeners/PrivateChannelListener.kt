package my.amanz.pusher.listeners

import com.pusher.client.channel.PrivateChannelEventListener
import java.lang.Exception

class PrivateChannelListener(instanceID: String, isLoggingEnabled: Boolean = false): EventChannelListener(instanceID, isLoggingEnabled), PrivateChannelEventListener {

    override fun onSubscriptionSucceeded(channelName: String?) {
        this.onEvent(toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT, null, null));
    }

    override fun onAuthenticationFailure(message: String?, e: Exception?) {
        onError(e)
    }

}
