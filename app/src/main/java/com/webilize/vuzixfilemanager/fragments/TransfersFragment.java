package com.webilize.vuzixfilemanager.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.activities.MainActivity;
import com.webilize.vuzixfilemanager.adapters.TransfersAdapter;
import com.webilize.vuzixfilemanager.databinding.FragmentTransfersBinding;
import com.webilize.vuzixfilemanager.dbutils.DBHelper;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
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
    private TransfersAdapter transfersAdapter;
    private PopupMenu popupMenu, itemPopUpMenu;
    private TransferModel selectedFile;

    public static TransfersFragment newInstance() {
        return new TransfersFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mainActivity = (MainActivity) context;
            cp = CommunicationProtocol.getInstance();
            dbHelper = new DBHelper(mainActivity);
        } catch (Exception e) {
            e.printStackTrace();
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
            if (cp.isConnected()) {
                fragmentTransfersBinding.txtDeviceName.setText(StaticUtils.getDeviceName(mainActivity));
            } else fragmentTransfersBinding.txtDeviceName.setText("");
            fragmentTransfersBinding.txtConnectionType.setText(StaticUtils.getConnectionType());
        } catch (Exception e) {
            e.printStackTrace();
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
        selectedFile = transferModelArrayListOutGoing.get(position);
        switch (view.getId()) {
            case R.id.imgMore:
                prepareItemPopUpMenu(view, transferModelArrayListOutGoing.get(position));
                break;
        }
    }

    @Override
    public void onLongClick(View view, int position) {
//        if (navigationListener != null)
//            navigationListener.openDetails(mainActivity.viewModel.getFile(position));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketConnected(OnSocketConnected onSocketConnected) {
        try {
            fragmentTransfersBinding.txtDeviceName.setText(StaticUtils.getDeviceName(mainActivity));
            fragmentTransfersBinding.txtConnectionType.setText(StaticUtils.getConnectionType());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onProgressUpdated(OnProgressUpdated onProgressUpdated) {
        try {
            if (onProgressUpdated.transferModel != null) {
                if (transferModelArrayListOutGoing.contains(onProgressUpdated.transferModel)) {
                    getIndex(onProgressUpdated.transferModel);
                } else {
                    transferModelArrayListOutGoing.add(0, onProgressUpdated.transferModel);
                    setAdapter();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    private void getIndex(TransferModel transferModel) {
        if (transferModelArrayListOutGoing != null && !transferModelArrayListOutGoing.isEmpty()) {
            for (int i = 0; i < transferModelArrayListOutGoing.size(); i++) {
                if (transferModel.id == transferModelArrayListOutGoing.get(i).id) {
                    transferModelArrayListOutGoing.set(i, transferModel);
                    if (transfersAdapter != null) transfersAdapter.notifyItemChanged(i);
//                    fragmentTransfersBinding.recyclerView.smoothScrollToPosition(i);
                    return;
                }
            }
            getTransfersData();
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
                    mainActivity.cancelTransfers(getOnGoingTransfers());
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

    private ArrayList<TransferModel> getOnGoingTransfers() {
        ArrayList<TransferModel> onGoingTransfersArrayList = new ArrayList<>();
        for (TransferModel transferModel : transferModelArrayListOutGoing) {
            if (transferModel.status == AppConstants.CONST_TRANSFER_ONGOING) {
                onGoingTransfersArrayList.add(transferModel);
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
                        mainActivity.cancelTransfer(selectedFile);
                    }
                    return false;
                case R.id.menuShowLocation:
                    mainActivity.open(new FileFolderItem(new File(transferModel.folderLocation)));
                    return false;
                case R.id.menuRemove:
                    DialogUtils.showDeleteDialog(mainActivity, getString(R.string.are_you_sure_you_want_to_delete_the_files), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dbHelper.deleteTransferModel(selectedFile.id + "");
                            transferModelArrayListOutGoing.remove(selectedFile);
                            transfersAdapter.notifyDataSetChanged();
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
        RecyclerView recyclerView;
        TextView txtNoDataFound;

        public TransfersPagerAdapter(Context context) {
            mContext = context;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.fragment_transfers_list, collection, false);
            recyclerView = layout.findViewById(R.id.recyclerView);
            txtNoDataFound = layout.findViewById(R.id.txtNoDataFound);
            if (position == 0) {
                recyclerView.addItemDecoration(new DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL_LIST));
                recyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));
                transfersAdapter = new TransfersAdapter(mainActivity, transferModelArrayListIncoming, new IClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        selectedFile = transferModelArrayListIncoming.get(position);
                        switch (view.getId()) {
                            case R.id.imgMore:
                                prepareItemPopUpMenu(view, selectedFile);
                                break;
                        }
                    }

                    @Override
                    public void onLongClick(View view, int position) {

                    }
                });
                recyclerView.setAdapter(transfersAdapter);
                checkForVisibility(true);
            } else {
                recyclerView.addItemDecoration(new DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL_LIST));
                recyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));
                transfersAdapter = new TransfersAdapter(mainActivity, transferModelArrayListOutGoing, new IClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        selectedFile = transferModelArrayListOutGoing.get(position);
                        switch (view.getId()) {
                            case R.id.imgMore:
                                prepareItemPopUpMenu(view, selectedFile);
                                break;
                        }
                    }

                    @Override
                    public void onLongClick(View view, int position) {

                    }
                });
                recyclerView.setAdapter(transfersAdapter);
                checkForVisibility(false);
            }
            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
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

        private void checkForVisibility(boolean isIncoming) {
            if (isIncoming) {
                if (transferModelArrayListOutGoing == null || transferModelArrayListOutGoing.isEmpty()) {
                    txtNoDataFound.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    txtNoDataFound.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            } else {
                if (transferModelArrayListIncoming == null || transferModelArrayListIncoming.isEmpty()) {
                    txtNoDataFound.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    txtNoDataFound.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        }

    }

    private void setAdapter() {
        fragmentTransfersBinding.viewPager.setAdapter(new TransfersPagerAdapter(mainActivity));
        fragmentTransfersBinding.tabLayout.setupWithViewPager(fragmentTransfersBinding.viewPager);
    }

}
