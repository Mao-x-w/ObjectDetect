package com.example.laomao.opencvdemo;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;

/**
 * User: laomao
 * Date: 2016-12-21
 * Time: 11-36
 */

public class BaseActivity extends AppCompatActivity {

    public Context getContext() {
        return this;
    }

    public void startActivity(Class<? extends BaseActivity> clazz) {
        startActivity(new Intent(getContext(), clazz));
    }


    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

    }

}