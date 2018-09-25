/*
 * Copyright (c) 2018. Mark Soft - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.mark.zumo.client.core.repository;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.mark.zumo.client.core.appserver.AppServerServiceProvider;
import com.mark.zumo.client.core.appserver.NetworkRepository;
import com.mark.zumo.client.core.dao.AppDatabaseProvider;
import com.mark.zumo.client.core.dao.DiskRepository;
import com.mark.zumo.client.core.entity.SnsToken;
import com.mark.zumo.client.core.entity.Store;
import com.mark.zumo.client.core.entity.user.GuestUser;
import com.mark.zumo.client.core.security.SecurePreferences;
import com.mark.zumo.client.core.util.DebugUtil;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by mark on 18. 4. 30.
 */

public enum SessionRepository {
    INSTANCE;

    public static final String KEY_CUSTOMER_UUID = "customer_uuid";
    public static final String KEY_STORE_UUID = "store_uuid";

    private static final String TAG = "SessionRepository";
    private static final Object sessionHeaderLock = new Object();
    private final SecurePreferences securePreferences;
    private final NetworkRepository networkRepository;
    private final DiskRepository diskRepository;
    private GuestUser guestUser;
    private boolean isBuiltSessionHeader;

    SessionRepository() {
        securePreferences = SecuredRepository.INSTANCE.securePreferences();
        networkRepository = AppServerServiceProvider.INSTANCE.networkRepository;
        diskRepository = AppDatabaseProvider.INSTANCE.diskRepository;
    }

    private GuestUser saveGuestUser(final GuestUser guestUser) {
        securePreferences.put(SessionRepository.KEY_CUSTOMER_UUID, guestUser.uuid);
        return guestUser;
    }

    public GuestUser getCustomerFromSecuredRepository() {
        if (guestUser != null) {
            return guestUser;
        }

        try {
            String guestUserUuid = securePreferences.getString(SessionRepository.KEY_CUSTOMER_UUID);
            if (TextUtils.isEmpty(guestUserUuid)) {
                return null;
            }

            return guestUser = new GuestUser(guestUserUuid);
        } catch (SecurePreferences.SecurePreferencesException e) {
            Log.e(TAG, "getCustomerFromSecuredRepository: ", e);
            return null;
        }
    }

    public Maybe<GuestUser> getSessionUser() {
        return Maybe.fromCallable(this::getCustomerFromSecuredRepository)
                .switchIfEmpty(createGuestUser())
                .doOnSuccess(this::buildGuestUserSessionHeader);
    }

    public Maybe<String> getStoreFromCache() {
        //TODO: remove test data
        return Maybe.fromCallable(DebugUtil::store)
                .map(store -> store.uuid);
    }

    public Maybe<GuestUser> createGuestUser() {
        return networkRepository.createGuestUser()
                .doOnSuccess(this::saveGuestUser)
                .flatMap(this::registerSnsTokenOnCreateUser)
                .retryWhen(errors -> errors.flatMap(error -> Flowable.timer(3, TimeUnit.SECONDS)))
                .retry(2);
    }

    private Maybe<GuestUser> registerSnsTokenOnCreateUser(GuestUser guestUser) {
        return diskRepository.getLatestSnsToken()
                .map(snsToken -> new SnsToken(guestUser.uuid, snsToken.tokenType, snsToken.tokenValue))
                .flatMap(this::registerSnsToken)
                .map(x -> guestUser);
    }

    private void buildGuestUserSessionHeader(GuestUser guestUser) {
        if (!isBuiltSessionHeader) {
            synchronized (sessionHeaderLock) {
                if (!isBuiltSessionHeader) {
                    new SessionBuilder()
                            .put(SessionRepository.KEY_CUSTOMER_UUID, guestUser.uuid)
                            .build();
                    isBuiltSessionHeader = true;
                }
            }
        }
    }

    private Bundle buildGuestUserSessionHeader2(GuestUser guestUser) {
        return new SessionBuilder()
                .put(SessionRepository.KEY_CUSTOMER_UUID, guestUser.uuid)
                .build();
    }

    public Maybe<SnsToken> registerSnsToken(SnsToken snsToken) {
        return networkRepository.createSnsToken(snsToken)
                .doOnSuccess(diskRepository::insertSnsToken)
                .subscribeOn(Schedulers.io());
    }

    public Maybe<Bundle> getCustomerSession() {
        return Maybe.fromCallable(this::getCustomerFromSecuredRepository)
                .switchIfEmpty(createGuestUser())
                .map(this::buildGuestUserSessionHeader2);
    }

    public Maybe<Bundle> getStoreSession() {
        //TODO: remove test data
        return Maybe.fromCallable(DebugUtil::store)
                .map(this::buildStoreSessionHeader);
    }

    public Bundle buildStoreSessionHeader(Store store) {
        return new SessionBuilder()
                .put(SessionRepository.KEY_STORE_UUID, store.uuid)
                .build();
    }

    private static class SessionBuilder {
        private final Bundle bundle;

        private SessionBuilder() {
            bundle = new Bundle();
        }

        public SessionBuilder put(String key, String value) {
            bundle.putString(key, value);
            return this;
        }

        public Bundle build() {
            AppServerServiceProvider.INSTANCE.buildNetworkRepository(bundle);
            return bundle;
        }
    }
}
