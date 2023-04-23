package com.jz.pluginexplore;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;

/**
 * @author: JerryZhu
 * @datetime: 2023/4/21
 */
public class PluginLoader {


    public static AssetManager sPluginAssetManager;
    public static Resources sPluginResources;

    public static void load(Context context,ClassLoader hostClassLoader,String pluginApkPath) {

        loadClass(context,hostClassLoader,pluginApkPath);

        loadResource(context,pluginApkPath);
    }

    public static void loadClass(Context context,ClassLoader hostClassLoader,String apkPath) {
        Log.e(Constant.TAG,"开始拷贝插件apk到app沙盒内 ==========");
        //把下载后的插件apk拷贝到app沙盒路径内
        String pluginPath = copy2AppBox(context, apkPath);
        Log.e(Constant.TAG,"拷贝完成 ==========");
        String dexOptPath = context.getDir("dexopt", 0).getAbsolutePath();

        //DexClassLoader可以用于加载外部apk,jar等
        DexClassLoader pluginDexClassLoader = new DexClassLoader(pluginPath, dexOptPath, null, hostClassLoader);

    /*    //加载插件中的一个类测试是否加载成功
        try {
            Class<?> pluginTestClass = pluginDexClassLoader.loadClass("com.jz.plugin.PluginTestClass");
            Log.e(Constant.TAG,"pluginTestClass ==========" +pluginTestClass.getName());
            try {
                Object instance = pluginTestClass.newInstance();
                Method sum = pluginTestClass.getMethod("sum", int.class, int.class);
                int result = (int)sum.invoke(instance, 10, 20);
                Log.e(Constant.TAG,"host result: " + result);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }*/

        /*
         * 合并插件和宿主的DexPathList的dexElements字段并设置到宿主的dexElements字段中
         * 由于加载好的dex最终存储在BaseDexClassLoader的dexPathList对象的dexElements字段中,
         * dexElements是个数组,所以这里需要将插件中的dexElements与宿主apk中dexElements进行合并
         * */
        Log.e(Constant.TAG,"hostClassLoader ==========" + hostClassLoader);
        Log.e(Constant.TAG,"开始合并插件和宿主的DexPathList的dexElements ==========");
        try {
            Field dexPathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
            dexPathListField.setAccessible(true);

            //获取插件中的dexElements
            Object pluginDexPathList= dexPathListField.get(pluginDexClassLoader);
            Field pluginDexElementsFiled = pluginDexPathList.getClass().getDeclaredField("dexElements");
            pluginDexElementsFiled.setAccessible(true);
            Object[] pluginDexElements = (Object[]) pluginDexElementsFiled.get(pluginDexPathList);

            //获取宿主中的dexElements
            Field dexPathListField1 = BaseDexClassLoader.class.getDeclaredField("pathList");
            dexPathListField1.setAccessible(true);
            Object hostDexPathList = dexPathListField1.get(hostClassLoader);
            Field hostDexElementsField = hostDexPathList.getClass().getDeclaredField("dexElements");
            hostDexElementsField.setAccessible(true);
            Object[] hostDexElements = (Object[]) hostDexElementsField.get(hostDexPathList);

            //合并插件和宿主的dexElements
            Object[] newDexElements = (Object[]) Array.newInstance(hostDexElements.getClass().getComponentType(),
                    pluginDexElements.length + hostDexElements.length);
            System.arraycopy(pluginDexElements, 0, newDexElements, 0, pluginDexElements.length);
            System.arraycopy(hostDexElements, 0, newDexElements, pluginDexElements.length, hostDexElements.length);

            //合并后的dexElements字段设置到宿主的dexElements字段中
            hostDexElementsField.set(hostDexPathList,newDexElements);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        Log.e(Constant.TAG,"合并完成 ==========");
    }

    private static String copy2AppBox(Context context, String pluginApkPath) {

        String des = context.getFilesDir().getAbsolutePath() + File.separator + "plugin.apk";
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(new File(pluginApkPath));
            outputStream = new BufferedOutputStream(new FileOutputStream(des));
            byte[] temp = new byte[1024];
            int len;
            while ((len = (inputStream.read(temp))) != -1) {
                outputStream.write(temp, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return des;
    }

    public static void loadResource(Context context,String apkPath) {
        try {
            Log.e(Constant.TAG,"加载插件资源 ==========");
            sPluginAssetManager = AssetManager.class.getConstructor().newInstance();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            addAssetPath.invoke(sPluginAssetManager,apkPath);
            sPluginResources = new Resources(sPluginAssetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
            Log.e(Constant.TAG,"资源加载完成 ==========");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

}
