
本文基于 android 12 (SDK 31) 源码

## 1、使用方法

追踪 `LayoutInflater` 加载布局重载方法，最终都会调用如下代码，同样 `Activity#setContentView(layoutId)` 也是通过这种方式去加载布局

```kotlin
 LayoutInflater.from(context).inflate(layoutId,root,attachToRoot)
```
- `context`: 是上下文对象
- `layoutId`: 是要加载的布局的 id
- `root`: 是布局要加载到的根布局，是一个 ViewGroup。
- `attachToRoot`: 要加载的布局是否 attach 到 root 中 (`root.addView(view)`)。

使用 merge 加载布局的时候可以把 attachToRoot 设置为 true。

## 2、获取 LayoutInflater 对象

`LayoutInflater#from(context)` 方法内部通过调用 `context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)` 方法获取一个 `LayoutInflater` 对象。代码如下：

```java
public static LayoutInflater from(Context context) {
    LayoutInflater LayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    if (LayoutInflater == null) {
        throw new AssertionError("LayoutInflater not found.");
    }
    return LayoutInflater;
}
```
而 `context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)` 其实是调用 ContextImpl 类的`getSystemService(name)` 方法。
`ContextImpl` 是 `Context` 的实现类。在 `ContextImpl` 内部会继续调用 `SystemServiceRegistry.getSystemService(this, name)` 方法获取 LayoutInflater 服务对象。

```java
@Override
public Object getSystemService(String name) {
    return SystemServiceRegistry.getSystemService(this, name);
}
```
`SystemServiceRegistry` 是所有能通过 `Context#getSystemService()` 返回的 `system_service` 的管理类。我们看下和 `LayoutInflater` 相关的代码。

```java
// system service 的容器
private static final Map<String, ServiceFetcher<?>> SYSTEM_SERVICE_FETCHERS = new ArrayMap<String, ServiceFetcher<?>>();

static {
    ...
     // 1. 在 static 代码块中注册 Context.LAYOUT_INFLATER_SERVICE 与 服务获取器的映射
    registerService(Context.LAYOUT_INFLATER_SERVICE, LayoutInflater.class, new CachedServiceFetcher<LayoutInflater>() {
        @Override
        public LayoutInflater createService(ContextImpl ctx) {
            // getOuterContext()，参数使用的是 ContextImpl 的代理对象，一般是 Activity
            return new PhoneLayoutInflater(ctx.getOuterContext());
        }});
    ...
}
// 2. 调用 getSystemService 方法根据 name 获取服务对象
public static Object getSystemService(ContextImpl ctx, String name) {
    ServiceFetcher<?> fetcher = SYSTEM_SERVICE_FETCHERS.get(name);
    return fetcher != null ? fetcher.getService(ctx) : null;
}

private static <T> void registerService(String serviceName, Class<T> serviceClass, ServiceFetcher<T> serviceFetcher) {
    SYSTEM_SERVICE_FETCHERS.put(serviceName, serviceFetcher);
}
```
- 1、在 `SystemServiceRegistry` 内维护了一个 `name:ServiceFetcher` 的映射。在静态代码块中注册对应的 `name:ServiceFetcher` 映射。
- 2、当调用 `getSystemService()` 方法时，根据传入的 `name` 获取对应的 `ServiceFetcher` 对象。
- 3、然后 `ServiceFetcher` 通过 `getService(context)` 返回持有的 Server 对象。

### 2.1 `ServiceFetcher` 接口及其子类

`ServiceFetcher` 提供了 `Server` 对象的生成方法。`ServiceFetcher`是一个单一方法的接口
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

### 2.2 LayoutInflater 对应 ServiceFetcher

 LayoutInflater 对应的 `ServiceFetcher` 是一个 `CachedServiceFetcher` 类。

`CachedServiceFetcher` 调用 `createService(ContextImpl)` 方法创建一个 `PhoneLayoutInflater` 对象。

从单例域可知，对于同一个 `Context` 对象，获得的 `LayoutInflater` 对象是单例的。

### 本章小结：

LayoutInflater 获取流程

```shell
LayoutInflater#from(context) -> ContextImpl#getSystemService(Context.LAYOUT_INFLATER_SERVICE) —> SystemServiceRegistry#getSystemService(this, name)
```

## 3、`LayoutInflater.inflate(...)` 分析

我们通过 `LayoutInflater#from(context)` 方法获取 `LayoutInflater` 对象后，调用 `LayoutInflater#inflate(...)` 方法来实现布局的加载。

`LayoutInflater#inflate(...)`有多个重载方法，最终都会调用：

```java
public View inflate(@LayoutRes int resource, @Nullable ViewGroup root, boolean attachToRoot) {
    final Resources res = getContext().getResources();
    // 1、获取到预编译布局直接返回
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
- 1、该方法会先判断是否存在预编译的布局，如果存在直接返回。
- 2、否则就继续调用 `inflate(parser, root, attachToRoot)` 方法继续处理。


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
如果 `mUseCompiledView` 为 `true`，会通过反射获取预编译的 `CompiledView` 中 `layout_name` 对应的 `view`。我们来看下 `mUseCompiledView` 赋值过程。

```java
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
    } catch ...
    
    if (!mUseCompiledView) {
        mPrecompiledClassLoader = null;
    }
}
```
根据上面的代码，`mUseCompiledView` 始终为 `false`，所以预编译布局获取始终为 `null`。

### 3.2 `inflate(parser,root,attachToRoot)` 方法

在分析 `LayoutInflater#inflate(resource,root,attachToRoot)` 方法时我们了解到如果`预编译布局`为 `null`，将继续调用 `inflate(parser,root,attachToRoot)` 去加载布局。

其中 parser 是 XML解析器。

```java
public View inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot) {
    synchronized (mConstructorArgs) {
        ...
        View result = root;
        try {
            final String name = parser.getName();
            if (TAG_MERGE.equals(name)) { // 是 merge 标签，调用 rInflate 解析
                if (root == null || !attachToRoot) {
                    throw new InflateException("<merge /> can be used only with a valid ViewGroup root and attachToRoot=true");
                }
                rInflate(parser, root, inflaterContext, attrs, false);
            } else {
                final View temp = createViewFromTag(root, name, inflaterContext, attrs); // 根据 tag 创建 temp view
                ViewGroup.LayoutParams params = null;
                if (root != null) {
                    params = root.generateLayoutParams(attrs);
                    if (!attachToRoot) {
                        temp.setLayoutParams(params);
                    }
                }
                rInflateChildren(parser, temp, attrs, true);  // 把 temp 赋值为 parent，传入 rInflateChildren 方法
                if (root != null && attachToRoot) { // root 不为空并且 attachToRoot 为 true，将 temp 添加到 root 中
                    root.addView(temp, params);
                }       
                if (root == null || !attachToRoot) { // root 为空或者 attachToRoot 为 false，返回 temp 对象
                    result = temp;
                }
            }
        } catch ...
        return result; 
    }
}
```
- 如果是 `merge` 标签，调用 `rInflate` 解析 root。
- 如果不是 `merge`，先通过 `createViewFromTag(...)` 方法创建一个 `temp` View，再使用`rInflateChildren(parser, temp, attrs, true)`解析 temp。


### 3.3 `rInflate(parser,parent,context,attrs,finishInflate)` 方法

```java
// rInflateChildren 内部继续调用 rInflate 相关方法
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
该方法流程如下:

- 获取 View 树的深度
- 逐个 View 解析
- 解析 android:focusable="true", 获取 View 的焦点
- 解析 android:tag 标签
- 解析 include 标签，并且 include 标签不能作为根布局
- 解析 merge 标签，并且 merge 标签必须作为根布局
- 根据元素名解析，生成对应的 View
- rInflateChildren 方法内部调用的 rInflate 方法，深度优先遍历解析所有的子 View
- 添加解析的 View

### 3.4 `createViewFromTag(...)` 根据 Tag 创建 View

对于普通的 `View` 标签, 通过 `createViewFromTag(...)` 创建对应的 `view` 实例。

```java
View createViewFromTag(View parent, String name, Context context, AttributeSet attrs, boolean ignoreThemeAttr) {
    ...
    try {
        View view = tryCreateView(parent, name, context, attrs); // 1、使用 tryCreateView 创建 view

        if (view == null) {
            final Object lastContext = mConstructorArgs[0];
            mConstructorArgs[0] = context;
            try { 
                if (-1 == name.indexOf('.')) {// 2、解析内置View，如 TextView，名字中不带 '.'
                    view = onCreateView(context, parent, name, attrs); // -> createView(name, "android.view.", attrs)
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
代码很简单:

- 1.先调用`tryCreateView(parent, name, context, attrs)` 创建 `view`，如过 View 不为空直接返回。
- 2.如果 View 为空，通过 `onCreateView(...)`创建基本 View，如：TextView、ImageView。
- 3.或者通过 `createView(...)` 创建 自定义`view`，名字中带'.'的 view。

`onCreateView(context, parent, name, attrs)` 最终也会调用 ` LayoutInflater.createView(...)` 。我们先看下 PhoneLayoutInflater 类中对 `onCreateView(...)`方法的处理。

### 3.5 PhoneLayoutInflater 类代码

`PhoneLayoutInflater` 是 `LayoutInflater` 的子类。该类重写了 `onCreateView(name, attrs)`方法，该类的主要作用是给内置的 View 前面加一个前缀（如果能匹配到）。

源码如下，由于 `CachedServiceFetcher` 返回的 `LayoutInlfater` 对象是 `PhoneLayoutInflater` 类型，所以先调用子类 `PhoneLayoutInflater` 的 `onCreateView` 方法，如果没有匹配 `sClassPrefixList` 中的前缀，再调用 `LayoutInlfater(super)` 的 `onCreateView` 方法。

```java
public class PhoneLayoutInflater extends LayoutInflater {
    private static final String[] sClassPrefixList = {
        "android.widget.",
        "android.webkit.",
        "android.app."
    };
    @Override protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
        for (String prefix : sClassPrefixList) {// 过滤 sClassPrefixList 中的 prefix
            try {
                View view = createView(name, prefix, attrs);
                if (view != null) {
                    return view;
                }
            } catch (ClassNotFoundException e) {
            }
        }
        return super.onCreateView(name, attrs);
    }
  	...
}
```

### 3.6 `createView(viewContext, name, prefix, attrs)`方法

继续看一下 `createView(viewContext, name, prefix, attrs)` 方法，代码如下：

```java
@Nullable
public final View createView(@NonNull Context viewContext, @NonNull String name, @Nullable String prefix, @Nullable AttributeSet attrs) throws ClassNotFoundException, InflateException {
       
    Constructor<? extends View> constructor = sConstructorMap.get(name); // 1、从缓存获取 view 的 constructor
    try {
        if (constructor == null) { // 2、constructor 为空，根据 prefix + name 去加载 class，生成 constructor。
            clazz = Class.forName(prefix != null ? (prefix + name) : name, false, mContext.getClassLoader()).asSubclass(View.class);
            constructor = clazz.getConstructor(mConstructorSignature);
            sConstructorMap.put(name, constructor);
        } 
        ...
        try {
            final View view = constructor.newInstance(args); // 3、通过 constructor 生成 view 对象
            if (view instanceof ViewStub) { // ViewStub 优化
                final ViewStub viewStub = (ViewStub) view;
                viewStub.setLayoutInflater(cloneInContext((Context) args[0]));
            }
            return view;
        } ...
    } catch ...
}
```
- 1、先判断 `sConstructorMap` 是否存在和 `name` 对应的 `Constructor`，
- 2、如果没有就去通过 `prefix + name` 去加载对应的 `Class`，并通过 `Class` 生成 `Constructor` 并存入 `sConstructorMap`。
- 3、然后调用 `constructor.newInstance(args)` 返回 `view`
- 4、如果是 viewStub，对 viewStub 单独处理。

LayoutInflater.class 中解释此方法是是 `Low-level function for instantiating a view by name`。

### 3.7 `tryCreateView(...)` 尝试创建 View

`tryCreateView(...)` 方法中会按照 `mFactory2`、`mFactory`、`mPrivateFactory` 的顺序去创建 `View`，代码如下:

```java
@Nullable
public final View tryCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
    ...
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
我们通过 `LayoutInflater.tryCreateView(...)` 方法分析了实例化 `View` 的优先顺序为：Factory2 / Factory -> mPrivateFactory -> PhoneLayoutInflater。使用 Factory2/Factory 接口可以拦截实例化 View 对象的步骤；

## 4、Factory2 接口

`Factory2` 的 `onCreateView` 方法比 `Factory` 多一个 `parent` 参数。

```java
public interface Factory {
    @Nullable
    View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs);    
}

public interface Factory2 extends Factory {
    @Nullable
    View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs);         
}
```

### 4.2 Factory2 的设置

我们继续看下 setFactory2 方法，代码如下：

```java
public void setFactory2(Factory2 factory) {
    if (mFactorySet) {
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
/**
 * @hide for use by framework
 */
@UnsupportedAppUsage
public void setPrivateFactory(Factory2 factory) {
    if (mPrivateFactory == null) {
        mPrivateFactory = factory;
    } else {
        mPrivateFactory = new FactoryMerger(factory, factory, mPrivateFactory, mPrivateFactory);
    }
}
```

`setFactory2` 设置的时候会先判断是否已经设置过，如果 `mFactorySet` 为 true 时会抛出异常。`setPrivateFactory` 方法是隐藏的

### 4.3  setFactory2 调用时机

在 AppCompatActivity 类中

```java
public AppCompatActivity() {
    super();
    initDelegate();
}
@ContentView
public AppCompatActivity(@LayoutRes int contentLayoutId) {
    super(contentLayoutId);
    initDelegate();
}    
private void initDelegate() {
    ...
    addOnContextAvailableListener(new OnContextAvailableListener() {
        @Override
        public void onContextAvailable(@NonNull Context context) {
            final AppCompatDelegate delegate = getDelegate();
            delegate.installViewFactory(); //  设置 Factory2
            delegate.onCreate(getSavedStateRegistry().consumeRestoredStateForKey(DELEGATE_TAG));
        }
    });
}
```

在 AppCompatDialog 类中

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    getDelegate().installViewFactory(); // 设置 Factory2
    super.onCreate(savedInstanceState);
    getDelegate().onCreate(savedInstanceState);
}
```

通过上面的代码我们知道，不管是 `Activity` 还是 `Dialog` 都是使用 `AppCompatDelegate` 来实现 `Factory2` 设置的。AppCompatDelegateImpl 是 AppCompatDelegate 的实现类。
我们直接看一下 AppCompatDelegateImpl 类中相关的代码。 

```java
@Override
public void installViewFactory() {
    LayoutInflater layoutInflater = LayoutInflater.from(mContext);
    if (layoutInflater.getFactory() == null) {
        LayoutInflaterCompat.setFactory2(layoutInflater, this); //AppCompatDelegateImpl 实现了 Factory2 接口，并将自身赋值给 LayoutInflater 的 Factory2
    } else ...
}
// LayoutInflaterCompat # setFactory2
public static void setFactory2(@NonNull LayoutInflater inflater, @NonNull LayoutInflater.Factory2 factory) {
    inflater.setFactory2(factory); // 设置 LayoutInflater 的 Factory2 为 AppCompatDelegateImpl 对象
    if (Build.VERSION.SDK_INT < 21) {}//兼容 5.0
}
```

AppCompatDelegateImpl 实现了 Factory2 接口。在 installViewFactory 方法中将自身设置为 LayoutInflater 的 Factory2 对象。

我们看下 AppCompatDelegateImpl 中的 onCreateView 和 createView 方法接口。

```java
class AppCompatDelegateImpl extends AppCompatDelegate implements MenuBuilder.Callback, LayoutInflater.Factory2 {

    @Override
    public final View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return createView(parent, name, context, attrs); // 内部调用 createView
    }
    
    @Override
    public View createView(View parent, final String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        if (mAppCompatViewInflater == null) {
            mAppCompatViewInflater = new AppCompatViewInflater(); // 生成一个 AppCompatViewInflater 对象赋值给 mAppCompatViewInflater
        } 
        return mAppCompatViewInflater.createView(parent, name, context, attrs, inheritContext, IS_PRE_LOLLIPOP, true, VectorEnabledTintResources.shouldBeUsed()); // 通过 mAppCompatViewInflater#createView(...) 创建 view
    }
}
```

`AppCompatViewInflater` 与 `LayoutInflater` 的核心流程差不多，主要差别是前者会将 `<TextView>` 等标签解析为 `AppCompatTextView` 对象：

```java
// AppCompatViewInflater.java
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
```

### 4.4 setPrivateFactory()

setPrivateFactory() 是一个 hide 方法，在 Activity 中被调用，相关源码简化如下：

在 Activity 类中

```java
final FragmentController mFragments = FragmentController.createController(new HostCallbacks());

final void attach(Context context, ActivityThread aThread,...) {
    attachBaseContext(context);
    mFragments.attachHost(null /*parent*/);
    mWindow = new PhoneWindow(this, window, activityConfigCallback);
    mWindow.setWindowControllerCallback(this);
    mWindow.setCallback(this);
    mWindow.setOnWindowDismissedCallback(this);
    // 关注点：设置 Factory2
    mWindow.getLayoutInflater().setPrivateFactory(this);
    ...
}
```
可以看到，这里设置的 `Factory2` 其实就是 `Activity` 本身（this），这说明 `Activity` 也实现了 `Factory2` 接口 ：

```java
public class Activity extends ContextThemeWrapper implements LayoutInflater.Factory2,...{
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (!"fragment".equals(name)) {
            return onCreateView(name, context, attrs);
        }
        return mFragments.onCreateView(parent, name, context, attrs);
    }
}
```
`<fragment>` 标签是通过在这里设置的 Factory2 处理的。

### 小结：
- 使用 `setFactory2()` 和 `setPrivateFactory()` 可以设置 `Factory2` 接口（拦截器），同一个 LayoutInflater 的 setFactory2() 不能重复设置，setPrivateFactory() 是 hide 方法；
- AppCompatDialog & AppCompatActivity 初始化时，调用了setFactory2()，会将一些 <tag> 转换为AppCompat版本；
- Activity 初始化时，调用了 setPrivateFactory()，用来处理 <fragment> 标签。

## 参考
- [Android | 带你探究 LayoutInflater 布局解析原理](https://www.jianshu.com/p/a4dd4892c84e)
- [APK 加载流程之资源加载](https://zhuanlan.zhihu.com/p/138259652)
