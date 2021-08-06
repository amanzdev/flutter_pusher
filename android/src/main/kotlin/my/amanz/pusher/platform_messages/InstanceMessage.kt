package my.amanz.pusher.platform_messages

class InstanceMessage {
    private lateinit var instanceID : String

    fun getInstanceID() : String {
        return instanceID
    }

    fun setInstanceID(instanceID : String) {
        this.instanceID = instanceID
    }
}