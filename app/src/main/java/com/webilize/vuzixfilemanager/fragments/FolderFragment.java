package com.webilize.vuzixfilemanager.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.webilize.vuzixfilemanager.BaseApplication;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.activities.MainActivity;
import com.webilize.vuzixfilemanager.adapters.FileFoldersAdapter;
import com.webilize.vuzixfilemanager.databinding.FragmentFolderBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.interfaces.NavigationListener;
import com.webilize.vuzixfilemanager.models.FileFolderItem;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.AppStorage;
import com.webilize.vuzixfilemanager.utils.DialogUtils;
import com.webilize.vuzixfilemanager.utils.FileUtils;
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.eventbus.OnSocketConnected;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class FolderFragment extends BaseFragment implements IClickListener, View.OnClickListener, AdapterView.OnItemSelectedListener {

    private FragmentFolderBinding folderFragmentBinding;
    private static final String ARG_FOLDER_ITEM = "ARG_FOLDER_ITEM";
    private FileFoldersAdapter fileFoldersAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private MainActivity mainActivity;
    private FileFolderItem currentFileFolderItem;
    private NavigationListener navigationListener;
    private Disposable loadDisposable;
    private Animation fabOpen, fabClose, fabClock, fabAnticlock;
    private boolean isOpen = false;
    private PopupMenu popupMenu;
    private FileFolderItem selectedFile;
    private boolean isLongPressed;
    private int counter = 0;
    private String metaData = "";
    private CommunicationProtocol cp;

    public static FolderFragment newInstance() {
        return new FolderFragment();
    }

    public static FolderFragment newInstance(FileFolderItem fileFolderItem) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_FOLDER_ITEM, fileFolderItem);
        FolderFragment folderFragment = new FolderFragment();
        folderFragment.setArguments(bundle);
        return folderFragment;
    }

    public static FolderFragment newInstance(String metaData) {
        Bundle bundle = new Bundle();
//        bundle.putParcelable(ARG_FOLDER_ITEM, fileFolderItem);
        bundle.putString("metaData", metaData);
        FolderFragment folderFragment = new FolderFragment();
        folderFragment.setArguments(bundle);
        return folderFragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mainActivity = (MainActivity) context;
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
        folderFragmentBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_folder, container, false);
        setUpAnimation();
        updateImageIconAndLayoutManager();
        setRecyclerViewAdapter();
        return folderFragmentBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getFilesData();
    }

    @Override
    void initComponents() {
        folderFragmentBinding.layouFab.fabAdd.setOnClickListener(this);
        folderFragmentBinding.layouFab.txtFolder.setOnClickListener(this);
//        folderFragmentBinding.layouFab.txtFile.setOnClickListener(this);
//        folderFragmentBinding.layouFab.fabFile.setOnClickListener(this);
        folderFragmentBinding.layouFab.fabFolder.setOnClickListener(this);
        folderFragmentBinding.imgListMode.setOnClickListener(this);
        folderFragmentBinding.imgSortMode.setOnClickListener(this);
        mainActivity.activityMainBinding.imgMore.setOnClickListener(this);
        setUpSpinner();
        setSortModeIcon(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fabAdd:
                handleFabClick();
                break;
//            case R.id.fabFile:
//            case R.id.txtFile:
//                StaticUtils.showToast(mainActivity, "New File");
//                break;
            case R.id.fabFolder:
            case R.id.txtFolder:
                createNewFolder();
                handleFabClick();
                break;
            case R.id.imgMore:
                if (popupMenu == null) preparePopUpMenu();
                showMenuOptions();
                break;
            case R.id.imgSortMode:
                setSortModeIcon(true);
                break;
            case R.id.imgListMode:
                AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_LIST_MODE, AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_LIST_MODE, AppConstants.SHOW_GRID) == AppConstants.SHOW_GRID ? AppConstants.SHOW_LIST : AppConstants.SHOW_GRID);
                updateImageIconAndLayoutManager();
                break;
            default:
                break;
        }
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
            getFilesData();
        } else {
            folderFragmentBinding.imgSortMode.setImageDrawable(mode == AppConstants.CONST_SORT_ASC ? ContextCompat.getDrawable(mainActivity, R.drawable.ic_sort_asc) : ContextCompat.getDrawable(mainActivity, R.drawable.ic_sort_desc));
        }

    }

    @Override
    public void onClick(View view, int position) {
        if (isLongPressed) {
            selectItem(position);
        } else {
            selectedFile = mainActivity.viewModel.getCurrentFiles().get(position);
            if (view.getId() == R.id.imgMore) {
                showMoreBottomSheet();
            } else {
                if (navigationListener != null)
                    navigationListener.open(mainActivity.viewModel.getFile(position));
            }
        }
    }

    @Override
    public void onLongClick(View view, int position) {
        isLongPressed = true;
        selectItem(position);
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
    public void onSocketConnected(OnSocketConnected onSocketConnected) {
        try {
            setConnectedDeviceData();
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void selectItem(int position) {
        FileFolderItem fileFolderItem = mainActivity.viewModel.getCurrentFiles().get(position);
        fileFolderItem.isSelected = !fileFolderItem.isSelected;
        fileFoldersAdapter.notifyItemChanged(position);
        if (!anyItemSelected()) {
            mainActivity.updateTitle(currentFileFolderItem.file.getName());
            isLongPressed = false;
        } else mainActivity.useSelectionTopBar(counter + " Selected");
    }

    public void deselectAll() {
        mainActivity.updateTitle(currentFileFolderItem.file.getName());
        isLongPressed = false;
        removeAllListSelection();
    }

    private void removeAllListSelection() {
        for (FileFolderItem fileFolderItem : mainActivity.viewModel.getCurrentFiles()) {
            fileFolderItem.isSelected = false;
            counter = 0;
        }
        fileFoldersAdapter.notifyDataSetChanged();
    }

    private void doAllListSelection() {
        for (FileFolderItem fileFolderItem : mainActivity.viewModel.getCurrentFiles()) {
            fileFolderItem.isSelected = true;
        }
        isLongPressed = true;
        counter = mainActivity.viewModel.getCurrentFiles().size();
        fileFoldersAdapter.notifyDataSetChanged();
        mainActivity.useSelectionTopBar(counter + " Selected");
    }

    private boolean anyItemSelected() {
        boolean isAnySelected = false;
        counter = 0;
        for (FileFolderItem fileFolderItem : mainActivity.viewModel.getCurrentFiles()) {
            if (fileFolderItem.isSelected) {
                isAnySelected = true;
                counter++;
            }
        }
        return isAnySelected;
    }

    private void onBottomSheetItemClicked(int viewid) {
        switch (viewid) {
            case R.id.txtShare:
                Uri uriToShare = FileProvider.getUriForFile(mainActivity,
                        mainActivity.getPackageName() + ".fileprovider", selectedFile.file
                );
                FileUtils.openShareFileIntent(mainActivity, uriToShare);
                break;
            case R.id.txtDelete:
                DialogUtils.showDeleteDialog(mainActivity, getString(R.string.are_you_sure_you_want_to_delete_the_file), (dialogInterface, i) -> mainActivity.runOnUiThread(() -> {
                    if (mainActivity.viewModel.deleteFileFromFolder(selectedFile)) {
                        StaticUtils.showToast(mainActivity, getString(R.string.successfully_deleted));
                        getFilesData();
                    } else
                        StaticUtils.showToast(mainActivity, getString(R.string.failed_to_delete));
                }));
                break;
            case R.id.txtCopy:
                StaticUtils.copyFileToClipBoard(mainActivity, selectedFile.file.getAbsolutePath());
                break;
            case R.id.txtRename:
                performRename();
                break;
            case R.id.txtSendToDeviceBT:
                if (selectedFile.file.isFile()) {
                    mainActivity.sendFileToBladeBT(selectedFile.file);
                } else {
                    if (selectedFile.file.list() != null && selectedFile.file.list().length > 0) {
                        mainActivity.sendFilesToBladeBT(selectedFile.file.list());
                    } else StaticUtils.showToast(mainActivity, "No files in the folder.");
                }
                break;
            case R.id.txtSendToDevice:
                if (selectedFile.file.isFile()) {
                    mainActivity.sendFileToBlade(selectedFile.file);
                } else {
                    if (selectedFile.file.list() != null && selectedFile.file.list().length > 0) {
                        mainActivity.sendFilesToBlade(selectedFile.file.list());
                    } else StaticUtils.showToast(mainActivity, "No files in the folder.");
                }
                break;
            case R.id.txtBookMark:
                addToBookMark();
                break;
            case R.id.txtRemoveBookMark:
                removeBookMark();
                break;
            default:
                break;
        }
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
    public void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        isOpen = false;
        if (loadDisposable != null)
            loadDisposable.dispose();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (i != AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SORT_MODE, 0)) {
            AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SORT_MODE, i);
            BaseApplication.filterSortingMode = i;
            getFilesData();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void getBundleData() {
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            if (bundle.containsKey(ARG_FOLDER_ITEM)) {
                currentFileFolderItem = bundle.getParcelable(ARG_FOLDER_ITEM);
            }
            if (bundle.containsKey("metaData")) metaData = bundle.getString("metaData");
        }
    }

    private void setUpAnimation() {
        fabClose = AnimationUtils.loadAnimation(mainActivity.getApplicationContext(), R.anim.fab_close);
        fabOpen = AnimationUtils.loadAnimation(mainActivity.getApplicationContext(), R.anim.fab_open);
        fabClock = AnimationUtils.loadAnimation(mainActivity.getApplicationContext(), R.anim.fab_rotate_clock);
        fabAnticlock = AnimationUtils.loadAnimation(mainActivity.getApplicationContext(), R.anim.fab_rotate_anticlock);
    }

    private void getFilesData() {
        if (!TextUtils.isEmpty(metaData)) {
            switch (metaData) {
                case AppConstants.CONST_IMAGES:
                    loadDisposable = mainActivity.viewModel.getAllImages()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::updateAdapterData, throwable -> {
                                Log.e("get files: ", throwable.toString());
                                StaticUtils.showToast(folderFragmentBinding.getRoot().getContext(), "Error getting the files");
                            });
                    break;
                case AppConstants.CONST_VIDEOS:
                    loadDisposable = mainActivity.viewModel.getAllVideos()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::updateAdapterData, throwable -> {
                                Log.e("get files: ", throwable.toString());
                                StaticUtils.showToast(folderFragmentBinding.getRoot().getContext(), "Error getting the files");
                            });
                    break;
                case AppConstants.CONST_AUDIO:
                    loadDisposable = mainActivity.viewModel.getAllAudio()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::updateAdapterData, throwable -> {
                                Log.e("get files: ", throwable.toString());
                                StaticUtils.showToast(folderFragmentBinding.getRoot().getContext(), "Error getting the files");
                            });
                    break;
                case AppConstants.CONST_RECENT:
                    loadDisposable = mainActivity.viewModel.getAllFilesRecent()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::updateAdapterData, throwable -> {
                                Log.e("get files: ", throwable.toString());
                                StaticUtils.showToast(folderFragmentBinding.getRoot().getContext(), "Error getting the files");
                            });
                    break;
            }
        } else {
            loadDisposable = mainActivity.viewModel.getFiles(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_HIDDEN, false),
                    currentFileFolderItem)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateAdapterData, throwable -> {
                        Log.e("get files: ", throwable.toString());
                        StaticUtils.showToast(folderFragmentBinding.getRoot().getContext(), "Error getting the files");
                    });
            if (currentFileFolderItem == null || currentFileFolderItem.file == null) {
                currentFileFolderItem = new FileFolderItem(AppConstants.homeDirectory);
            }
        }
    }

    private void updateAdapterData() {
        if (mainActivity.viewModel.isFolderEmpty()) {
            folderFragmentBinding.recyclerView.setVisibility(View.GONE);
            folderFragmentBinding.txtNoDataFound.setVisibility(View.VISIBLE);
        } else {
            folderFragmentBinding.recyclerView.setVisibility(View.VISIBLE);
            folderFragmentBinding.txtNoDataFound.setVisibility(View.GONE);
        }
        fileFoldersAdapter.setFileFolderItemArrayList(mainActivity.viewModel.getCurrentFiles());
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
        folderFragmentBinding.recyclerView.setHasFixedSize(true);
        folderFragmentBinding.recyclerView.setNestedScrollingEnabled(false);
        folderFragmentBinding.recyclerView.setLayoutManager(layoutManager);

        setAdapter();

        folderFragmentBinding.recyclerView.setFocusable(true);
        folderFragmentBinding.recyclerView.requestFocus();
    }

    private void setAdapter() {
        fileFoldersAdapter = new FileFoldersAdapter(mainActivity, mainActivity.viewModel.getCurrentFiles(), this);
        folderFragmentBinding.recyclerView.setAdapter(fileFoldersAdapter);
    }

    private void handleFabClick() {
        if (isOpen) {
            folderFragmentBinding.layouFab.txtFolder.setVisibility(View.INVISIBLE);
            folderFragmentBinding.layouFab.fabFolder.startAnimation(fabClose);
            folderFragmentBinding.layouFab.fabAdd.startAnimation(fabAnticlock);
            folderFragmentBinding.layouFab.fabFolder.setClickable(false);
        } else {
            folderFragmentBinding.layouFab.txtFolder.setVisibility(View.VISIBLE);
            folderFragmentBinding.layouFab.fabFolder.startAnimation(fabOpen);
            folderFragmentBinding.layouFab.fabAdd.startAnimation(fabClock);
            folderFragmentBinding.layouFab.fabFolder.setClickable(true);
        }
        isOpen = !isOpen;
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
    }

    private void preparePopUpMenu() {
        popupMenu = new PopupMenu(mainActivity, mainActivity.activityMainBinding.imgMore);
        popupMenu.getMenuInflater().inflate(R.menu.menu_home, popupMenu.getMenu());
        popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_SHOW_HIDDEN).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_HIDDEN, false));
        popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_SHOW_EMPTY).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_EMPTY_FOLDERS, true));
        popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_SHOW_ONLY_FILES).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FILES, false));
        popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_SHOW_ONLY_FOLDERS).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FOLDERS, false));
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menuShowHiddenFiles:
                    if (item.isChecked()) item.setChecked(false);
                    else item.setChecked(true);
                    AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_HIDDEN, item.isChecked());
                    getFilesData();
                    return false;
                case R.id.menuShowEmptyFolders:
                    if (item.isChecked()) item.setChecked(false);
                    else item.setChecked(true);
                    AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_EMPTY_FOLDERS, item.isChecked());
                    getFilesData();
                    return false;
                case R.id.menuSendToBlade:
                    if (cp.isConnected())
                        if (isLongPressed)
                            mainActivity.sendFilesToBlade(getSelectedFiles());
                    return false;
                case R.id.menuShowOnlyFolders:
                    if (item.isChecked()) item.setChecked(false);
                    else item.setChecked(true);
                    if (AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FILES, false)) {
                        AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_ONLY_FILES, false);
                    }
                    AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_ONLY_FOLDERS, item.isChecked());
                    getFilesData();
                    return false;
                case R.id.menuShowFilesOnly:
                    if (item.isChecked()) item.setChecked(false);
                    else item.setChecked(true);
                    if (AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FOLDERS, false)) {
                        AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_ONLY_FOLDERS, false);
                    }
                    AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_ONLY_FILES, item.isChecked());
                    getFilesData();
                    return false;
                case R.id.menuDelete:
                    DialogUtils.showDeleteDialog(mainActivity, getString(R.string.are_you_sure_you_want_to_delete_the_files), (dialogInterface, i) -> mainActivity.runOnUiThread(() -> {
                        mainActivity.viewModel.deleteMultipleFiles(getSelectedFiles());
                        StaticUtils.showToast(mainActivity, getString(R.string.successfully_deleted));
                        isLongPressed = false;
                        getFilesData();
                        fileFoldersAdapter.notifyDataSetChanged();
                    }));
                    return false;
                case R.id.menuCopy:
                    if (isLongPressed)
                        StaticUtils.copyFilesToClipBoard(mainActivity, getSelectedFiles());
                    else
                        StaticUtils.copyFileToClipBoard(mainActivity, currentFileFolderItem.file.getAbsolutePath());
                    return false;
                case R.id.menuNewFolder:
                    createNewFolder();
                    return false;
                case R.id.menuPaste:
                    String[] filesToPaste = StaticUtils.getFilesFromClipBoard(mainActivity);
                    if (filesToPaste != null && filesToPaste.length > 0) {
                        for (String path : filesToPaste) {
                            FileUtils.performCopy(mainActivity, path, currentFileFolderItem.file);
                        }
                        AppStorage.getInstance(mainActivity).clearClipBoardUri();
                    }
                    getFilesData();
                    StaticUtils.showToast(mainActivity, "Pasted contents successfully.");
                    return false;
                case R.id.menuSelectAll:
                    if (item.getTitle().toString().equalsIgnoreCase(getString(R.string.select_all)))
                        doAllListSelection();
                    else deselectAll();
                    return false;
                case R.id.menuDetails:
                    DialogUtils.showFileInfoDialog(mainActivity, currentFileFolderItem);
                    return false;
                default:
                    return true;
            }
        });
    }

    private void showMenuOptions() {
        popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_PASTE).setEnabled(StaticUtils.isPrimaryUriAvailableInClipboard(mainActivity));
        popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_SHOW_HIDDEN).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_HIDDEN, false));
        if (isLongPressed) {
            popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_NEW_FOLDER).setEnabled(false);
            popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_COPY).setTitle(R.string.copy);
            popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_DELETE).setEnabled(true);
            popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_TRANSFER_SELECTED_FILES).setEnabled(cp.isConnected());
        } else {
            popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_NEW_FOLDER).setEnabled(true);
            popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_COPY).setTitle(R.string.copy_path);
            popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_DELETE).setEnabled(false);
            popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_TRANSFER_SELECTED_FILES).setEnabled(false);
        }
        if (counter == mainActivity.viewModel.getCurrentFiles().size()) {
            popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_SELECT_ALL).setTitle(R.string.de_select_all);
        } else
            popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_SELECT_ALL).setTitle(R.string.select_all);
        popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_SHOW_EMPTY).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_EMPTY_FOLDERS, true));
        popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_SHOW_ONLY_FILES).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FILES, false));
        popupMenu.getMenu().getItem(AppConstants.CONST_POPUP_SHOW_ONLY_FOLDERS).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_ONLY_FOLDERS, false));

        popupMenu.show();
    }

    private String[] getSelectedFiles() {
        List<String> selectedPaths = new ArrayList<>();
        for (FileFolderItem fileFolderItem : mainActivity.viewModel.getCurrentFiles()) {
            if (fileFolderItem.isSelected) selectedPaths.add(fileFolderItem.file.getAbsolutePath());
        }
        return selectedPaths.toArray(new String[selectedPaths.size()]);
    }

    private void createNewFolder() {
        DialogUtils.showCreateNewFolderDialog(mainActivity, currentFileFolderItem, view -> mainActivity.runOnUiThread(() -> {
            if (view.getTag() != null && !TextUtils.isEmpty(view.getTag().toString())) {
                File newFolder = new File(currentFileFolderItem.file, view.getTag().toString());
                mainActivity.viewModel.addNewFolder(new FileFolderItem(newFolder));
                StaticUtils.showToast(mainActivity, "Created New Folder");
                updateAdapterData();
            } else StaticUtils.showToast(mainActivity, "Failed to create folder");
        }));
    }

    private void showMoreBottomSheet() {
        FileOptionsBottomDialogFragment addPhotoBottomDialogFragment = FileOptionsBottomDialogFragment.newInstance(selectedFile);
        addPhotoBottomDialogFragment.setTargetFragment(this, 0);
        addPhotoBottomDialogFragment.show(mainActivity.getSupportFragmentManager(), FileOptionsBottomDialogFragment.class.getSimpleName());
    }

    private void performRename() {
        DialogUtils.showRenameDialog(mainActivity, selectedFile, view -> {
            if (view.getTag() != null && !TextUtils.isEmpty(view.getTag().toString())) {
//                mainActivity.viewModel.getCurrentFiles().remove(selectedFile);
                getFilesData();
            }
            StaticUtils.showToast(mainActivity, "Renamed Successfully.");
        });
    }

    private void removeBookMark() {
        StaticUtils.removeSavedLocationFromStorage(mainActivity, selectedFile.file.getAbsolutePath());
    }

    private void addToBookMark() {
        StaticUtils.addSavedLocationToStorage(mainActivity, selectedFile.file.getAbsolutePath());
    }

}
