package com.wave.fileuploadservice;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.wave.fileuploadservice.service.RestApiService;
import com.wave.fileuploadservice.service.RetrofitInstance;
import com.wave.fileuploadservice.utils.MIMEType;

import java.io.File;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class FileUploadService extends JobIntentService {
    private static final String TAG = "FileUploadService";
    /**
     * Unique job ID for this service.
     */
    private static final int JOB_ID = 102;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, FileUploadService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        /**
         * Download/Upload of file
         * The system or framework is already holding a wake lock for us at this point
         */

        // get file file here
        String mFileUri = intent.getStringExtra("mFileUri");
        if (mFileUri == null) {
            Log.e(TAG, "onHandleWork: Invalid file URI");
            return;
        }
        RestApiService apiService = RetrofitInstance.getApiService();
        apiService.onFileUpload(createRequestBodyFromText("inf@androidwave.com"), createMultipartBody("", MIMEType.IMAGE.value))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<ResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(ResponseBody responseBody) {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });
    }

    private RequestBody createRequestBodyFromFile(File file, String mimeType) {
        return RequestBody.create(MediaType.parse(mimeType), file);
    }

    private RequestBody createRequestBodyFromText(String mText) {
        return RequestBody.create(MediaType.parse("text/plain"), mText);
    }

    private MultipartBody.Part createMultipartBody(String filePath, String mimeType) {
        File file = new File(filePath);
        return MultipartBody.Part.createFormData("myFile", file.getName(), createRequestBodyFromFile(file, mimeType));
    }
}
