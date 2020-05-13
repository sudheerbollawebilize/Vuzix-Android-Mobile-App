package com.webilize.vuzixfilemanager.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.activities.MainActivity;
import com.webilize.vuzixfilemanager.databinding.FragmentImageVideoPlayerBinding;
import com.webilize.vuzixfilemanager.interfaces.NavigationListener;
import com.webilize.vuzixfilemanager.models.FileFolderItem;
import com.webilize.vuzixfilemanager.utils.DateUtils;
import com.webilize.vuzixfilemanager.utils.DialogUtils;
import com.webilize.vuzixfilemanager.utils.FileUtils;
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.audioplayer.MediaPlayerHolder;
import com.webilize.vuzixfilemanager.utils.audioplayer.PlaybackInfoListener;
import com.webilize.vuzixfilemanager.utils.audioplayer.PlayerAdapter;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

import java.io.File;

public class ImageVideoFragment extends DialogFragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "fileFolderItem";
    private static final String TAG = ImageVideoFragment.class.getSimpleName();
    private FragmentImageVideoPlayerBinding fragmentImageVideoPlayerBinding;

    private NavigationListener mListener;
    private MainActivity mainActivity;
    private FileFolderItem fileFolderItem;
    private PopupMenu popupMenu;

    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;
    private CommunicationProtocol cp;

    public ImageVideoFragment() {
    }

    /**
     * todo : This is for future enhancement. We will use viewpager later for this. using shared view model then.
     *
     * @param position
     * @return
     */
    public static ImageVideoFragment show(int position) {
        ImageVideoFragment fragment = new ImageVideoFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, position);
        fragment.setArguments(args);
        return fragment;
    }

    public static ImageVideoFragment show(FileFolderItem fileFolderItem) {
        ImageVideoFragment fragment = new ImageVideoFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAM1, fileFolderItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
//        this will make it fullscreen without top status bar
//        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        cp = CommunicationProtocol.getInstance();
        if (getArguments() != null && getArguments().containsKey(ARG_PARAM1)) {
            fileFolderItem = getArguments().getParcelable(ARG_PARAM1);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (fileFolderItem != null && mPlayerAdapter != null)
            mPlayerAdapter.loadMedia(fileFolderItem);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mainActivity.isChangingConfigurations() && mPlayerAdapter != null && mPlayerAdapter.isPlaying()) {
            Log.d(TAG, "onStop: don't release MediaPlayer as screen is rotating & playing");
        } else {
            if (mainActivity != null && mPlayerAdapter != null) mPlayerAdapter.release();
            Log.d(TAG, "onStop: release MediaPlayer");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentImageVideoPlayerBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_image_video_player, container, false);
        initComponents();
        return fragmentImageVideoPlayerBinding.getRoot();
    }

    private void initComponents() {
        fragmentImageVideoPlayerBinding.setMediaItem(fileFolderItem);
        hideAllLayouts();
        setUpMediaPlayer();
    }

    private void setUpMediaPlayer() {
        fragmentImageVideoPlayerBinding.txtFolderName.setText(fileFolderItem.name);
        preparePopUpMenu();
        if (fileFolderItem.isImageFile()) {
            setUpImageFile();
        } else if (fileFolderItem.isAudioFile()) {
            setUpAudioFile();
        } else if (fileFolderItem.isVideoFile()) {
            setUpVideoFile();
        }
        fragmentImageVideoPlayerBinding.imgBack.setOnClickListener(this);
        fragmentImageVideoPlayerBinding.imgPlay.setOnClickListener(this);
        fragmentImageVideoPlayerBinding.imgMenu.setOnClickListener(this);
    }

    private void hideAllLayouts() {
        fragmentImageVideoPlayerBinding.relAudioPlayer.setVisibility(View.GONE);
        fragmentImageVideoPlayerBinding.imgImage.setVisibility(View.GONE);
        fragmentImageVideoPlayerBinding.relVideoPlayer.setVisibility(View.GONE);
    }

    private void setUpVideoFile() {
        fragmentImageVideoPlayerBinding.relVideoPlayer.setVisibility(View.VISIBLE);
        fragmentImageVideoPlayerBinding.exoPlayerView.setSource(fileFolderItem.file.getPath());
    }

    private void setUpAudioFile() {
        fragmentImageVideoPlayerBinding.relAudioPlayer.setVisibility(View.VISIBLE);
        initializeAudioPlayer();
    }

    private void initializeAudioPlayer() {
        initializeSeekbar();
        fragmentImageVideoPlayerBinding.imgPlay.setSelected(true);
        fragmentImageVideoPlayerBinding.imgPlay.requestFocus();
        fragmentImageVideoPlayerBinding.imgPlay.setImageResource(R.drawable.ic_play);
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(mainActivity);
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
    }

    private void initializeSeekbar() {
        fragmentImageVideoPlayerBinding.seekBarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            userSelectedPosition = progress;
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    private void setUpImageFile() {
        fragmentImageVideoPlayerBinding.imgImage.setVisibility(View.VISIBLE);
        if (fileFolderItem.file == null) {
            if (fileFolderItem.usbFile != null) {
                File dir = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
                File op = new File(dir, fileFolderItem.usbFile.getName());
                if (op.exists()) {
                    FileUtils.loadImageWithGlide(op, fragmentImageVideoPlayerBinding.imgImage, StaticUtils.getFileDrawable(op));
                } else {
                    mainActivity.copyFilesToLocal(fileFolderItem.usbFile);
                    new Handler().postDelayed(() -> {
                        fileFolderItem = new FileFolderItem(op);
                        FileUtils.loadImageWithGlide(op, fragmentImageVideoPlayerBinding.imgImage, StaticUtils.getFileDrawable(op));
                    }, 1000);
                }
            } else StaticUtils.showToast(mainActivity, "File doesn't exist.");
        } else
            FileUtils.loadImageWithGlide(fileFolderItem.file, fragmentImageVideoPlayerBinding.imgImage, StaticUtils.getFileDrawable(fileFolderItem.file));
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) context;
        if (context instanceof NavigationListener) {
            mListener = (NavigationListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void preparePopUpMenu() {
        popupMenu = new PopupMenu(mainActivity, fragmentImageVideoPlayerBinding.imgMenu);
        popupMenu.getMenuInflater().inflate(R.menu.menu_image_video, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menuSendToBlade:
                    if (cp.isConnected())
                        if (fileFolderItem.file != null)
                            mainActivity.sendFileToBlade(fileFolderItem.file);
                        else if (fileFolderItem.usbFile != null) {
                            mainActivity.sendFileToBlade(fileFolderItem.usbFile);
                        }
                    break;
                case R.id.menuRename:
                    if (fileFolderItem.file != null)
                        performRename();
                    break;
                case R.id.menuCopy:
                    if (fileFolderItem.file != null)
                        StaticUtils.copyFileToClipBoard(mainActivity, fileFolderItem.file.getAbsolutePath());
                    else mainActivity.copyFilesToLocal(fileFolderItem.usbFile);
                    break;
                case R.id.menuDelete:
                    DialogUtils.showDeleteDialog(mainActivity, getString(R.string.are_you_sure_you_want_to_delete_the_file), (dialogInterface, i) -> mainActivity.runOnUiThread(() -> {
                        if (mainActivity.viewModel.deleteFileFromFolder(fileFolderItem)) {
                            StaticUtils.showToast(mainActivity, getString(R.string.successfully_deleted));
                        } else
                            StaticUtils.showToast(mainActivity, getString(R.string.failed_to_delete));
                    }));
                    break;
                case R.id.menuShare:
                    if (fileFolderItem.file != null) {
                        Uri uriToShare = FileProvider.getUriForFile(mainActivity,
                                mainActivity.getPackageName() + ".fileprovider", fileFolderItem.file
                        );
                        FileUtils.openShareFileIntent(mainActivity, uriToShare);
                    }
                    break;
                case R.id.menuDetails:
                    if (fileFolderItem.file != null)
                        DialogUtils.showFileInfoDialog(mainActivity, fileFolderItem);
                    break;
                default:
                    break;
            }
            return true;
        });
    }

    private void performRename() {
        DialogUtils.showRenameDialog(mainActivity, fileFolderItem, view -> {
            StaticUtils.showToast(mainActivity, "Renamed Successfully.");
            if (view.getTag() != null && !TextUtils.isEmpty(view.getTag().toString())) {
                fragmentImageVideoPlayerBinding.txtFolderName.setText(fileFolderItem.name);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgMenu:
                showMenuOptions();
                break;
            case R.id.imgBack:
                dismiss();
                break;
            case R.id.imgPlay:
                if (mPlayerAdapter.isPlaying()) {
                    mPlayerAdapter.pause();
                    fragmentImageVideoPlayerBinding.imgPlay.setImageResource(R.drawable.ic_play);
                } else {
                    mPlayerAdapter.play();
                    fragmentImageVideoPlayerBinding.imgPlay.setImageResource(R.drawable.ic_pause);
                }
                break;
            default:
                break;
        }
    }

    private void showMenuOptions() {
        if (popupMenu == null) preparePopUpMenu();
        popupMenu.getMenu().getItem(0).setEnabled(cp.isConnected());
        if (fileFolderItem.file == null) {
            popupMenu.getMenu().getItem(1).setEnabled(false);
            popupMenu.getMenu().getItem(2).setEnabled(false);
            popupMenu.getMenu().getItem(4).setEnabled(false);
        } else {
            popupMenu.getMenu().getItem(1).setEnabled(true);
            popupMenu.getMenu().getItem(2).setEnabled(true);
            popupMenu.getMenu().getItem(4).setEnabled(true);
        }
        popupMenu.show();
    }

    private class PlaybackListener extends PlaybackInfoListener {
        @Override
        public void onDurationChanged(int duration) {
            fragmentImageVideoPlayerBinding.seekBarAudio.setMax(duration);
            fragmentImageVideoPlayerBinding.txtFileDetails.setText(FileUtils.getFileSize(fileFolderItem.file) + DateUtils.getDateTimeFromTimeStamp(fileFolderItem.file.lastModified(), DateUtils.DATE_FORMAT_0) + " \n" +
                    mPlayerAdapter.getDuration());
            Log.d(TAG, String.format("setPlaybackDuration: setMax(%d)", duration));
        }

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fragmentImageVideoPlayerBinding.seekBarAudio.setProgress(position, true);
                } else fragmentImageVideoPlayerBinding.seekBarAudio.setProgress(position);
                Log.d(TAG, String.format("setPlaybackPosition: setProgress(%d)", position));
            }
        }

        @Override
        public void onStateChanged(int state) {
            Log.i("state", PlaybackInfoListener.convertStateToString(state));
            super.onStateChanged(state);
        }

        @Override
        public void onPlaybackCompleted() {
            fragmentImageVideoPlayerBinding.imgPlay.setImageResource(R.drawable.ic_play);
//            super.onPlaybackCompleted();
        }

        @Override
        public void onLogUpdated(String formattedMessage) {
//            super.onLogUpdated(formattedMessage);
        }

    }

}
