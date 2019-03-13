package com.wave.fileuploadservice;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.wave.fileuploadservice.service.CountingRequestBody;
import com.wave.fileuploadservice.service.RestApiService;
import com.wave.fileuploadservice.service.RetrofitInstance;
import com.wave.fileuploadservice.utils.MIMEType;

import java.io.File;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class FileUploadService extends JobIntentService {
    private static final String TAG = "FileUploadService";
    Disposable mDisposable;
    /**
     * Unique job ID for this service.
     */
    private static final int JOB_ID = 102;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, FileUploadService.class, JOB_ID, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        /**
         * Download/Upload of file
         * The system or framework is already holding a wake lock for us at this point
         */

        // get file file here
        String mFilePath = intent.getStringExtra("mFilePath");
        if (mFilePath == null) {
            Log.e(TAG, "onHandleWork: Invalid file URI");
            return;
        }
        RestApiService apiService = RetrofitInstance.getApiService();
        Flowable<Double> fileObservable = Flowable.create(emitter -> {
            apiService.onFileUpload(createRequestBodyFromText("info@androidwave.com"), createMultipartBody(mFilePath, emitter)).blockingGet();
            emitter.onComplete();
        }, BackpressureStrategy.LATEST);

        mDisposable = fileObservable.subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(progress -> onProgress(progress), throwable -> onErrors(throwable), () -> onSuccess());
    }

    private void onErrors(Throwable throwable) {
        sendBroadcastMeaasge("Error in file upload " + throwable.getMessage());
        Log.e(TAG, "onErrors: ", throwable);
    }

    private void onProgress(Double progress) {
        sendBroadcastMeaasge("Uploading in progress... " + (int) (100 * progress));
        Log.i(TAG, "onProgress: " + progress);
    }

    private void onSuccess() {
        sendBroadcastMeaasge("File uploading successful ");
        Log.i(TAG, "onSuccess: File Uploaded");
    }

    public void sendBroadcastMeaasge(String message) {
        Intent localIntent = new Intent("my.own.broadcast");
        localIntent.putExtra("result", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private RequestBody createRequestBodyFromFile(File file, String mimeType) {
        return RequestBody.create(MediaType.parse(mimeType), file);
    }

    private RequestBody createRequestBodyFromText(String mText) {
        return RequestBody.create(MediaType.parse("text/plain"), mText);
    }


    /**
     * return multi part body in format of FlowableEmitter
     *
     * @param filePath
     * @param emitter
     * @return
     */
    private MultipartBody.Part createMultipartBody(String filePath, FlowableEmitter<Double> emitter) {
        File file = new File(filePath);
        return MultipartBody.Part.createFormData("myFile", file.getName(), createCountingRequestBody(file, MIMEType.IMAGE.value, emitter));
    }

    private RequestBody createCountingRequestBody(File file, String mimeType, FlowableEmitter<Double> emitter) {
        RequestBody requestBody = createRequestBodyFromFile(file, mimeType);
        return new CountingRequestBody(requestBody, (bytesWritten, contentLength) -> {
            double progress = (1.0 * bytesWritten) / contentLength;
            emitter.onNext(progress);
        });
    }
}
