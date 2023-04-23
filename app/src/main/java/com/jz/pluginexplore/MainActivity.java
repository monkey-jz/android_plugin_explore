package com.jz.pluginexplore;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import java.util.ArrayList;

/**
 * @author: JerryZhu
 * @datetime: 2023/4/21
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HookUtils.hookHandler();
        HookUtils.hookInstrumentation(MainActivity.this);

        requestPermission();
        initView();

    }

    private void requestPermission() {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                ArrayList<String> requestPermissions = new ArrayList<>();
                int hasSdcardRead = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);
                if (hasSdcardRead != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
                }
                int hasSdcardWrite = checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (hasSdcardWrite != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                if (!requestPermissions.isEmpty()) {
                    String[] requestArray = new String[requestPermissions.size()];
                    for (int i = 0; i < requestArray.length; i++) {
                        requestArray[i] = requestPermissions.get(i);
                    }
                    ActivityCompat.requestPermissions(this, requestArray, 100);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initView() {

        findViewById(R.id.load_plugin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PluginLoader.load(MainActivity.this,MainActivity.this.getClassLoader(),"/sdcard/plugin-debug.apk");
            }
        });

        findViewById(R.id.jump2_plugin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.jz.plugin","com.jz.plugin.PluginActivity"));
                startActivity(intent);
            }
        });

    }
}
