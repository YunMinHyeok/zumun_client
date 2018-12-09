/*
 * Copyright (c) 2018. Mark Soft - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.mark.zumo.client.customer.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.mark.zumo.client.core.entity.Menu;
import com.mark.zumo.client.core.entity.MenuCategory;
import com.mark.zumo.client.core.entity.OrderDetail;
import com.mark.zumo.client.core.entity.Store;
import com.mark.zumo.client.customer.model.CartManager;
import com.mark.zumo.client.customer.model.MenuManager;
import com.mark.zumo.client.customer.model.StoreManager;
import com.mark.zumo.client.customer.model.entity.Cart;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by mark on 18. 5. 10.
 */
public class MenuViewModel extends AndroidViewModel {

    private final MenuManager menuManager;
    private final CartManager cartManager;
    private final StoreManager storeManager;

    private final CompositeDisposable disposables;

    private String currentStoreUuid;

    public MenuViewModel(@NonNull final Application application) {
        super(application);

        menuManager = MenuManager.INSTANCE;
        cartManager = CartManager.INSTANCE;
        storeManager = StoreManager.INSTANCE;

        disposables = new CompositeDisposable();
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        menuManager.clearClient();
    }

    public LiveData<Cart> getCart(String storeUuid) {
        currentStoreUuid = storeUuid;

        MutableLiveData<Cart> cartLiveData = new MutableLiveData<>();

        cartManager.getCart(storeUuid)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(cartLiveData::setValue)
                .doOnSubscribe(disposables::add)
                .subscribe();

        return cartLiveData;
    }

    public MutableLiveData<List<MenuCategory>> loadCombinedMenuCategoryList(String storeUuid) {
        MutableLiveData<List<MenuCategory>> liveData = new MutableLiveData<>();

        menuManager.getCombinedMenuCategoryList(storeUuid)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(liveData::setValue)
                .doOnSubscribe(disposables::add)
                .subscribe();

        return liveData;
    }

    public void addMenuToCart(Menu menu) {
        cartManager.getCart(currentStoreUuid)
                .firstElement()
                .doOnSuccess(cart -> cart.addCartItem(OrderDetail.fromMenu(menu)))
                .doOnSubscribe(disposables::add)
                .subscribe();
    }

    public LiveData<Store> getStore(String storeUuid) {
        MutableLiveData<Store> storeLiveData = new MutableLiveData<>();

        storeManager.getStore(storeUuid)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(storeLiveData::setValue)
                .doOnSubscribe(disposables::add)
                .subscribe();

        return storeLiveData;
    }
}
