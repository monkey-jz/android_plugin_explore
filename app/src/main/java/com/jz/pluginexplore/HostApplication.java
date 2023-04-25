package com.jz.pluginexplore;

import android.app.Application;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

/**
 * @author: JerryZhu
 * @datetime: 2023/4/21
 */
public class HostApplication extends Application {
    //用于区分多个插件此demo只使用了一个插件
    private String pluginName = "";

    @Override
    public AssetManager getAssets() {
        Log.e(Constant.TAG,"pluginA getAssets pluginName ========== " + pluginName);
        if (pluginName.equals("pluginA")){
            return PluginLoader.sPluginAssetManager != null ? PluginLoader.sPluginAssetManager :super.getAssets();
        }
        return super.getAssets();
    }

    @Override
    public Resources getResources() {
        Log.e(Constant.TAG,"pluginA getResources pluginName ==========" + pluginName);
        if (pluginName.equals("pluginA")){
            return PluginLoader.sPluginResources != null ? PluginLoader.sPluginResources : super.getResources();
        }
        return super.getResources();
    }
}
