package com.jz.pluginexplore;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * @author: JerryZhu
 * @datetime: 2023/4/21
 */
public class ProxyInstrumentation extends Instrumentation {


    private Instrumentation oriInstrumentation;

    public ProxyInstrumentation(Instrumentation instrumentation) {
        oriInstrumentation = instrumentation;
    }

    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target,
                                                                        Intent intent, int requestCode, Bundle options) {
        StringBuilder sb = new StringBuilder();
        sb.append("who = [").append(who).append("], ")
                .append("contextThread = [").append(contextThread).append("], ")
                .append("token = [").append(token).append("], ")
                .append("target = [").append(target).append("], ")
                .append("intent = [").append(intent).append("], ")
                .append("requestCode = [").append(requestCode).append("], ")
                .append("options = [").append(options).append("]");
        Intent StubIntent = new Intent(target, StubActivity.class);
        StubIntent.putExtra(Constant.TARGET_COMPONENT,intent);
        intent = StubIntent;
        try {
            Log.e(Constant.TAG, "Hook instrumentation,开始执行executeStartActivity,参数: " + sb);
            Method execStartActivity = Instrumentation.class.getDeclaredMethod("execStartActivity",
                    Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class);
            ActivityResult activityResult = (ActivityResult)execStartActivity.invoke(oriInstrumentation,who,contextThread,token,target,intent,requestCode,options);
            Log.e(Constant.TAG, "Hook instrumentation,执行executeStartActivity 完成,参数: " + sb);
            return activityResult;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
