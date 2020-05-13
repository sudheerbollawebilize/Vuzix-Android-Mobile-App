package com.webilize.vuzixfilemanager.fragments;/*
package com.webilize.filetransfermanager.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.webilize.filetransfermanager.R;
import com.webilize.filetransfermanager.interfaces.NavigationListener;

import java.io.File;
import java.util.ArrayList;

public class GalleryViewPagerFragment extends BaseFragment {

    private static final String TAG = "GalleryViewPagerFragment";

    private View view;
    private File currentFile;
    private int currPosition = 0;
private NavigationListener navigationListener;
    //region new instance
    public static GalleryViewPagerFragment newInstance(int position) {
        Bundle args = new Bundle();
        args.putInt("currPosition", position);
        GalleryViewPagerFragment fragment = new GalleryViewPagerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public GalleryViewPagerFragment() {
        // Required empty public constructor
    }
    //endregion

    //region lifecycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey("currPosition"))
            currPosition = getArguments().getInt("currPosition", 0);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            navigationListener = (NavigationListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement NavigationListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_view_pager_gallery, container, false);
        initComponents();
        return view;
    }

    private void setUpMenu() {
        ArrayList<CarouselItem> menuItems = new ArrayList<>();
        int selection = 0;
        menuItems.add(new CarouselItem("Back", R.drawable.ic_back_navigation));
        if (!(getActivity().getSupportFragmentManager().findFragmentById(R.id.main_frame) instanceof HelpFragment)) {
            menuItems.add(new CarouselItem("Help", R.drawable.ic_help));
            selection = 1;
        }
        menu = CarouselMenuFragment.newInstance(menuItems, selection, 1f, R.style.AppTheme);
        menu.setListener(this);
    }

    private void showMenu() {
        if (menu == null) {
            setUpMenu();
            showMenu();
        } else {
            menu.show(getChildFragmentManager(), "Custom Bottom Sheet");
        }
    }

    private void initComponents() {
        setUpMenu();
        viewPagerGallery = view.findViewById(R.id.viewPagerGallery);
        viewPagerGallery.setAdapter(new HelpPageAdapter());
        viewPagerGallery.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewPagerGallery.setOnViewPagerClickListener(viewPager -> showMenu());
    }

    @Override
    public void onItemClick(int i, Object o) {
        if (menu != null)
            menu.dismiss();
        switch (i) {
            case 0:
                navigationListener.back();
                break;
            case 1:
                navigationListener.openHelp();
                break;
        }
    }

    //endregion

    class HelpPageAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return galleryViewModel.getTotalMedia();
        }

        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.fragment_media, collection, false);

            ImageView imageView;
            VideoView videoView;
            imageView = layout.findViewById(R.id.imageView);
            videoView = layout.findViewById(R.id.videoView);

            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.GONE);
            MediaModel mediaModel = galleryViewModel.getMediaModel(currPosition);
            currentFile = mediaModel.getFile();
            if (mediaModel != null && currentFile != null) {
                if (ImagesUtils.isImageFile(mediaModel.getMimeType())) {
                    imageView.setVisibility(View.VISIBLE);
                    Glide.with(getActivity().getApplicationContext())
                            .load(currentFile)
                            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).format(DecodeFormat.PREFER_ARGB_8888))
                            .centerCrop()
                            .into(imageView);
                } else if (ImagesUtils.isVideoFile(mediaModel.getMimeType())) {
                    videoView.setVisibility(View.VISIBLE);
                } else if (ImagesUtils.isAudioFile(mediaModel.getMimeType())) {

                } else {
                    Toast.makeText(getActivity(), "Not Supported Media Type", Toast.LENGTH_SHORT).show();
                }
            }

            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object view) {
            container.removeView((View) view);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

    }
}
*/
