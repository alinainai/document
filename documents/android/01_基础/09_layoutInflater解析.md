本文基于 android 12 (SDK 31) 源码分

## 1、使用方法

追踪 LayoutInflater 加载布局重载方法，最终都会调用如下代码，同样 `Activity#setContentView(layoutId)` 也是通过这种方式去加载布局

```kotlin
 LayoutInflater.from(context).inflate(layoutId,viewGroup,attachToRoot)
```
- `context`: 是上下文对象
- `layoutId`: 是要加载的布局的 id
- `viewGroup`: 是布局要加载到的根布局
- `attachToRoot`: 是否 attach 到 viewGroup 中。一般为 false，采用 merge 形式加载布局的时候可以设置为 true。

## 2、`LayoutInflater.from(context)`方法

`from()` 方法内部通过 `context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)` 获取 `LayoutInflater` 对象。代码如下：

```java
public static LayoutInflater from(Context context) {
    LayoutInflater LayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    if (LayoutInflater == null) {
        throw new AssertionError("LayoutInflater not found.");
    }
    return LayoutInflater;
}
```
`ContextImpl` 是 `Context` 的实现类，`context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)` 会调用 `ContextImpl#getSystemService(name)` 方法。
在 `ContextImpl` 内部会继续调用 `SystemServiceRegistry.getSystemService(this, name)` 方法获取服务对象。

```java
@Override
public Object getSystemService(String name) {
    return SystemServiceRegistry.getSystemService(this, name);
}
```
`SystemServiceRegistry` 是所有能通过 `Context#getSystemService` 返回的 `system service` 的管理类。我们继续跟踪 `SystemServiceRegistry` 类的代码，看下 `LayoutInflater` 是怎么提供的

```java
private static final Map<String, ServiceFetcher<?>> SYSTEM_SERVICE_FETCHERS = new ArrayMap<String, ServiceFetcher<?>>();

static {
    ...
     // 1. 注册 Context.LAYOUT_INFLATER_SERVICE 与服务获取器
    registerService(Context.LAYOUT_INFLATER_SERVICE, LayoutInflater.class, new CachedServiceFetcher<LayoutInflater>() {
        @Override
        public LayoutInflater createService(ContextImpl ctx) {
            // getOuterContext()，参数使用的是 ContextImpl 的代理对象，一般是 Activity
            return new PhoneLayoutInflater(ctx.getOuterContext());
        }});
    ...
}

// 2. 根据 name 获取服务对象
public static Object getSystemService(ContextImpl ctx, String name) {
    ServiceFetcher<?> fetcher = SYSTEM_SERVICE_FETCHERS.get(name);
    return fetcher != null ? fetcher.getService(ctx) : null;
}

// 注册服务与服务获取器
private static <T> void registerService(String serviceName, Class<T> serviceClass, ServiceFetcher<T> serviceFetcher) {
    SYSTEM_SERVICE_FETCHERS.put(serviceName, serviceFetcher);
}
```
在 `SystemServiceRegistry` 内维护了一个 `name - ServiceFetcher` 的映射。在静态代码块中注册对应的 `name - ServiceFetcher` 的 `entry` 对象。
当调用它的 `getSystemService()` 方法时，会根据 `name` 获取对应的 `ServiceFetcher` 对象。ServiceFetcher 通过 getService(context) 返回持有的 Server 对象。

### 2.1 `ServiceFetcher` 接口和子类

我们先了解下 ServiceFetcher 接口和他的三个子类，ServiceFetcher 代码如下：是一个单一方法的接口

```java
static abstract interface ServiceFetcher<T> {
    T getService(ContextImpl ctx);
}
```
`ServiceFetcher` 的子类有三种类型，它们的 `getSystemService()` 都是线程安全的，主要差别体现在 `单例范围`，具体如下：

|ServiceFetcher子类|	单例范围|	描述|	举例|
| ---- | ----  |:----:| ---- |
|CachedServiceFetcher|	ContextImpl域|	/	|LayoutInflater、LocationManager等（最多）|
|StaticServiceFetcher|	进程域	|/	|InputManager、JobScheduler等|
|StaticApplicationContextServiceFetcher	|进程域	|使用 ApplicationContext 创建服务|	ConnectivityManager|

### 2.2 LayoutInflater 的 ServiceFetcher

从上面的代码中我们知道 LayoutInflater 对应的 `ServiceFetcher` 是一个 `CachedServiceFetcher` 类。

`CachedServiceFetcher` 调用 `createService(ContextImpl)` 方法创建一个 `PhoneLayoutInflater` 对象。

并且对于同一个 `Context` 对象，获得的 `LayoutInflater` 是单例形式的。

## 3、`LayoutInflater.inflate(...)` 分析

在开头的例子中，在通过 `LayoutInflater#from(context)` 方法获取 `LayoutInflater` 对象后，会通过 `LayoutInflater#inflate(...)` 方法来实现布局的加载。

`LayoutInflater#inflate(...)`有多个重载方法，但最终都会调用：

```java
public View inflate(@LayoutRes int resource, @Nullable ViewGroup root, boolean attachToRoot) {
    final Resources res = getContext().getResources();
    // 1、如果获取到预编译布局直接返回
    View view = tryInflatePrecompiled(resource, res, root, attachToRoot);
    if (view != null) {
        return view;
    }
    // 2、生成 XmlResourceParser 继续解析布局
    XmlResourceParser parser = res.getLayout(resource); 
    try {
        return inflate(parser, root, attachToRoot); 
    } finally {
        parser.close();
    }
}
```
- 1、该方法会先判断是否存在预编译的布局，如果存在直接返回
- 2、否则就继续调用 `inflate(parser, root, attachToRoot)` 方法继续处理


### 3.1 预编译布局

预编译布局是 `Android10` 添加，在 `Android12` 版本中还未开始支持。我们知道布局文件越复杂 `XmlPullParser` 解析 `XML` 越耗时, `tryInflatePrecompiled` 方法根据 `XML` 预编译生成 `compiled_view.dex`，然后通过反射来生成对应的 `View`，从而减少` XmlPullParser` 解析 `XML` 的时间。`tryInflatePrecompiled` 方法源码如下：

```java
private @Nullable View tryInflatePrecompiled(@LayoutRes int resource, Resources res, @Nullable ViewGroup root, boolean attachToRoot) {
    if (!mUseCompiledView) { // 是否允许使用编译布局，如果为 false 直接返回 null
        return null;
    }
    
    String pkg = res.getResourcePackageName(resource); // 根据 resourceId 获取包名
    String layout = res.getResourceEntryName(resource);
    
    try {
        Class clazz = Class.forName("" + pkg + ".CompiledView", false, mPrecompiledClassLoader); 
        Method inflater = clazz.getMethod(layout, Context.class, int.class);
        View view = (View) inflater.invoke(null, mContext, resource); // 通过反射生成 view
        if (view != null && root != null) {
            XmlResourceParser parser = res.getLayout(resource);
            try {
                AttributeSet attrs = Xml.asAttributeSet(parser);
                advanceToRootNode(parser);
                ViewGroup.LayoutParams params = root.generateLayoutParams(attrs);
                if (attachToRoot) {
                    root.addView(view, params);
                } else {
                    view.setLayoutParams(params);
                }
            } finally {
                parser.close();
            }
        }
        return view;
    } catch ...
    
    return null;
}
```
代码很简单，如果 mUseCompiledView 为 true，会通过反射获取预编译的 CompiledView 和 layout_name 对应的 view。

是否使用预编译布局主要和 mUseCompiledView 有关，我们来看下 mUseCompiledView 赋值过程。

```java
protected LayoutInflater(Context context) {
    ....
    initPrecompiledViews();
}
protected LayoutInflater(LayoutInflater original, Context newContext) {
    ...
    initPrecompiledViews();
}

private void initPrecompiledViews() {
    boolean enabled = false; // 这里始终为false
    initPrecompiledViews(enabled);
}

private void initPrecompiledViews(boolean enablePrecompiledViews) {
    mUseCompiledView = enablePrecompiledViews; // 给 mUseCompiledView 赋值
    if (!mUseCompiledView) {
        mPrecompiledClassLoader = null;
        return;
    }

    ApplicationInfo appInfo = mContext.getApplicationInfo();
    if (appInfo.isEmbeddedDexUsed() || appInfo.isPrivilegedApp()) {
        mUseCompiledView = false;
        return;
    }
    try {
        mPrecompiledClassLoader = mContext.getClassLoader();
        String dexFile = mContext.getCodeCacheDir() + COMPILED_VIEW_DEX_FILE_NAME; // compiled_view.dex
        if (new File(dexFile).exists()) { // 生成 compiled_view.dex 文件
            mPrecompiledClassLoader = new PathClassLoader(dexFile, mPrecompiledClassLoader);
        } else {
            mUseCompiledView = false;
        }
    } catch (Throwable e) {
        ...
        mUseCompiledView = false;
    }
    if (!mUseCompiledView) {
        mPrecompiledClassLoader = null;
    }
}
```
根据上面的代码，mUseCompiledView 始终为 false

### 3.2 `inflate(parser,root,attachToRoot)` 方法

如果`预编译布局`为 `null`，继续调用 `inflate(parser,root,attachToRoot)` 去加载布局
```java
public View inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot) {
    synchronized (mConstructorArgs) {
        final Context inflaterContext = mContext;
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        Context lastContext = (Context) mConstructorArgs[0];
        mConstructorArgs[0] = inflaterContext;
        View result = root;
        try {
            advanceToRootNode(parser);
            final String name = parser.getName();
            if (TAG_MERGE.equals(name)) { // 是 merge 标签，调用 rInflate 解析
                if (root == null || !attachToRoot) {
                    throw new InflateException("<merge /> can be used only with a valid ViewGroup root and attachToRoot=true");
                }
                rInflate(parser, root, inflaterContext, attrs, false);
            } else {// 不是 merge 标签则根据 tag 创建 temp view
                final View temp = createViewFromTag(root, name, inflaterContext, attrs); 
                ViewGroup.LayoutParams params = null;
                if (root != null) {
                    params = root.generateLayoutParams(attrs);
                    if (!attachToRoot) {
                        temp.setLayoutParams(params);
                    }
                }
                
                rInflateChildren(parser, temp, attrs, true); // 核心方法，把 temp 当成 parent 传入
                
                if (root != null && attachToRoot) { // 如果 attachToRoot 为 true ，将 temp 添加到 root 中
                    root.addView(temp, params);
                }
                               
                if (root == null || !attachToRoot) { // root 为空 或者 attachToRoot 为 false，返回 temp
                    result = temp;
                }
            }
        } catch (XmlPullParserException e) {
            ...
        } catch (Exception e) {
            ...
        } finally {
            // Don't retain static reference on context.
            mConstructorArgs[0] = lastContext;
            mConstructorArgs[1] = null;
        }
        return result; 
    }
}
```
在该方法中会区分 `merge` 标签和 `其他标签`。如果是 `merge` 标签，调用 `rInflate` 解析 root。如果不是 `merge`，先通过 `createViewFromTag(...)` 方法创建一个 `temp` View，再使用`rInflateChildren(parser, temp, attrs, true)`解析 temp。

### 3.3 `rInflate(parser,parent,context,attrs,finishInflate)` 方法

```java
// 内部会调用 rInflate 
final void rInflateChildren(XmlPullParser parser, View parent, AttributeSet attrs, boolean finishInflate) throws XmlPullParserException, IOException {
    rInflate(parser, parent, parent.getContext(), attrs, finishInflate);
}

void rInflate(XmlPullParser parser, View parent, Context context, AttributeSet attrs, boolean finishInflate) throws XmlPullParserException, IOException {
    final int depth = parser.getDepth(); // 获取 xml 文件的深度
    int type;
    boolean pendingRequestFocus = false;
    while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
    
        if (type != XmlPullParser.START_TAG) {
            continue;
        }
        final String name = parser.getName();
        if (TAG_REQUEST_FOCUS.equals(name)) {
            pendingRequestFocus = true;
            consumeChildElements(parser);
        } else if (TAG_TAG.equals(name)) {
            parseViewTag(parser, parent, attrs);
        } else if (TAG_INCLUDE.equals(name)) { 
            if (parser.getDepth() == 0) {
                throw new InflateException("<include /> cannot be the root element");
            }
            parseInclude(parser, context, parent, attrs); // 1、解析 include 标签
        } else if (TAG_MERGE.equals(name)) {
            throw new InflateException("<merge /> must be the root element");
        } else {
            final View view = createViewFromTag(parent, name, context, attrs); 
            final ViewGroup viewGroup = (ViewGroup) parent;
            final ViewGroup.LayoutParams params = viewGroup.generateLayoutParams(attrs);
            rInflateChildren(parser, view, attrs, true); // 2、递归解析子 view
            viewGroup.addView(view, params); // 3、将子 view 添加到 parent 
        }
    }
    if (pendingRequestFocus) {
        parent.restoreDefaultFocus();
    }
    if (finishInflate) {
        parent.onFinishInflate();// 调用 parent 的 onFinishInflate 方法
    }
}
```
已经做了相关的注释，代码也很简单，核心思想是：遍历 tag 标签并递归解析子 view

### 3.4 `createViewFromTag(...)` 根据 Tag 创建 View
```java
@UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
View createViewFromTag(View parent, String name, Context context, AttributeSet attrs, boolean ignoreThemeAttr) {
    if (name.equals("view")) {
        name = attrs.getAttributeValue(null, "class");
    }
    
    if (!ignoreThemeAttr) {
        final TypedArray ta = context.obtainStyledAttributes(attrs, ATTRS_THEME);
        final int themeResId = ta.getResourceId(0, 0);
        if (themeResId != 0) { // 注意这里，有时候通过 view 获取的 context 不是 Activity 类型，而是 ContextThemeWrapper 类型。
            context = new ContextThemeWrapper(context, themeResId);
        }
        ta.recycle();
    }

    try {
        View view = tryCreateView(parent, name, context, attrs); // 1、 尝试创建 `view`

        if (view == null) {
            final Object lastContext = mConstructorArgs[0];
            mConstructorArgs[0] = context;
            try { 
                if (-1 == name.indexOf('.')) {// 2、解析内置View，如 TextView，名字中不带 '.'
                    view = onCreateView(context, parent, name, attrs);
                } else {// 3、解析自定义 View
                    view = createView(context, name, null, attrs);
                }
            } finally {
                mConstructorArgs[0] = lastContext;
            }
        }
        
        return view;
    } catch ...
}
```
代码很简单，先用 `tryCreateView(parent, name, context, attrs)` 创建 `view`，获取为 `null` 再通过 `onCreateView(...)` 创建 `view`。
`onCreateView(context, parent, name, attrs)` 最终也会调用 ` createView(context, name, prefix, attrs)`  


### 3.5 `tryCreateView(...)` 尝试创建 View

先看下 `tryCreateView(...)` 方法的代码，该方法中会按照 `mFactory2`、`mFactory`、`mPrivateFactory` 的顺序去创建 `View`，代码如下:

```java
@Nullable
public final View tryCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
    if (name.equals(TAG_1995)) {// 没啥用，一个会布灵布灵的布局
        return new BlinkLayout(context, attrs);
    }
    
    View view;
    if (mFactory2 != null) { 
        view = mFactory2.onCreateView(parent, name, context, attrs); // 1、通过 mFactory2 创建
    } else if (mFactory != null) {
        view = mFactory.onCreateView(name, context, attrs); // 2、通过 mFactory 创建
    } else {
        view = null;
    }
    
    if (view == null && mPrivateFactory != null) {// 3、通过 mPrivateFactory 创建
        view = mPrivateFactory.onCreateView(parent, name, context, attrs);
    }
    return view;
}
```
后面在分析 Factory 相关的代码

### 3.6 `createView(viewContext, name, prefix, attrs)`方法

继续看一下 createView(viewContext, name, prefix, attrs) 方法，代码如下：

```java
@Nullable
public final View createView(@NonNull Context viewContext, @NonNull String name, @Nullable String prefix, @Nullable AttributeSet attrs) throws ClassNotFoundException, InflateException {
       
    Constructor<? extends View> constructor = sConstructorMap.get(name); // 1、从缓存获取 view 的 constructor
    try {
        if (constructor == null) { // 2、如果 constructor 为空，根据路径 prefix + name 去加载 class
            clazz = Class.forName(prefix != null ? (prefix + name) : name, false, mContext.getClassLoader()).asSubclass(View.class);
            constructor = clazz.getConstructor(mConstructorSignature);
            sConstructorMap.put(name, constructor);
        } else {
            if (mFilter != null) {
                ... // 这里判断一下 name 对应的class是否允许通过 inflate 的形式加载
            }
        }
        ...
        try {
            final View view = constructor.newInstance(args); // 3、生成 view 对象
            if (view instanceof ViewStub) { // ViewStub 优化
                final ViewStub viewStub = (ViewStub) view;
                viewStub.setLayoutInflater(cloneInContext((Context) args[0]));
            }
            return view;
        } finally {
            mConstructorArgs[0] = lastContext;
        }
    } catch ...
}
```
- 1、先判断 `sConstructorMap` 是否有 `name` 的对应的 `Constructor`，如果没有就去通过 `prefix + name` 去加载对应的 `Class` 
- 2、通过 `Class` 生成 `Constructor` 并存入 `sConstructorMap`。
- 3、然后调用 `constructor.newInstance(args)` 返回 `view`

LayoutInflater.class 中解释此方法是是 `Low-level function for instantiating a view by name`

## 4、Factory 相关

-> 4.1 <tag> 中没有.

PhoneLayoutInflater.java

private static final String[] sClassPrefixList = {
    "android.widget.",
    "android.webkit.",
    "android.app."
};

已简化
protected View onCreateView(String name, AttributeSet attrs) {
    for (String prefix : sClassPrefixList) {
        View view = createView(name, prefix, attrs);
            if (view != null) {
                return view;
            }
    }
    return super.onCreateView(name, attrs);
}
应用 ContextThemeWrapper 以支持android:theme，这是处理针对特定 View 设置主题；
使用 Factory2 / Factory 实例化 View，相当于拦截，我后文再说；
使用 mPrivateFactory 实例化View，相当于拦截，我后文再说；
调用 LayoutInflater 自身逻辑，分为：
4.1 <tag> 中没有.，这是处理<linearlayout>、<TextView>等标签，依次尝试拼接 3 个路径前缀，进入 3.2 实例化 View
4.2 <tag> 中有.，真正实例化 View 的地方，主要分为 4 步：
1) 缓存的构造器
2) 新建构造器
3) 实例化 View 对象
4) ViewStub 特殊处理
小结：

使用 Factory2 接口可以拦截实例化 View 对象的步骤；
实例化 View 的优先顺序为：Factory2 / Factory -> mPrivateFactory -> PhoneLayoutInflater；
使用反射实例化 View 对象，同时构造器对象做了缓存；

4. Factory2 接口
现在我们来讨论Factory2接口，上一节提到，Factory2可以拦截实例化 View 的步骤，在 LayoutInflater 中有两个方法可以设置：
LayoutInflater.java

方法1：
public void setFactory2(Factory2 factory) {
    if (mFactorySet) {
        关注点：禁止重复设置
        throw new IllegalStateException("A factory has already been set on this LayoutInflater");
    }
    if (factory == null) {
        throw new NullPointerException("Given factory can not be null");
    }
    mFactorySet = true;
    if (mFactory == null) {
        mFactory = mFactory2 = factory;
    } else {
        mFactory = mFactory2 = new FactoryMerger(factory, factory, mFactory, mFactory2);
    }
}

方法2 @hide
public void setPrivateFactory(Factory2 factory) {
    if (mPrivateFactory == null) {
        mPrivateFactory = factory;
    } else {
        mPrivateFactory = new FactoryMerger(factory, factory, mPrivateFactory, mPrivateFactory);
    }
}
现在，我们来看源码中哪里调用这两个方法：

4.1 setFactory2()
在 AppCompatActivity & AppCompatDialog 中，相关源码简化如下：

AppCompatDialog.java

@Override
protected void onCreate(Bundle savedInstanceState) {
    设置 Factory2
    getDelegate().installViewFactory();
    super.onCreate(savedInstanceState);
    getDelegate().onCreate(savedInstanceState);
}
AppCompatActivity.java

@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    final AppCompatDelegate delegate = getDelegate();
    设置 Factory2
    delegate.installViewFactory();
    delegate.onCreate(savedInstanceState);
    夜间主题相关
    if (delegate.applyDayNight() && mThemeId != 0) {
        if (Build.VERSION.SDK_INT >= 23) {
            onApplyThemeResource(getTheme(), mThemeId, false);
        } else {
            setTheme(mThemeId);
        }
    }
    super.onCreate(savedInstanceState);
}
AppCompatDelegateImpl.java

public void installViewFactory() {
    LayoutInflater layoutInflater = LayoutInflater.from(mContext);
    if (layoutInflater.getFactory() == null) {
        关注点：设置 Factory2 = this（AppCompatDelegateImpl）
        LayoutInflaterCompat.setFactory2(layoutInflater, this);
    } else {
        if (!(layoutInflater.getFactory2() instanceof AppCompatDelegateImpl)) {
            Log.i(TAG, "The Activity's LayoutInflater already has a Factory installed"
                        + " so we can not install AppCompat's");
        }
    }
}
LayoutInflaterCompat.java

public static void setFactory2(@NonNull LayoutInflater inflater, @NonNull LayoutInflater.Factory2 factory) {
    inflater.setFactory2(factory);

    if (Build.VERSION.SDK_INT < 21) {
        final LayoutInflater.Factory f = inflater.getFactory();
        if (f instanceof LayoutInflater.Factory2) {
            forceSetFactory2(inflater, (LayoutInflater.Factory2) f);
        } else {
            forceSetFactory2(inflater, factory);
        }
    }
}
可以看到，在 AppCompatDialog & AppCompatActivity 初始化时，都通过setFactory2()设置了拦截器，设置的对象是 AppCompatDelegateImpl：

AppCompatDelegateImpl.java

已简化
class AppCompatDelegateImpl extends AppCompatDelegate
        implements MenuBuilder.Callback, LayoutInflater.Factory2 {

    @Override
    public final View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return createView(parent, name, context, attrs);
    }

    @Override
    public View createView(View parent, final String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        if (mAppCompatViewInflater == null) {
            mAppCompatViewInflater = new AppCompatViewInflater();
        }
    }
    委托给 AppCompatViewInflater 处理
    return mAppCompatViewInflater.createView(...)
}
AppCompatViewInflater 与 LayoutInflater 的核心流程差不多，主要差别是前者会将<TextView>等标签解析为AppCompatTextView对象：

AppCompatViewInflater.java

final View createView(...) {
    ...

    switch (name) {
        case "TextView":
            view = createTextView(context, attrs);
            break;
        ...
        default:
            view = createView(context, name, attrs);
    }
    return view;
}

@NonNull
protected AppCompatTextView createTextView(Context context, AttributeSet attrs) {
    return new AppCompatTextView(context, attrs);
}
4.2 setPrivateFactory()
setPrivateFactory()是 hide 方法，在 Activity 中调用，相关源码简化如下：

Activity.java

final FragmentController mFragments = FragmentController.createController(new HostCallbacks());

final void attach(Context context, ActivityThread aThread,...) {
    attachBaseContext(context);
    mFragments.attachHost(null /*parent*/);

    mWindow = new PhoneWindow(this, window, activityConfigCallback);
    mWindow.setWindowControllerCallback(this);
    mWindow.setCallback(this);
    mWindow.setOnWindowDismissedCallback(this);
    关注点：设置 Factory2
    mWindow.getLayoutInflater().setPrivateFactory(this);
    ...
}
可以看到，这里设置的 Factory2 其实就是 Activity 本身（this），这说明 Activity 也实现了 Factory2 ：

public class Activity extends ContextThemeWrapper implements LayoutInflater.Factory2,...{

    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (!"fragment".equals(name)) {
            return onCreateView(name, context, attrs);
        }

        return mFragments.onCreateView(parent, name, context, attrs);
    }
}
原来<fragment>标签的处理是在这里设置的 Factory2 处理的，关于FragmentController#onCreateView(...)内部如何生成 Fragment 以及返回 View 的逻辑，我们在这篇文章里讨论，请关注：《Android | 考点爆满，带你全方位图解 Fragment 源码》。

小结：

使用 setFactory2() 和 setPrivateFactory() 可以设置 Factory2 接口（拦截器），其中同一个 LayoutInflater 的setFactory2()不能重复设置，setPrivateFactory() 是 hide 方法；
AppCompatDialog & AppCompatActivity 初始化时，调用了setFactory2()，会将一些<tag>转换为AppCompat版本；
Activity 初始化时，调用了setPrivateFactory()，用来处理<fragment>标签。

## 参考
- [Android | 带你探究 LayoutInflater 布局解析原理](https://www.jianshu.com/p/a4dd4892c84e)
- [APK 加载流程之资源加载](https://zhuanlan.zhihu.com/p/138259652)
