# android_plugin_explore
android插件化实践

此项目是基于android27即8.1的demo

插件化可以实现功能热拔插,可以减小原始apk的大小,所以有必要对其原理进行探索.

相关技术:

一.Hook技术

所谓hook技术就是继承android系统官方的类重写某些方法添加自己的逻辑然后把系统中引用到这个类的对象的地方换成自定义的类的对象
以实现在执行这个类的对象的方法的时候可以执行我们自己添加的逻辑.

由于activity是android中最常用的四大组件所以这里以启动插件中的activity来揭示插件化的原理.

activity的启动过程中在Instrumentation的execStartActivity里交给ams来执行activity任务栈的管理和一些校验然后将结果返回给客户端进程:
```
   public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        ...
        try {
            intent.migrateExtraStreamToClipData();
            intent.prepareToLeaveProcess(who);
            int result = ActivityManager.getService()
                .startActivity(whoThread, who.getBasePackageName(), intent,
                        intent.resolveTypeIfNeeded(who.getContentResolver()),
                        token, target != null ? target.mEmbeddedID : null,
                        requestCode, 0, null, options);
            checkStartActivityResult(result, intent);//客户端进程对ams校验的结果进行处理
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from system", e);
        }
        return null;
    }
```    
执行完后会通过checkStartActivityResult方法检查校验结果:
```
    /** @hide */
    public static void checkStartActivityResult(int res, Object intent) {
        ...
        switch (res) {
            case ActivityManager.START_INTENT_NOT_RESOLVED:
            case ActivityManager.START_CLASS_NOT_FOUND:
                if (intent instanceof Intent && ((Intent)intent).getComponent() != null)
                    throw new ActivityNotFoundException(
                            "Unable to find explicit activity class "
                            + ((Intent)intent).getComponent().toShortString()
                            + "; have you declared this activity in your AndroidManifest.xml?");
                throw new ActivityNotFoundException(
                        "No Activity found to handle " + intent);
            ...
        }
    }
```
如果一个activity没有在清单文件中注册就会启动失败并在此抛出异常.而插件中的activity没存在于宿主apk的清单文件中,因此如何解决这个问题成为关键.
常规的解决方法是启动插件中的activity的流程中在ams校验之前即代码执行到Instrumentation的execStartActivity时将要启动的插件actitivity替换成在宿主中已经存在并在清单文件中注册过的activity即所谓activity插桩,而在后续的步骤中再恢复为插件中的activity,如何恢复也是个关键.而这里需要hook Instrumentation类的execStartActivity方法:
```
//创建自己的Instrumentation并重写execStartActivity方法
public class ProxyInstrumentation extends Instrumentation {

    //系统原始Instrumentation对象用来执行Instrumentation原对象的execStartActivity方法
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
        //这里新建一个intent将启动的activity换成宿主中已经存在的activity
        Intent stubIntent = new Intent(target, StubActivity.class);
        //把存有要启动的插件中的Activity的Intent存起来,以便后续取出.
        stubIntent.putExtra(Constant.TARGET_COMPONENT,intent);
        intent = stubIntent;
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
```
在此方法中完成activity的替换.

在ams完成这一步后通过binder调用回到客户端进程的ApplicationThread然后通过ActivityThread的handler发送消息告知客户端进程可以创建activity了.
```
   final H mH = new H();
   private class H extends Handler {
      ...
      public void handleMessage(Message msg) {
            if (DEBUG_MESSAGES) Slog.v(TAG, ">>> handling: " + codeToString(msg.what));
            switch (msg.what) {
                case LAUNCH_ACTIVITY: {
                    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
                   final ActivityClientRecord r = (ActivityClientRecord) msg.obj;

                   r.packageInfo = getPackageInfoNoCheck(
                           r.activityInfo.applicationInfo, r.compatInfo);
                    handleLaunchActivity(r, null, "LAUNCH_ACTIVITY");
                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                } break;
            ...
      }          
   }
   ```
  因此我们需要在这里取出之前保存的插件中的activity并修改msg中intent内的组件信息,这个mH是不可变的,而在Handler的dispatchMessage方法中的消息处理是这样的,如果mCallback成员不为null那么就调用mCallback的handleMessage方法,而mH在创建的时候mCallback正好是为null的,所以我们可以hook mCallback并重写mCallback的handleMessage方法来加入我们想要的逻辑:
  ```
   public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }
    
    public class ProxyHandlerCallback implements Handler.Callback {
    //系统Handler对象用来执行Handler对象的handleMessage方法,这个可以根据Callback中handleMessage的返回值来确定是否传递
    //根据上面的代码如果这里的handleMessage返回true,Handler自身的handleMessage方法就不会被调用,如果返回false会继续调用Handler自身的handleMessage.
    private Handler mHandler;

    public ProxyHandlerCallback(Handler handler) {
        mHandler = handler;
    }

    @Override
    public boolean handleMessage(Message message) {
        Log.e(Constant.TAG,"进入ProxyHandlerCallback ========== message :" +message.toString());
        if(message.what == Constant.LAUNCH_ACTIVITY) {
            Object obj = message.obj;
            try {
                Log.e(Constant.TAG,"handleMessage obj ==========" + obj);
                Field intentField = obj.getClass().getDeclaredField("intent");
                intentField.setAccessible(true);
                Intent intent = (Intent) intentField.get(obj);
                //取出之前存储的intent并获取插件中activity作为要启动的组件设置到intent中去
                Intent targetIntent = intent.getParcelableExtra(Constant.TARGET_COMPONENT);
                ComponentName component = targetIntent.getComponent();
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
        //原逻辑是继续执行handleMessage,因此最后还需要交给系统handler来执行.
        mHandler.handleMessage(message);
        Log.e(Constant.TAG,"ProxyHandlerCallback 结束 ==========");
        return true;
    }
}

  ```
  上面我们编写的是hook相关类和方法后加入的自定义的代码逻辑. 接下来就是将系统中有关类的对象替换为这些类的对象:
```      
      //hook Instrumentation
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
    //hook HandlerCallback
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
 ``` 
  至此绕过ams对activity校验的过程就完成了.
二.插件类的加载
   1.要启动插件中的类需要首先要把插件中的类加载进内存,Android中的ClassLoader类型分为两种类型:
    
   系统内置: 
   
     BootClassLoader:加载一些系统Framework层级需要的类.
     
     PathClassLoader:加载系统类和已经安装的应用程序的类,如果是加载非系统应用程序类,则会加载data/app/目录下的dex文件以及包含dex的apk文件或jar文件.
     
     DexClassLoader:可加载外部路径包含dex的apk文件或jar文件,支持从SD卡加载.
     
   自定义:继承系统的ClassLoader
   
   因此如果我们想要加载插件中的类就需要使用DexClassLoader.
   
   2.ClassLoader与双亲委托
   上述类加载器中BootClassLoader直接继承ClassLoader,PathClassLoader和DexClassLoader继承BaseDexClassLoader,BaseDexClassLoader继承ClassLoader.
   ClassLoader是一个抽象类,是所有classloader的最终父类,ClassLoader中重要的方法是loadClass(String name),其他的子类都继承了此方法且没有进行复写.
     
     
         protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException{
             // 首先检查类是否被加载过
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    if (parent != null) {
                        //调用父类的加载器加载
                        c = parent.loadClass(name, false);
                    } else {
                        //如果父类加载器为空就调用启动加载器加载
                        c = findBootstrapClassOrNull(name);
                    }
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the non-null parent class loader
                }
                //如果父类加载失败,就调用自己的findClass加载
                if (c == null) {
                    // If still not found, then invoke findClass in order
                    // to find the class.
                    c = findClass(name);
                }
            }
            return c;
    }
    
    一个类被加载时首先会检查类是否被加载过,如果没有被加载过,就调用父类的加载器加载,如果父类加载器为空就调用启动加载器加载,如果父类加载失败,就调用自己的findClass加载.相关类的构造方法和findClass方法:
    
 ```    
     //BaseDexClassLoader的构造方法
     public BaseDexClassLoader(String dexPath, File optimizedDirectory,
            String librarySearchPath, ClassLoader parent) {
        super(parent);
        this.pathList = new DexPathList(this, dexPath, librarySearchPath, null);

        if (reporter != null) {
            reporter.report(this.pathList.getDexPaths());
        }
    }
    
    ...
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        List<Throwable> suppressedExceptions = new ArrayList<Throwable>();
        //调用的是DexPathList对象的findClass方法
        Class c = pathList.findClass(name, suppressedExceptions);
        if (c == null) {
            ClassNotFoundException cnfe = new ClassNotFoundException(
                    "Didn't find class \"" + name + "\" on path: " + pathList);
            for (Throwable t : suppressedExceptions) {
                cnfe.addSuppressed(t);
            }
            throw cnfe;
        }
        return c;
    }
    
    //DexPathList的构造方法
    public DexPathList(ClassLoader definingContext, String dexPath,String librarySearchPath, File optimizedDirectory) {
         ....

        this.definingContext = definingContext;

        ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
        // 创建dexElements
        this.dexElements = makeDexElements(splitDexPath(dexPath), optimizedDirectory,
                                           suppressedExceptions, definingContext);
        ....
    }
    
    
   public Class<?> findClass(String name, List<Throwable> suppressed) {      //注释1
        for (Element element : dexElements) {
            //最终通过element对象加载类
            Class<?> clazz = element.findClass(name, definingContext, suppressed);
            if (clazz != null) {
                return clazz;
            }
        }

        if (dexElementsSuppressedExceptions != null) {
            suppressed.addAll(Arrays.asList(dexElementsSuppressedExceptions));
        }
        return null;
    }

   ```
     



三.插件资源的加载

四.demo演示启动插件中的Activity

