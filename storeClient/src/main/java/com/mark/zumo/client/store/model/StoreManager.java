/*
 * Copyright (c) 2018. Mark Soft - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.mark.zumo.client.store.model;

import com.google.android.gms.maps.model.LatLng;
import com.mark.zumo.client.core.entity.Store;
import com.mark.zumo.client.core.repository.StoreRepository;

import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by mark on 18. 4. 30.
 */

public enum StoreManager {

    INSTANCE;

    private final StoreRepository storeRepository;

    StoreManager() {
        storeRepository = StoreRepository.INSTANCE;
    }

    public Maybe<Store> updateStoreName(Store store, String newName) {
        store.name = newName;
        return storeRepository.updateStore(store)
                .subscribeOn(Schedulers.io());
    }

    public Maybe<Store> updateStoreLocation(Store store, LatLng latLng) {
        store.latitude = latLng.latitude;
        store.longitude = latLng.longitude;
        return storeRepository.updateStore(store)
                .subscribeOn(Schedulers.io());
    }

    public Maybe<Store> updateStoreCoverImageUrl(Store store, String coverImageUrl) {
        store.coverImageUrl = coverImageUrl;
        return storeRepository.updateStore(store)
                .subscribeOn(Schedulers.io());

    }

    public Maybe<Store> updateStoreThumbnailImageUrl(Store store, String thumbnailImageUrl) {
        store.thumbnailUrl = thumbnailImageUrl;
        return storeRepository.updateStore(store)
                .subscribeOn(Schedulers.io());
    }
}
