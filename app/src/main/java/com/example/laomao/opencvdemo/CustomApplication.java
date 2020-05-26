package com.example.laomao.opencvdemo;

import android.app.Application;


/**
 * User: mao
 * Date: 2017/2/12
 * Time: 11:47
 */

public class CustomApplication extends Application {

    private static CustomApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance=this;
        Configuration.init(this);
        Configuration.getInstance().debug(BuildConfig.DEBUG);

//        ActivityBuilder.INSTANCE.init(this);

    }

    public static CustomApplication getAppInstance(){
        return mInstance;
    }

}
