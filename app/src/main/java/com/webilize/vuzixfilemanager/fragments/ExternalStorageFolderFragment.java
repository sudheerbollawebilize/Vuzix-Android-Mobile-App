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

import com.github.mjdev.libaums.fs.UsbFile;
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
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.eventbus.OnSocketConnected;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class ExternalStorageFolderFragment extends BaseFragment implements IClickListener, View.OnClickListener, AdapterView.OnItemSelectedListener {

    private FragmentFolderBinding folderFragmentBinding;
    private static final String ARG_FOLDER_ITEM = "ARG_FOLDER_ITEM";
    private FileFoldersAdapter fileFoldersAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private MainActivity mainActivity;
    private FileFolderItem currentFileFolderItem;
    private NavigationListener navigationListener;
    private PopupMenu popupMenu;
    private FileFolderItem selectedFile;
    private boolean isLongPressed;
    private int counter = 0;
    private CommunicationProtocol cp;

    public static ExternalStorageFolderFragment newInstance(FileFolderItem fileFolderItem) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_FOLDER_ITEM, fileFolderItem);
        ExternalStorageFolderFragment folderFragment = new ExternalStorageFolderFragment();
        folderFragment.setArguments(bundle);
        return folderFragment;
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
        getBundleData();
        cp = CommunicationProtocol.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        folderFragmentBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_folder, container, false);
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
        folderFragmentBinding.layouFab.getRoot().setVisibility(View.GONE);
        folderFragmentBinding.imgListMode.setOnClickListener(this);
        mainActivity.activityMainBinding.imgMore.setOnClickListener(this);
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
            default:
                break;
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

    private void selectItem(int position) {
        FileFolderItem fileFolderItem = mainActivity.viewModel.getCurrentFiles().get(position);
        fileFolderItem.isSelected = !fileFolderItem.isSelected;
        fileFoldersAdapter.notifyItemChanged(position);
        if (!anyItemSelected()) {
            mainActivity.updateTitle(currentFileFolderItem.usbFile.getName());
            isLongPressed = false;
        } else mainActivity.useSelectionTopBar(counter + " Selected");
    }

    public void deselectAll() {
        mainActivity.updateTitle(currentFileFolderItem.usbFile.getName());
        isLongPressed = false;
        removeAllListSelection();
    }

    private void removeAllListSelection() {
        for (FileFolderItem fileFolderItem : mainActivity.viewModel.getCurrentFiles()) {
            fileFolderItem.isSelected = false;
            counter = 0;
        }
        fileFoldersAdapter.notifyDataSetChanged();
        mainActivity.updateTitle(currentFileFolderItem.usbFile.getName());
        isLongPressed = false;
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
            case R.id.txtDelete:
                DialogUtils.showDeleteDialog(mainActivity, getString(R.string.are_you_sure_you_want_to_delete_the_file), (dialogInterface, i) -> mainActivity.runOnUiThread(() -> {
                    if (mainActivity.viewModel.deleteFileFromFolder(selectedFile)) {
                        StaticUtils.showToast(mainActivity, getString(R.string.successfully_deleted));
                        fileFoldersAdapter.notifyDataSetChanged();
                    } else
                        StaticUtils.showToast(mainActivity, getString(R.string.failed_to_delete));
                }));
                break;
            case R.id.txtCopy:
                StaticUtils.copyFileToClipBoard(mainActivity, selectedFile.usbFile.getAbsolutePath());
                break;
            case R.id.txtSendToDevice:
                StaticUtils.showToast(mainActivity, getString(R.string.send_to_device));
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
        EventBus.getDefault().unregister(this);
        super.onStop();
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
        }
    }

    //    java.lang.ClassCastException: com.github.mjdev.libaums.fs.fat32.FatFile cannot be cast to java.lang.Comparable
    private void getFilesData() {
        mainActivity.viewModel.getUsbFiles(currentFileFolderItem);
        updateAdapterData();
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
        popupMenu.getMenuInflater().inflate(R.menu.menu_usb, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menuCopy:
                    if (isLongPressed)
                        mainActivity.copyFilesToLocal(getSelectedFiles());
                    else mainActivity.copyFilesToLocal(selectedFile.usbFile);
                    return false;
                case R.id.menuSelectAll:
                    if (item.getTitle().toString().equalsIgnoreCase(getString(R.string.select_all)))
                        doAllListSelection();
                    else removeAllListSelection();
                    return false;
                case R.id.menuSendToBlade:
                    if (isLongPressed) mainActivity.sendFileToBlade(getSelectedFiles());
                    else
                        mainActivity.sendFileToBlade(selectedFile.usbFile);
                    StaticUtils.showToast(mainActivity, getString(R.string.send_to_device));
                    return false;
                default:
                    return true;
            }
        });
    }

    private void showMenuOptions() {
        if (isLongPressed) {
            if (cp.isConnected()) {
                popupMenu.getMenu().getItem(2).setEnabled(true);
            } else popupMenu.getMenu().getItem(2).setEnabled(false);
        } else {
            popupMenu.getMenu().getItem(2).setEnabled(false);
        }
        if (counter == mainActivity.viewModel.getCurrentFiles().size()) {
            popupMenu.getMenu().getItem(1).setTitle(R.string.de_select_all);
        } else
            popupMenu.getMenu().getItem(1).setTitle(R.string.select_all);
        popupMenu.show();
    }

    private UsbFile[] getSelectedFiles() {
        List<UsbFile> selectedPaths = new ArrayList<>();
        for (FileFolderItem fileFolderItem : mainActivity.viewModel.getCurrentFiles()) {
            if (fileFolderItem.isSelected)
                selectedPaths.add(fileFolderItem.usbFile);
        }
        return selectedPaths.toArray(new UsbFile[selectedPaths.size()]);
    }

    private void showMoreBottomSheet() {
        FileOptionsBottomDialogFragment addPhotoBottomDialogFragment = FileOptionsBottomDialogFragment.newInstance(selectedFile);
        addPhotoBottomDialogFragment.setTargetFragment(this, 0);
        addPhotoBottomDialogFragment.show(mainActivity.getSupportFragmentManager(), FileOptionsBottomDialogFragment.class.getSimpleName());
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
        String name = StaticUtils.getDeviceName(mainActivity);
        if (TextUtils.isEmpty(name) || name.equalsIgnoreCase(getString(R.string.no_dev_connected)) || !cp.isConnected()) {
            folderFragmentBinding.linDevice.txtDeviceName.setTextColor(Color.LTGRAY);
            folderFragmentBinding.linDevice.txtDeviceName.setText(getString(R.string.no_dev_connected));
        } else {
            folderFragmentBinding.linDevice.txtDeviceName.setTextColor(Color.BLACK);
            folderFragmentBinding.linDevice.txtDeviceName.setText(name);
        }
        folderFragmentBinding.linDevice.txtConnectionType.setText(StaticUtils.getConnectionType());
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

}
