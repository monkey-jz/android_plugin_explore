package com.jz.plugin;

import android.util.Log;

/**
 * @author: JerryZhu
 * @datetime: 2023/4/21
 */
public class PluginTestClass {
    private static final String TAG = "TestClass";

    public int sum(int num1,int num2) {
        Log.e(TAG, "PluginLoader sum: 进入插件的sum 方法. " );
        return num1 + num2;
    }
}
