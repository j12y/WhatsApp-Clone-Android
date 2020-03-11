package com.example.whatsappclone.incoming

import android.R
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import com.example.whatsappclone.ui.call.CallActivity
import com.voxeet.sdk.push.center.invitation.IIncomingInvitationListener
import com.voxeet.sdk.push.center.invitation.InvitationBundle
import com.voxeet.sdk.utils.AndroidManifest
import java.security.SecureRandom


class IncomingNotification : IIncomingInvitationListener {
    private val random: SecureRandom
    private var notificationId = -1

    override fun onInvitation(
        context: Context,
        invitationBundle: InvitationBundle
    ) {
        println("IncomingNotification onInvitation")
        notificationId = random.nextInt(Int.MAX_VALUE / 2)
        if (null != invitationBundle.conferenceId) {
            notificationId = invitationBundle.conferenceId.hashCode()
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = getChannelId(context)

        // Get intents for all actions
        val acceptAudio = CallActivity.createAudioIntent(notificationId, context, invitationBundle)
        val dismiss = NotificationCancelReceiver.createDismissIntent(notificationId, context)
        val acceptVideo = CallActivity.createVideoIntent(notificationId, context, invitationBundle)

        if (null == acceptVideo || null == acceptAudio) {
            println("onInvitation: accept intent is null !!")
            return
        }
        val inviterName =
            if (!TextUtils.isEmpty(invitationBundle.inviterName)) invitationBundle.inviterName else "(Unknown)"

        val lastNotification: Notification = NotificationCompat.Builder(context, channelId as String)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle(
                "Incoming call from "+inviterName
            )
            .setContentText("Would you like to join to the conference?")
            .setSmallIcon(R.drawable.sym_action_call)
            .addAction(
                R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismiss
            )
            .addAction(
                R.drawable.ic_menu_call,
                "Join audio",
                acceptAudio
            )
            .addAction(
                R.drawable.ic_menu_camera,
                "Join video",
                acceptVideo
            )
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
        // Notify user
        notificationManager.notify(notificationId, lastNotification)
    }

    override fun onInvitationCanceled( context: Context, conferenceId: String ) {
        println("IncomingNotification onInvitationCanceled")
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (-1 != notificationId) notificationManager.cancel(notificationId)
        notificationId = 0
    }

    companion object {
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

    init {
        random = SecureRandom()
    }
}

class NotificationCancelReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val notificationId = intent.getIntExtra(IncomingNotification.EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            // Cancel your ongoing Notification
            val notificationManager =  context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
            notificationManager!!.cancel(notificationId)
        }
    }

    companion object {
        fun createDismissIntent(notificationId: Int ,  context: Context): PendingIntent {
            val cancel = Intent("com.example.whatsappclone.cancelNotification")
            cancel.putExtra(IncomingNotification.EXTRA_NOTIFICATION_ID, notificationId);
            val dismissIntent =
                PendingIntent.getBroadcast(context, IncomingNotification.INCOMING_NOTIFICATION_REQUEST_CODE+2, cancel, PendingIntent.FLAG_CANCEL_CURRENT)
            return dismissIntent;
        }
    }
}