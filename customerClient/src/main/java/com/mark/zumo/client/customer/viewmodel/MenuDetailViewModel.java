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
import android.util.Log;
import android.widget.Toast;

import com.mark.zumo.client.core.entity.Menu;
import com.mark.zumo.client.core.entity.MenuOption;
import com.mark.zumo.client.core.entity.MenuOrder;
import com.mark.zumo.client.core.entity.OrderDetail;
import com.mark.zumo.client.core.util.context.ContextHolder;
import com.mark.zumo.client.customer.R;
import com.mark.zumo.client.customer.model.CartManager;
import com.mark.zumo.client.customer.model.MenuManager;
import com.mark.zumo.client.customer.model.OrderManager;
import com.mark.zumo.client.customer.model.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by mark on 18. 5. 24.
 */
public class MenuDetailViewModel extends AndroidViewModel {

    private static final String TAG = "MenuDetailViewModel";

    private final MenuManager menuManager;
    private final CartManager cartManager;
    private final OrderManager orderManager;
    private final SessionManager sessionManager;

    private final Map<String, List<MenuOption>> menuOptionMap;
    private final Map<String, MenuOption> selectedOptionMap;
    private final Map<String, MutableLiveData<MenuOption>> selectedOptionLiveDataMap;

    private final CompositeDisposable disposables;

    private MutableLiveData<Integer> amountLiveData;

    public MenuDetailViewModel(@NonNull final Application application) {
        super(application);
        menuManager = MenuManager.INSTANCE;
        cartManager = CartManager.INSTANCE;
        orderManager = OrderManager.INSTANCE;
        sessionManager = SessionManager.INSTANCE;

        menuOptionMap = new LinkedHashMap<>();
        selectedOptionMap = new HashMap<>();
        selectedOptionLiveDataMap = new HashMap<>();

        disposables = new CompositeDisposable();
    }

    public LiveData<Menu> getMenu(String uuid) {
        MutableLiveData<Menu> liveData = new MutableLiveData<>();

        menuManager.getMenuFromDisk(uuid)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(liveData::setValue)
                .doOnSubscribe(disposables::add)
                .subscribe();

        return liveData;
    }

    public LiveData<Map<String, List<MenuOption>>> getMenuOptionMap(String menuUuid) {
        MutableLiveData<Map<String, List<MenuOption>>> liveData = new MutableLiveData<>();
        loadMenuOptions(liveData, menuUuid);
        return liveData;
    }

    public void insertOrderDetailFromCart(String storeUuid, int cartIndex) {
        cartManager.getCart(storeUuid)
                .map(cart -> cart.getOrderDetail(cartIndex))
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(this::insertMenuOptionDataFromOrderDetail)
                .doOnSubscribe(disposables::add)
                .subscribe();
    }

    private void insertMenuOptionDataFromOrderDetail(OrderDetail orderDetail) {
        amountLiveData.setValue(orderDetail.quantity);

        for (String menuOptionUuid : orderDetail.menuOptionUuidList) {
            selectMenuOption(menuOptionUuid);
        }
    }

    private void loadMenuOptions(MutableLiveData<Map<String, List<MenuOption>>> liveData, String menuUuid) {
        menuOptionMap.clear();
        selectedOptionMap.clear();

        menuManager.getMenuOptionList(menuUuid)
                .flatMapSingle(Observable::toList)
                .doOnNext(list -> Log.d(TAG, "loadMenuOptions: " + list))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(menuOptions -> menuOptionMap.put(menuOptions.get(0).name, menuOptions))
                .doOnComplete(() -> liveData.postValue(menuOptionMap))
                .doOnSubscribe(disposables::add)
                .subscribe();
    }

    public LiveData<Integer> menuAmount() {
        if (amountLiveData == null) {
            amountLiveData = new MutableLiveData<>();
            amountLiveData.setValue(1);
        }
        return amountLiveData;
    }

    public void increaseAmount() {
        amountLiveData.setValue(amountLiveData.getValue() + 1);
    }

    public void decreaseAmount() {
        Integer amount = amountLiveData.getValue();
        amountLiveData.setValue(amount > 1 ? --amount : amount);
    }

    public void selectMenuOption(MenuOption menuOption) {
        Log.d(TAG, "selectMenuOption: " + menuOption);
        selectedOptionMap.put(menuOption.name, menuOption);
        MutableLiveData<MenuOption> liveData = selectedOptionLiveDataMap.get(menuOption.name);
        if (liveData == null) {
            liveData = new MutableLiveData<>();
            selectedOptionLiveDataMap.put(menuOption.name, liveData);
        }
        liveData.setValue(menuOption);
    }

    private void selectMenuOption(String menuOptionUuid) {
        menuManager.getMenuOptionFromDisk(menuOptionUuid)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(this::selectMenuOption)
                .doOnSubscribe(disposables::add)
                .subscribe();
    }

    public void deselectMenuOption(String key) {
        selectedOptionMap.remove(key);
        MutableLiveData<MenuOption> liveData = selectedOptionLiveDataMap.get(key);
        liveData.setValue(null);
    }

    public LiveData<MenuOption> getSelectedOption(String key) {
        MutableLiveData<MenuOption> liveData = selectedOptionLiveDataMap.get(key);
        if (liveData == null) {
            liveData = new MutableLiveData<>();
            selectedOptionLiveDataMap.put(key, liveData);
        }

        liveData.setValue(selectedOptionMap.get(key));
        return liveData;
    }

    public void addToCartCurrentItems(String storeUuid, Menu menu) {
        int price = menu.price;
        ArrayList<String> menuOptionUuidList = new ArrayList<>();
        for (MenuOption menuOption : selectedOptionMap.values()) {
            menuOptionUuidList.add(menuOption.uuid);
            price += menuOption.price;
        }

        OrderDetail orderDetail = new OrderDetail("", storeUuid, menu.uuid, menu.name, "", menuOptionUuidList, amountLiveData.getValue(), price);
        cartManager.getCart(storeUuid)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(cart -> cart.addCartItem(orderDetail))
                .doOnSuccess(cart -> selectedOptionMap.clear())
                .doOnSuccess(cart -> selectedOptionLiveDataMap.clear())
                .doOnSuccess(cart -> showAddToCartSucceedToast())
                .doOnSubscribe(disposables::add)
                .subscribe();
    }

    public void updateToCartCurrentItems(String storeUuid, Menu menu, int cartIndex) {
        int price = menu.price;
        ArrayList<String> menuOptionUuidList = new ArrayList<>();
        for (MenuOption menuOption : selectedOptionMap.values()) {
            menuOptionUuidList.add(menuOption.uuid);
            price += menuOption.price;
        }

        OrderDetail orderDetail = new OrderDetail("", storeUuid, menu.uuid, menu.name, "", menuOptionUuidList, amountLiveData.getValue(), price);
        cartManager.getCart(storeUuid)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(cart -> cart.updateCartItem(orderDetail, cartIndex))
                .doOnSuccess(cart -> selectedOptionMap.clear())
                .doOnSuccess(cart -> selectedOptionLiveDataMap.clear())
                .doOnSubscribe(disposables::add)
                .subscribe();
    }

    public LiveData<MenuOrder> placeOrder(String storeUuid, Menu menu) {
        MutableLiveData<MenuOrder> liveData = new MutableLiveData<>();

        ArrayList<String> menuOptionUuidList = new ArrayList<>();
        int price = menu.price;
        for (MenuOption menuOption : selectedOptionMap.values()) {
            menuOptionUuidList.add(menuOption.uuid);
            price += menuOption.price;
        }

        OrderDetail orderDetail = new OrderDetail("", storeUuid, menu.uuid, menu.name, "", menuOptionUuidList, amountLiveData.getValue(), price);
        orderDetail.menuOrderName = menu.name;

        orderManager.createMenuOrder(orderDetail)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(unused -> selectedOptionMap.clear())
                .doOnSuccess(unused -> selectedOptionLiveDataMap.clear())
                .doOnSuccess(liveData::setValue)
                .doOnSubscribe(disposables::add)
                .subscribe();

        return liveData;
    }

    private void showAddToCartSucceedToast() {
        Toast.makeText(ContextHolder.getContext(), R.string.toast_add_to_cart_item_succeed, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCleared() {
        disposables.clear();
    }
}
