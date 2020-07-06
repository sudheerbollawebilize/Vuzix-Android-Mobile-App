package com.webilize.vuzixfilemanager.activities;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.webilize.vuzixfilemanager.BuildConfig;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ActivityMainBinding;
import com.webilize.vuzixfilemanager.dbutils.DBHelper;
import com.webilize.vuzixfilemanager.fragments.BladeFolderFragment;
import com.webilize.vuzixfilemanager.fragments.ConnectivityFragment;
import com.webilize.vuzixfilemanager.fragments.ExternalStorageFolderFragment;
import com.webilize.vuzixfilemanager.fragments.FolderFragment;
import com.webilize.vuzixfilemanager.fragments.ImageVideoFragment;
import com.webilize.vuzixfilemanager.fragments.TransfersFragment;
import com.webilize.vuzixfilemanager.interfaces.NavigationListener;
import com.webilize.vuzixfilemanager.models.BladeItem;
import com.webilize.vuzixfilemanager.models.FileFolderItem;
import com.webilize.vuzixfilemanager.models.Memory;
import com.webilize.vuzixfilemanager.models.TransferModel;
import com.webilize.vuzixfilemanager.services.RXConnectionFGService;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.AppStorage;
import com.webilize.vuzixfilemanager.utils.DialogUtils;
import com.webilize.vuzixfilemanager.utils.FileUtils;
import com.webilize.vuzixfilemanager.utils.QRCodeDialog;
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.ThemeHelper;
import com.webilize.vuzixfilemanager.utils.eventbus.OnConnectionError;
import com.webilize.vuzixfilemanager.utils.eventbus.OnJSONObjectReceived;
import com.webilize.vuzixfilemanager.utils.eventbus.OnSocketConnected;
import com.webilize.vuzixfilemanager.utils.eventbus.OnTCPInitialized;
import com.webilize.vuzixfilemanager.utils.eventbus.OnThumbsReceived;
import com.webilize.vuzixfilemanager.utils.eventbus.WifiDevicesList;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;
import com.webilize.vuzixfilemanager.utils.transferutils.SocialBladeProtocol;
import com.webilize.vuzixfilemanager.utils.usb.UsbDataBinder;
import com.webilize.vuzixfilemanager.viewmodels.FolderViewModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements NavigationListener, View.OnClickListener {

    /* region start*/
    private static final String TAG = MainActivity.class.getSimpleName();
    public ActivityMainBinding activityMainBinding;
    public FolderViewModel viewModel;
    private AppStorage appStorage;
    private Bundle savedInstanceState;
    private static long back_pressed;
    private ActionBarDrawerToggle drawerToggle;
    private boolean isSelectionTopBar;

    private HashMap<UsbDevice, UsbDataBinder> mHashMap = new HashMap<>();
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private static final String ACTION_USB_PERMISSION = BuildConfig.APPLICATION_ID + ".USB_PERMISSION";
    private HashMap<String, UsbDevice> deviceList;
    private UsbDevice usbDevice;
    private QRCodeDialog qrCodeDialog;

    BroadcastReceiver mUsbDetachReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    UsbDataBinder binder = mHashMap.get(device);
                    if (binder != null) {
                        binder.onDestroy();
                        mHashMap.remove(device);
                        deviceList.remove(device);
                        showDevices();
                    }
                }
            }
        }
    };

    BroadcastReceiver mUsbAttachReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                showDevices();
            }
        }
    };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbDataBinder binder = new UsbDataBinder(mUsbManager, device);
                            mHashMap.put(device, binder);
                        }
                    }
                }
            }
        }
    };
    private FileSystem currentFs;
    /* end region */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbDetachReceiver);
        unregisterReceiver(mUsbAttachReceiver);
        unregisterReceiver(mUsbReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appStorage = AppStorage.getInstance(this);
        setUpTheme();
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        viewModel = ViewModelProviders.of(this).get(FolderViewModel.class);
        this.savedInstanceState = savedInstanceState;
        setUpNavAndTopBar();
        setBottomBar();
        dealWithUSB();
        if ((getIntent().getAction() != null && getIntent().getAction().equalsIgnoreCase("stop")))
            stopServiceManually(this);
    }

    @Override
    void initComponents() {
        setClickListeners();
    }

    @Override
    public void open(FileFolderItem fileFolderItem) {
        if (fileFolderItem.file != null) {
            if (fileFolderItem.file.isDirectory()) {
                updateTitle(fileFolderItem.file.getName());
                replaceFragment(FolderFragment.newInstance(fileFolderItem), true);
            } else {
                if (FileUtils.isImage(fileFolderItem.mimeType) || FileUtils.isVideo(fileFolderItem.mimeType) ||
                        FileUtils.isAudio(fileFolderItem.mimeType)) {
                    ImageVideoFragment.show(fileFolderItem).show(getSupportFragmentManager(), ImageVideoFragment.class.getCanonicalName());
                } else if (FileUtils.isAPKFile(fileFolderItem.file.getAbsolutePath())) {
                    FileUtils.installApk(this, fileFolderItem.file);
                } else {
                    FileUtils.showFile(this,fileFolderItem.file);

                }
            }
        } else {
            if (fileFolderItem.usbFile.isDirectory()) {
                updateTitle(fileFolderItem.usbFile.getName());
                replaceFragment(ExternalStorageFolderFragment.newInstance(fileFolderItem), true);
            } else {
                if (FileUtils.isImage(fileFolderItem.mimeType) || FileUtils.isVideo(fileFolderItem.mimeType) ||
                        FileUtils.isAudio(fileFolderItem.mimeType)) {
                    ImageVideoFragment.show(fileFolderItem).show(getSupportFragmentManager(), ImageVideoFragment.class.getCanonicalName());
                } else {
                    StaticUtils.showToast(this, "Files format not supported yet.");
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void back() {
        onBackPressed();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgBack:
                onBackPressed();
                break;
            /*case R.id.imgHome:
                if (viewModel != null && !viewModel.getCurrentFileFolderItem().file.getName().equalsIgnoreCase("0")) {
                    clearBackStackCompletely();
                    updateTitle(viewModel.getCurrentFileFolderItem().file.getName());
                    replaceFragmentWithoutAnimation(FolderFragment.newInstance(viewModel.getCurrentFileFolderItem()), false);
                }
                break;*/
            case R.id.imgSettings:
                break;
            case R.id.imgSearch:
                Intent intent = new Intent(this, SearchActivity.class);
                startActivityForResult(intent, AppConstants.REQUEST_SEARCH);
                break;
//            case R.id.imgMore:
//                if (popupMenu == null) preparePopUpMenu();
//                showMenuOptions();
//                break;
            case R.id.imgTransfer:
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (activityMainBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            activityMainBinding.drawerLayout.closeDrawers();
            return;
        }
        Fragment curr = getCurrentFragment();
        if (curr instanceof FolderFragment && isSelectionTopBar) {
            ((FolderFragment) curr).deselectAll();
        } else if (curr instanceof FolderFragment && getSupportFragmentManager().getBackStackEntryCount() > 0) {
            popBackStack();
        } else if (!(curr instanceof FolderFragment)) {
            if (curr instanceof BladeFolderFragment) {
                popBackStack();
                updateTitle(getString(R.string.blade_files));
            } else if (activityMainBinding.bottomBar.getSelectedItemId() != R.id.navFilesManager) {
                activityMainBinding.bottomBar.setSelectedItemId(R.id.navFilesManager);
            }
        } else if (back_pressed + AppConstants.BACK_PRESSED_TIME > System.currentTimeMillis())
            super.onBackPressed();
        else
            StaticUtils.showToast(getApplicationContext(), getString(R.string.press_once_again_to_exit));
        back_pressed = System.currentTimeMillis();
        try {
            if (activityMainBinding.bottomBar.getSelectedItemId() != R.id.navFilesManager)
                updateTitle(viewModel.getCurrentFileFolderItem().file == null ? (viewModel.getCurrentFileFolderItem().usbFile == null ? "" : viewModel.getCurrentFileFolderItem().usbFile.getName()) : viewModel.getCurrentFileFolderItem().file.getName());
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
            if (AppConstants.REQUEST_SEARCH == requestCode && data != null && data.hasExtra("selectedFile")) {
                open(data.getParcelableExtra("selectedFile"));
            } else if (AppConstants.REQUEST_BLADE_FOLDERS == requestCode) {
                if (data != null) {
                    if (data.hasExtra("addToFav")) {
                        Intent serviceIntent = new Intent(this, RXConnectionFGService.class);
                        serviceIntent.putExtra("inputExtra", "addToFav");
                        serviceIntent.putExtra("addToFav", data.getStringExtra("addToFav"));
                        serviceIntent.putExtra("addToFavPath", data.getStringExtra("addToFavPath"));
                        serviceIntent.putExtra("addToFavName", data.getStringExtra("addToFavName"));
                        ContextCompat.startForegroundService(this, serviceIntent);
                    } else if (data.hasExtra("inputExtra") && data.getStringExtra("inputExtra").equalsIgnoreCase("destinationPath")) {
                        Intent serviceIntent = new Intent(this, RXConnectionFGService.class);
                        serviceIntent.putExtra("inputExtra", "destinationPath");
                        if (data.hasExtra("destinationPath")) {
                            serviceIntent.putExtra("destinationPath", data.getStringExtra("destinationPath"));
                        }
                        ContextCompat.startForegroundService(this, serviceIntent);
//                        new DBHelper(this).addDeviceFavouritesModel()
                    } else {
                        Intent serviceIntent = new Intent(this, RXConnectionFGService.class);
                        serviceIntent.putExtra("inputExtra", "destinationPath");
                        if (data.hasExtra("file")) {
                            serviceIntent.putExtra("file", data.getSerializableExtra("file"));
                        }
                        if (data.hasExtra("destinationPath")) {
                            serviceIntent.putExtra("destinationPath", data.getStringExtra("destinationPath"));
                        }
                        if (data.hasExtra("files")) {
                            serviceIntent.putExtra("files", data.getStringArrayExtra("files"));
                        }
                        ContextCompat.startForegroundService(this, serviceIntent);
                    }
                    activityMainBinding.bottomBar.setSelectedItemId(R.id.navTransfers);
                }
            }
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWifiDevicesListFound(WifiDevicesList connectionEvent) {
        Fragment fragment = getCurrentFragment();
        if (connectionEvent.wifiP2pDeviceArrayList != null) {
            viewModel.updateAvailableWifiP2PDevices(connectionEvent.wifiP2pDeviceArrayList);
            if ((fragment instanceof ConnectivityFragment)) {
                ConnectivityFragment connectivityFragment = (ConnectivityFragment) fragment;
                connectivityFragment.onWifiDevicesListFound(connectionEvent.wifiP2pDeviceArrayList);
            }
        } else if ((fragment instanceof ConnectivityFragment)) {
            ConnectivityFragment connectivityFragment = (ConnectivityFragment) fragment;
            connectivityFragment.onWifiDevicesListFound(connectionEvent.wifiP2pDeviceArrayList);
        }
//            showPickWifiDirectDeviceDialog(connectionEvent.wifiP2pDeviceArrayList);
//        else {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectionError(OnConnectionError onSocketConnected) {
        StaticUtils.showToast(this, "Error connecting to device");
        DialogUtils.dismissProgressDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketConnected(OnSocketConnected onSocketConnected) {
        StaticUtils.showToast(this, "Connected");
        if (qrCodeDialog != null && qrCodeDialog.isShowing()) qrCodeDialog.close();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onOnThumbsReceived(OnThumbsReceived onThumbsReceived) {
        Log.e("size ", onThumbsReceived.filesArrayList.size() + "");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTCPInitialized(OnTCPInitialized onTCPInitialized) {
        try {
            openQRCode(onTCPInitialized.ip, onTCPInitialized.port);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOnJSONObjectReceived(OnJSONObjectReceived onThumbsReceived) {
        ArrayList<BladeItem> bladeItemArrayList = new ArrayList<>();
        JSONObject jsonObject = onThumbsReceived.jsonObject;
        JSONArray jsonArray = new JSONArray();
        if (jsonObject.has("folders")) {
            jsonArray = jsonObject.optJSONArray("folders");
            for (int i = 0; i < jsonArray.length(); i++) {
                bladeItemArrayList.add(new BladeItem(jsonArray.optJSONObject(i)));
            }
        }
        String folderPath = "";
        if (jsonObject.has("folderPath")) {
            folderPath = jsonObject.optString("folderPath");
        }
        if (TextUtils.isEmpty(folderPath)) {
            folderPath = AppConstants.HOME_DIRECTORY.getAbsolutePath();
        }
        if (bladeItemArrayList.isEmpty()) {
            StaticUtils.showToast(this, getString(R.string.folder_is_empty));
        } else {
            addFragment(BladeFolderFragment.newInstance(bladeItemArrayList, folderPath), true);
        }
        Log.e("size ", onThumbsReceived.jsonObject.length() + "");
    }

    private void openQRCode(String ip, Integer port) {
        if (qrCodeDialog == null || !qrCodeDialog.isShowing()) {
            qrCodeDialog = new QRCodeDialog(this, R.style.CameraDialog, ip, port);
            qrCodeDialog.show();
        }
    }

    public static void stopServiceManually(Context context) {
        Intent serviceIntent = new Intent(context, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "stop");
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    private void connectToWifiDirect(WifiP2pDevice serviceSelected) {
        Intent serviceIntent = new Intent(this, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "connect");
        serviceIntent.putExtra("wifiDevice", serviceSelected);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void sendFileToBlade(UsbFile file) {
        File dest = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
        File op = new File(dest, file.getName());
        if (op.exists()) StaticUtils.showToast(this, "File already exists.");
        else {
            try {
                op.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
        CopyTaskParam copyTaskParam = new CopyTaskParam();
        copyTaskParam.from = file;
        copyTaskParam.to = op;
        new CopyTask(true).execute(copyTaskParam);
    }

    public void sendFileToBlade(UsbFile[] usbfiles) {
        ArrayList<CopyTaskParam> copyTaskParams = new ArrayList<>();
        File dest = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
        for (UsbFile file : usbfiles) {
            File op = new File(dest, file.getName());
            if (op.exists()) StaticUtils.showToast(this, "File already exists.");
            else {
                try {
                    op.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            }
            CopyTaskParam copyTaskParam = new CopyTaskParam();
            copyTaskParam.from = file;
            copyTaskParam.to = op;
            copyTaskParams.add(copyTaskParam);
        }
        new MultipleFilesTask(true).execute(copyTaskParams);
    }

    public void copyFilesToLocal(UsbFile file) {
        File dest = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
        File op = new File(dest, file.getName());
        if (op.exists()) StaticUtils.showToast(this, "File already exists.");
        else {
            try {
                op.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
/*
        int index = file.getName().lastIndexOf(".") > 0 ? file.getName().lastIndexOf(".") : file.getName().length();
        String prefix = file.getName().substring(0, index);
        String ext = file.getName().substring(index);
        if (prefix.length() < 3) {
            prefix += "file";
        }
*/
        CopyTaskParam copyTaskParam = new CopyTaskParam();
        copyTaskParam.from = file;
        copyTaskParam.to = op/*File.createTempFile(prefix, ext, dest)*/;
        new CopyTask(false).execute(copyTaskParam);
    }

    public void copyFilesToLocal(UsbFile[] usbfiles) {
        ArrayList<CopyTaskParam> copyTaskParams = new ArrayList<>();
        File dest = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
        for (UsbFile file : usbfiles) {
//            File op = new File(dest, file.getName());
//            if (op.exists()) StaticUtils.showToast(this, "File already exists.");
            File op = new File(dest, file.getName());
            if (op.exists()) StaticUtils.showToast(this, "File already exists.");
            else {
                try {
                    op.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            }
            CopyTaskParam copyTaskParam = new CopyTaskParam();
            copyTaskParam.from = file;
            copyTaskParam.to = op;
            copyTaskParams.add(copyTaskParam);
        }
        new MultipleFilesTask(false).execute(copyTaskParams);
    }

    public void sendFileToBlade(File file) {
        DialogUtils.showSendFileDialog(this, "Do you want to send file to default folder, or change the Destination?",
                (dialog, which) -> {
                    Intent intent = new Intent(this, BladeFoldersActivity.class);
                    intent.putExtra("inputExtra", "send");
                    intent.putExtra("file", file);
                    startActivityForResult(intent, AppConstants.REQUEST_BLADE_FOLDERS);
                }, (dialog, which) -> {
                    Intent serviceIntent = new Intent(this, RXConnectionFGService.class);
                    serviceIntent.putExtra("inputExtra", "send");
                    serviceIntent.putExtra("file", file);
                    ContextCompat.startForegroundService(this, serviceIntent);
                    activityMainBinding.bottomBar.setSelectedItemId(R.id.navTransfers);
                });
    }

    public void sendFileToBladeBT(File file) {
        DialogUtils.showSendFileDialogBT(this, "Send file via Bluetooth?",
                (dialog, which) -> {
                    Intent serviceIntent = new Intent(this, RXConnectionFGService.class);
                    serviceIntent.putExtra("inputExtra", "send");
                    serviceIntent.putExtra("file", file);
                    serviceIntent.putExtra("bt", true);
                    ContextCompat.startForegroundService(this, serviceIntent);
                    activityMainBinding.bottomBar.setSelectedItemId(R.id.navTransfers);
                });
    }

    private void sendFileBT(File file) {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_SEND);
        i.setType("*/*");
        if (Build.VERSION.SDK_INT >= 24) {
            i.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file));
        } else i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        PackageManager pm = getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(i, 0);
        if (list.size() > 0) {
            String packageName = null;
            String className = null;
            boolean found = false;
            for (ResolveInfo info : list) {
                packageName = info.activityInfo.packageName;
                if (packageName.equals("com.android.bluetooth")) {
                    className = info.activityInfo.name;
                    found = true;
                    break;
                }
            }
            if (!found) {
                Toast.makeText(this, "Bluetooth not been found", Toast.LENGTH_LONG).show();
            } else {
                i.setClassName(packageName, className);
                startActivity(i);
            }
        }
    }

    public void sendFilesToBlade(String[] files) {
        DialogUtils.showSendFileDialog(this, "Do you want to send files to default folder, or change the Destination?",
                (dialog, which) -> {
                    Intent intent = new Intent(this, BladeFoldersActivity.class);
                    intent.putExtra("inputExtra", "send");
                    intent.putExtra("files", files);
                    startActivityForResult(intent, AppConstants.REQUEST_BLADE_FOLDERS);
                }, (dialog, which) -> {
                    Intent serviceIntent = new Intent(this, RXConnectionFGService.class);
                    serviceIntent.putExtra("inputExtra", "send");
                    serviceIntent.putExtra("files", files);
                    ContextCompat.startForegroundService(this, serviceIntent);
                    activityMainBinding.bottomBar.setSelectedItemId(R.id.navTransfers);
                });
    }

    public void sendFilesToBladeBT(String[] files) {
        DialogUtils.showSendFileDialogBT(this, "Send files via Bluetooth?", (dialog, which) -> {
            Intent serviceIntent = new Intent(this, RXConnectionFGService.class);
            serviceIntent.putExtra("inputExtra", "send");
            serviceIntent.putExtra("files", files);
            serviceIntent.putExtra("bt", true);
            ContextCompat.startForegroundService(this, serviceIntent);
            activityMainBinding.bottomBar.setSelectedItemId(R.id.navTransfers);
        });
    }

    public void requestForFolderWOFrag(String folderPath) {
        if (CommunicationProtocol.getInstance().isConnected()) {
            Intent serviceIntent = new Intent(this, RXConnectionFGService.class);
            serviceIntent.putExtra("inputExtra", "folder");
            serviceIntent.putExtra("folderPath", folderPath);
            ContextCompat.startForegroundService(this, serviceIntent);
        } else
            replaceFragmentWithoutAnimation(BladeFolderFragment.newInstance(new ArrayList<>(), ""), false);
        updateTitle(TextUtils.isEmpty(folderPath) ? getString(R.string.blade_files) : folderPath);
    }

    public void requestForFolder(String folderPath) {
        showSearchInTopBar();
        showMenuInTopBar();
        requestForFolderWOFrag(folderPath);
    }

    public void passCommandToBlade(JSONObject jsonObject) {

    }

    public void requestForFilesOriginal(long size, ArrayList<String> fileNames) {
        String defaultPath = AppStorage.getInstance(this).getValue(AppStorage.SP_DEFAULT_INCOMING_FOLDER, "");
        if (TextUtils.isEmpty(defaultPath)) {
            defaultPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SocialBladeProtocol.BLADE_FOLDER).getAbsolutePath();
        }
        DialogUtils.showDownloadFileDialog(this, "Do you want to change the download path from " + defaultPath,
                (dialog, which) -> {
                    DialogUtils.showFavouritesDialog(MainActivity.this, false, v -> requestForOriginalFiles(size, fileNames));
                }, (dialog, which) -> {
                    requestForOriginalFiles(size, fileNames);
                });
    }

    private void requestForOriginalFiles(long size, ArrayList<String> fileNames) {
        if (fileNames.size() == 1) {
        } else
            activityMainBinding.bottomBar.setSelectedItemId(R.id.navTransfers);
        Intent serviceIntent = new Intent(MainActivity.this, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "folder");
        serviceIntent.putExtra("fileNames", fileNames);
        serviceIntent.putExtra("size", size);
        ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
    }

    public void updateTitle(String folderName) {
        isSelectionTopBar = false;
        try {
            if (TextUtils.isEmpty(folderName) || folderName.equalsIgnoreCase("0")) {
                activityMainBinding.txtFolderName.setText(R.string.phone_files);
                activityMainBinding.imgBack.setVisibility(View.GONE);
            } else {
                activityMainBinding.txtFolderName.setText(folderName);
                activityMainBinding.imgBack.setVisibility(View.VISIBLE);
            }
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                activityMainBinding.imgBack.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public void useSelectionTopBar(String heading) {
        try {
            isSelectionTopBar = true;
            activityMainBinding.txtFolderName.setText(heading);
            activityMainBinding.imgBack.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.mainFrame);
    }

    public void cancelTransfer(TransferModel transferModel) {
        new DBHelper(this).cancelTransfer(transferModel);
        requestToStopTransfer();
    }

    public void cancelTransfers(ArrayList<TransferModel> transferModels) {
        if (transferModels.isEmpty()) {
            StaticUtils.showToast(this, "No Ongoing Transfers to Cancel.");
        } else {
            new DBHelper(this).cancelTransfers(transferModels);
            requestToStopTransfer();
        }
    }

    private void requestToStopTransfer() {
        Intent serviceIntent = new Intent(this, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "stopTransfer");
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void dealWithUSB() {
        if (mUsbManager == null)
            mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(mUsbAttachReceiver, filter);
        filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbDetachReceiver, filter);

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        showDevices();

    }

    private void showDevices() {
        Drawable folder = ContextCompat.getDrawable(this, R.drawable.ic_usb);
        activityMainBinding.sideNavigationView.getMenu().removeGroup(2);
        deviceList = mUsbManager.getDeviceList();
        int counter = 0;
        for (UsbDevice device : deviceList.values()) {
            counter++;
            if (!mUsbManager.hasPermission(device))
                mUsbManager.requestPermission(device, mPermissionIntent);
            try {
                usbDevice = device;
                StaticUtils.showToast(this, device.getDeviceName() + "\n" + device.getManufacturerName() + "\n" + device.getProductName());
                activityMainBinding.sideNavigationView.getMenu().add(R.id.menuExternalDevices, counter, counter, device.getManufacturerName() + " " + device.getProductName()).setIcon(folder);
            } catch (Exception e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
    }

    private void setBottomBar() {
        activityMainBinding.bottomBar.setOnNavigationItemSelectedListener(item -> {
            Fragment currFrag = getCurrentFragment();
            switch (item.getItemId()) {
                case R.id.navFilesManager:
                    showSearchInTopBar();
                    showMenuInTopBar();
                    if (!(currFrag instanceof FolderFragment)) {
                        clearBackStackCompletely();
                        updateTitle(AppConstants.HOME_DIRECTORY.getName());
                        replaceFragmentWithoutAnimation(FolderFragment.newInstance(), false);
                    }
                    return true;
                case R.id.navConnectivity:
                    updateTitle(getString(R.string.connectivity));
                    hideSearchInTopBar();
                    hideMenuInTopBar();
                    clearBackStackCompletely();
                    replaceFragmentWithoutAnimation(new ConnectivityFragment(), false);
                    return true;
                case R.id.navDeviceFiles:
                    hideSearchInTopBar();
                    showMenuInTopBar();
                    clearBackStackCompletely();
                    requestForFolder("");
                    return true;
                case R.id.navTransfers:
                    updateTitle(getString(R.string.transfers));
                    hideSearchInTopBar();
                    showMenuInTopBar();
                    clearBackStackCompletely();
                    replaceFragmentWithoutAnimation(TransfersFragment.newInstance(), false);
                    return true;
                default:
                    break;
            }
            return false;
        });
        activityMainBinding.bottomBar.setSelectedItemId(R.id.navFilesManager);
    }

    private void hideSearchInTopBar() {
        activityMainBinding.imgSearch.setVisibility(View.GONE);
    }

    private void showSearchInTopBar() {
        activityMainBinding.imgSearch.setVisibility(View.VISIBLE);
    }

    private void hideMenuInTopBar() {
        activityMainBinding.imgMore.setVisibility(View.GONE);
    }

    private void showMenuInTopBar() {
        activityMainBinding.imgMore.setVisibility(View.VISIBLE);
    }

    Memory internalMemory = null;

    private void setUpNavAndTopBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            internalMemory = FileUtils.showStorageVolumes(this);
            if (internalMemory == null) {
                long totalInternalValue = FileUtils.getTotalInternalMemorySize();
                long freeInternalValue = FileUtils.getAvailableInternalMemorySize();
                long usedInternalValue = totalInternalValue - freeInternalValue;
                internalMemory = new Memory(FileUtils.getFileSize(usedInternalValue), FileUtils.getFileSize(freeInternalValue), FileUtils.getFileSize(totalInternalValue));
            }
        } else {
            long totalInternalValue = FileUtils.getTotalInternalMemorySize();
            long freeInternalValue = FileUtils.getAvailableInternalMemorySize();
            long usedInternalValue = totalInternalValue - freeInternalValue;
            internalMemory = new Memory(FileUtils.getFileSize(usedInternalValue), FileUtils.getFileSize(freeInternalValue), FileUtils.getFileSize(totalInternalValue));
        }
        setSupportActionBar(activityMainBinding.relTopBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_ham_menu);
        }
        drawerToggle = new ActionBarDrawerToggle(this, activityMainBinding.drawerLayout, activityMainBinding.relTopBar,
                0, 0) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                setDrawerIndicatorEnabled(true);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        activityMainBinding.relTopBar.setNavigationOnClickListener(v -> {
            toggleHamMenu();
        });
        activityMainBinding.drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        activityMainBinding.relTopBar.setNavigationIcon(R.drawable.ic_ham_menu);
//        activityMainBinding.sideNavigationView.getMenu().getItem(4).setTitle(Build.MANUFACTURER + " " + Build.MODEL);
        activityMainBinding.sideNavigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menuImages:
                    selectEasyAccessMenu(AppConstants.CONST_IMAGES);
                    break;
                case R.id.menuVideos:
                    selectEasyAccessMenu(AppConstants.CONST_VIDEOS);
                    break;
                case R.id.menuAudio:
                    selectEasyAccessMenu(AppConstants.CONST_AUDIO);
                    break;
                case R.id.menuInternalStorage:
                    StaticUtils.showToast(this, "Used: " + internalMemory.getUsedSpace() + " out of " + internalMemory.getTotalSpace() + "\n Available Memory: " + internalMemory.getFreeSpace());
                    clearBackStackCompletely();
                    updateTitle(AppConstants.HOME_DIRECTORY.getName());
                    activityMainBinding.bottomBar.setSelectedItemId(R.id.navFilesManager);
                    replaceFragmentWithoutAnimation(FolderFragment.newInstance(), false);
                    break;
                case R.id.menuDrive:
                    DialogUtils.showImportFromDriveDialog(this, v -> viewModel.requestDownloadFile(/*getApplicationContext(),*/viewModel.getCurrentFileFolderItem().file, (String) v.getTag()));
                    break;
                case R.id.menuDownloads:
                    FileFolderItem fileFolderItem = new FileFolderItem(new File(AppConstants.HOME_DIRECTORY, Environment.DIRECTORY_DOWNLOADS));
                    updateTitle(getString(R.string.downloads));
                    activityMainBinding.bottomBar.setSelectedItemId(R.id.navFilesManager);
                    replaceFragmentWithoutAnimation(FolderFragment.newInstance(fileFolderItem), true);
                    break;
                case R.id.menuSettings:
                    startActivity(new Intent(this, SettingsActivity.class));
                    break;
                default:
                    if (item.getGroupId() == R.id.menuExternalDevices) {
                        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this);
                        for (UsbMassStorageDevice device : devices) {
                            try {
                                device.init();
                                currentFs = device.getPartitions().get(0).getFileSystem();
                            } catch (IOException e) {
                                e.printStackTrace();
                                FirebaseCrashlytics.getInstance().recordException(e);
                            }
                        }
                        UsbFile root = currentFs.getRootDirectory();
                        clearBackStackCompletely();
                        updateTitle(root.getName());
                        replaceFragment(ExternalStorageFolderFragment.newInstance(new FileFolderItem(root)), true);
                    }
                    break;
            }
            toggleHamMenu();
            return true;
        });
    }

    private void selectEasyAccessMenu(String constMenu) {
        clearBackStackCompletely();
        updateTitle(constMenu);
        activityMainBinding.bottomBar.setSelectedItemId(R.id.navFilesManager);
        replaceFragment(FolderFragment.newInstance(constMenu), true);
    }

    private void setUpTheme() {
        ThemeHelper.applyTheme(this, appStorage.getValue(AppStorage.SP_DEVICE_MODE, ThemeHelper.defaultMode));
    }

    private void setClickListeners() {
        activityMainBinding.imgBack.setOnClickListener(this);
        activityMainBinding.imgSettings.setOnClickListener(this);
        activityMainBinding.imgSearch.setOnClickListener(this);
    }

    private void toggleHamMenu() {
        if (activityMainBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            activityMainBinding.drawerLayout.closeDrawers();
        } else activityMainBinding.drawerLayout.openDrawer(GravityCompat.START, true);
    }

    public static class CopyTaskParam {
        UsbFile from;
        File to;
    }

    private class CopyTask extends AsyncTask<CopyTaskParam, Integer, Void> {

        private ProgressDialog dialog;
        private CopyTaskParam param;
        private boolean sendToBlade;

        public CopyTask(boolean sendToBlade) {
            this.sendToBlade = sendToBlade;
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setTitle("Copying files");
            dialog.setMessage("Copying the files to the internal storage, this can take some time!");
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Void doInBackground(CopyTaskParam... params) {
            long time = System.currentTimeMillis();
            param = params[0];
            try {
                OutputStream out = new BufferedOutputStream(new FileOutputStream(param.to));
                InputStream inputStream = new UsbFileInputStream(param.from);
                byte[] bytes = new byte[currentFs.getChunkSize()];
                int count;
                long total = 0;
//                Log.d(TAG, "Copy file with length: " + param.from.getLength());
                while ((count = inputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, count);
                    total += count;
                    int progress = (int) total;
                    if (param.from.getLength() > Integer.MAX_VALUE) {
                        progress = (int) (total / 1024);
                    }
                    publishProgress(progress);
                }
                out.close();
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "error copying!", e);
                FirebaseCrashlytics.getInstance().recordException(e);
            }
            Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();
            File file = new File(param.to.getAbsolutePath());
            if (sendToBlade) {
                try {
                    sendFileToBlade(file);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            } else {
                StaticUtils.showToast(MainActivity.this, "Copied to Downloads folder.");
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int max = (int) param.from.getLength();
            if (param.from.getLength() > Integer.MAX_VALUE) {
                max = (int) (param.from.getLength() / 1024);
            }
            dialog.setMax(max);
            dialog.setProgress(values[0]);
        }

    }

    private class MultipleFilesTask extends AsyncTask<ArrayList<CopyTaskParam>, Integer, Void> {

        private ProgressDialog dialog;
        private ArrayList<CopyTaskParam> param;
        private boolean sendToBlade;

        public MultipleFilesTask(boolean sendToBlade) {
            this.sendToBlade = sendToBlade;
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setTitle("Copying files");
            dialog.setMessage("Copying the files to the internal storage, this can take some time!");
            dialog.setIndeterminate(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
        }

        @Override
        protected Void doInBackground(ArrayList<CopyTaskParam>... arrayLists) {
            long time = System.currentTimeMillis();
            param = arrayLists[0];
            for (CopyTaskParam copyTaskParam : param) {
                try {
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(copyTaskParam.to));
                    InputStream inputStream = new UsbFileInputStream(copyTaskParam.from);
                    byte[] bytes = new byte[currentFs.getChunkSize()];
                    int count;
                    long total = 0;
                    while ((count = inputStream.read(bytes)) != -1) {
                        out.write(bytes, 0, count);
                        total += count;
                        int progress = (int) total;
                        if (copyTaskParam.from.getLength() > Integer.MAX_VALUE) {
                            progress = (int) (total / 1024);
                        }
                        publishProgress(progress);
                    }
                    out.close();
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "error copying!", e);
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time));
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();
            String[] paths = new String[param.size()];
            for (int i = 0; i < param.size(); i++) {
                paths[i] = param.get(i).to.getAbsolutePath();
            }
            if (sendToBlade) {
                try {
                    sendFilesToBlade(paths);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            } else {
                StaticUtils.showToast(MainActivity.this, "Copied files to Downloads folder.");
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }

    }

    /*
    try {
            if (!entry.isDirectory()) {
                CopyTaskParam param = new CopyTaskParam();
                param.from = entry;
                File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/usbfileman/cache");
                f.mkdirs();
                int index = entry.getName().lastIndexOf(".") > 0 ? entry.getName().lastIndexOf(".") : entry.getName().length();
                String prefix = entry.getName().substring(0, index);
                String ext = entry.getName().substring(index);
                // prefix must be at least 3 characters
                if (prefix.length() < 3) {
                    prefix += "pad";
                }
                param.to = File.createTempFile(prefix, ext, f);
                new CopyTask().execute(param);
            }
        } catch (IOException e) {
            Log.e(TAG, "error staring to copy!", e);
        }
    */

}