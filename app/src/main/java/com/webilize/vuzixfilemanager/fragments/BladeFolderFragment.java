package com.webilize.vuzixfilemanager.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.webilize.vuzixfilemanager.BaseApplication;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.activities.BladeFoldersActivity;
import com.webilize.vuzixfilemanager.activities.MainActivity;
import com.webilize.vuzixfilemanager.adapters.BladeFileFoldersAdapter;
import com.webilize.vuzixfilemanager.databinding.FragmentBladeFolderBinding;
import com.webilize.vuzixfilemanager.dbutils.DBHelper;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.interfaces.NavigationListener;
import com.webilize.vuzixfilemanager.models.BladeItem;
import com.webilize.vuzixfilemanager.models.DeviceFavouritesModel;
import com.webilize.vuzixfilemanager.models.DeviceModel;
import com.webilize.vuzixfilemanager.models.FileFolderItem;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.AppStorage;
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.eventbus.OnSocketConnected;
import com.webilize.vuzixfilemanager.utils.eventbus.OnThumbsReceived;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class BladeFolderFragment extends BaseFragment implements IClickListener, View.OnClickListener, AdapterView.OnItemSelectedListener {

    private FragmentBladeFolderBinding folderFragmentBinding;
    private static final String ARG_FOLDER_ITEM = "ARG_FOLDER_ITEM";
    private static final String ARG_FOLDER_LIST = "ARG_FOLDER_LIST";
    private BladeFileFoldersAdapter fileFoldersAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private MainActivity mainActivity;
    private NavigationListener navigationListener;
    private String folderPath = "";
    private PopupMenu popupMenu;
    private BladeItem selectedFile;
    private boolean isLongPressed;
    private int counter = 0;
    private CommunicationProtocol cp;
    private ArrayList<BladeItem> bladeItemArrayList;
    private Animation fabOpen, fabClose, fabClock, fabAnticlock;
    private boolean isOpen = false;
    private long size = 0;

    public static BladeFolderFragment newInstance(String folderPath) {
        BladeFolderFragment bladeFolderFragment = new BladeFolderFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_FOLDER_ITEM, folderPath);
        bladeFolderFragment.setArguments(bundle);
        return bladeFolderFragment;
    }

    public static BladeFolderFragment newInstance(ArrayList<BladeItem> bladeItemArrayList) {
        BladeFolderFragment bladeFolderFragment = new BladeFolderFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(ARG_FOLDER_LIST, bladeItemArrayList);
        bladeFolderFragment.setArguments(bundle);
        return bladeFolderFragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mainActivity = (MainActivity) context;
            if (context instanceof NavigationListener)
                navigationListener = (NavigationListener) context;
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationListener = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cp = CommunicationProtocol.getInstance();
        getBundleData();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        folderFragmentBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_blade_folder, container, false);
        updateImageIconAndLayoutManager();
        setRecyclerViewAdapter();
        return folderFragmentBinding.getRoot();
    }

    @Override
    void initComponents() {
        setUpAnimation();
        setListeners();
        setUpSpinner();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgMore:
                if (popupMenu == null) preparePopUpMenu();
                showMenuOptions();
                break;
            case R.id.imgListMode:
                AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_LIST_MODE, AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_LIST_MODE, AppConstants.SHOW_GRID) == AppConstants.SHOW_GRID ? AppConstants.SHOW_LIST : AppConstants.SHOW_GRID);
                updateImageIconAndLayoutManager();
                break;
            case R.id.btnConnect:
                mainActivity.activityMainBinding.bottomBar.setSelectedItemId(R.id.navConnectivity);
                break;
            case R.id.fabFoldersMenu:
                handleFabClick();
                break;
            case R.id.fabAllFolders:
            case R.id.txtAllFolders:
                handleFabClick();
                break;
            case R.id.fabFavourites:
            case R.id.txtFavourites:
//                showLocations(false);
                handleFabClick();
                Intent intent = new Intent(mainActivity, BladeFoldersActivity.class);
                intent.putExtra("inputExtra", "destinationPath");
                mainActivity.startActivityForResult(intent, AppConstants.REQUEST_BLADE_FOLDERS);
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View view, int position) {
        if (isLongPressed) {
            selectItem(position);
        } else {
            try {
                selectedFile = bladeItemArrayList.get(position);
                if (view.getId() == R.id.imgMore) {
                    showMoreBottomSheet();
                } else {
                    if (selectedFile.isFolder) {
                        mainActivity.requestForFolder(selectedFile.path);
                    } else {
                        requestForFile();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
    }

    @Override
    public void onLongClick(View view, int position) {
        isLongPressed = true;
        selectItem(position);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (data != null && data.hasExtra("viewid")) {
                onBottomSheetItemClicked(data.getIntExtra("viewid", 0));
            }
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        isOpen = false;
    }

    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (i != AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SORT_MODE, 0)) {
            AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SORT_MODE, i);
            BaseApplication.filterSortingMode = i;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            setConnectedDeviceData();
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketConnected(OnSocketConnected onSocketConnected) {
        try {
            setConnectedDeviceData();
            mainActivity.popBackStack();
            mainActivity.requestForFolder("");
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void setConnectedDeviceData() {
        try {
            String name = StaticUtils.getDeviceName(mainActivity);
            if (TextUtils.isEmpty(name) || name.equalsIgnoreCase(getString(R.string.no_dev_connected)) || !cp.isConnected()) {
                folderFragmentBinding.linDevice.txtDeviceName.setTextColor(Color.LTGRAY);
                folderFragmentBinding.linDevice.txtDeviceName.setText(getString(R.string.no_dev_connected));
            } else {
                folderFragmentBinding.linDevice.txtDeviceName.setTextColor(Color.BLACK);
                folderFragmentBinding.linDevice.txtDeviceName.setText(name);
            }
            folderFragmentBinding.linDevice.txtConnectionType.setText(StaticUtils.getConnectionType());
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnThumbsReceived(OnThumbsReceived onThumbsReceived) {
        if (onThumbsReceived.filesArrayList != null && !onThumbsReceived.filesArrayList.isEmpty()) {
            if (onThumbsReceived.filesArrayList.size() == 1) {
                navigationListener.open(new FileFolderItem(onThumbsReceived.filesArrayList.get(0)));
            }
        }
    }

    private void setListeners() {
        folderFragmentBinding.layoutFab.fabAllFolders.setOnClickListener(this);
        folderFragmentBinding.layoutFab.fabFavourites.setOnClickListener(this);
        folderFragmentBinding.layoutFab.fabFoldersMenu.setOnClickListener(this);
        folderFragmentBinding.layoutFab.txtAllFolders.setOnClickListener(this);
        folderFragmentBinding.layoutFab.txtFavourites.setOnClickListener(this);
        folderFragmentBinding.imgListMode.setOnClickListener(this);
        mainActivity.activityMainBinding.imgMore.setOnClickListener(this);
        folderFragmentBinding.btnConnect.setOnClickListener(this);
    }

    private void setUpAnimation() {
        fabClose = AnimationUtils.loadAnimation(mainActivity.getApplicationContext(), R.anim.fab_close);
        fabOpen = AnimationUtils.loadAnimation(mainActivity.getApplicationContext(), R.anim.fab_open);
        fabClock = AnimationUtils.loadAnimation(mainActivity.getApplicationContext(), R.anim.fab_rotate_clock);
        fabAnticlock = AnimationUtils.loadAnimation(mainActivity.getApplicationContext(), R.anim.fab_rotate_anticlock);
    }

    private void handleFabClick() {
        if (isOpen) {
            folderFragmentBinding.layoutFab.txtAllFolders.setVisibility(View.INVISIBLE);
            folderFragmentBinding.layoutFab.fabAllFolders.startAnimation(fabClose);
            folderFragmentBinding.layoutFab.fabFoldersMenu.startAnimation(fabAnticlock);
            folderFragmentBinding.layoutFab.fabAllFolders.setClickable(false);
            folderFragmentBinding.layoutFab.txtFavourites.setVisibility(View.INVISIBLE);
            folderFragmentBinding.layoutFab.fabFavourites.startAnimation(fabClose);
            folderFragmentBinding.layoutFab.fabFavourites.setClickable(false);
        } else {
            folderFragmentBinding.layoutFab.txtAllFolders.setVisibility(View.VISIBLE);
            folderFragmentBinding.layoutFab.fabAllFolders.startAnimation(fabOpen);
            folderFragmentBinding.layoutFab.fabFoldersMenu.startAnimation(fabClock);
            folderFragmentBinding.layoutFab.fabAllFolders.setClickable(true);
            folderFragmentBinding.layoutFab.txtFavourites.setVisibility(View.VISIBLE);
            folderFragmentBinding.layoutFab.fabFavourites.startAnimation(fabOpen);
            folderFragmentBinding.layoutFab.fabFavourites.setClickable(true);
        }
        isOpen = !isOpen;
    }

    private void getBundleData() {
        bladeItemArrayList = new ArrayList<>();
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            if (bundle.containsKey(ARG_FOLDER_ITEM)) {
                folderPath = bundle.getString(ARG_FOLDER_ITEM, "");
            }
            if (bundle.containsKey(ARG_FOLDER_LIST)) {
                bladeItemArrayList = bundle.getParcelableArrayList(ARG_FOLDER_LIST);
            }
        }
//        if (cp.isConnected()) mainActivity.requestForFolderWOFrag(folderPath);
    }

    private void setUpSpinner() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(mainActivity, R.array.files_filter,
                android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        folderFragmentBinding.spinnerFilter.setAdapter(spinnerAdapter);
        folderFragmentBinding.spinnerFilter.setOnItemSelectedListener(null);
        folderFragmentBinding.spinnerFilter.setSelection(BaseApplication.filterSortingMode);
        folderFragmentBinding.spinnerFilter.setOnItemSelectedListener(this);
    }

    private void setRecyclerViewAdapter() {
        folderFragmentBinding.recyclerView.setAdapter(null);
        folderFragmentBinding.recyclerView.setLayoutManager(layoutManager);
        fileFoldersAdapter = new BladeFileFoldersAdapter(mainActivity, bladeItemArrayList, this);
        folderFragmentBinding.recyclerView.setAdapter(fileFoldersAdapter);

        updateListVisibility();
    }

    private void updateImageIconAndLayoutManager() {
        if (AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_LIST_MODE, AppConstants.SHOW_GRID) == AppConstants.SHOW_LIST) {
            layoutManager = new LinearLayoutManager(mainActivity);
            folderFragmentBinding.imgListMode.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.ic_grid));
        } else {
            layoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
            folderFragmentBinding.imgListMode.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.ic_list));
        }
        if (layoutManager != null)
            folderFragmentBinding.recyclerView.setLayoutManager(layoutManager);
        if (fileFoldersAdapter != null) fileFoldersAdapter.notifyDataSetChanged();
        updateListVisibility();
    }

    private void updateListVisibility() {
        if (cp.isConnected()) {
            folderFragmentBinding.btnConnect.setVisibility(View.GONE);
            folderFragmentBinding.layoutFab.getRoot().setVisibility(TextUtils.isEmpty(folderPath) ? View.VISIBLE : View.GONE);
            if (bladeItemArrayList == null || bladeItemArrayList.isEmpty()) {
                folderFragmentBinding.relFilter.setVisibility(View.GONE);
                folderFragmentBinding.recyclerView.setVisibility(View.GONE);
                folderFragmentBinding.txtNoDataFound.setVisibility(View.VISIBLE);
                folderFragmentBinding.txtNoDataFound.setText(R.string.folder_is_empty);
            } else {
                folderFragmentBinding.relFilter.setVisibility(View.VISIBLE);
                folderFragmentBinding.recyclerView.setVisibility(View.VISIBLE);
                folderFragmentBinding.txtNoDataFound.setVisibility(View.GONE);
            }
        } else {
            folderFragmentBinding.layoutFab.getRoot().setVisibility(View.GONE);
            folderFragmentBinding.relFilter.setVisibility(View.GONE);
            folderFragmentBinding.recyclerView.setVisibility(View.GONE);
            folderFragmentBinding.btnConnect.setVisibility(View.VISIBLE);
            folderFragmentBinding.txtNoDataFound.setVisibility(View.VISIBLE);
            folderFragmentBinding.txtNoDataFound.setText(R.string.currently_no_device_is_connected_nplease_connect_to_blade_to_view_data);
        }
    }

    private void preparePopUpMenu() {
        popupMenu = new PopupMenu(mainActivity, mainActivity.activityMainBinding.imgMore);
        popupMenu.getMenuInflater().inflate(R.menu.menu_blade, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menuSelectAll:
                    if (item.getTitle().toString().equalsIgnoreCase(getString(R.string.select_all)))
                        doAllListSelection();
                    else removeAllListSelection();
                    return false;
                case R.id.menuSaveToPhone:
                    requestForFiles();
                    return false;
                default:
                    return true;
            }
        });
    }

    private void requestForFiles() {
        size = 0;
        ArrayList<String> files = getSelectedFiles();
        mainActivity.requestForFilesOriginal(size, files);
        deselectAll();
    }

    private void requestForFile() {
        ArrayList<String> selectedPaths = new ArrayList<>();
        selectedPaths.add(selectedFile.path);
        mainActivity.requestForFilesOriginal(selectedFile.size, selectedPaths);
        deselectAll();
    }

    private void showMenuOptions() {
        if (cp.isConnected()) {
            if (counter == bladeItemArrayList.size()) {
                popupMenu.getMenu().getItem(0).setTitle(R.string.de_select_all);
            } else
                popupMenu.getMenu().getItem(0).setTitle(R.string.select_all);
            if (counter > 0) {
                popupMenu.getMenu().getItem(1).setEnabled(true);
            } else popupMenu.getMenu().getItem(1).setEnabled(false);
        } else {
            popupMenu.getMenu().getItem(0).setEnabled(false);
            popupMenu.getMenu().getItem(1).setEnabled(false);

        }
        popupMenu.show();
    }

    private ArrayList<String> getSelectedFiles() {
        ArrayList<String> selectedPaths = new ArrayList<>();
        for (BladeItem fileFolderItem : bladeItemArrayList) {
            if (fileFolderItem.isSelected) {
                size += fileFolderItem.size;
                selectedPaths.add(fileFolderItem.path);
            }
        }
        return selectedPaths;
    }

    private void showMoreBottomSheet() {
        BladeOptionsBottomDialogFragment addPhotoBottomDialogFragment = BladeOptionsBottomDialogFragment.newInstance(selectedFile);
        addPhotoBottomDialogFragment.setTargetFragment(this, 0);
        addPhotoBottomDialogFragment.show(mainActivity.getSupportFragmentManager(), BladeOptionsBottomDialogFragment.class.getSimpleName());
    }

    private void selectItem(int position) {
        BladeItem fileFolderItem = bladeItemArrayList.get(position);
        fileFolderItem.isSelected = !fileFolderItem.isSelected;
        fileFoldersAdapter.notifyItemChanged(position);
        if (!anyItemSelected()) {
            mainActivity.updateTitle("Blade Files");
            isLongPressed = false;
        } else mainActivity.useSelectionTopBar(counter + " Selected");
    }

    public void deselectAll() {
        mainActivity.updateTitle("Blade Files");
        isLongPressed = false;
        removeAllListSelection();
    }

    private void removeAllListSelection() {
        for (BladeItem fileFolderItem : bladeItemArrayList) {
            fileFolderItem.isSelected = false;
            counter = 0;
        }
        fileFoldersAdapter.notifyDataSetChanged();
        mainActivity.updateTitle("Blade Files");
        isLongPressed = false;
    }

    private void doAllListSelection() {
        for (BladeItem fileFolderItem : bladeItemArrayList) {
            fileFolderItem.isSelected = true;
        }
        isLongPressed = true;
        counter = bladeItemArrayList.size();
        fileFoldersAdapter.notifyDataSetChanged();
        mainActivity.useSelectionTopBar(counter + " Selected");
    }

    private boolean anyItemSelected() {
        boolean isAnySelected = false;
        counter = 0;
        for (BladeItem fileFolderItem : bladeItemArrayList) {
            if (fileFolderItem.isSelected) {
                isAnySelected = true;
                counter++;
            }
        }
        return isAnySelected;
    }

    private void onBottomSheetItemClicked(int viewid) {
        switch (viewid) {
            case R.id.txtSaveToPhone:
                requestForFile();
                break;
            case R.id.txtBookMark:
                addBookMark();
                break;
            case R.id.txtRemoveBookMark:
                removeBookMark();
                break;
            default:
                break;
        }
    }

    private void removeBookMark() {
        try {
            DBHelper dbHelper = new DBHelper(mainActivity);
            dbHelper.deleteDeviceFav(dbHelper.getDeviceFavModel(selectedFile.path));
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void addBookMark() {
        try {
            String dev = AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_DEVICE_ADDRESS, "");
            DBHelper dbHelper = new DBHelper(mainActivity);
            DeviceModel deviceModel = dbHelper.getDeviceModel(dev);
            if (deviceModel != null && deviceModel.id != -1) {
                if (!TextUtils.isEmpty(selectedFile.path)) {
                    DeviceFavouritesModel deviceFavouritesModel = new DeviceFavouritesModel();
                    deviceFavouritesModel.deviceId = deviceModel.id;
                    deviceFavouritesModel.isDefault = false;
                    deviceFavouritesModel.path = selectedFile.path;
                    deviceFavouritesModel.name = selectedFile.name;
                    deviceFavouritesModel.id = dbHelper.addDeviceFavouritesModel(deviceFavouritesModel);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

}