package com.webilize.vuzixfilemanager.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.activities.MainActivity;
import com.webilize.vuzixfilemanager.adapters.TransfersAdapter;
import com.webilize.vuzixfilemanager.databinding.FragmentTransfersBinding;
import com.webilize.vuzixfilemanager.dbutils.DBHelper;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.interfaces.NavigationListener;
import com.webilize.vuzixfilemanager.models.FileFolderItem;
import com.webilize.vuzixfilemanager.models.TransferModel;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.AppStorage;
import com.webilize.vuzixfilemanager.utils.DialogUtils;
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.customviews.DividerItemDecoration;
import com.webilize.vuzixfilemanager.utils.eventbus.OnProgressUpdated;
import com.webilize.vuzixfilemanager.utils.eventbus.OnSocketConnected;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;

public class TransfersFragment extends BaseFragment implements IClickListener, View.OnClickListener {

    private FragmentTransfersBinding fragmentTransfersBinding;
    private View rootView;
    private MainActivity mainActivity;
    private DBHelper dbHelper;
    private CommunicationProtocol cp;
    private ArrayList<TransferModel> transferModelArrayListOutGoing, transferModelArrayListIncoming;
    private TransfersAdapter transfersAdapterIncoming, transfersAdapterOutGoing;
    private PopupMenu popupMenu, itemPopUpMenu;
    private TransferModel selectedFile;
    private NavigationListener navigationListener;
    private TransfersPagerAdapter transfersPagerAdapter;

    public static TransfersFragment newInstance() {
        return new TransfersFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mainActivity = (MainActivity) context;
            navigationListener = (NavigationListener) context;
            cp = CommunicationProtocol.getInstance();
            dbHelper = new DBHelper(mainActivity);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
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
                fragmentTransfersBinding.linDevice.txtDeviceName.setTextColor(Color.LTGRAY);
                fragmentTransfersBinding.linDevice.txtDeviceName.setText(getString(R.string.no_dev_connected));
            } else {
                fragmentTransfersBinding.linDevice.txtDeviceName.setTextColor(Color.BLACK);
                fragmentTransfersBinding.linDevice.txtDeviceName.setText(name);
            }
            fragmentTransfersBinding.linDevice.txtConnectionType.setText(StaticUtils.getConnectionType());
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentTransfersBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_transfers, container, false);
        rootView = fragmentTransfersBinding.getRoot();
        transferModelArrayListOutGoing = new ArrayList<>();
        transferModelArrayListIncoming = new ArrayList<>();
        return rootView;
    }

    @Override
    void initComponents() {
        mainActivity.activityMainBinding.imgMore.setOnClickListener(this);
        getTransfersData();
    }

    @Override
    public void onClick(View view, int position) {
        if (fragmentTransfersBinding.tabLayout.getSelectedTabPosition() == 0)
            selectedFile = transferModelArrayListIncoming.get(position);
        else selectedFile = transferModelArrayListOutGoing.get(position);
        if (view.getId() == R.id.imgMore) {
            prepareItemPopUpMenu(view, selectedFile);
        }
    }

    @Override
    public void onLongClick(View view, int position) {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onProgressUpdated(OnProgressUpdated onProgressUpdated) {
        try {
            if (onProgressUpdated.transferModel != null) {
                if (onProgressUpdated.transferModel.isIncoming) {
                    if (transferModelArrayListIncoming.contains(onProgressUpdated.transferModel)) {
                        getIndex(true, onProgressUpdated.transferModel);
                    } else {
                        transferModelArrayListIncoming.add(0, onProgressUpdated.transferModel);
                        transfersAdapterIncoming.notifyItemInserted(0);
                    }
                    fragmentTransfersBinding.tabLayout.selectTab(fragmentTransfersBinding.tabLayout.getTabAt(0));
                } else {
                    if (transferModelArrayListOutGoing.contains(onProgressUpdated.transferModel)) {
                        getIndex(false, onProgressUpdated.transferModel);
                    } else {
                        transferModelArrayListOutGoing.add(0, onProgressUpdated.transferModel);
                        transfersAdapterOutGoing.notifyItemInserted(0);
                    }
                    fragmentTransfersBinding.tabLayout.selectTab(fragmentTransfersBinding.tabLayout.getTabAt(1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgMore:
                if (popupMenu == null) preparePopUpMenu();
                showMenuOptions();
                break;
        }
    }

    private void getIndex(boolean isIncoming, TransferModel transferModel) {
        if (isIncoming) {
            if (transferModelArrayListIncoming != null && !transferModelArrayListIncoming.isEmpty()) {
                for (int i = 0; i < transferModelArrayListIncoming.size(); i++) {
                    if (transferModel.id == transferModelArrayListIncoming.get(i).id) {
                        transferModelArrayListIncoming.set(i, transferModel);
                        if (transfersAdapterIncoming != null)
                            transfersAdapterIncoming.notifyItemChanged(i);
                        return;
                    }
                }
                getTransfersData();
            }
        } else {
            if (transferModelArrayListOutGoing != null && !transferModelArrayListOutGoing.isEmpty()) {
                for (int i = 0; i < transferModelArrayListOutGoing.size(); i++) {
                    if (transferModel.id == transferModelArrayListOutGoing.get(i).id) {
                        transferModelArrayListOutGoing.set(i, transferModel);
                        if (transfersAdapterOutGoing != null)
                            transfersAdapterOutGoing.notifyItemChanged(i);
                        return;
                    }
                }
                getTransfersData();
            }
        }
    }

    private void showMenuOptions() {
        popupMenu.getMenu().getItem(0).setChecked(AppStorage.getInstance(mainActivity).getValue(AppStorage.SP_SHOW_FINISHED, false));
        popupMenu.show();
    }

    private void preparePopUpMenu() {
        popupMenu = new PopupMenu(mainActivity, mainActivity.activityMainBinding.imgMore);
        popupMenu.getMenuInflater().inflate(R.menu.menu_transfer, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menuCancelAllDownloads:
                    mainActivity.cancelTransfers(getOnGoingTransfers(fragmentTransfersBinding.tabLayout.getSelectedTabPosition() == 0));
                    return false;
                case R.id.menuShowFinished:
                    if (item.isChecked()) item.setChecked(false);
                    else item.setChecked(true);
                    AppStorage.getInstance(mainActivity).setValue(AppStorage.SP_SHOW_FINISHED, item.isChecked());
                    getTransfersData();
                    return false;
                default:
                    return true;
            }
        });
    }

    private ArrayList<TransferModel> getOnGoingTransfers(boolean isIncoming) {
        ArrayList<TransferModel> onGoingTransfersArrayList = new ArrayList<>();
        if (isIncoming) {
            for (TransferModel transferModel : transferModelArrayListIncoming) {
                if (transferModel.status == AppConstants.CONST_TRANSFER_ONGOING) {
                    onGoingTransfersArrayList.add(transferModel);
                }
            }
        } else {
            for (TransferModel transferModel : transferModelArrayListOutGoing) {
                if (transferModel.status == AppConstants.CONST_TRANSFER_ONGOING) {
                    onGoingTransfersArrayList.add(transferModel);
                }
            }
        }

        return onGoingTransfersArrayList;
    }

    private void prepareItemPopUpMenu(View v, TransferModel transferModel) {
        itemPopUpMenu = new PopupMenu(mainActivity, v);
        itemPopUpMenu.getMenuInflater().inflate(R.menu.menu_item_transfer, itemPopUpMenu.getMenu());
        itemPopUpMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menuCancel:
                    if (transferModel.status == AppConstants.CONST_TRANSFER_ONGOING) {
                        mainActivity.cancelTransfer(transferModel);
                    }
                    return false;
                case R.id.menuShowLocation:
                    mainActivity.activityMainBinding.bottomBar.setSelectedItemId(R.id.navFilesManager);
                    navigationListener.open(new FileFolderItem(new File(transferModel.folderLocation)));
                    return false;
                case R.id.menuRemove:
                    DialogUtils.showDeleteDialog(mainActivity, getString(R.string.are_you_sure_you_want_to_clear_the_files), getString(R.string.clear), (dialog, which) -> {
                        dbHelper.deleteTransferModel(transferModel.id + "");
                        if (fragmentTransfersBinding.tabLayout.getSelectedTabPosition() == 0) {
                            transferModelArrayListIncoming.remove(transferModel);
                            transfersAdapterIncoming.notifyDataSetChanged();
                            transfersPagerAdapter.checkForVisibility(true);
                        } else {
                            transferModelArrayListOutGoing.remove(transferModel);
                            transfersAdapterOutGoing.notifyDataSetChanged();
                            transfersPagerAdapter.checkForVisibility(false);
                        }
                    });
                    return false;
                default:
                    return true;
            }
        });
        if (transferModel.status == AppConstants.CONST_TRANSFER_ONGOING) {
            itemPopUpMenu.getMenu().getItem(0).setEnabled(true);
        } else
            itemPopUpMenu.getMenu().getItem(0).setEnabled(false);

        itemPopUpMenu.show();
    }

    private void getTransfersData() {
        if (transferModelArrayListOutGoing == null)
            transferModelArrayListOutGoing = new ArrayList<>();
        else transferModelArrayListOutGoing.clear();
        if (transferModelArrayListIncoming == null)
            transferModelArrayListIncoming = new ArrayList<>();
        else transferModelArrayListIncoming.clear();
        transferModelArrayListOutGoing.addAll(dbHelper.getTransferModelsList(false));
        transferModelArrayListIncoming.addAll(dbHelper.getTransferModelsList(true));
        setAdapter();
    }

    public class TransfersPagerAdapter extends PagerAdapter {

        private Context mContext;
        RecyclerView recyclerViewIncoming, recyclerViewOutGoing;
        TextView txtNoDataFoundIncoming, txtNoDataFoundOutGoing;

        public TransfersPagerAdapter(Context context) {
            mContext = context;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.fragment_transfers_list, collection, false);
            if (position == 0) {
                recyclerViewIncoming = layout.findViewById(R.id.recyclerView);
                txtNoDataFoundIncoming = layout.findViewById(R.id.txtNoDataFound);
                recyclerViewIncoming.addItemDecoration(new DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL_LIST));
                recyclerViewIncoming.setLayoutManager(new LinearLayoutManager(mainActivity));
                transfersAdapterIncoming = new TransfersAdapter(mainActivity, transferModelArrayListIncoming, new IClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        selectedFile = transferModelArrayListIncoming.get(position);
                        if (view.getId() == R.id.imgMore) {
                            prepareItemPopUpMenu(view, selectedFile);
                        }
                    }

                    @Override
                    public void onLongClick(View view, int position) {

                    }
                });
                recyclerViewIncoming.setAdapter(transfersAdapterIncoming);
                checkForVisibility(true);
            } else {
                recyclerViewOutGoing = layout.findViewById(R.id.recyclerView);
                txtNoDataFoundOutGoing = layout.findViewById(R.id.txtNoDataFound);
                recyclerViewOutGoing.addItemDecoration(new DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL_LIST));
                recyclerViewOutGoing.setLayoutManager(new LinearLayoutManager(mainActivity));
                transfersAdapterOutGoing = new TransfersAdapter(mainActivity, transferModelArrayListOutGoing, new IClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        selectedFile = transferModelArrayListOutGoing.get(position);
                        if (view.getId() == R.id.imgMore) {
                            prepareItemPopUpMenu(view, selectedFile);
                        }
                    }

                    @Override
                    public void onLongClick(View view, int position) {

                    }
                });
                recyclerViewOutGoing.setAdapter(transfersAdapterOutGoing);
                checkForVisibility(false);
            }
            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return getString(R.string.incoming);
            } else {
                return getString(R.string.outgoing);
            }
        }

        public void checkForVisibility(boolean isIncoming) {
            if (isIncoming) {
                if (transferModelArrayListIncoming == null || transferModelArrayListIncoming.isEmpty()) {
                    txtNoDataFoundIncoming.setVisibility(View.VISIBLE);
                    recyclerViewIncoming.setVisibility(View.GONE);
                } else {
                    txtNoDataFoundIncoming.setVisibility(View.GONE);
                    recyclerViewIncoming.setVisibility(View.VISIBLE);
                }
            } else {
                if (transferModelArrayListOutGoing == null || transferModelArrayListOutGoing.isEmpty()) {
                    txtNoDataFoundOutGoing.setVisibility(View.VISIBLE);
                    recyclerViewOutGoing.setVisibility(View.GONE);
                } else {
                    txtNoDataFoundOutGoing.setVisibility(View.GONE);
                    recyclerViewOutGoing.setVisibility(View.VISIBLE);
                }
            }
        }

    }

    private void setAdapter() {
        transfersPagerAdapter = new TransfersPagerAdapter(mainActivity);
        fragmentTransfersBinding.viewPager.setAdapter(transfersPagerAdapter);
        fragmentTransfersBinding.tabLayout.setupWithViewPager(fragmentTransfersBinding.viewPager);
    }

}
