package com.example.whatsappclone.ui.channel

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.whatsappclone.R
import com.example.whatsappclone.databinding.FragmentChannelBinding
import com.getstream.sdk.chat.StreamChat
import com.getstream.sdk.chat.rest.User
import com.getstream.sdk.chat.view.MessageListView
import com.getstream.sdk.chat.viewmodel.ChannelViewModel
import com.getstream.sdk.chat.viewmodel.ChannelViewModelFactory
import com.voxeet.sdk.VoxeetSdk
import com.voxeet.sdk.events.sdk.ConferenceStatusUpdatedEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class ChannelFragment : Fragment() {

    private val args: ChannelFragmentArgs by navArgs()
    lateinit var binding: FragmentChannelBinding
    private var channelMenu: Menu? = null

    // setup data binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // we're using data binding in this example
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_channel, container, false)
        return binding.root
    }

    // enable the options menu
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onResume() {
        super.onResume();
        VoxeetSdk.instance()!!.register(this)
//        updateViews();
    }
    override fun onPause() {
        VoxeetSdk.instance()!!.unregister(this)
        super.onPause();
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_channel, menu)
        channelMenu = menu;
        super.onCreateOptionsMenu(menu,inflater)
        updateViews();
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            findNavController().navigateUp()
            return true
        } else if(menuItem.itemId == R.id.miPhone) {
            println("About to start audio call" );

            findNavController().navigate(
                ChannelFragmentDirections.navCall2(args.channelId, false, args.channelType)
            )
        } else if(menuItem.itemId == R.id.miVideo) {
            println("About to start video call" );

            findNavController().navigate(
                ChannelFragmentDirections.navCall2(args.channelId, true, args.channelType)
            )
        }
        return false
    }

    // setup the toolbar and viewmodel
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity : AppCompatActivity = activity as AppCompatActivity

        // toolbar setup
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        activity.supportActionBar!!.setDisplayShowHomeEnabled(true)
        activity.supportActionBar!!.setDisplayShowTitleEnabled(false)

        val client = StreamChat.getInstance(activity.application)
        val view = view
        binding.lifecycleOwner = this
        val channel = client.channel(args.channelType, args.channelId)
        val factory = ChannelViewModelFactory(activity.application, channel)
        val viewModel: ChannelViewModel by viewModels { factory }

        // connect the view model
        binding.viewModel = viewModel
        binding.messageList.setViewModel(viewModel, this)
        binding.messageInputView.setViewModel(viewModel, this)

        val messageList : MessageListView = view!!.findViewById(R.id.messageList)

        val otherUsers: List<User> = channel.channelState.otherUsers
        binding.avatarGroup.setChannelAndLastActiveUsers(channel, otherUsers, messageList.style)
        binding.channelName.text = channel.name

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ConferenceStatusUpdatedEvent) {
        println("Voxeet ConferenceStatusUpdatedEvent: " + event.toString());
        updateViews();
    }

    fun updateViews()
    {
        val miPhone: MenuItem = channelMenu!!.findItem(R.id.miPhone)
        val miVideo: MenuItem = channelMenu!!.findItem(R.id.miVideo)
        println("Socket open: " + VoxeetSdk.session()!!.isSocketOpen().toString());
        miVideo.setEnabled(VoxeetSdk.session()!!.isSocketOpen())
        miPhone.setEnabled(VoxeetSdk.session()!!.isSocketOpen())
    }
}