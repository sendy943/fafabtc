package com.fafabtc.app.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

import com.fafabtc.app.utils.ExecutorManager;
import com.fafabtc.app.utils.RxUtils;
import com.fafabtc.data.data.repo.DataRepo;
import com.fafabtc.data.global.AssetsStateRepository;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.DaggerService;
import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by jastrelax on 2018/1/8.
 */

public class MainService extends DaggerService {

    private static final int MSG_REFRESH_TICKER = 1;
    public static final int UPDATE_PERIOD = 60 * 1000;

    @Inject
    DataRepo dataRepo;

    @Inject
    AssetsStateRepository assetsStateRepository;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH_TICKER:
                    refreshTickers();
                    return true;
            }
            return false;
        }
    });

    private ScheduledExecutorService scheduledExecutorService;

    public static void start(Context context) {
        Intent starter = new Intent(context, MainService.class);
        context.startService(starter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        startRefreshTickers();
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initData();
        return START_NOT_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopUpdating();
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopUpdating();
    }

    private void stopUpdating() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService = null;
        }
    }

    private void initData() {
        dataRepo.init()
                .compose(RxUtils.completableAsyncIO())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        Timber.d("initData complete");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }
                });
    }

    private void startRefreshTickers() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                handler.obtainMessage(MSG_REFRESH_TICKER).sendToTarget();
            }
        }, getInitialDelay(), UPDATE_PERIOD, TimeUnit.MILLISECONDS);
    }

    private long initialDelay = 0;
    private long getInitialDelay() {
        initialDelay = 0;
        assetsStateRepository.getUpdateTime()
                .subscribe(new SingleObserver<Date>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Date date) {
                        long updatetime = date.getTime();
                        long now = new Date().getTime();
                        long elapsed = now - updatetime;
                        initialDelay = elapsed > UPDATE_PERIOD ? 0 : UPDATE_PERIOD - elapsed;
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }
                });
        return initialDelay;
    }

    private void refreshTickers() {
        dataRepo.refreshTickers()
                .subscribeOn(Schedulers.from(ExecutorManager.getIO()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Timber.d("");
                    }

                    @Override
                    public void onComplete() {
                        Timber.d("refreshTickers complete");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }
                });
    }
}
