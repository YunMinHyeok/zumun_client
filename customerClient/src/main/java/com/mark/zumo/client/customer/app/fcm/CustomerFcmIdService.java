/*
 * Copyright (c) 2018. Mark Soft - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package com.mark.zumo.client.customer.app.fcm;

import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.mark.zumo.client.customer.model.CustomerSessionManager;

/**
 * Created by mark on 18. 6. 8.
 */
public class CustomerFcmIdService extends FirebaseInstanceIdService {

    private static final String TAG = "CustomerFcmIdService";

    private CustomerSessionManager customerSessionManager;

    @Override
    public void onCreate() {
        super.onCreate();

        customerSessionManager = CustomerSessionManager.INSTANCE;

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        registerToken(refreshedToken);
    }

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        registerToken(refreshedToken);

    }

    private void registerToken(final String refreshedToken) {
        if (TextUtils.isEmpty(refreshedToken)) {
            return;
        }

        customerSessionManager.getSessionUser()
                .flatMap(user -> customerSessionManager.registerToken(user, refreshedToken))
                .doOnSuccess(snsToken -> Log.d(TAG, "onTokenRefresh: updated Success-" + snsToken))
                .subscribe();
    }
}
