package com.webilize.vuzixfilemanager.utils.transferutils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.webilize.transfersdk.RXConnection;
import com.webilize.transfersdk.socket.DataWrapper;
import com.webilize.transfersdk.socket.SocketState;
import com.webilize.vuzixfilemanager.utils.AppStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

public class SocialBladeProtocol {

    private static final String TAG = "SocialBladeProtocol";
    private static final Integer DEFAULT_PAGE_SIZE = 30;

    //region folder names
    private static final String THUMBNAILS_FOLDER = "thumbnails";
    public static final String BLADE_FOLDER = "blade";
    //endregion

    //region folders
    public static File getThumbnailsFolder(Context context) {
        File folder = new File(context.getApplicationContext().getCacheDir(), THUMBNAILS_FOLDER);
        if (!folder.exists())
            folder.mkdirs();
        return folder;
    }

    public static File getExternalFolder(Context context) {
        String defaultPath = AppStorage.getInstance(context).getValue(AppStorage.SP_DEFAULT_INCOMING_FOLDER, "");
        File folder = TextUtils.isEmpty(defaultPath) ? new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), BLADE_FOLDER) : new File(defaultPath);
        if (!folder.exists())
            folder.mkdirs();
        return folder;
    }
    //endregion

    // region server methods
    public static Single<Response> onRead(RXConnection rxConnection, PublishSubject<DataWrapper> publisher) {
        return rxConnection.read(publisher).map(data -> {
            if (data.getSocketState() == SocketState.JSON_RECEIVED) {
                JSONObject jsonObject = (JSONObject) data.getData();
                Log.d(TAG, "onRead: " + jsonObject.toString());
                return onJSONReceived(jsonObject);
            } else if (data.getSocketState() == SocketState.DISCONNECTED ||
                    data.getSocketState() == SocketState.CLIENT_DISCONNECTED) {
                rxConnection.clientDisconnected();

                throw new Exception("Connection lost");
            }
            return null;
        });
    }

    public static Response onJSONReceived(JSONObject jsonObject) throws JSONException {
        Response response;
        if (jsonObject.has("thumbnails")) {
            response = new Response(Response.Type.THUMBNAILS);
            if (jsonObject.has("firstLoad")) {
                response.setFirstLoad(jsonObject.getBoolean("firstLoad"));
            }
            if (jsonObject.has("pageSize")) {
                response.setPageSize(jsonObject.getInt("pageSize"));
            }
            //todo: resolution
            if (jsonObject.has("thumbnails")) {
                JSONArray thumbnails = jsonObject.getJSONArray("thumbnails");
                ArrayList<String> fileNames = new ArrayList<>();

                for (int i = 0; i < thumbnails.length(); i++) {
                    fileNames.add(thumbnails.getJSONObject(i).getString("fileName"));
                    //todo: dates & sizes
                }
                response.setFileNames(fileNames);
            }
//            if (jsonObject.has(""))
        } else {
            response = new Response(Response.Type.ORIGINALS);
            if (jsonObject.has("selectedThumbnails")) {
                JSONArray thumbnails = jsonObject.getJSONArray("selectedThumbnails");
                ArrayList<String> fileNames = new ArrayList<>();
                for (int i = 0; i < thumbnails.length(); i++) {
                    fileNames.add(thumbnails.getJSONObject(i).getString("fileName"));
                    //todo: dates & sizes
                }
                response.setFileNames(fileNames);
            }
        }
        return response;
    }

    public static Completable sendThumbnails(RXConnection rxConnection, List<File> files, PublishSubject<DataWrapper> publisher) {

        Log.d(TAG, "sendThumbnails: " + files.size());

        return rxConnection.write(files, publisher);
    }

    public static Single<JSONObject> sendOriginals(RXConnection rxConnection, List<File> files, PublishSubject<DataWrapper> publisher) {
        Log.d(TAG, "sendOriginals: " + files.size());
        return rxConnection.write(files, publisher).andThen(rxConnection.read(publisher)).map(dataWrapper -> {
            if (dataWrapper.getSocketState() == SocketState.DISCONNECTED ||
                    dataWrapper.getSocketState() == SocketState.CLIENT_DISCONNECTED) {
                rxConnection.clientDisconnected();
                throw new Exception("Connection lost");
            } else {
                if (dataWrapper.getSocketState() == SocketState.JSON_RECEIVED) {
                    return (JSONObject) dataWrapper.getData();
                } else if (dataWrapper.getSocketState() == SocketState.MULTIPLE_FILES) {
                    return new JSONObject();
                }
                return new JSONObject();
            }
        });
    }
    // endregion

    //region client methods
    public static Single<List<File>> requestThumbnails(Context context, final RXConnection rxConnection
            , PublishSubject<DataWrapper> publisher) throws JSONException {
        File folder = getThumbnailsFolder(context);
        return requestFiles(rxConnection, cacheStatus(folder, DEFAULT_PAGE_SIZE), folder, publisher);
    }

    public static Single<JSONObject> requestFolders(Context context, String folderPath, final RXConnection rxConnection
            , PublishSubject<DataWrapper> publisher, boolean isOnlyFolders) throws JSONException {
        File folder = getThumbnailsFolder(context);
        return requestFolders(rxConnection, cacheStatus(folderPath, isOnlyFolders), folder, publisher);
    }

    public static Single<JSONObject> sendCommand(JSONObject jsonObject, final RXConnection rxConnection, PublishSubject<DataWrapper> publisher) throws JSONException {
        return rxConnection.write(jsonObject, publisher).andThen(rxConnection.read(publisher)).map(dataWrapper -> {
            if (dataWrapper.getSocketState() == SocketState.DISCONNECTED ||
                    dataWrapper.getSocketState() == SocketState.CLIENT_DISCONNECTED) {
                rxConnection.clientDisconnected();
                throw new Exception("Connection lost");
            } else {
                if (dataWrapper.getSocketState() == SocketState.JSON_RECEIVED) {
                    return (JSONObject) dataWrapper.getData();
                } else if (dataWrapper.getSocketState() == SocketState.MULTIPLE_FILES) {
                    return new JSONObject();
                }
                return new JSONObject();
            }
        });
    }

    public static Single<JSONObject> setDestinationFolder(Context context, final RXConnection rxConnection
            , PublishSubject<DataWrapper> publisher, String destinationPath) throws JSONException {
        File folder = getThumbnailsFolder(context);
        return setDestinationFolder(rxConnection, cacheStatus(destinationPath), folder, publisher);
    }

    public static Single<List<File>> requestOriginals(Context context, final RXConnection rxConnection, List<String> fileNames, PublishSubject<DataWrapper> publisher) throws JSONException {
        File folder = getExternalFolder(context);
        return requestFiles(rxConnection, selectedThumbnails(fileNames), folder, publisher);
    }

    public static Single<List<File>> requestOriginals(Context context, final RXConnection rxConnection, String[] fileNames, PublishSubject<DataWrapper> publisher) throws JSONException {
        File folder = getExternalFolder(context);
        return requestFiles(rxConnection, selectedFilePaths(fileNames), folder, publisher);
    }
    //endregion

    //region privates
    private static Single<List<File>> requestFiles(final RXConnection rxConnection, JSONObject jsonObject, File folder
            , PublishSubject<DataWrapper> publisher) {
        rxConnection.setDefaultFolder(folder);
        return rxConnection.write(jsonObject, publisher)
                .andThen(rxConnection.read(publisher))
                .map(dataWrapper -> {
                    if (dataWrapper.getSocketState() == SocketState.MULTIPLE_FILES) {
                        return (List<File>) dataWrapper.getData();
                    } else if (dataWrapper.getSocketState() == SocketState.DISCONNECTED ||
                            dataWrapper.getSocketState() == SocketState.CLIENT_DISCONNECTED) {
                        rxConnection.clientDisconnected();
                        throw new Exception("Connection lost");
                    }
                    return new ArrayList<>();
                });
    }

    private static Single<JSONObject> requestFolders(final RXConnection rxConnection, JSONObject jsonObject, File folder
            , PublishSubject<DataWrapper> publisher) {
        rxConnection.setDefaultFolder(folder);
        return rxConnection.write(jsonObject, publisher).andThen(rxConnection.read(publisher)).map(dataWrapper -> {
            if (dataWrapper.getSocketState() == SocketState.DISCONNECTED ||
                    dataWrapper.getSocketState() == SocketState.CLIENT_DISCONNECTED) {
                rxConnection.clientDisconnected();
                throw new Exception("Connection lost");
            } else {
                if (dataWrapper.getSocketState() == SocketState.JSON_RECEIVED) {
                    return (JSONObject) dataWrapper.getData();
                } else if (dataWrapper.getSocketState() == SocketState.MULTIPLE_FILES) {
                    return new JSONObject();
                }
                return new JSONObject();
            }
        });
    }

    public static Single<JSONObject> requestForDeviceDetails(final RXConnection rxConnection, PublishSubject<DataWrapper> publisher) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("deviceDetails", "deviceDetails");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rxConnection.write(jsonObject, publisher).andThen(rxConnection.read(publisher)).map(dataWrapper -> {
            if (dataWrapper.getSocketState() == SocketState.DISCONNECTED || dataWrapper.getSocketState() == SocketState.CLIENT_DISCONNECTED) {
                rxConnection.clientDisconnected();
                throw new Exception("Connection lost");
            } else {
                if (dataWrapper.getSocketState() == SocketState.JSON_RECEIVED) {
                    return (JSONObject) dataWrapper.getData();
                } else if (dataWrapper.getSocketState() == SocketState.MULTIPLE_FILES) {
                    return jsonObject;
                }
                return jsonObject;
            }
        });
    }

    private static Single<JSONObject> setDestinationFolder(final RXConnection rxConnection, JSONObject jsonObject, File folder, PublishSubject<DataWrapper> publisher) {
        rxConnection.setDefaultFolder(folder);
        return rxConnection.write(jsonObject, publisher).andThen(rxConnection.read(publisher)).map(dataWrapper -> {
            if (dataWrapper.getSocketState() == SocketState.DISCONNECTED ||
                    dataWrapper.getSocketState() == SocketState.CLIENT_DISCONNECTED) {
                rxConnection.clientDisconnected();
                throw new Exception("Connection lost");
            } else {
                if (dataWrapper.getSocketState() == SocketState.JSON_RECEIVED) {
                    return (JSONObject) dataWrapper.getData();
                } else if (dataWrapper.getSocketState() == SocketState.MULTIPLE_FILES) {
                    return new JSONObject();
                }
                return new JSONObject();
            }
        });
    }

    public static JSONObject filesToSendJSON(List<File> files) throws JSONException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (File file : files) {
            JSONObject metadata = new JSONObject();
            metadata.put("fileName", file.getName());
            metadata.put("size", file.length());

            Date date = new Date(file.lastModified());

            metadata.put("date", df.format(date));
            jsonArray.put(metadata);
        }
        jsonObject.put("thumbnails", jsonArray);
        jsonObject.put("folderPath", "root");
        return jsonObject;
    }

    private static JSONObject selectedThumbnails(List<String> mediaFiles) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        JSONArray selectedThumbnails = new JSONArray();

        for (String mediaFile : mediaFiles) {
            JSONObject fileJSON = new JSONObject();
            fileJSON.put("fileName", mediaFile);
            selectedThumbnails.put(fileJSON);
        }

        jsonObject.put("selectedThumbnails", selectedThumbnails);
        return jsonObject;
    }

    private static JSONObject selectedFilePaths(String[] mediaFiles) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        JSONArray selectedThumbnails = new JSONArray();

        for (String mediaFile : mediaFiles) {
            JSONObject fileJSON = new JSONObject();
            fileJSON.put("fileName", mediaFile);
            selectedThumbnails.put(fileJSON);
        }

        jsonObject.put("selectedThumbnails", selectedThumbnails);
        return jsonObject;
    }

    private static JSONObject cacheStatus(File folder, int pageSize) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("pageSize", pageSize);
        // todo: add resolution  jsonObject.put("resolution", );
        JSONArray thumbnails = new JSONArray();
        for (File file : folder.listFiles()) {
            JSONObject fileJson = new JSONObject();
            fileJson.put("fileName", file.getName());
            fileJson.put("size", file.length());
            fileJson.put("date", dateToString(new Date(file.lastModified())));
            thumbnails.put(fileJson);
        }
        jsonObject.put("thumbnails", thumbnails);
        return jsonObject;
    }

    private static JSONObject cacheStatus(String folderPath, boolean isOnlyFolders) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("folderPath", folderPath);
        if (isOnlyFolders)
            jsonObject.put("isOnlyFolders", isOnlyFolders);
        return jsonObject;
    }

    private static JSONObject cacheStatus(String destinationPath) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("destinationPath", destinationPath);
        return jsonObject;
    }

    private static String dateToString(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
        return df.format(date);
    }
    //endregion

}
