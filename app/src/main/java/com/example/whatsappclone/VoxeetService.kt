package com.example.whatsappclone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.example.whatsappclone.incoming.IncomingNotification
import com.example.whatsappclone.incoming.NotificationCancelReceiver
import com.voxeet.promise.solve.ErrorPromise
import com.voxeet.promise.solve.PromiseExec
import com.voxeet.promise.solve.Solver
import com.voxeet.sdk.VoxeetSdk
import com.voxeet.sdk.json.ParticipantInfo
import com.voxeet.sdk.models.Conference
import com.voxeet.sdk.models.v1.CreateConferenceResult
import com.voxeet.sdk.push.center.NotificationCenterFactory
import com.voxeet.sdk.push.center.management.EnforcedNotificationMode
import com.voxeet.sdk.push.center.management.NotificationMode
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class VoxeetService : Service() {

    private val TAG = "Whatsappclone - VoxeetService"
    private val mReceiver: NotificationCancelReceiver = NotificationCancelReceiver()

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onCreate() {

        super.onCreate()
        Log.i(TAG, "Service onCreate")

        // Voxeet SDK initialization
        VoxeetSdk.initialize(
            APP_ID,
            APP_PASSWORD
        )
        // Register the current activity in the Voxeet SDK
        VoxeetSdk.instance()!!.register(this)

        // Register the channelId "VideoConference"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val CHANNEL_ID = "VideoConference"
            val name: CharSequence = "VideoConference"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)

            val notificationManager =
                this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        // Registering into the NotificationService
        NotificationCenterFactory.instance.register(
            NotificationMode.OVERHEAD_INCOMING_CALL,
            IncomingNotification()
        )
        // Setting the application notification mode
        VoxeetSdk.notification()!!.setEnforcedNotificationMode(EnforcedNotificationMode.OVERHEAD_INCOMING_CALL);

        // Register broadcast receiver
        val filter = IntentFilter();
        filter.addAction(BROADCAST_ACTION);
        this.registerReceiver(mReceiver,filter);

        // Start user session
        VoxeetSdk.session()!!.open(ParticipantInfo(NAME, USER_ID, IMAGE))
            .then { result: Boolean?, solver: Solver<Any?>? ->
                println("Voxeet session started")
                Toast.makeText(this, "Voxeet session started...", Toast.LENGTH_SHORT).show()
            }
            .error(error())
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
    /**
     * Close session
     */
    override fun onDestroy() {
        Log.i(TAG, "Service onDestroy")

        // Unregister the current activity in the Voxeet SDK
        VoxeetSdk.instance()!!.unregister(this)
        // Close user session
        VoxeetSdk.session()!!.close()
            .then { result: Boolean?, solver: Solver<Any?>? ->
                 Log.i(TAG,"Voxeet session stopped")
            }.error(error())
        this.unregisterReceiver(mReceiver);

        super.onDestroy()
    }

    private fun error(): ErrorPromise? {
        return ErrorPromise { error: Throwable ->
            error.printStackTrace()
        }
    }

    // This method will be called when a JoinEvent is posted
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onJoinEvent(event: JoinEvent) {
        Log.i(TAG, "Got onJoinEvent:"+event.toString())

        // Just join, we have conferenceId
        VoxeetSdk.conference()!!.join(event.conferenceId)
            .then { result: Conference?, solver: Solver<Any?>? ->
                Toast.makeText(this, "Joined...", Toast.LENGTH_SHORT).show()
                if (event.isVideoCall) {
                    println("Voxeet CallView - About to start video");
                    VoxeetSdk.conference()!!.startVideo()
                        .then { result: Boolean?, solver: Solver<Any?>? ->
                            Log.i(TAG,"Voxeet video started")
                        }.error(error())
                }
            }
            .error(error())
    }

    // This method will be called when a CreateJoinEvent is posted
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCreateJoinEvent(event: CreateJoinEvent) {
        Log.i(TAG, "Got onCreateJoinEvent:"+event.toString())

        VoxeetSdk.conference()!!.create(event.conferenceAlias)
            .then(PromiseExec { result: CreateConferenceResult?, solver: Solver<Conference?> ->
                solver.resolve(
                    VoxeetSdk.conference()!!.join(result!!.conferenceId)
                )
                // Invite other only for new conference
                if(result!!.isNew)
                    inviteParticipants(result!!.conferenceId, event.participants);
            })
            .then { result: Conference?, solver: Solver<Any?>? ->
                Toast.makeText(this, "Joined...", Toast.LENGTH_SHORT).show()
                if (event.isVideoCall) {
                    Log.i(TAG,"About to start video")
                    VoxeetSdk.conference()!!.startVideo()
                        .then { result: Boolean?, solver: Solver<Any?>? ->
                            Log.i(TAG,"Voxeet conference left")
                        }.error(error())
                }
            }
            .error(error())
    }

    // This method will be called when a LeaveEvent is posted
    @Subscribe
    fun onLeaveEvent(event: LeaveEvent?) {
        Log.i(TAG, "Got onLeaveEvent:"+event.toString())
        VoxeetSdk.conference()!!.leave()
            .then { result: Boolean?, solver: Solver<Any?>? ->
                Log.i(TAG,"Voxeet conference left")
            }.error(error())
    }


    fun inviteParticipants(conferenceId: String, participants: List<ParticipantInfo>?)
    {
        Log.i(TAG, "About to invite participants: "+conferenceId+", "+participants.toString())
        if (participants!=null) {
            // ...then invite
            VoxeetSdk.notification()!!.invite(conferenceId, participants)
                        .then { result: Boolean?, solver: Solver<Any?>? ->
                            Log.i(TAG,"Voxeet invite sent")
                        }.error(error())
        }

    }

    companion object {
        // Voxeet integration params
        const val NAME = "Paranoid Android";
        const val IMAGE = "https://bit.ly/2TIt8NR";
        const val USER_ID = "empty-queen-5"
//        const val NAME = "Carmen Velasco";
//        const val IMAGE = "https://randomuser.me/api/portraits/women/31.jpg";
//        const val USER_ID = "cac036a2-a4ba-4ad9-a2e4-c51c8de29912"
        const val APP_ID = "(YOUR VOXEET CONSUMER KEY)"
        const val APP_PASSWORD = "(YOUR VOXEET CONSUMER SECRET)"
        const val BROADCAST_ACTION = "com.example.whatsappclone.cancelNotification";


    }

    class JoinEvent(val conferenceId: String, val isVideoCall: Boolean)
    class CreateJoinEvent(val conferenceAlias: String, val isVideoCall: Boolean,
                          val participants: List<ParticipantInfo>? )
    class LeaveEvent()

}
