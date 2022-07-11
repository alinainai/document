本文代码基于 [retrofit-2.3.0](https://github.com/square/retrofit/tree/parent-2.3.0)，源码包含三个库：

- com.squareup.retrofit2:retrofit
- com.squareup.retrofit2:adapter-rxjava2
- com.squareup.retrofit2:converter-gson

## 1. 使用例子
首先看日常使用中最简单的一个例子：

### 1.1 Api接口的定义
```kotlin
interface ApiService {
    @GET("rest/app/update")
    fun checkUpdate(@Query("versionCode") versionCode: String): Observable<VersionRes>
}
```

### 1.2 利用Retrofit生成ApiService接口的实现

```kotlin
val retrofit = Retrofit.Builder()
    .client(okHttpClient)
    .baseUrl(apiUrl)
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .addConverterFactory(GsonConverterFactory.create())
    .build()
//使用 Service 
retrofit.create(ApiService::class.java).checkUpdate(version_code)
```

先看2个相关的类

- CallAdapter<R, T>: 将一个Call从响应类型R适配成T类型的适配器。
- Converter<F, T>:将F转换为T类型的值的转换器。

## 2. Retrofit.create 代码分析
先上代码，部分代码说明补充在了注释中：
```java
public <T> T create(final Class<T> service) {
  // 检查类型是不是接口，定义的接口数是否大于0
  Utils.validateServiceInterface(service);
  // 如果为true，则会先加载全部的非default方法，同时缓存到map中；默认为false
  if (validateEagerly) {
    eagerlyValidateMethods(service);
  }
  // 动态代理
  return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
      new InvocationHandler() {
        private final Platform platform = Platform.get();

        @Override public Object invoke(Object proxy, Method method, @Nullable Object[] args)
            throws Throwable {
          // 如果是调用的Object中的方法，那就直接执行此方法
          // If the method is a method from Object then defer to normal invocation.
          if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
          }
          // 如果是default方法(Java8中引进)，那就调用default方法
          // 由于plaform是Android不是Java8，所以此处是false的
          if (platform.isDefaultMethod(method)) {
            return platform.invokeDefaultMethod(method, service, proxy, args);
          }
          // 核心的三行代码
          ServiceMethod<Object, Object> serviceMethod =
              (ServiceMethod<Object, Object>) loadServiceMethod(method);
          OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
          return serviceMethod.callAdapter.adapt(okHttpCall);
        }
      });
}
```

## 2. loadServiceMethod 流程

`loadServiceMethod`方法的相关代码：

```java
private final Map<Method, ServiceMethod<?, ?>> serviceMethodCache = new ConcurrentHashMap<>();

ServiceMethod<?, ?> loadServiceMethod(Method method) {
  ServiceMethod<?, ?> result = serviceMethodCache.get(method);
  if (result != null) return result;

  synchronized (serviceMethodCache) {
    result = serviceMethodCache.get(method);//取缓存
    if (result == null) {
      //核心代码，通过 builer 创建一个 ServiceMethod 对象
      result = new ServiceMethod.Builder<>(this, method).build();
      serviceMethodCache.put(method, result);//存缓存
    }
  }
  return result;
}
```
先看下核心代码 ServiceMethod.Builder<>(this, method).build()。

### 2.1 ServiceMethod.Builder

`ServiceMethod.Builder`的构造方法，会取出要调用方法的注解、参数以及参数的注解：

```java
Builder(Retrofit retrofit, Method method) {
  this.retrofit = retrofit;
  this.method = method;
  this.methodAnnotations = method.getAnnotations();
  this.parameterTypes = method.getGenericParameterTypes();
  this.parameterAnnotationsArray = method.getParameterAnnotations();
}
```

再看`ServiceMethod.Builder.build`方法

```java
public ServiceMethod build() {
  // 1、根据method的返回值类型以及方法注解返回对用的 CallAdapter，这里得到的是 RxJava2CallAdapterFactory 创建的 RxJava2CallAdapter
  callAdapter = createCallAdapter();
  // 我们可以直接使用的真正的返回值类型，在例子中此处是VersionRes
  responseType = callAdapter.responseType();
  if (responseType == Response.class || responseType == okhttp3.Response.class) {
    throw methodError("'"
        + Utils.getRawType(responseType).getName()
        + "' is not a valid response body type. Did you mean ResponseBody?");
  }
  // 2、根据 responseType 以及方法注解返回对应的 Converter
  // 由于内置的 BuiltInConverters 无法处理 VersionRes 类型的返回值，此处返回 GsonConverterFactory 创建的 GsonResponseBodyConverter
  responseConverter = createResponseConverter();

  // 根据注解的类型初始化一些参数，如实例中，httpMethod 为 GET,hasBody为false，relativeUrl 为 rest/app/update
  for (Annotation annotation : methodAnnotations) {
    parseMethodAnnotation(annotation);
  }

  if (httpMethod == null) {
    throw methodError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
  }

  if (!hasBody) {
    if (isMultipart) {
      throw methodError(
          "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
    }
    if (isFormEncoded) {
      throw methodError("FormUrlEncoded can only be specified on HTTP methods with "
          + "request body (e.g., @POST).");
    }
  }

  // 3、将每个参数以及其注解封装成为一个 ParameterHandler 对象
  int parameterCount = parameterAnnotationsArray.length;
  parameterHandlers = new ParameterHandler<?>[parameterCount];
  for (int p = 0; p < parameterCount; p++) {
  
    Type parameterType = parameterTypes[p];
    if (Utils.hasUnresolvableType(parameterType)) {
      throw parameterError(p, "Parameter type must not include a type variable or wildcard: %s",
          parameterType);
    }

    Annotation[] parameterAnnotations = parameterAnnotationsArray[p];
    if (parameterAnnotations == null) {
      throw parameterError(p, "No Retrofit annotation found.");
    }

    // ParameterHandler.Query(
    //   name = "VersionCode",
    //   encoded = false,
    //   valueConverter = BuiltInConverters.ToStringConverter
    // )
    parameterHandlers[p] = parseParameter(p, parameterType, parameterAnnotations);
  }

  if (relativeUrl == null && !gotUrl) {
    throw methodError("Missing either @%s URL or @Url parameter.", httpMethod);
  }
  if (!isFormEncoded && !isMultipart && !hasBody && gotBody) {
    throw methodError("Non-body HTTP method cannot contain @Body.");
  }
  if (isFormEncoded && !gotField) {
    throw methodError("Form-encoded method must contain at least one @Field.");
  }
  if (isMultipart && !gotPart) {
    throw methodError("Multipart method must contain at least one @Part.");
  }

  // 4、创建ServiceMethod对象，内部就是一些赋值操作
  return new ServiceMethod<>(this);
}
```

整体分析完了，我们先看一下CallAdapter、Converter的创建，然后再看各种注解的解析。

### 2.2 callAdapter 的选择由 createCallAdapter 完成：

```java
private CallAdapter<T, R> createCallAdapter() {
  // returnType 为 Observable<VersionRes>
  Type returnType = method.getGenericReturnType();
  if (Utils.hasUnresolvableType(returnType)) {
    throw methodError(
        "Method return type must not include a type variable or wildcard: %s", returnType);
  }
  if (returnType == void.class) {
    throw methodError("Service methods cannot return void.");
  }
  // annotations为[@GET("rest/app/update")]
  Annotation[] annotations = method.getAnnotations();
  try {
    // 转到retrofit进行处理
    //noinspection unchecked
    return (CallAdapter<T, R>) retrofit.callAdapter(returnType, annotations);
  } catch (RuntimeException e) { // Wide exception range because factories are user code.
    throw methodError(e, "Unable to create call adapter for %s", returnType);
  }
}
```
继续跟踪Retrofit.callAdapter方法：

```java
public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
  return nextCallAdapter(null, returnType, annotations);
}

public CallAdapter<?, ?> nextCallAdapter(@Nullable CallAdapter.Factory skipPast, Type returnType,
    Annotation[] annotations) {
  checkNotNull(returnType, "returnType == null");
  checkNotNull(annotations, "annotations == null");

  // start = -1 + 1 = 0，也就是顺序遍历
  // 从RxJava2CallAdapterFactory、ExecutorCallAdapterFactory中找到满足条件的
  int start = adapterFactories.indexOf(skipPast) + 1;
  for (int i = start, count = adapterFactories.size(); i < count; i++) {
    CallAdapter<?, ?> adapter = adapterFactories.get(i).get(returnType, annotations, this);
    if (adapter != null) {
      return adapter;
    }
  }

  ...
  throw new IllegalArgumentException(...);
}
```
`RxJava2CallAdapterFactory` 是满足条件的，我们看看其 `get` 方法：

```java
@Override
public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
  // returnType 为 Observable<VersionRes>，因此 rawType 就是Observable类型
  Class<?> rawType = getRawType(returnType);

  if (rawType == Completable.class) {
    // Completable is not parameterized (which is what the rest of this method deals with) so it
    // can only be created with a single configuration.
    return new RxJava2CallAdapter(Void.class, scheduler, isAsync, false, true, false, false,
        false, true);
  }

  boolean isFlowable = rawType == Flowable.class;
  boolean isSingle = rawType == Single.class;
  boolean isMaybe = rawType == Maybe.class;
  if (rawType != Observable.class && !isFlowable && !isSingle && !isMaybe) {
    return null;
  }

  boolean isResult = false;
  boolean isBody = false;
  Type responseType;
  if (!(returnType instanceof ParameterizedType)) {
    String name = isFlowable ? "Flowable"
        : isSingle ? "Single"
        : isMaybe ? "Maybe" : "Observable";
    throw new IllegalStateException(name + " return type must be parameterized"
        + " as " + name + "<Foo> or " + name + "<? extends Foo>");
  }

  // observableType 为 VersionRes 类型
  Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);
  // rawObservableType 也为 VersionRes 类型
  Class<?> rawObservableType = getRawType(observableType);
  if (rawObservableType == Response.class) {
    if (!(observableType instanceof ParameterizedType)) {
      throw new IllegalStateException("Response must be parameterized"
          + " as Response<Foo> or Response<? extends Foo>");
    }
    responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
  } else if (rawObservableType == Result.class) {
    if (!(observableType instanceof ParameterizedType)) {
      throw new IllegalStateException("Result must be parameterized"
          + " as Result<Foo> or Result<? extends Foo>");
    }
    responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
    isResult = true;
  } else {
    // 因此走这个分支，responseType就是VersionRes类型了
    responseType = observableType;
    isBody = true;
  }

  // 返回了一个RxJava2CallAdapter，而不是null，也就意味着找到了满足条件的CallAdapter
  return new RxJava2CallAdapter(responseType, scheduler, isAsync, isResult, isBody, isFlowable,
      isSingle, isMaybe, false);
}
```
从上面分析可以看出，这里的 `callAdapter` 就等于 `RxJava2CallAdapter(VersionRes, null, false, false, true, false, false, false, false)`。

接下来看 `responseConverter` 的创建方法 `createResponseConverter()`：

```java
private Converter<ResponseBody, T> createResponseConverter() {
  // annotations为[@GET("rest/app/update")]
  Annotation[] annotations = method.getAnnotations();
  try {
    // responseType为VersionRes
    return retrofit.responseBodyConverter(responseType, annotations);
  } catch (RuntimeException e) { // Wide exception range because factories are user code.
    throw methodError(e, "Unable to create converter for %s", responseType);
  }
}
```
还是转到了`Retrofit`中：

```java
public <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
  return nextResponseBodyConverter(null, type, annotations);
}

public <T> Converter<ResponseBody, T> nextResponseBodyConverter(
    @Nullable Converter.Factory skipPast, Type type, Annotation[] annotations) {
  checkNotNull(type, "type == null");
  checkNotNull(annotations, "annotations == null");

  // 依然是从0开始，依次尝试 BuiltInConverters、GsonConverterFactory
  int start = converterFactories.indexOf(skipPast) + 1;
  for (int i = start, count = converterFactories.size(); i < count; i++) {
    Converter<ResponseBody, ?> converter =
        converterFactories.get(i).responseBodyConverter(type, annotations, this);
    if (converter != null) {
      //noinspection unchecked
      return (Converter<ResponseBody, T>) converter;
    }
  }
  ...
  throw new IllegalArgumentException(...);
}
```
我们先看看 `BuiltInConverters` 能不能处理：

```java
@Override
public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
    Retrofit retrofit) {
  if (type == ResponseBody.class) {
    return Utils.isAnnotationPresent(annotations, Streaming.class)
        ? StreamingResponseBodyConverter.INSTANCE
        : BufferingResponseBodyConverter.INSTANCE;
  }
  if (type == Void.class) {
    return VoidResponseBodyConverter.INSTANCE;
  }
  return null;
}
```
我们可以看到`BuiltInConverters`只能处理`ResponseBody`类型和`Void`类型两种类型的返回值类型。
所以，我们接着看第二个转换器`GsonConverterFactory`：

```java
@Override
public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
    Retrofit retrofit) {
  TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
  return new GsonResponseBodyConverter<>(gson, adapter);
}
```
这里调用了`Gson`的相关方法，是可以完成任务的。所以就返回了`GsonResponseBodyConverter`。

回到`ServiceMethod.Builder.build`方法，接下来就是处理方法注解以及参数注解了。代码很简单，`if-else`判断出属于约定好的哪种注解，就设置对应的值。这里就不展开说了。

最后是`return new ServiceMethod<>(this);`，这里面就是干了赋值的操作。

## 3.serviceMethod.callAdapter.adapt

`loadServiceMethod`完成之后，会将这个`ServiceMethod`与方法入参一起组成了一个`OkHttpCall`对象：

```kitlin
ServiceMethod<Object, Object> serviceMethod =
    (ServiceMethod<Object, Object>) loadServiceMethod(method);
OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
return serviceMethod.callAdapter.adapt(okHttpCall);
```

到目前为止，都只是做一些准备工作，还没有真正开始网络请求。那么这一步肯定就干了这件事。我们一点点往下看。

我们在前面已经知道了`serviceMethod.callAdapter`是一个`RxJava2CallAdapter`对象，所以我们直接看其`adapt`方法：

```java
@Override public Object adapt(Call<R> call) {
  // isAsync在RxJava2CallAdapterFactory.create()中被赋值，为false
  Observable<Response<R>> responseObservable = isAsync
      ? new CallEnqueueObservable<>(call)
      : new CallExecuteObservable<>(call);

  Observable<?> observable;
  if (isResult) {
    observable = new ResultObservable<>(responseObservable);
  } else if (isBody) {
    // isResult 为 false，isBody 为 true，所以走这个
    observable = new BodyObservable<>(responseObservable);
  } else {
    observable = responseObservable;
  }

  // scheduler默认为null
  if (scheduler != null) {
    observable = observable.subscribeOn(scheduler);
  }

  // 以下boolean都为false
  if (isFlowable) {
    return observable.toFlowable(BackpressureStrategy.LATEST);
  }
  if (isSingle) {
    return observable.singleOrError();
  }
  if (isMaybe) {
    return observable.singleElement();
  }
  if (isCompletable) {
    return observable.ignoreElements();
  }
  return observable;
}
```
从上面可以看出，这里会经过两个`Observable`，分别是`CallExecuteObservable`以及`BodyObservable`。前者作为参数传递给了后者。

我们先看看`CallExecuteObservable`干了什么：

```java
final class CallExecuteObservable<T> extends Observable<Response<T>> {
  // originalCall实际上就是OkHttpCall
  private final Call<T> originalCall;

  CallExecuteObservable(Call<T> originalCall) {
    this.originalCall = originalCall;
  }

  @Override protected void subscribeActual(Observer<? super Response<T>> observer) {
    // 此处的observer是BodyObservable.BodyObserver
    // Since Call is a one-shot type, clone it for each new observer.
    Call<T> call = originalCall.clone();
    // 注意这里，如果dispose了Observable，call同时也会被cancel
    // 调用BodyObserver.onSubscribe
    observer.onSubscribe(new CallDisposable(call));

    boolean terminated = false;
    try {
      // 调用OkHttpCall.execute方法执行同步请求
      Response<T> response = call.execute();
      if (!call.isCanceled()) {
        // 调用BodyObserver.onNext
        observer.onNext(response);
      }
      if (!call.isCanceled()) {
        terminated = true;
        // 调用BodyObserver.onComplete
        observer.onComplete();
      }
    } catch (Throwable t) {
      Exceptions.throwIfFatal(t);
      if (terminated) {
        RxJavaPlugins.onError(t);
      } else if (!call.isCanceled()) {
        try {
          // 调用BodyObserver.onError
          observer.onError(t);
        } catch (Throwable inner) {
          Exceptions.throwIfFatal(inner);
          RxJavaPlugins.onError(new CompositeException(t, inner));
        }
      }
    }
  }

  private static final class CallDisposable implements Disposable {
    private final Call<?> call;

    CallDisposable(Call<?> call) {
      this.call = call;
    }

    @Override public void dispose() {
      call.cancel();
    }

    @Override public boolean isDisposed() {
      return call.isCanceled();
    }
  }
}
```
在上面这段代码中，`call.execute()`是重点，在这段代码里面完成了`ServiceMethod`的乱七八糟的参数的组装，最后才执行`RealCall.execute`，我们最后再说。

接下来看看`BodyObservable`的相关代码：

```java
final class BodyObservable<T> extends Observable<T> {
  // 上面的CallExecuteObservable
  private final Observable<Response<T>> upstream;

  BodyObservable(Observable<Response<T>> upstream) {
    this.upstream = upstream;
  }

  @Override protected void subscribeActual(Observer<? super T> observer) {
    // 这里的入参observer就是我们客户端定义的用来响应网络请求的observer了
    upstream.subscribe(new BodyObserver<T>(observer));
  }

  /** 该类的作用就是判断请求是否成功，并将成功的Response<R>转换为R，传给客户端 */
  private static class BodyObserver<R> implements Observer<Response<R>> {
    private final Observer<? super R> observer;
    private boolean terminated;

    BodyObserver(Observer<? super R> observer) {
      this.observer = observer;
    }

    @Override public void onSubscribe(Disposable disposable) {
      observer.onSubscribe(disposable);
    }

    @Override public void onNext(Response<R> response) {
      // 判断请求是否成功
      if (response.isSuccessful()) {
        // 若成功，将成功的Response<R>转换为R，传给客户端
        observer.onNext(response.body());
      } else {
        // 否则，向客户端抛出HttpException异常
        terminated = true;
        Throwable t = new HttpException(response);
        try {
          observer.onError(t);
        } catch (Throwable inner) {
          Exceptions.throwIfFatal(inner);
          RxJavaPlugins.onError(new CompositeException(t, inner));
        }
      }
    }

    @Override public void onComplete() {
      if (!terminated) {
        observer.onComplete();
      }
    }

    @Override public void onError(Throwable throwable) {
      if (!terminated) {
        observer.onError(throwable);
      } else {
        // This should never happen! onNext handles and forwards errors automatically.
        Throwable broken = new AssertionError(
            "This should never happen! Report as a bug with the full stacktrace.");
        //noinspection UnnecessaryInitCause Two-arg AssertionError constructor is 1.7+ only.
        broken.initCause(throwable);
        RxJavaPlugins.onError(broken);
      }
    }
  }
}
```
小结一下，`CallExecuteObservable`就是用来执行网络请求的，`BodyObservable`会将网络请求的结果(`Response<VersionRes>`)转换为客户端需要的结果(`VersionRes`)。

### 4.OkHttpCall.execute
回想一下`CallExecuteObservable`的关键代码，网络请求需要有一个`Request，但是在`Retrofit`中目前没有发现任何设置的地方，所以这部分代码肯定在`OkHttpCall.execute`中：

```java
@Override public Response<T> execute() throws IOException {
  okhttp3.Call call;

  synchronized (this) {
    if (executed) throw new IllegalStateException("Already executed.");
    executed = true;

    if (creationFailure != null) {
      if (creationFailure instanceof IOException) {
        throw (IOException) creationFailure;
      } else {
        throw (RuntimeException) creationFailure;
      }
    }

    call = rawCall;
    if (call == null) {
      try {
        call = rawCall = createRawCall();
      } catch (IOException | RuntimeException e) {
        creationFailure = e;
        throw e;
      }
    }
  }

  if (canceled) {
    call.cancel();
  }

  return parseResponse(call.execute());
}
```
上面抛开一些同步处理、健康检查，其实就两行代码：

```java
call = rawCall = createRawCall();
return parseResponse(call.execute())
```
先看`createRawCall`方法：

```java
private okhttp3.Call createRawCall() throws IOException {
  // 构造一个Request对象
  Request request = serviceMethod.toRequest(args);
  // serviceMethod.callFactory是我们传入的OkHttpClient对象
  // 这行代码就是new一个RealCall对象
  okhttp3.Call call = serviceMethod.callFactory.newCall(request);
  if (call == null) {
    throw new NullPointerException("Call.Factory returned null.");
  }
  return call;
}
```
我们看看s`erviceMethod.toRequest(args)`如何拼凑出一个`Request`对象：

```java
/** Builds an HTTP request from method arguments. */
Request toRequest(@Nullable Object... args) throws IOException {
  // 还是用例子来说，此处RequestBuilder里面的参数依次为
  // GET, http://aaa.bbb.ccc/, rest/app/update, null, null, false, false, false
  RequestBuilder requestBuilder = new RequestBuilder(httpMethod, baseUrl, relativeUrl, headers,
      contentType, hasBody, isFormEncoded, isMultipart);

  // handlers只有一个：
  // ParameterHandler.Query(
  //   name = "VersionCode",
  //   encoded = false,
  //   valueConverter = BuiltInConverters.ToStringConverter
  // )
  @SuppressWarnings("unchecked") // It is an error to invoke a method with the wrong arg types.
  ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;

  int argumentCount = args != null ? args.length : 0;
  if (argumentCount != handlers.length) {
    throw new IllegalArgumentException("Argument count (" + argumentCount
        + ") doesn't match expected count (" + handlers.length + ")");
  }

  // 调用ParameterHandler.apply方法
  for (int p = 0; p < argumentCount; p++) {
    handlers[p].apply(requestBuilder, args[p]);
  }

  return requestBuilder.build();
}
```
上面调用了`ParameterHandler.Query.apply`方法：

```java
// 此处value就是实例中versionCode的值，假设是10000
@Override void apply(RequestBuilder builder, @Nullable T value) throws IOException {
  if (value == null) return; // Skip null values.

  // valueConverter是BuiltInConverters.ToStringConverter
  // 所以queryValue的值也是10000
  String queryValue = valueConverter.convert(value);
  if (queryValue == null) return; // Skip converted but null values

  // 最后调用了`RequestBuilder.addQueryParam`方法
  builder.addQueryParam(name, queryValue, encoded);
}
```
继续跟踪一下`RequestBuilder.addQueryParam`方法：

```java
void addQueryParam(String name, @Nullable String value, boolean encoded) {
  // relativeUrl为rest/app/update
  if (relativeUrl != null) {
    // Do a one-time combination of the built relative URL and the base URL.
    // 接口URL的拼接，urlBuilder可以简单理解为baseUrl+relativeUrl
    urlBuilder = baseUrl.newBuilder(relativeUrl);
    if (urlBuilder == null) {
      throw new IllegalArgumentException(
          "Malformed URL. Base: " + baseUrl + ", Relative: " + relativeUrl);
    }
    relativeUrl = null;
  }

  if (encoded) {
    //noinspection ConstantConditions Checked to be non-null by above 'if' block.
    urlBuilder.addEncodedQueryParameter(name, value);
  } else {
    // 不加密，走这里
    // 里面会将参数value以及参数name进行UTF-8编码，涉及到的特殊字符会进行转义
    // urlBuilder是OkHttp3库中的，这里不做深入了解了
    //noinspection ConstantConditions Checked to be non-null by above 'if' block.
    urlBuilder.addQueryParameter(name, value);
  }
}
```
回到上面，执行完`createRawCall`之后，就继续执行`parseResponse(call.execute())`。由于此时的`call`是`RealCall`类型了，所以也不用多说。接下来就是`parseResponse`方法。

```java
Response<T> parseResponse(okhttp3.Response rawResponse) throws IOException {
  ResponseBody rawBody = rawResponse.body();

  // Remove the body's source (the only stateful object) so we can pass the response along.
  rawResponse = rawResponse.newBuilder()
      .body(new NoContentResponseBody(rawBody.contentType(), rawBody.contentLength()))
      .build();

  int code = rawResponse.code();
  if (code < 200 || code >= 300) {
    try {
      // Buffer the entire body to avoid future I/O.
      ResponseBody bufferedBody = Utils.buffer(rawBody);
      return Response.error(bufferedBody, rawResponse);
    } finally {
      rawBody.close();
    }
  }

  if (code == 204 || code == 205) {
    rawBody.close();
    return Response.success(null, rawResponse);
  }

  ExceptionCatchingRequestBody catchingBody = new ExceptionCatchingRequestBody(rawBody);
  try {
    T body = serviceMethod.toResponse(catchingBody);
    return Response.success(body, rawResponse);
  } catch (RuntimeException e) {
    // If the underlying source threw an exception, propagate that rather than indicating it was
    // a runtime exception.
    catchingBody.throwIfCaught();
    throw e;
  }
}
```
该方法前面几部分比较原始，我们关注一下`T body = serviceMethod.toResponse(catchingBody)`;，

```java
/** Builds a method return value from an HTTP response body. */
R toResponse(ResponseBody body) throws IOException {
  return responseConverter.convert(body);
}
```
这里面的`responseConverter`就是很早之前就创建好的`GsonResponseBodyConverter`。其`convert`方法如下所示：

```java
@Override public T convert(ResponseBody value) throws IOException {
  JsonReader jsonReader = gson.newJsonReader(value.charStream());
  try {
    return adapter.read(jsonReader);
  } finally {
    value.close();
  }
}
```    
## 5. 小结

`Retrofit`使用了动态代理实现了我们定义的接口。
    
在实现接口方法时，`Retrofit`会为接口的每一个方法构建了一个`ServiceMethod`对象，并会缓存到`ConcurrentHashMap`中。
    
在`ServiceMethod`构建时，会根据接口方法的注解类型、参数类型以及参数注解来拼接请求参数、确定请求类型、构建请求体等，同时会根据接口方法的注解和返回类型确定使用哪个`CallAdapter`包装`OkHttpCall`，同时根据接口方法的泛型类型参数以及方法注解确定使用哪个`Converter`提供请求体、响应体以及字符串转换服务。所有准备工作完成之后，调用了`CallAdapter.adapt`，在这里面真正开始了网络请求。


