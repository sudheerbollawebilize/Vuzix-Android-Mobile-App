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
import com.webilize.vuzixfilemanager.utils.BladeLastModifiedComparator;
import com.webilize.vuzixfilemanager.utils.BladeNameComparator;
import com.webilize.vuzixfilemanager.utils.BladeSizeComparator;
import com.webilize.vuzixfilemanager.utils.DateUtils;
import com.webilize.vuzixfilemanager.utils.DialogUtils;
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.eventbus.OnSocketConnected;
import com.webilize.vuzixfilemanager.utils.eventbus.OnThumbsReceived;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

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
    private ArrayList<BladeItem> bladeItemArrayList, bladeItemArrayListOriginal;
    private Animation fabOpen, fabClose, fabClock, fabAnticlock;
    private boolean isOpen = false;
    private long size = 0;

    public static BladeFolderFragment newInstance(ArrayList<BladeItem> bladeItemArrayList, String folderPath) {
        BladeFolderFragment bladeFolderFragment = new BladeFolderFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_FOLDER_ITEM, folderPath);
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
        setSortModeIcon(false);
    }

    private void setSortModeIcon(boolean change) {
        int mode = AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SORT_DIR, AppConstants.CONST_SORT_ASC);
        if (change) {
            if (mode == AppConstants.CONST_SORT_ASC) {
                folderFragmentBinding.imgSortMode.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.ic_sort_desc));
                AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SORT_DIR, AppConstants.CONST_SORT_DESC);
            } else {
                folderFragmentBinding.imgSortMode.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.ic_sort_asc));
                AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SORT_DIR, AppConstants.CONST_SORT_ASC);
            }
            updateMenuList();
        } else {
            folderFragmentBinding.imgSortMode.setImageDrawable(mode == AppConstants.CONST_SORT_ASC ? ContextCompat.getDrawable(mainActivity, R.drawable.ic_sort_asc) : ContextCompat.getDrawable(mainActivity, R.drawable.ic_sort_desc));
        }

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
            case R.id.imgSortMode:
                setSortModeIcon(true);
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
            updateMenuList();
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
        folderFragmentBinding.imgSortMode.setOnClickListener(this);
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
        bladeItemArrayListOriginal = new ArrayList<>();
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            if (bundle.containsKey(ARG_FOLDER_ITEM)) {
                folderPath = bundle.getString(ARG_FOLDER_ITEM, "");
            }
            if (bundle.containsKey(ARG_FOLDER_LIST)) {
                bladeItemArrayListOriginal = bundle.getParcelableArrayList(ARG_FOLDER_LIST);
                bladeItemArrayList.addAll(bladeItemArrayListOriginal);
            }
        }
    }

    private void updateMenuList() {
        boolean showHidden = AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_HIDDEN, false);
        boolean showEmpty = AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_EMPTY_FOLDERS, true);
        boolean showOnlyFiles = AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FILES, false);
        boolean showOnlyFolders = AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FOLDERS, false);
        int mode = AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SORT_DIR, AppConstants.CONST_SORT_ASC);
        bladeItemArrayList.clear();
        if (bladeItemArrayListOriginal != null && bladeItemArrayListOriginal.size() > 0) {
            BladeItem[] bladeItems = bladeItemArrayListOriginal.toArray(new BladeItem[]{});
            switch (BaseApplication.filterSortingMode) {
                case AppConstants.CONST_NAME:
                    Arrays.sort(bladeItems, new BladeNameComparator(mode));
                    break;
                case AppConstants.CONST_MODIFIED:
                    Arrays.sort(bladeItems, new BladeLastModifiedComparator(mode));
                    break;
                case AppConstants.CONST_SIZE:
                    Arrays.sort(bladeItems, new BladeSizeComparator(mode));
                    break;
                default:
                    break;
            }
            if (showOnlyFiles) {
                for (BladeItem file : bladeItems) {
                    if ((showHidden || !file.isHidden) && !file.isFolder) {
                        bladeItemArrayList.add(file);
                    }
                }
            } else if (showOnlyFolders) {
                for (BladeItem file : bladeItems) {
                    if ((showEmpty || file.size != 0) && (showHidden || !file.isHidden) && file.isFolder) {
                        bladeItemArrayList.add(file);
                    }
                }
            } else {
                for (BladeItem file : bladeItems) {
                    if (!file.isFolder) {
                        if ((showHidden || !file.isHidden)) {
                            bladeItemArrayList.add(file);
                        }
                    } else if ((showEmpty || file.size != 0) && (showHidden || !file.isHidden)) {
                        bladeItemArrayList.add(file);
                    }
                }
            }
        }
        fileFoldersAdapter.notifyDataSetChanged();
    }

    private void setUpSpinner() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(mainActivity, R.array.blade_files_filter, android.R.layout.simple_spinner_item);
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
//            folderFragmentBinding.layoutFab.getRoot().setVisibility(TextUtils.isEmpty(folderPath) ? View.VISIBLE : View.GONE);
            folderFragmentBinding.layoutFab.getRoot().setVisibility(View.VISIBLE);
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
        popupMenu.getMenu().getItem(AppConstants.CONST_SHOW_HIDDEN).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_HIDDEN, false));
        popupMenu.getMenu().getItem(AppConstants.CONST_SHOW_EMPTY).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_EMPTY_FOLDERS, true));
        popupMenu.getMenu().getItem(AppConstants.CONST_SHOW_ONLY_FILES).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FILES, false));
        popupMenu.getMenu().getItem(AppConstants.CONST_SHOW_ONLY_FOLDERS).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FOLDERS, false));
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menuNewFolder:
                    DialogUtils.showCreateNewFolderInBladeDialog(mainActivity, v -> {
                        JSONObject jsonObject = new JSONObject();
                        String name = (String) v.getTag();
                        try {
                            jsonObject.put("command", AppConstants.NEW_FOLDER);
                            jsonObject.put("folderName", name);
                            jsonObject.put("rootPath", folderPath);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        mainActivity.passCommandToBlade(jsonObject);
                        BladeItem bladeItem = createNewBladeItem(name, folderPath);
                        if (!bladeItemArrayListOriginal.contains(bladeItem)) {
                            bladeItemArrayListOriginal.add(bladeItem);
                            bladeItemArrayList.add(bladeItem);
                            fileFoldersAdapter.notifyDataSetChanged();
                        }
                    });
                    return false;
                case R.id.menuDelete:
                    DialogUtils.showDeleteDialog(mainActivity, getString(R.string.are_you_sure_you_want_to_delete_the_files), (dialog, which) -> {
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("command", AppConstants.DELETE);
                            JSONArray jsonArray = new JSONArray();
                            for (String path : getSelectedFiles()) {
                                jsonArray.put(path);
                            }
                            jsonObject.put("fileNames", jsonArray);
                            jsonObject.put("rootPath", folderPath);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        mainActivity.passCommandToBlade(jsonObject);
                        bladeItemArrayListOriginal.remove(selectedFile);
                        bladeItemArrayList.remove(selectedFile);
                        fileFoldersAdapter.notifyDataSetChanged();
                    });
                    return false;
                case R.id.menuShowHiddenFiles:
                    if (item.isChecked()) item.setChecked(false);
                    else item.setChecked(true);
                    AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_HIDDEN, item.isChecked());
                    updateMenuList();
                    return false;
                case R.id.menuShowEmptyFolders:
                    if (item.isChecked()) item.setChecked(false);
                    else item.setChecked(true);
                    AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_EMPTY_FOLDERS, item.isChecked());
                    updateMenuList();
                    return false;
                case R.id.menuShowOnlyFolders:
                    if (item.isChecked()) item.setChecked(false);
                    else item.setChecked(true);
                    if (AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FILES, false)) {
                        AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_ONLY_FILES, false);
                    }
                    AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_ONLY_FOLDERS, item.isChecked());
                    updateMenuList();
                    return false;
                case R.id.menuShowOnlyFiles:
                    if (item.isChecked()) item.setChecked(false);
                    else item.setChecked(true);
                    if (AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FOLDERS, false)) {
                        AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_ONLY_FOLDERS, false);
                    }
                    AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_ONLY_FILES, item.isChecked());
                    updateMenuList();
                    return false;
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

    private BladeItem createNewBladeItem(String name, String path) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
            jsonObject.put("path", path);
            jsonObject.put("size", 0);
            jsonObject.put("lastModified", Calendar.getInstance().getTimeInMillis());
            jsonObject.put("isHidden", false);
            jsonObject.put("isFavourite", false);
            jsonObject.put("isFolder", true);
            jsonObject.put("fileInfo", DateUtils.getDateTimeFromTimeStamp(Calendar.getInstance().getTimeInMillis(), DateUtils.DATE_FORMAT_0));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new BladeItem(jsonObject);
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
                popupMenu.getMenu().getItem(AppConstants.CONST_SELECT_ALL).setTitle(R.string.de_select_all);
            } else
                popupMenu.getMenu().getItem(AppConstants.CONST_SELECT_ALL).setTitle(R.string.select_all);
            if (counter > 0) {
                popupMenu.getMenu().getItem(AppConstants.CONST_TRANSFER_SELECTED_FILES).setEnabled(true);
                popupMenu.getMenu().getItem(AppConstants.CONST_DELETE).setEnabled(true);
            } else {
                popupMenu.getMenu().getItem(AppConstants.CONST_TRANSFER_SELECTED_FILES).setEnabled(false);
                popupMenu.getMenu().getItem(AppConstants.CONST_DELETE).setEnabled(false);
            }
            popupMenu.getMenu().getItem(AppConstants.CONST_SHOW_EMPTY).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_EMPTY_FOLDERS, true));
            popupMenu.getMenu().getItem(AppConstants.CONST_SHOW_ONLY_FILES).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FILES, false));
            popupMenu.getMenu().getItem(AppConstants.CONST_SHOW_ONLY_FOLDERS).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FOLDERS, false));
            popupMenu.getMenu().getItem(AppConstants.CONST_SHOW_HIDDEN).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_HIDDEN, false));

            popupMenu.show();
        }
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