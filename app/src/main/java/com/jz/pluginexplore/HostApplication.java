package com.jz.pluginexplore;

import android.app.Application;
import android.content.res.AssetManager;
import android.content.res.Resources;

/**
 * @author: JerryZhu
 * @datetime: 2023/4/21
 */
public class HostApplication extends Application {

    @Override
    public AssetManager getAssets() {
        return PluginLoader.sPluginAssetManager != null ? PluginLoader.sPluginAssetManager :super.getAssets();
    }

    @Override
    public Resources getResources() {
        return PluginLoader.sPluginResources != null ? PluginLoader.sPluginResources : super.getResources();
    }
}
