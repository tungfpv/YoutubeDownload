package com.tungpv.youtubedownload;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.androidnetworking.interfaces.DownloadProgressListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class DownloadService extends Service {
    public static final String TAG = "tungfpv";

    private NotificationManager notificationManager;
    private int notificationId = 1;
    private YouTubeExtractor mYouTubeExtractor;
    private String mVideoTitle;

    @Override
    public void onCreate() {
        super.onCreate();
        mYouTubeExtractor = new YouTubeExtractor(this) {
            @Override
            protected void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                if (ytFiles != null) {
                    //getBestVideo
                    YtFile ytFile = ytFiles.get(sortItagsByQualityDesc(ytFiles).get(0));
                    String url = ytFile.getUrl();
                    String ext = ytFile.getFormat().getExt();
                    mVideoTitle = videoMeta.getTitle();
                    Log.i(TAG, "video url " + url);
                    Log.i(TAG, "video title " + mVideoTitle);
                    Log.i(TAG, "video ext " + ext);
                    startDownload(url, ext, mVideoTitle);


                }

            }
        };
    }

    private void startDownload(String videoUrl, String ext, String videoTitle) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel();


        AndroidNetworking.download(videoUrl, getExternalFilesDir(null).getPath() + "/Download/YoutubeDownload" ,videoTitle + "." + ext)
                .setPriority(Priority.HIGH)
                .setTag("download")
                .build()
                .setDownloadProgressListener(new DownloadProgressListener() {
                    @Override
                    public void onProgress(long bytesDownloaded, long totalBytes) {
                        Log.i(TAG, "onProgress");
                        int progress = (int) ((bytesDownloaded * 100) / totalBytes);
                        updateNotification(progress);
                    }
                })
                .startDownload(new DownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        Log.i(TAG, "onDownloadComplete");
                        updateNotification(100);
                        stopForeground(true);
                    }

                    @Override
                    public void onError(ANError error) {
                        Log.i(TAG, "onError");
                        updateNotification(-1);
                        stopForeground(true);
                    }
                });
        startForeground(notificationId, createNotification(0));

    }


    public static List<Integer> sortItagsByQualityDesc(SparseArray<YtFile> ytFiles) {
        List<Integer> itags = new ArrayList<>();

        // Lấy danh sách tất cả các ITAG
        for (int i = 0; i < ytFiles.size(); i++) {
            itags.add(ytFiles.keyAt(i));

        }

        // Sắp xếp danh sách ITAG theo chất lượng giảm dần
        Collections.sort(itags, new Comparator<Integer>() {
            @Override
            public int compare(Integer itag1, Integer itag2) {
                YtFile file1 = ytFiles.get(itag1);
                YtFile file2 = ytFiles.get(itag2);


                // Sắp xếp theo chất lượng giảm dần
                return Integer.compare(file2.getFormat().getHeight(), file1.getFormat().getHeight());
            }
        });

        return itags;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_SEND)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                handleYoutubeLink(sharedText);
            }
        }



        return START_NOT_STICKY;
    }

    private Notification createNotification(int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "download_channel")
                .setContentTitle("Downloading video...")
                .setContentText(mVideoTitle)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, progress, false)
                .setOngoing(true);
        return builder.build();
    }

    private void updateNotification(int progress) {
        notificationManager.notify(notificationId, createNotification(progress));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("download_channel", "Download Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleYoutubeLink(String sharedText) {
        if (sharedText != null) {
            if (sharedText != null) {
                if (sharedText.contains("youtu.be") || sharedText.contains("youtube.com")) {
                    String videoId = sharedText.substring(sharedText.lastIndexOf("/") + 1);
                    String youtubeLink = "https://www.youtube.com/watch?v=" + videoId;
                    mYouTubeExtractor.extract(youtubeLink);
                }
            }
        }
    }
}
