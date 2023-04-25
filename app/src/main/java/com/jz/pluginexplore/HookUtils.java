package com.jz.pluginexplore;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author: JerryZhu
 * @datetime: 2023/4/21
 */
public class HookUtils {

    public static void hookInstrumentation(Activity activity) {
        try {
            Log.e(Constant.TAG,"hookInstrumentation ==================" );
            Field instrumentationFiled = Activity.class.getDeclaredField("mInstrumentation");
            instrumentationFiled.setAccessible(true);
            Instrumentation oriInstrumentation = (Instrumentation) instrumentationFiled.get(activity);
            ProxyInstrumentation proxyInstrumentation = new ProxyInstrumentation(oriInstrumentation);
            instrumentationFiled.set(activity,proxyInstrumentation);
            Log.e(Constant.TAG,"hookInstrumentation 结束 ==================" );
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    public static void hookHandlerCallback() {
        try {
            Log.e(Constant.TAG,"hookHandlerCallback ==================" );
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            //获取当前环境ActivityThread对象
            Object activityThread = currentActivityThreadMethod.invoke(null);
            //获取ActivityThread中的mH对象
            Field mHField = activityThreadClass.getDeclaredField("mH");
            mHField.setAccessible(true);
            Handler mH = (Handler) mHField.get(activityThread);

            Field mCallbackField = Handler.class.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);
            //设置mH对象的mCallback成员为自定义的ProxyHandlerCallback
            mCallbackField.set(mH,new ProxyHandlerCallback(mH));
            Log.e(Constant.TAG,"hookHandlerCallback 结束 ==================" );
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }
}
