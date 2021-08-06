package my.amanz.pusher.listeners

import com.pusher.client.channel.PresenceChannelEventListener
import com.pusher.client.channel.User
import java.lang.Exception

class PresenceChannelListener(instanceID: String, isLoggingEnabled: Boolean = false): EventChannelListener(instanceID, isLoggingEnabled), PresenceChannelEventListener {
    override fun onSubscriptionSucceeded(channelName: String?) {
        this.onEvent(toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT, null, null))
    }

    override fun onAuthenticationFailure(message: String?, e: Exception?) {
        onError(e)
    }

    override fun onUsersInformationReceived(channelName: String?, users: MutableSet<User>?) {
        this.onEvent(toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT, null, users.toString()))
    }

    override fun userSubscribed(channelName: String?, user: User?) {
        this.onEvent(toPusherEvent(channelName, MEMBER_ADDED_EVENT, user?.id, null))
    }

    override fun userUnsubscribed(channelName: String?, user: User?) {
        this.onEvent(toPusherEvent(channelName, MEMBER_REMOVED_EVENT, user?.id, null))
    }

}
