package com.webilize.vuzixfilemanager.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.webilize.transfersdk.helpers.WifiHelper;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.services.RXConnectionFGService;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class QRCodeDialog extends Dialog implements DialogInterface.OnDismissListener {
    private static final String TAG = "QRCodeDialog";

    private static final int DEFAULT_QR_SIZE = 300;
    private Integer port;
    private String ip;
    boolean onSuccess = false;

    CompositeDisposable bag = new CompositeDisposable();

    //region UI Elements
    private ImageView qrImageView;
    private ProgressBar progressBar;
    private Button skipButton;
    //endregion

    //region constructors
    public QRCodeDialog(@NonNull Context context) {
        super(context);
    }

    public QRCodeDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    public QRCodeDialog(@NonNull Context context, int themeResId, String ip, Integer port) {
        super(context, themeResId);
        this.ip = ip;
        this.port = port;
    }

    protected QRCodeDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }
    //endregion

    //region implements DialogInterface.OnDismissListener
    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        if (!onSuccess) {
            CommunicationProtocol cp = CommunicationProtocol.getInstance();
            bag.add(cp.disconnect().subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                                Log.d(TAG, "Current server disconnected");
                                bag.dispose();
                            }
                            , throwable -> Log.e(TAG, "Error disconnecting server")));

        } else {
            onSuccess = false;
        }
    }

    public static void stopServiceManually(Context context) {
        Intent serviceIntent = new Intent(context, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "stop");
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    //endregion

    //region lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.qr_code_dialog);

        setOnDismissListener(this);

        qrImageView = findViewById(R.id.qrCodeView);
        progressBar = findViewById(R.id.progressBar);
        skipButton = findViewById(R.id.skipButton);
        skipButton.setOnClickListener(view -> {
            dismiss();
            stopServiceManually(getContext().getApplicationContext());
        });

        startGeneratingQR();
    }
    //endregion

    //region private
    private void setLoading(boolean loading) {
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            qrImageView.setVisibility(View.INVISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            qrImageView.setVisibility(View.VISIBLE);
        }
    }

    private Bitmap encodeAsBitmap(String str, int WIDTH) throws WriterException {
        Log.d(TAG, "encodeAsBitmap: width" + WIDTH);
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                //pixels[offset + x] = result.get(x, y) ? getContext().getResources().getColor(R.color.white) : getContext().getResources().getColor(R.color.charcoal);
                pixels[offset + x] = result.get(x, y) ? getContext().getResources().getColor(R.color.colorBlack) : getContext().getResources().getColor(R.color.colorWhite);
                // pixels[offset + x] = result.get(x, y) ? getContext().getResources().getColor(R.color.colorAccent) : getContext().getResources().getColor(R.color.charcoal);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, WIDTH, 0, 0, w, h);
        return bitmap;
    }

    private void startGeneratingQR() {
        setLoading(true);
        qrImageView.setImageDrawable(null);
        Single<Bitmap> observable = Single.create(emitter -> {
            try {
                Context context = getContext().getApplicationContext();
                String ip = this.ip != null ? this.ip : WifiHelper.getIp(context);
                int port = this.port != null ? this.port : CommunicationProtocol.getInstance().getConnectionPort();
                String ssid = WifiHelper.getSSID(context);
                try {
                    JSONObject jsonObject = CommunicationProtocol.createQRContent(ip, port, ssid);
                    Log.d(TAG, "Connection JSON " + jsonObject.toString());
                    Bitmap bitmap = encodeAsBitmap(jsonObject.toString(), DEFAULT_QR_SIZE);
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(bitmap);
                    }
                } catch (JSONException e) {
                    emitter.onError(e);
                }
            } catch (Exception ex) {
                emitter.onError(ex);
            }
        });
        observable.subscribeOn(Schedulers.io())               //observable will run on IO thread.
                .observeOn(AndroidSchedulers.mainThread())  //Observer will run on main thread
                .subscribe(new SingleObserver<Bitmap>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        bag.add(d);
                    }

                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        setLoading(false);
                        qrImageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(Throwable e) {
                        setLoading(false);
                        Toast.makeText(getContext(), "Error generating QR Code", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void close() {
        onSuccess = true;
        dismiss();
    }
    //endregion

}
