package com.example.whatsappclone.ui.call

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.voxeet.sdk.VoxeetSdk
import com.voxeet.sdk.push.center.invitation.InvitationBundle
import com.voxeet.sdk.utils.AndroidManifest
import com.example.whatsappclone.incoming.IncomingNotification
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class CallActivity : AppCompatActivity() {


    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cancel your ongoing Notification
        val notificationId = intent.getIntExtra(IncomingNotification.EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            val notificationManager =  this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
            notificationManager!!.cancel(notificationId)
        }

        setContentView(com.example.whatsappclone.R.layout.activity_call)

        // Register the current activity in the Voxeet SDK
        VoxeetSdk.instance()!!.register(this)

        val channelId = getChannelId(this@CallActivity)
        val extras = intent.extras
        val conferenceId : String?
        if (extras != null) {
            conferenceId = extras.getString("ConfId")
        }
        else
        {
            conferenceId = "dev-portal"
        }
        val isVideoEnabled = intent.getBooleanExtra("isVideoEnabled", true)

        println("CallActivity found conferenceId:" + conferenceId)
        if(conferenceId==null) {
            return;
        }

        // This will pass the parameter to the fragment
        val fragment: CallView = CallView.newInstance(conferenceId, isVideoEnabled)

        val fm: FragmentManager = supportFragmentManager
        if (savedInstanceState == null) {

            supportFragmentManager.beginTransaction()
                .replace(com.example.whatsappclone.R.id.call_fragment_container, fragment)
                .commitNow()
        }

    }

    override fun onDestroy() {

        // Register the current activity in the Voxeet SDK
        VoxeetSdk.instance()!!.unregister(this)
        // Close user session
        super.onDestroy()
    }

    companion object {

        public fun createVideoIntent(
            notificationId: Int,
            context: Context,
            invitationBundle: InvitationBundle
        ): PendingIntent? {
            val extras = invitationBundle.asBundle()

            val notificationIntent = Intent(context, CallActivity::class.java)

            notificationIntent.putExtras(extras)
            notificationIntent.putExtra(IncomingNotification.EXTRA_NOTIFICATION_ID, notificationId);
            notificationIntent.putExtra("isVideoEnabled", true);

            val intent = PendingIntent.getActivity(
                context, INCOMING_NOTIFICATION_REQUEST_CODE,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT
            )

            // If invalid, returning null
            if (null == intent)
                return null

            return intent
        }

        public fun createAudioIntent(
            notificationId: Int,
            context: Context,
            invitationBundle: InvitationBundle
        ): PendingIntent? {
            val extras = invitationBundle.asBundle()

            val notificationIntent = Intent(context, CallActivity::class.java)

            notificationIntent.putExtras(extras)
            notificationIntent.putExtra(IncomingNotification.EXTRA_NOTIFICATION_ID, notificationId);
            notificationIntent.putExtra("isVideoEnabled", false);

            val intent = PendingIntent.getActivity(
                context, INCOMING_NOTIFICATION_REQUEST_CODE+1,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT
            )

            // If invalid, returning null
            if (null == intent)
                return null

            return intent
        }

        private const val SDK_CHANNEL_ID = "voxeet_sdk_channel_id"
        private const val DEFAULT_ID = "VideoConference"
        const val INCOMING_NOTIFICATION_REQUEST_CODE = 928
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        fun getChannelId(context: Context): String? {
            return AndroidManifest.readMetadata(
                context,
                SDK_CHANNEL_ID,
                DEFAULT_ID
            )
        }
    }

}