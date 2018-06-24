/*
 * Copyright (c) 2018. Mark Soft - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.mark.zumo.client.store.model;

import com.mark.zumo.client.core.repository.StoreRepository;

/**
 * Created by mark on 18. 4. 30.
 */

public enum StoreManager {

    INSTANCE;

    private StoreRepository storeRepository;

    StoreManager() {
        storeRepository = StoreRepository.INSTANCE;
    }
}