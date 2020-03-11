package com.example.whatsappclone.ui.call

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.whatsappclone.R
import com.example.whatsappclone.VoxeetService
import com.getstream.sdk.chat.StreamChat
import com.getstream.sdk.chat.model.Member
import com.voxeet.android.media.MediaStream
import com.voxeet.android.media.MediaStreamType
import com.voxeet.promise.Promise
import com.voxeet.promise.solve.ErrorPromise
import com.voxeet.promise.solve.PromiseExec
import com.voxeet.promise.solve.Solver
import com.voxeet.sdk.VoxeetSdk
import com.voxeet.sdk.events.sdk.ConferenceStatusUpdatedEvent
import com.voxeet.sdk.events.v2.StreamAddedEvent
import com.voxeet.sdk.events.v2.StreamRemovedEvent
import com.voxeet.sdk.events.v2.StreamUpdatedEvent
import com.voxeet.sdk.events.v2.VideoStateEvent
import com.voxeet.sdk.json.ParticipantInfo
import com.voxeet.sdk.models.Conference
import com.voxeet.sdk.models.Participant
import com.voxeet.sdk.models.v1.CreateConferenceResult
import com.voxeet.sdk.views.VideoView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class CallView : Fragment() {

    private val args: CallViewArgs by navArgs()
    private val mEventBus: EventBus? = VoxeetSdk.instance()!!.eventBus
    private var mShouldClose: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var inf: View = inflater.inflate(R.layout.fragment_call_view, container, false)
        mShouldClose = false

        return inf;
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val floating_hangup_button =
            view!!.findViewById(R.id.floating_hangup_button) as View?

        floating_hangup_button!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                mShouldClose = true
                leave()
            }
        })
    }

    override fun onResume() {
        super.onResume();
        VoxeetSdk.instance()!!.register(this)
        join()

    }
    override fun onPause() {
        try {
            if(VoxeetSdk.conference()!!.isLive) {
                // Should not be here
                leave()
            }
        } catch (e:java.lang.Exception) {
            e.printStackTrace()
        } finally {
            VoxeetSdk.instance()!!.unregister(this)
        }
        super.onPause();
    }

    fun join() {
        updateViews()
        if(args.conferenceId==null) {
            // Must create conference
            mEventBus!!.post( VoxeetService.CreateJoinEvent(args.channelId.toString(),
                args.isVideoCall, getParticipants()))
        } else {
            mEventBus!!.post( VoxeetService.JoinEvent(args.conferenceId.toString(), args.isVideoCall))
        }
    }

    fun leave() {
        mEventBus!!.post( VoxeetService.LeaveEvent())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ConferenceStatusUpdatedEvent) {
        Log.i(TAG,"Voxeet ConferenceStatusUpdatedEvent: " + event.toString());
        updateViews();
        if(mShouldClose && !VoxeetSdk.conference()!!.isLive){
            // exit view
            try {
                val navc: NavController? = findNavController()
                if(navc!=null)
                    navc.navigateUp()

            } catch (e:Exception){
                getActivity()!!.onBackPressed();
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: StreamAddedEvent) {
        Log.i(TAG,"Voxeet StreamAddedEvent: " + event.toString());
        updateParticipantStream(event.participant, event.mediaStream)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: StreamUpdatedEvent) {
        Log.i(TAG,"Voxeet StreamUpdatedEvent: " + event.toString());
        updateParticipantStream(event.participant, event.mediaStream)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: StreamRemovedEvent) {
        Log.i(TAG,"Voxeet StreamRemovedEvent: " + event.toString());
        removeParticipantStream(event.participant, event.mediaStream)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: VideoStateEvent) {
        Log.i(TAG,"Voxeet VideoStateEvent: $event");
    }

    /**
     * Find appropriate view and attach the stream if has video track
     */
    fun updateParticipantStream(participant: Participant? = null,
                                mediaStream: MediaStream? = null) {
        val isLocal = participant!!.id == VoxeetSdk.session()!!.participantId
        var tview: VideoView? = null;
        var stream: MediaStream? = mediaStream;

        if(isLocal) {
            tview = view!!.findViewById(R.id.pip_video) as VideoView
        } else {
            val t_user_name = view!!.findViewById<View>(R.id.user_name) as TextView

            t_user_name.text = participant.info!!.name.toString()
            tview = view!!.findViewById(R.id.remote_video) as VideoView
        }

        if (null != stream && !stream.videoTracks().isEmpty() && tview!=null) {
            tview.attach(participant.id!!, stream)
            tview.visibility = View.VISIBLE
        }
    }

    /**
     * Find appropriate view and unattach the stream
     */
    fun removeParticipantStream(participant: Participant? = null,
                                mediaStream: MediaStream? = null) {
        val isLocal = participant!!.id == VoxeetSdk.session()!!.participantId
        var tview: VideoView? = null;

        if(isLocal) {
            tview = view!!.findViewById(R.id.pip_video) as VideoView

        } else {
            val t_user_name = view!!.findViewById<View>(R.id.user_name) as TextView

            tview = view!!.findViewById(R.id.remote_video) as VideoView
            t_user_name.text = ""

        }

        if(tview!=null) {
            tview.visibility = View.INVISIBLE
            tview.unAttach()
        }

    }

    fun updateViews() {
        val progressBar = view!!.findViewById(R.id.conecting_progress) as ProgressBar
        if (VoxeetSdk.conference()!!.isLive)
            progressBar.visibility = View.GONE
        else
            progressBar.visibility = View.VISIBLE
    }

    fun getParticipants():List<ParticipantInfo>?
    {
        try {
            if (args.channelType == null)
                return null

            // Add new ParticipantInfo(....) for each channel Participant to invite
            val activity: AppCompatActivity = activity as AppCompatActivity
            val client = StreamChat.getInstance(activity.application)
            val channel = client.channel(args.channelType, args.channelId)
//            // ... get other users
//            val otherUsers: List<com.getstream.sdk.chat.rest.User> = channel.channelState.otherUsers
//            val participants: List<ParticipantInfo> =
//                otherUsers.map { user -> ParticipantInfo(user.name, user.id, user.image) }
            // ...get all members
            val otherUsers: List<Member> =
                channel.channelState.members
            val participants: List<ParticipantInfo> =
                otherUsers.map { user -> ParticipantInfo(user.user.name, user.user.id, user.user.image) }

            return participants
        } catch (e:Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun error(): ErrorPromise? {
        return ErrorPromise { error: Throwable ->
            Toast.makeText(activity!!.applicationContext, "ERROR...", Toast.LENGTH_SHORT).show()
            error.printStackTrace()
            updateViews()

            getActivity()!!.onBackPressed();
        }
    }

    companion object {

        fun newInstance(conferenceId: String?, isVideoCall: Boolean?): CallView {
            val fragment = CallView()
            val args = Bundle()
            args.putString("conference_id", conferenceId)
            args.putBoolean("is_video_call", isVideoCall as Boolean)
            fragment.arguments = args
            return fragment
        }
    }

    private val TAG = "Whatsappclone - CallView"
}

