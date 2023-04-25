package com.jz.plugin;

import android.app.Activity;
import android.app.Application;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import java.lang.reflect.Field;

/**
 * @author: JerryZhu
 * @datetime: 2023/4/21
 */
public class PluginActivity extends Activity {
    private static final String TAG = "PluginActivity";
    private static final String PLUGIN_NAME = "pluginA";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);
        Log.e(TAG,"PluginExplore 插件内的界面 ===========");
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PluginActivity.this, "插件中的吐司", Toast.LENGTH_SHORT).show();
            }
        });

    }

    /*插件Activity的相关资源对象需要返回在宿主application中创建的*/
    @Override
    public Resources getResources() {
        //设置插件名称
        setPluginName();
        return getApplication() != null && getApplication().getResources() != null ?  getApplication().getResources() :  super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        return getApplication() != null && getApplication().getAssets() != null ?  getApplication().getAssets() :  super.getAssets();
    }

    private void setPluginName() {
        Application application = getApplication();
        if (application != null) {
            Class<? extends Application> aClass = application.getClass();
            Field mPluginNameField = null;
            try {
                mPluginNameField= aClass.getDeclaredField("pluginName");
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            mPluginNameField.setAccessible(true);
            try {
                mPluginNameField.set(application,PLUGIN_NAME);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
