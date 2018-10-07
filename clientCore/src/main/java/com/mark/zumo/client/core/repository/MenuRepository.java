/*
 * Copyright (c) 2018. Mark Soft - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.mark.zumo.client.core.repository;

import android.os.Bundle;

import com.mark.zumo.client.core.appserver.AppServerServiceProvider;
import com.mark.zumo.client.core.appserver.NetworkRepository;
import com.mark.zumo.client.core.dao.AppDatabaseProvider;
import com.mark.zumo.client.core.dao.DiskRepository;
import com.mark.zumo.client.core.entity.Menu;
import com.mark.zumo.client.core.entity.MenuCategory;
import com.mark.zumo.client.core.entity.MenuOption;
import com.mark.zumo.client.core.util.BundleUtils;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.observables.GroupedObservable;

/**
 * Created by mark on 18. 4. 30.
 */

public class MenuRepository {

    private static final String TAG = "MenuRepository";

    private static Bundle session;
    private static MenuRepository sInstance;

    private final DiskRepository diskRepository;
    private final NetworkRepository networkRepository;

    private MenuRepository(final Bundle session) {
        networkRepository = AppServerServiceProvider.INSTANCE.buildNetworkRepository(session);
        diskRepository = AppDatabaseProvider.INSTANCE.diskRepository;
        MenuRepository.session = session;
    }

    public static MenuRepository getInstance(Bundle session) {
        if (sInstance == null || !BundleUtils.equalsBundles(MenuRepository.session, session)) {
            synchronized (MenuRepository.class) {
                if (sInstance == null) {
                    sInstance = new MenuRepository(session);
                }
            }
        }

        return sInstance;
    }

    public Observable<List<Menu>> getMenuListOfStore(String storeUuid) {
        Maybe<List<Menu>> menuListDB = diskRepository.getMenuList(storeUuid);
        Maybe<List<Menu>> menuListApi = networkRepository.getMenuList(storeUuid)
                .doOnSuccess(diskRepository::insertMenuList);

        return Maybe.merge(menuListDB, menuListApi)
                .toObservable()
                .distinctUntilChanged();
    }

    public Maybe<List<Menu>> getMenuItemsOfStoreFromDisk(String storeUuid) {
        return diskRepository.getMenuList(storeUuid);
    }

    public Observable<GroupedObservable<String, MenuOption>> getMenuOptionGroupByMenu(String menuUuid) {
        Maybe<List<MenuOption>> menuOptionListDB = diskRepository.getMenuOptionListByMenuUuid(menuUuid);
        Maybe<List<MenuOption>> menuOptionListApi = networkRepository.getMenuOptionListByMenuUuid(menuUuid)
                .doOnSuccess(diskRepository::insertMenuOptionList);

        return Observable.merge(
                menuOptionListDB.flatMapObservable(Observable::fromIterable)
                        .groupBy(menuOption -> menuOption.name),
                menuOptionListApi.flatMapObservable(Observable::fromIterable)
                        .groupBy(menuOption -> menuOption.name));
    }

    public Maybe<Menu> getMenuFromDisk(final String uuid) {
        return diskRepository.getMenu(uuid);
    }

    public Maybe<MenuOption> getMenuOptionFromDisk(final String menuOptionUuid) {
        return diskRepository.getMenuOption(menuOptionUuid);
    }

    public Observable<List<MenuOption>> getMenuOptionList(List<String> menuOptionUuidList) {
        Maybe<List<MenuOption>> menuOptionListDB = Observable.fromIterable(menuOptionUuidList)
                .flatMapMaybe(diskRepository::getMenuOption)
                .toList().toMaybe();

        Maybe<List<MenuOption>> menuOptionListApi = Observable.fromIterable(menuOptionUuidList)
                .flatMapMaybe(networkRepository::getMenuOptionList)
                .toList()
                .doOnSuccess(diskRepository::insertMenuOptionList)
                .toMaybe();

        return Maybe.merge(menuOptionListDB, menuOptionListApi)
                .toObservable()
                .distinctUntilChanged();
    }

    public Maybe<Menu> updateMenu(final Menu menu) {
        return networkRepository.updateMenu(menu.uuid, menu)
                .doOnSuccess(diskRepository::insertMenu);
    }

    public Maybe<Menu> updateCategoryInMenu(final String menuUuid, final MenuCategory menuCategory) {
        return networkRepository.updateCategoryInMenu(menuUuid, menuCategory)
                .doOnSuccess(diskRepository::insertMenu);
    }
}
