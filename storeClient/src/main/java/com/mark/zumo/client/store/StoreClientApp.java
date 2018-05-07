package com.mark.zumo.client.store;

import android.app.Application;

import com.kakao.auth.KakaoSDK;
import com.mark.zumo.client.core.signup.kakao.KakaoSdkAdapter;

/**
 * Created by mark on 18. 5. 7.
 */

public class StoreClientApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        KakaoSDK.init(new KakaoSdkAdapter(this));
    }
}
