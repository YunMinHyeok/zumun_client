/*
 * Copyright (c) 2018. Mark Soft - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.mark.zumo.client.store.model;

import android.support.annotation.NonNull;
import android.util.Log;

import com.mark.zumo.client.core.entity.Menu;
import com.mark.zumo.client.core.entity.MenuOption;
import com.mark.zumo.client.core.entity.MenuOptionCategory;
import com.mark.zumo.client.core.entity.MenuOptionDetail;
import com.mark.zumo.client.core.repository.CategoryRepository;
import com.mark.zumo.client.core.repository.MenuDetailRepository;
import com.mark.zumo.client.core.repository.MenuRepository;
import com.mark.zumo.client.core.repository.SessionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by mark on 18. 12. 5.
 */
public enum MenuOptionManager {
    INSTANCE;


    private final static String TAG = "StoreMenuManager";

    private final Maybe<MenuRepository> menuRepositoryMaybe;
    private final Maybe<CategoryRepository> categoryRepositoryMaybe;
    private final Maybe<MenuDetailRepository> menuDetailRepositoryMaybe;

    private final SessionRepository sessionRepository;

    MenuOptionManager() {
        sessionRepository = SessionRepository.INSTANCE;

        menuRepositoryMaybe = sessionRepository.getStoreSession()
                .map(MenuRepository::getInstance);
        categoryRepositoryMaybe = sessionRepository.getStoreSession()
                .map(CategoryRepository::getInstance);
        menuDetailRepositoryMaybe = sessionRepository.getStoreSession()
                .map(MenuDetailRepository::getInstance);
    }

    public Observable<List<MenuOption>> getMenuOptionList(List<String> menuOptionUuidList) {
        return menuRepositoryMaybe.flatMapObservable(menuRepository -> menuRepository.getMenuOptionList(menuOptionUuidList))
                .subscribeOn(Schedulers.io());
    }

    public Observable<List<MenuOptionCategory>> getCombinedMenuOptionCategoryListByStoreUuid(String storeUuid) {
        return Observable.create((ObservableOnSubscribe<List<MenuOptionCategory>>) e -> {
            List<MenuOptionCategory> menuOptionCategoryList = new ArrayList<>();
            Map<String, List<MenuOption>> menuOptionMap = new HashMap<>();
            List<Menu> menuList = new ArrayList<>();
            Map<String, List<MenuOptionDetail>> menuOptionDetailMap = new HashMap<>();

            Deque<Object> loadingToken = new ConcurrentLinkedDeque<>();

            loadingToken.add(new Object());
            menuRepositoryMaybe.flatMapObservable(menuRepository -> menuRepository.getMenuOptionCategoryListByStoreUuid(storeUuid))
                    .lastElement()
                    .doOnSuccess(menuOptionCategories -> {
                        menuOptionCategoryList.clear();
                        menuOptionCategoryList.addAll(menuOptionCategories);
                        loadingToken.pop();
                        if (loadingToken.isEmpty()) {
                            List<MenuOptionCategory> combinedMenuOptionCategory = mapMenuOptionCategoryWithMenuOption(menuOptionCategoryList, menuOptionMap, menuList, menuOptionDetailMap);
                            if (!combinedMenuOptionCategory.isEmpty()) {
                                e.onNext(combinedMenuOptionCategory);
                                e.onComplete();
                            }
                        }
                    }).subscribeOn(Schedulers.newThread())
                    .subscribe();

            loadingToken.add(new Object());
            menuRepositoryMaybe.flatMapSingle(menuRepository ->
                    menuRepository.getGroupedMenuOptionListByStoreUuid(storeUuid)
                            .subscribeOn(Schedulers.newThread())
                            .flatMapSingle(Observable::toList)
                            .toMap(menuOptionDetails -> menuOptionDetails.get(0).menuOptionCategoryUuid))
                    .doOnSuccess(menuOptions -> {
                        menuOptionMap.clear();
                        menuOptionMap.putAll(menuOptions);
                        loadingToken.pop();
                        if (loadingToken.isEmpty()) {
                            List<MenuOptionCategory> combinedMenuOptionCategory = mapMenuOptionCategoryWithMenuOption(menuOptionCategoryList, menuOptionMap, menuList, menuOptionDetailMap);
                            if (!combinedMenuOptionCategory.isEmpty()) {
                                e.onNext(combinedMenuOptionCategory);
                                e.onComplete();
                            }
                        }
                    }).subscribeOn(Schedulers.newThread())
                    .subscribe();

            loadingToken.add(new Object());
            menuRepositoryMaybe.flatMapObservable(menuRepository -> menuRepository.getMenuListOfStore(storeUuid))
                    .lastElement()
                    .doOnSuccess(menus -> {
                        menuList.clear();
                        menuList.addAll(menus);
                        loadingToken.pop();
                        if (loadingToken.isEmpty()) {
                            List<MenuOptionCategory> combinedMenuOptionCategory = mapMenuOptionCategoryWithMenuOption(menuOptionCategoryList, menuOptionMap, menuList, menuOptionDetailMap);
                            if (!combinedMenuOptionCategory.isEmpty()) {
                                e.onNext(combinedMenuOptionCategory);
                                e.onComplete();
                            }
                        }
                    }).subscribeOn(Schedulers.newThread())
                    .subscribe();

            loadingToken.add(new Object());
            menuRepositoryMaybe.flatMapSingle(menuRepository ->
                    menuRepository.getMenuOptionDetailListByStoreUuid(storeUuid)
                            .subscribeOn(Schedulers.newThread())
                            .flatMapSingle(Observable::toList)
                            .toMap(menuOptionDetails -> menuOptionDetails.get(0).menuOptionCategoryUuid)
            ).doOnSuccess(menuOptionDetails -> {
                menuOptionDetailMap.clear();
                menuOptionDetailMap.putAll(menuOptionDetails);
                loadingToken.pop();
                if (loadingToken.isEmpty()) {
                    List<MenuOptionCategory> combinedMenuOptionCategory = mapMenuOptionCategoryWithMenuOption(menuOptionCategoryList, menuOptionMap, menuList, menuOptionDetailMap);
                    if (!combinedMenuOptionCategory.isEmpty()) {
                        e.onNext(combinedMenuOptionCategory);
                        e.onComplete();
                    }
                }
            }).subscribeOn(Schedulers.newThread())
                    .subscribe();

        }).subscribeOn(Schedulers.io());
    }

    private List<MenuOptionCategory> mapMenuOptionCategoryWithMenuOption(@NonNull final List<MenuOptionCategory> menuOptionCategoryList,
                                                                         @NonNull final Map<String, List<MenuOption>> menuOptionMap,
                                                                         @NonNull final List<Menu> menuList,
                                                                         @NonNull final Map<String, List<MenuOptionDetail>> menuOptionDetailMap) {
        final List<MenuOptionCategory> resultMenuOptionCategoryList = new ArrayList<>(menuOptionCategoryList);
        final Map<String, Menu> menuMap = new HashMap<>();
        for (Menu menu : menuList) {
            menuMap.put(menu.uuid, menu);
        }

        for (MenuOptionCategory menuOptionCategory : resultMenuOptionCategoryList) {
            if (menuOptionMap.containsKey(menuOptionCategory.uuid)) {
                menuOptionCategory.menuOptionList = new ArrayList<>();
                menuOptionCategory.menuOptionList.addAll(menuOptionMap.get(menuOptionCategory.uuid));
                Collections.sort(menuOptionCategory.menuOptionList, (o1, o2) -> o1.seqNum - o2.seqNum);
            }

            if (menuOptionDetailMap.containsKey(menuOptionCategory.uuid)) {
                List<Menu> includedMenuList = new ArrayList<>();
                for (MenuOptionDetail menuOptionDetail : menuOptionDetailMap.get(menuOptionCategory.uuid)) {
                    if (!menuMap.containsKey(menuOptionDetail.menuUuid)) {
                        continue;
                    }

                    includedMenuList.add(menuMap.get(menuOptionDetail.menuUuid));
                }
                menuOptionCategory.menuList = new ArrayList<>();
                menuOptionCategory.menuList.addAll(includedMenuList);
            }
        }

        return resultMenuOptionCategoryList;
    }

    public Observable<List<MenuOption>> getMenuOptionListByStoreUuid(String storeUuid) {
        return menuRepositoryMaybe.flatMapObservable(
                menuRepository -> menuRepository.getMenuOptionListByStoreUuid(storeUuid)
        ).subscribeOn(Schedulers.io());
    }

    public Maybe<List<Menu>> createMenuOptionDetailListAsMenu(final String storeUuid,
                                                              final String menuOptionCategoryUuid,
                                                              final List<Menu> menuList) {

        return menuRepositoryMaybe.flatMap(menuRepository ->
                Observable.fromIterable(menuList)
                        .map(menu -> menu.uuid)
                        .map(menuUuid -> MenuOptionDetail.create(storeUuid, menuOptionCategoryUuid, menuUuid))
                        .toList().toMaybe()
                        .flatMap(menuRepository::createMenuOptionDetailList)
                        .flatMap(menuOptionDetailList -> Observable.fromIterable(menuOptionDetailList)
                                .map(menuOptionDetail -> menuOptionDetail.menuUuid)
                                .flatMapMaybe(menuRepository::getMenuFromDisk)
                                .toList().toMaybe()
                        ).subscribeOn(Schedulers.io())
        );
    }

    public Maybe<MenuOptionCategory> createMenuOptionCategory(String storeUuid, String name) {
        MenuOptionCategory menuOptionCategory = MenuOptionCategory.create(name, storeUuid);
        return menuRepositoryMaybe.flatMap(menuRepository -> menuRepository.createMenuOptionCategory(menuOptionCategory))
                .subscribeOn(Schedulers.io());
    }

    public Maybe<MenuOptionCategory> createMenuOptionCategory(String storeUuid, String name,
                                                              List<MenuOption> menuOptionList) {
        return createMenuOptionCategory(storeUuid, name)
                .flatMap(menuOptionCategory -> createMenuOptionList(menuOptionCategory, menuOptionList)
                        .doOnSuccess(createdMenuOptionList -> menuOptionCategory.menuOptionList = createdMenuOptionList)
                        .map(x -> menuOptionCategory))
                .subscribeOn(Schedulers.io());
    }

    public Maybe<List<MenuOption>> createMenuOptionList(MenuOptionCategory menuOptionCategory, List<MenuOption> menuOptionList) {
        for (MenuOption menuOption : menuOptionList) {
            menuOption.menuOptionCategoryUuid = menuOptionCategory.uuid;
        }
        return menuRepositoryMaybe.flatMap(menuRepository -> menuRepository.createMenuOptionList(menuOptionList))
                .subscribeOn(Schedulers.io());
    }

    public Maybe<List<MenuOptionCategory>> deleteMenuOptionCategories(List<MenuOptionCategory> menuOptionCategoryList) {
        return menuRepositoryMaybe.flatMap(menuRepository -> menuRepository.deleteMenuOptionCategories(menuOptionCategoryList))
                .subscribeOn(Schedulers.io());
    }

    public Maybe<List<MenuOptionCategory>> updateMenuOptionCategories(List<MenuOptionCategory> menuOptionCategoryList) {
        return menuRepositoryMaybe.flatMap(menuRepository -> menuRepository.updateMenuOptionCategories(menuOptionCategoryList))
                .subscribeOn(Schedulers.io());
    }

    public Maybe<List<MenuOption>> deleteMenuOptions(List<MenuOption> menuOptionList) {
        return menuRepositoryMaybe.flatMap(menuRepository -> menuRepository.deleteMenuOptions(menuOptionList))
                .subscribeOn(Schedulers.io());
    }

    public Maybe<List<Menu>> deleteMenuOptionDetails(String menuOptionCategoryUuid, List<Menu> menuList) {
        return menuRepositoryMaybe.flatMap(menuRepository ->
                Observable.fromIterable(menuList)
                        .map(menu -> menu.uuid)
                        .flatMapMaybe(menuUuid -> menuRepository.getMenuOptionDetailFromDisk(menuOptionCategoryUuid, menuUuid))
                        .flatMapMaybe(menuRepository::deleteMenuOptionDetail)
                        .map(menuOptionDetail -> menuOptionDetail.menuUuid)
                        .flatMapMaybe(menuRepository::getMenuFromDisk)
                        .doOnNext(menu -> Log.d(TAG, "deleteMenuOptionDetails: " + menu))
                        .toList().toMaybe()
        ).subscribeOn(Schedulers.io());
    }

    public Maybe<List<MenuOption>> updateMenuOptions(List<MenuOption> menuOptionList) {
        return menuRepositoryMaybe.flatMap(menuRepository -> menuRepository.updateMenuOptions(menuOptionList))
                .subscribeOn(Schedulers.io());
    }
}
