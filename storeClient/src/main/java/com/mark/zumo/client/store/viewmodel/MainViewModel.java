/*
 * Copyright (c) 2018. Mark Soft - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.mark.zumo.client.store.viewmodel;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import com.mark.zumo.client.core.entity.Store;
import com.mark.zumo.client.core.p2p.P2pServer;
import com.mark.zumo.client.store.model.StoreStoreManager;
import com.mark.zumo.client.store.model.StoreUserManager;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;


/**
 * Created by mark on 18. 6. 9.
 */
public class MainViewModel extends AndroidViewModel {

    private static final String TAG = "MainViewModel";

    private final StoreUserManager storeUserManager;
    private final StoreStoreManager storeStoreManager;

    private final CompositeDisposable compositeDisposable;

    private P2pServer p2pServer;

    private MutableLiveData<Store> sessionStoreLiveData;

    public MainViewModel(@NonNull final Application application) {
        super(application);

        storeUserManager = StoreUserManager.INSTANCE;
        storeStoreManager = StoreStoreManager.INSTANCE;

        compositeDisposable = new CompositeDisposable();
        sessionStoreLiveData = new MutableLiveData<>();
    }

    public boolean hasSessionStore() {
        return storeUserManager.getSessionStoreSync() != null;
    }

    public LiveData<Boolean> hasSessionStoreAsync() {
        MutableLiveData<Boolean> liveData = new MutableLiveData<>();
        liveData.setValue(false);

        storeUserManager.getSessionStoreAsync()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(sessionStore -> liveData.setValue(sessionStore != null))
                .doOnComplete(() -> liveData.setValue(liveData.getValue()))
                .doOnSubscribe(compositeDisposable::add)
                .subscribe();
        return liveData;
    }

    public boolean hasStoreUserSession() {
        return storeUserManager.getStoreUserSessionSync() != null;
    }

    public LiveData<Store> getSessionStoreFlowable() {
        Disposable disposable = storeUserManager.getSessionStoreAsyncFlowable()
                .observeOn(AndroidSchedulers.mainThread())
//                .doOnNext()
                .subscribe(sessionStoreLiveData::setValue);

//        compositeDisposable.add(disposable);

        return sessionStoreLiveData;
    }

    public LiveData<Store> getSessionStore() {
        storeUserManager.getSessionStoreAsync()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(sessionStoreLiveData::setValue)
                .doOnSubscribe(Disposable::dispose)
                .subscribe();

        return sessionStoreLiveData;
    }

    public LiveData<Store> setSessionStore(String storeUuid) {

        storeStoreManager.getStore(storeUuid)
                .flatMapMaybe(storeUserManager::setSessionStore)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(sessionStoreLiveData::setValue)
                .doOnSubscribe(compositeDisposable::add)
                .subscribe();

        return sessionStoreLiveData;
    }

    public LiveData<List<Store>> getStoreUserContractedStoreList() {

        MutableLiveData<List<Store>> liveData = new MutableLiveData<>();

        Maybe.fromCallable(storeUserManager::getStoreUserSessionSync)
                .switchIfEmpty(storeUserManager.getStoreUserSessionAsync())
                .map(storeUserSession -> storeUserSession.uuid)
                .flatMapObservable(storeUserManager::getStoreUserContract)
                .flatMapMaybe(storeUserContractList ->
                        Observable.fromIterable(storeUserContractList)
                                .map(storeUserContract -> storeUserContract.storeUuid)
                                .flatMap(storeStoreManager::getStore)
                                .toList().toMaybe()
                ).observeOn(AndroidSchedulers.mainThread())
                .doOnNext(liveData::setValue)
                .doOnSubscribe(compositeDisposable::add)
                .subscribe();

        return liveData;
    }

    public void findCustomer(Activity activity) {
        storeUserManager.getSessionStoreAsync()
                .map(store -> p2pServer = new P2pServer(activity, store))
                .flatMapObservable(P2pServer::findCustomer)
                .doOnSubscribe(compositeDisposable::add)
                .subscribe();
    }

    public void signOut() {
        storeUserManager.signOut();
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "onCleared: ");
        compositeDisposable.clear();
        if (p2pServer != null) {
            p2pServer.stopAdvertising();
        }
    }
}
