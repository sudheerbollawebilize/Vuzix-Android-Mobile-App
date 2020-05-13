package com.webilize.vuzixfilemanager.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.adapters.FavouriteLocationsAdapter;
import com.webilize.vuzixfilemanager.adapters.SelectFolderAdapter;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.models.FileFolderItem;
import com.webilize.vuzixfilemanager.utils.customviews.AppEditText;
import com.webilize.vuzixfilemanager.utils.customviews.AppTextView;
import com.webilize.vuzixfilemanager.utils.customviews.DividerItemDecoration;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DialogUtils {

    private static AlertDialog alert;
    private static Dialog progressDialog;
    static Dialog alertDialog;
    static File currentFolder;
    static SelectFolderAdapter selectFolderAdapter;
    static ArrayList<File> stringArrayList;

    public static void showProgressDialog(Context mContext, String message, View.OnClickListener onClickListener) {
        try {
            AppTextView txtHeading = null, txtMessage = null, txtCancel = null;
            ProgressBar progressBar;

            progressDialog = new Dialog(mContext, R.style.AlertDialogCustom);
            progressDialog.setCancelable(false);
            progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            progressDialog.setContentView(R.layout.dialog_progressbar);

            txtHeading = progressDialog.findViewById(R.id.txtHeading);
            txtMessage = progressDialog.findViewById(R.id.txtMessage);
            txtCancel = progressDialog.findViewById(R.id.txtCancel);
            progressBar = progressDialog.findViewById(R.id.progressBar);

            progressDialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogCustom;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            Window window = progressDialog.getWindow();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);

            if (TextUtils.isEmpty(message)) {
                txtMessage.setText(R.string.loading);
            } else {
                txtMessage.setText(message);
            }

            txtCancel.setOnClickListener(v -> {
                progressDialog.dismiss();
                if (onClickListener != null) onClickListener.onClick(v);
            });

            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    public static void showDropDownListStrings(String title, Context context, final TextView textView, final String[] categoryNames,
                                               final View.OnClickListener clickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setItems(categoryNames, (dialog, item) -> {
            alert.dismiss();
            if (textView != null) {
                textView.setText(categoryNames[item]);
                textView.setTag(categoryNames[item]);
            }
            if (clickListener != null) {
                if (textView != null)
                    clickListener.onClick(textView);
            }
        });
        alert = builder.create();
        alert.show();
    }

    public static void showDropDownListStrings(Context context, final String[] categoryNames, final View view,
                                               String heading, final View.OnClickListener clickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (TextUtils.isEmpty(heading))
            builder.setTitle(R.string.app_name);
        else builder.setTitle(heading);
        builder.setItems(categoryNames, (dialog, item) -> {
            alert.dismiss();
            if (view != null) view.setTag(categoryNames[item]);
            if (clickListener != null) {
                clickListener.onClick(view);
            }
        });
        alert = builder.create();
        alert.setCancelable(true);
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    }

    static FavouriteLocationsAdapter favouriteLocationsAdapter;

    public static Dialog showFavouritesDialog(Context context, boolean showRemove) {
        return showFavouritesDialog(context, showRemove, null);
    }

    public static Dialog showFavouritesDialog(Context context, boolean showRemove, View.OnClickListener onClickListener) {
        try {
            List<String> locations = StaticUtils.getBookMarkedLocations(context);
            AppTextView txtHeading = null, txtHint, txtCancel;
            RecyclerView recyclerView;
            alertDialog = new Dialog(context, R.style.AlertDialogCustom);
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertDialog.setContentView(R.layout.dialog_fav_locations);
            txtHeading = alertDialog.findViewById(R.id.txtHeading);
            txtHint = alertDialog.findViewById(R.id.txtHint);
            txtCancel = alertDialog.findViewById(R.id.txtCancel);
            recyclerView = alertDialog.findViewById(R.id.recyclerView);

            alertDialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogCustom;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            Window window = alertDialog.getWindow();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);

            txtCancel.setOnClickListener(v -> alertDialog.dismiss());
            favouriteLocationsAdapter = new FavouriteLocationsAdapter(context, locations, new IClickListener() {
                @Override
                public void onClick(View view, int position) {
                    DialogUtils.showDeleteDialog(context, showRemove ? "Are you sure you want to delete the favourite?" : "Are you sure you want to set this path?", showRemove ? "Delete" : "Set", (dialog, which) -> {
                        if (showRemove) {
                            StaticUtils.removeSavedLocationFromStorage(context, locations.get(position));
                            favouriteLocationsAdapter.notifyDataSetChanged();
                        } else {
                            AppStorage.getInstance(context).setValue(AppStorage.SP_DEFAULT_INCOMING_FOLDER, locations.get(position));
                            if (onClickListener != null) onClickListener.onClick(view);
                            alertDialog.dismiss();
                        }
                    });
                }

                @Override
                public void onLongClick(View view, int position) {
                }
            }, showRemove);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
            recyclerView.setAdapter(favouriteLocationsAdapter);

            alertDialog.setCancelable(true);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return alertDialog;
    }

    public static Dialog showFolderSelectionDialog(Context context) {
        try {
            stringArrayList = new ArrayList<>();
            final File home = AppConstants.homeDirectory;
            currentFolder = home;
            setFoldersList(context);
            AppTextView txtHeading = null, txtHint, txtBack, txtCancel;
            RecyclerView recyclerView;
            alertDialog = new Dialog(context, R.style.AlertDialogCustom);
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertDialog.setContentView(R.layout.dialog_list);
            txtHeading = alertDialog.findViewById(R.id.txtHeading);
            txtHint = alertDialog.findViewById(R.id.txtHint);
            txtCancel = alertDialog.findViewById(R.id.txtCancel);
            txtBack = alertDialog.findViewById(R.id.txtBack);
            recyclerView = alertDialog.findViewById(R.id.recyclerView);

            alertDialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogCustom;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            Window window = alertDialog.getWindow();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);

            txtBack.setOnClickListener(v -> {
                String currentPath = currentFolder.getAbsolutePath();
                if (currentPath.equalsIgnoreCase(home.getAbsolutePath())) {
                    alertDialog.dismiss();
                } else {
                    getPreviousFolderList(context);
                    selectFolderAdapter.notifyDataSetChanged();
                }
            });

            txtCancel.setOnClickListener(v -> alertDialog.dismiss());
            selectFolderAdapter = new SelectFolderAdapter(context, stringArrayList, new IClickListener() {
                @Override
                public void onClick(View view, int position) {
                    currentFolder = stringArrayList.get(position);
                    setFoldersList(context);
                    if (stringArrayList.isEmpty()) {
                        StaticUtils.showToast(context, "No subfolders inside this folder");
                    }
                    selectFolderAdapter.notifyDataSetChanged();
                }

                @Override
                public void onLongClick(View view, int position) {
                    alertDialog.dismiss();
                    AppStorage.getInstance(context).setValue(AppStorage.SP_DEFAULT_INCOMING_FOLDER,
                            stringArrayList.get(position).getAbsolutePath());
                    StaticUtils.showToast(context, "Succesfully Set Incoming folder!");
                }
            });
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
            recyclerView.setAdapter(selectFolderAdapter);

            alertDialog.setCancelable(true);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return alertDialog;
    }

    private static void getPreviousFolderList(Context context) {
        String currentPath = currentFolder.getAbsolutePath();
        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
        currentFolder = new File(currentPath);
        setFoldersList(context);
    }

    private static void setFoldersList(Context context) {
        ArrayList<File> temp = new ArrayList<>();
        if (currentFolder.list() != null && currentFolder.list().length > 0) {
            for (File file : currentFolder.listFiles()) {
                if (file.isDirectory() && !file.isHidden()) {
                    temp.add(file);
                }
            }
        }
        if (!temp.isEmpty()) {
            stringArrayList.clear();
            stringArrayList.addAll(temp);
        } else StaticUtils.showToast(context, "No subfolders inside this folder");

    }

    public static void showDeleteDialog(Context context, final String message, final DialogInterface.OnClickListener clickListener) {
        showDeleteDialog(context, message, "Delete", clickListener);
    }

    public static void showDeleteDialog(Context context, final String message, String pMessage, final DialogInterface.OnClickListener clickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setTitle(context.getString(R.string.app_name));
        builder.setCancelable(false);
        builder.setPositiveButton(pMessage, (dialog, which) -> {
            clickListener.onClick(dialog, which);
            dialog.dismiss();
        });
        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public static void showSendFileDialog(Context context, final String message,
                                          final DialogInterface.OnClickListener changeClickListener,
                                          final DialogInterface.OnClickListener proceedClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setTitle(context.getString(R.string.app_name));
        builder.setCancelable(false);
        builder.setPositiveButton("Change", (dialog, which) -> {
            changeClickListener.onClick(dialog, which);
            dialog.dismiss();
        });
        builder.setNegativeButton("Proceed", (dialog, which) -> {
            proceedClickListener.onClick(dialog, which);
            dialog.dismiss();
        });
        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public static void showDownloadFileDialog(Context context, final String message,
                                              final DialogInterface.OnClickListener changeClickListener,
                                              final DialogInterface.OnClickListener proceedClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setTitle(context.getString(R.string.app_name));
        builder.setCancelable(false);
        builder.setPositiveButton("Change", (dialog, which) -> {
            changeClickListener.onClick(dialog, which);
            dialog.dismiss();
        });
        builder.setNegativeButton("Proceed", (dialog, which) -> {
            proceedClickListener.onClick(dialog, which);
            dialog.dismiss();
        });
        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public static void showSimpleDialog(final Context mContext, final String message, final View.OnClickListener positiveClick,
                                        final View.OnClickListener negativeClick) {
        showSimpleDialog(mContext, null, message, null, null, positiveClick, negativeClick, false);
    }

    public static void showSimpleDialog(final Context mContext, final String message, final View.OnClickListener positiveClick,
                                        final View.OnClickListener negativeClick, final boolean singleButton) {
        showSimpleDialog(mContext, null, message, null, null, positiveClick, negativeClick, singleButton);
    }

    public static void showSimpleDialog(final Context mContext, final String heading, final String message,
                                        final View.OnClickListener positiveClick, final View.OnClickListener negativeClick,
                                        final boolean singleButton) {
        showSimpleDialog(mContext, heading, message, null, null, positiveClick, negativeClick, singleButton, true);
    }

    public static void showSimpleDialog(final Context mContext, final String heading, final String message, final String positiveText,
                                        final String negativeText, final View.OnClickListener positiveClick,
                                        final View.OnClickListener negativeClick, final boolean singleButton) {
        showSimpleDialog(mContext, heading, message, positiveText, negativeText, positiveClick, negativeClick, singleButton, true);
    }

    public static void showSimpleDialog(final Context mContext, final String heading, final String message, final String positiveText,
                                        final String negativeText, final View.OnClickListener positiveClick,
                                        final View.OnClickListener negativeClick, final boolean singleButton, final boolean isCancelable) {
        try {
            AppTextView txtHeading = null, txtMessage = null, txtPositiveButton = null, txtNegativeButton = null;

            final Dialog alertDialog = new Dialog(mContext, R.style.AlertDialogCustom);
            alertDialog.setCancelable(false);
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//            alertDialog.setContentView(R.layout.layout_dialog);
//            txtHeading = alertDialog.findViewById(R.id.txtHeading);
//            txtMessage = alertDialog.findViewById(R.id.txtMessage);
//            txtPositiveButton = alertDialog.findViewById(R.id.txtPositive);
//            txtNegativeButton = alertDialog.findViewById(R.id.txtNegative);

            alertDialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogCustom;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            Window window = alertDialog.getWindow();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);

            txtHeading.setText(TextUtils.isEmpty(heading) ? mContext.getString(R.string.app_name) : (heading.equalsIgnoreCase("null") ? "" : heading));
            txtMessage.setText(message);

            txtPositiveButton.setText(TextUtils.isEmpty(positiveText) ? "OK" : positiveText);

            if (singleButton) {
                txtNegativeButton.setVisibility(View.GONE);
            }

            txtNegativeButton.setText(TextUtils.isEmpty(negativeText) ? "Close" : negativeText);

            txtPositiveButton.setOnClickListener(v -> {
                alertDialog.dismiss();
                if (positiveClick != null) {
                    positiveClick.onClick(v);
                }
            });

            txtNegativeButton.setOnClickListener(v -> {
                alertDialog.dismiss();
                if (negativeClick != null) {
                    negativeClick.onClick(v);
                }
            });

            alertDialog.setCancelable(isCancelable);
            alertDialog.setCanceledOnTouchOutside(isCancelable);
            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showRenameDialog(final Context mContext, final FileFolderItem fileFolderItem, final View.OnClickListener positiveClick) {
        try {
            AppTextView txtHeading = null, txtPositiveButton = null, txtNegativeButton = null, txtErrorMessage;
            AppEditText edtMessage;
            final Dialog alertDialog = new Dialog(mContext, R.style.AlertDialogCustom);
            alertDialog.setCancelable(false);
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertDialog.setContentView(R.layout.layout_edittext_dialog);
            txtHeading = alertDialog.findViewById(R.id.txtHeading);
            txtErrorMessage = alertDialog.findViewById(R.id.txtErrorMessage);
            edtMessage = alertDialog.findViewById(R.id.edtMessage);
            txtPositiveButton = alertDialog.findViewById(R.id.txtRename);
            txtNegativeButton = alertDialog.findViewById(R.id.txtCancel);

            alertDialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogCustom;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            Window window = alertDialog.getWindow();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);

            txtPositiveButton.setOnClickListener(v -> {
                String fileName = edtMessage.getText().toString().trim();
                if (!TextUtils.isEmpty(fileName)) {
                    if (fileName.matches(AppConstants.ReservedChars)) {
                        txtErrorMessage.setVisibility(View.VISIBLE);
                        txtErrorMessage.setText("Special Characters " + AppConstants.ReservedChars + " are Not Allowed");
                    } else {
                        if (renameFile(fileFolderItem.file, fileName)) {
                            v.setTag(fileName);
                            if (positiveClick != null) {
                                positiveClick.onClick(v);
                            }
                            txtErrorMessage.setVisibility(View.INVISIBLE);
                            alertDialog.dismiss();
                        } else {
                            txtErrorMessage.setText(R.string.file_name_already_exists);
                            txtErrorMessage.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    txtErrorMessage.setVisibility(View.VISIBLE);
                    txtErrorMessage.setText("Folder name cannot be empty");
                }
            });

            txtNegativeButton.setOnClickListener(v -> {
                alertDialog.dismiss();
            });


            alertDialog.setCancelable(false);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showImportFromDriveDialog(final Context mContext, final View.OnClickListener positiveClick) {
        try {
            AppTextView txtHeading = null, txtPositiveButton = null, txtNegativeButton = null, txtErrorMessage;
            AppEditText edtMessage;
            final Dialog alertDialog = new Dialog(mContext, R.style.AlertDialogCustom);
            alertDialog.setCancelable(false);
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertDialog.setContentView(R.layout.layout_drive_dialog);
            txtHeading = alertDialog.findViewById(R.id.txtHeading);
            txtErrorMessage = alertDialog.findViewById(R.id.txtErrorMessage);
            edtMessage = alertDialog.findViewById(R.id.edtMessage);
            txtPositiveButton = alertDialog.findViewById(R.id.txtDownload);
            txtNegativeButton = alertDialog.findViewById(R.id.txtCancel);

            alertDialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogCustom;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            Window window = alertDialog.getWindow();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);

            txtPositiveButton.setOnClickListener(v -> {
                String fileName = edtMessage.getText().toString().trim();
                if (!TextUtils.isEmpty(fileName)) {
                    v.setTag(fileName);
                    if (positiveClick != null) {
                        positiveClick.onClick(v);
                    }
                    txtErrorMessage.setVisibility(View.INVISIBLE);
                    alertDialog.dismiss();
                } else {
                    txtErrorMessage.setVisibility(View.VISIBLE);
                    txtErrorMessage.setText("Please enter URL.");
                }
            });

            txtNegativeButton.setOnClickListener(v -> {
                alertDialog.dismiss();
            });

            alertDialog.setCancelable(false);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean renameFile(File file, String suffix) {
        String ext = FilenameUtils.getExtension(file.getAbsolutePath());
        File dir = file.getParentFile();
        if (dir.exists()) {
            File from = new File(dir, file.getName());
            String name = file.getName();
            int pos = name.lastIndexOf(".");
            if (pos > 0) {
                name = name.substring(0, pos);
            }
            File to = new File(dir, suffix + "." + ext);
            if (to.exists()) return false;
            if (from.exists())
                return from.renameTo(to);
        }
        return false;
    }

    public static void showCreateNewFolderDialog(final Context mContext, final FileFolderItem fileFolderItem, final View.OnClickListener positiveClick) {
        try {
            AppTextView txtHeading = null, txtPositiveButton = null, txtNegativeButton = null, txtErrorMessage;
            AppEditText edtMessage;
            final Dialog alertDialog = new Dialog(mContext, R.style.AlertDialogCustom);
            alertDialog.setCancelable(false);
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertDialog.setContentView(R.layout.layout_edittext_dialog);
            txtHeading = alertDialog.findViewById(R.id.txtHeading);
            txtErrorMessage = alertDialog.findViewById(R.id.txtErrorMessage);
            edtMessage = alertDialog.findViewById(R.id.edtMessage);
            txtPositiveButton = alertDialog.findViewById(R.id.txtRename);
            txtNegativeButton = alertDialog.findViewById(R.id.txtCancel);

            txtPositiveButton.setText(R.string.create);
            txtErrorMessage.setText(R.string.folder_already_exists);

            alertDialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogCustom;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            Window window = alertDialog.getWindow();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);

            txtPositiveButton.setOnClickListener(v -> {
                String fileName = edtMessage.getText().toString().trim();
                if (!TextUtils.isEmpty(fileName)) {
                    if (fileName.matches(AppConstants.ReservedChars)) {
                        txtErrorMessage.setVisibility(View.VISIBLE);
                        txtErrorMessage.setText("Special Characters " + AppConstants.ReservedChars + " are Not Allowed");
                    } else {
                        if (FileUtils.createNewFolder(mContext, fileFolderItem.file, fileName)) {
                            v.setTag(fileName);
                            if (positiveClick != null) {
                                positiveClick.onClick(v);
                            }
                            txtErrorMessage.setVisibility(View.INVISIBLE);
                            alertDialog.dismiss();
                        } else {
                            txtErrorMessage.setVisibility(View.VISIBLE);
                            txtErrorMessage.setText(R.string.folder_already_exists);
                        }
                    }
                } else {
                    txtErrorMessage.setVisibility(View.VISIBLE);
                    txtErrorMessage.setText("Folder name cannot be empty");
                }
            });

            txtNegativeButton.setOnClickListener(v -> {
                alertDialog.dismiss();
            });

            alertDialog.setCancelable(false);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showFileInfoDialog(Context context, final FileFolderItem fileFolderItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(fileFolderItem.file.getName().equalsIgnoreCase("0") ? "Home" : fileFolderItem.file.getName());
        String message = "Type:  ";
        if (fileFolderItem.file.isDirectory()) {
            message += "Folder";
            if (fileFolderItem.file.lastModified() != 0) {
                message += "\nModified: ";
                message += DateUtils.getDateTimeFromTimeStamp(fileFolderItem.file.lastModified(), DateUtils.DATE_FORMAT_0);
            }
            if (fileFolderItem.file.list() != null && fileFolderItem.file.list().length > 0) {
                message += "\nNumber of Items: ";
                message += fileFolderItem.file.list().length + " Items";
            }
        } else {
            message += "File " + fileFolderItem.mimeType;
            if (fileFolderItem.file.lastModified() != 0) {
                message += "\nModified: ";
                message += DateUtils.getDateTimeFromTimeStamp(fileFolderItem.file.lastModified(), DateUtils.DATE_FORMAT_0);
            }
            if (fileFolderItem.file.length() != 0) {
                message += "\nSize: ";
                message += FileUtils.getFileSize(fileFolderItem.file);
            }
        }

        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton("Ok", (dialog, which) -> {
            dialog.dismiss();
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


}
