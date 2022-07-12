本文代码基于 [retrofit-2.3.0](https://github.com/square/retrofit/tree/parent-2.3.0)，源码包含三个库：

- com.squareup.retrofit2:retrofit
- com.squareup.retrofit2:adapter-rxjava2
- com.squareup.retrofit2:converter-gson

## 1. 使用例子
首先看日常使用中最简单的一个例子：

### 1.1 Api接口的定义

```kotlin
// 1、定义一个进行网络请求的接口
interface ApiService {
    @GET("rest/app/update")
    fun checkUpdate(@Query("ver_code") code: String): Observable<VerRes>
}

// 2、利用Retrofit生成ApiService接口的实现
val retrofit = Retrofit.Builder()
    .client(okHttpClient)
    .baseUrl(apiUrl)
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    
//3、使用 retrofit.create(Class<T>) 创建一个 ApiService.class 对象。然后调用 ApiService.checkUpdate(version_code) 方法。
retrofit.create(ApiService::class.java).checkUpdate(version_code)

```

从入口开始，先看下 `Retrofit.create(Class<T>)` 的源码

## 2. Retrofit.create 代码分析

```java
public <T> T create(final Class<T> service) {
  // 检查类型是不是接口，定义的接口数是否大于0
  Utils.validateServiceInterface(service);
  // 如果为true，则会先加载全部的非default方法，同时缓存到map中；默认为false
  if (validateEagerly) {
    eagerlyValidateMethods(service);
  }
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
          // 1、根据接口的 method 创建一个 ServiceMethod 对象
          ServiceMethod<Object, Object> serviceMethod =
              (ServiceMethod<Object, Object>) loadServiceMethod(method);
          // 2、将 ServiceMethod 和 参数 封装为一个 OkHttpCall 对象
          OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
          // 3、调用 serviceMethod.callAdapter.adapt(Call<R>) 实现网络请求和数据适配
          return serviceMethod.callAdapter.adapt(okHttpCall);
        }
      });
}
```
`Retrofit.create` 采用动态代理技术实现接口中方法的调用

## 2. loadServiceMethod(Method) 流程分析

`loadServiceMethod` 返回一个 `ServiceMethod` 对象，这个方法的代码很简单。

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
核心代码是通过`ServiceMethod.Builder<>(this, method).build()`创建一个`ServiceMethod`对象

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

再看`ServiceMethod.Builder.build`方法，去掉了一些数据校验的判断

```java
public ServiceMethod build() {
  // 1、根据 method 的 返回值类型 以及 注解 获取一个合适的 CallAdapter，例子中会返回一个 RxJava2CallAdapter 对象
  callAdapter = createCallAdapter();
  // 我们可以直接使用的真正的返回值类型，在例子是 VerRes
  responseType = callAdapter.responseType();

  // 2、根据 responseType 以及方法注解返回对应的 Converter
  // 由于内置的 BuiltInConverters 无法处理 VersionRes 类型的返回值，此处返回 GsonConverterFactory 创建的 GsonResponseBodyConverter
  responseConverter = createResponseConverter();

  // 根据注解的类型初始化一些参数，如实例中，httpMethod 为 GET，hasBody 为 false，relativeUrl 为 rest/app/update
  for (Annotation annotation : methodAnnotations) {
    parseMethodAnnotation(annotation);
  }
  ...
  // 3、将每个参数以及其注解封装成为一个 ParameterHandler 对象
  int parameterCount = parameterAnnotationsArray.length;
  parameterHandlers = new ParameterHandler<?>[parameterCount];
  for (int p = 0; p < parameterCount; p++) {
    Type parameterType = parameterTypes[p];
    Annotation[] parameterAnnotations = parameterAnnotationsArray[p];
    parameterHandlers[p] = parseParameter(p, parameterType, parameterAnnotations);
  }
  
  // 4、创建 ServiceMethod 对象，内部就是一些赋值操作
  return new ServiceMethod<>(this);
}
```

整体分析完了，再看一下 `CallAdapter`、`Converter`的创建，然后再看各种注解的解析。

### 2.2 callAdapter 的选择由 createCallAdapter 完成：

```java
private CallAdapter<T, R> createCallAdapter() {
  Type returnType = method.getGenericReturnType(); // Observable<VerRes>
  Annotation[] annotations = method.getAnnotations();// [@GET("rest/app/update")]
  try {
    return (CallAdapter<T, R>) retrofit.callAdapter(returnType, annotations); // 调用 retrofit进行处理
  } catch (RuntimeException e) { // Wide exception range because factories are user code.
    throw methodError(e, "Unable to create call adapter for %s", returnType);
  }
}
```
继续跟踪`Retrofit.callAdapter`方法：

```java
public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
  return nextCallAdapter(null, returnType, annotations);
}
public CallAdapter<?, ?> nextCallAdapter(@Nullable CallAdapter.Factory skipPast, Type returnType, Annotation[] annotations) {
  // 从 RxJava2CallAdapterFactory、ExecutorCallAdapterFactory 中找到满足条件的
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
因为例子中 `ApiService#checkUpdate` 方法的返回值是 `Observable<VerRes>` 类型，所以这里 `RxJava2CallAdapterFactory` 是满足条件的，我们看看其 `get` 方法：

```java
@Override
public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {

  // 1.returnType 为 Observable<VersionRes>，returnType 的原生类型(rawType)就是 Observable 
  Class<?> rawType = getRawType(returnType); 
  
  Type responseType;
  
  // observableType 为 VerRes 类型
  Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType); 
  // rawObservableType 还是 VerRes 类型
  Class<?> rawObservableType = getRawType(observableType); 
  
  if (rawObservableType == Response.class) {
    responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
  } else if (rawObservableType == Result.class) {
    responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
    isResult = true;
  } else {
    // 将 responseType 赋值为 VerRes 类型
    responseType = observableType; 
    isBody = true;
  }

  // 最后返回一个 `RxJava2CallAdapter(VerRes, null, false, false, true, false, false, false, false)` 类型的适配器对象
  return new RxJava2CallAdapter(responseType, scheduler, isAsync, isResult, isBody, isFlowable,
      isSingle, isMaybe, false);
}
```
接下来看 `responseConverter` 的创建方法 `createResponseConverter()`：

```java
private Converter<ResponseBody, T> createResponseConverter() {
  Annotation[] annotations = method.getAnnotations();
  return retrofit.responseBodyConverter(responseType, annotations);
}
```
还是转到了`Retrofit`中：

```java
public <T> Converter<ResponseBody, T> nextResponseBodyConverter(@Nullable Converter.Factory skipPast, Type type, Annotation[] annotations) {
  
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

总结一下 loadServiceMethod 方法的整个流程：

- 1.根据`method`的返回值类型和注解获取适配的`CallAdapter`对象
- 2.根据`method`的返回值类型和注解获取对应的`Converter`对象
- 3.将`method`中的所有参数及其注解封装成为一个 `ParameterHandler` 数组
- 4.`new` 一个 `ServiceMethod` 返回

在`loadServiceMethod`完成之后，会将这个`ServiceMethod`与方法入参一起组成了一个`OkHttpCall`对象：

```kitlin
ServiceMethod<Object, Object> serviceMethod =
    (ServiceMethod<Object, Object>) loadServiceMethod(method);
OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
return serviceMethod.callAdapter.adapt(okHttpCall);
```

到目前为止，都只是做一些准备工作，还没有真正开始网络请求。那么这一步肯定就干了这件事。我们一点点往下看。

## 3.serviceMethod.callAdapter.adapt 方法

在前面已经知道了`serviceMethod.callAdapter`是一个`RxJava2CallAdapter`对象，所以我们直接看其`RxJava2CallAdapter.adapt(Call)`方法：

```java
@Override public Object adapt(Call<R> call) {
  // isAsync 在 RxJava2CallAdapterFactory.create() 中被赋值，为false
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

  if (scheduler != null) {// scheduler默认为null
    observable = observable.subscribeOn(scheduler);
  }
  ...
  return observable;
}
```
从上面可以看出，这里会生成两个`Observable`，分别是 `CallExecuteObservable` 以及 `BodyObservable`。前者作为上游参数传递给了后者。

我们先看看`CallExecuteObservable`干了什么：

```java
final class CallExecuteObservable<T> extends Observable<Response<T>> {

  private final Call<T> originalCall;

  CallExecuteObservable(Call<T> originalCall) {
    this.originalCall = originalCall;
  }

  // 在例子中，此处的 observer 其实是一个 BodyObservable#BodyObserver 类
  @Override protected void subscribeActual(Observer<? super Response<T>> observer) {
    
    Call<T> call = originalCall.clone();
    // 注意这里：将 call 包装成一个 CallDisposable。如果 dispose 了 Observable，call 同时也会被 cancel
    observer.onSubscribe(new CallDisposable(call));

    boolean terminated = false;
    try {
      // 核心代码：调用OkHttpCall.execute方法执行同步请求
      Response<T> response = call.execute();
      if (!call.isCanceled()) {
        observer.onNext(response);
      }
      if (!call.isCanceled()) {
        terminated = true;
        observer.onComplete();
      }
    } catch (Throwable t) {
      ...
    }
  }
}
```
在上面这段代码中，`call.execute()` 是核心代码，在 `call.execute()` 方法中完成了 `ServiceMethod` 的乱七八糟的参数的组装，最后执行 `RealCall.execute`。

接下来看看`BodyObservable`的相关代码：

```java
final class BodyObservable<T> extends Observable<T> {
  // 上面的 CallExecuteObservable
  private final Observable<Response<T>> upstream;

  BodyObservable(Observable<Response<T>> upstream) {
    this.upstream = upstream;
  }

  @Override protected void subscribeActual(Observer<? super T> observer) {
    // 这里的入参observer就是我们客户端定义的用来响应网络请求的observer了
    upstream.subscribe(new BodyObserver<T>(observer));
  }

  /** 该类的作用就是判断请求是否成功，并将成功的 Response<R> 转换为 R，传给客户端 */
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
    ...
  }
}
```
两个`Observable` 的作用: `CallExecuteObservable` 用来执行网络请求的，`BodyObservable` 会将网络请求的结果 `Response<VerRes>` 转换为客户端需要的结果 `VerRes`。

### 4.OkHttpCall.execute

回看一下 `CallExecuteObservable` 的关键代码，我们知道 OkHttp 的进行网络请求需要有一个`Request`，但是在`Retrofit`中目前没有发现任何设置的地方，所以这部分代码肯定在`OkHttpCall.execute`中：

```java
@Override public Response<T> execute() throws IOException {
  okhttp3.Call call;

  synchronized (this) {
    if (executed) throw new IllegalStateException("Already executed.");
    executed = true;
    ...
    call = rawCall;
    if (call == null) {
      try {
        // 1. 创建一个 okhttp 的 call
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
  // 2.执行网络请求
  return parseResponse(call.execute());
}
```
上面抛开一些同步处理、健康检查，上面的核心代码其实就两行，标志1和标注2。先看`createRawCall`方法：

```java
private okhttp3.Call createRawCall() throws IOException {
  // 构造一个Request对象
  Request request = serviceMethod.toRequest(args);
  // serviceMethod.callFactory 是我们传入的 OkHttpClient 对象
  // 这行代码就是 new 一个 RealCall 对象
  okhttp3.Call call = serviceMethod.callFactory.newCall(request);
  if (call == null) {
    throw new NullPointerException("Call.Factory returned null.");
  }
  return call;
}
```
我们看看 `serviceMethod.toRequest(args)` 如何拼凑出一个 `Request`对象：

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
// 此处 value 就是实例中 ver_code的值，假设是370
@Override void apply(RequestBuilder builder, @Nullable T value) throws IOException {
  if (value == null) return; // Skip null values.

  // valueConverter是BuiltInConverters.ToStringConverter
  // 所以 queryValue 的值也是 370
  String queryValue = valueConverter.convert(value);
  if (queryValue == null) return; // Skip converted but null values

  // 最后调用了`RequestBuilder.addQueryParam`方法
  builder.addQueryParam(name, queryValue, encoded);
}
```
继续跟踪一下`RequestBuilder.addQueryParam`方法：

```java
void addQueryParam(String name, @Nullable String value, boolean encoded) {
  // relativeUrl 为 rest/app/update
  if (relativeUrl != null) {
    // Do a one-time combination of the built relative URL and the base URL.
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


