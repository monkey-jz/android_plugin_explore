package com.jz.pluginexplore;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.lang.reflect.Field;

/**
 * @author: JerryZhu
 * @datetime: 2023/4/21
 */
public class ProxyHandlerCallback implements Handler.Callback {

    private Handler mHandler;

    public ProxyHandlerCallback(Handler handler) {
        mHandler = handler;
    }

    @Override
    public boolean handleMessage(Message message) {
        Log.e(Constant.TAG,"进入ProxyHandlerCallback ==========");
        Log.e(Constant.TAG,"进入ProxyHandlerCallback ========== message :" +message.toString());
        if(message.what == Constant.LAUNCH_ACTIVITY) {
            Object obj = message.obj;
            try {
                Log.e(Constant.TAG,"handleMessage obj ==========" + obj);
                Field intentField = obj.getClass().getDeclaredField("intent");
                intentField.setAccessible(true);
                Intent intent = (Intent) intentField.get(obj);

                Intent tarentIntent = intent.getParcelableExtra(Constant.TARGET_COMPONENT);
                ComponentName component = tarentIntent.getComponent();
                if (component != null) {
                    intent.setComponent(component);
                    Log.e(Constant.TAG,"设置真实组件完成 ==========" + component);
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        //原逻辑是继续执行handleMessage,因此最后还需要交给原handler来执行
        mHandler.handleMessage(message);
        Log.e(Constant.TAG,"ProxyHandlerCallback 结束 ==========");
        return true;
    }
}
