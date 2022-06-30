## 1.基本使用方式

### 1.1用建造者模式初始化一个 OkHttpClient 对象

```java
OkHttpClient.Builder builder = new OkHttpClient.Builder();
builder.connectTimeout(10, TimeUnit.SECONDS);

builder.addInterceptor(new Interceptor() {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Log.d(TAG, "Interceptor url:" + chain.request().url().toString());
        return chain.proceed(chain.request());
    }
});

builder.addNetworkInterceptor(new Interceptor() {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Log.d(TAG, "NetworkInterceptor url:" + chain.request().url().toString());
        return chain.proceed(chain.request());
    }
});

OkHttpClient clent = builder.build();
```
### 1.2.再初始化一个 Request 对象

```java
Request request = new Request.Builder()
        .url("https://www.baidu.com")
        .build();
```

### 1.3 调用 OkHttpClient#newCall(Request) 方法生成一个 Call 对象

```java
Call call = clent.newCall(request);
```
### 1.4 Call 调用 enqueue() 或者 execute() 方法实现请求
```java
call.enqueue(new Callback() {
    @Override
    public void onFailure(Call call, IOException e) {
        Log.d(TAG, "onFailure: " + e.getMessage());
    }
    @Override
    public void onResponse(Call call, Response response) throws IOException {
        Log.d(TAG, "response:" + response.body().string());
    }
});
```


## 2.OkHttpClient 的建造者参数

`OkHttpClient` 相当于配置中心，所有的请求都会共享这些配置。比如：出错是否重试、共享连接池。

```java
public OkHttpClient() {
  this(new Builder());
}

OkHttpClient(Builder builder) {
  this.dispatcher = builder.dispatcher;
  this.proxy = builder.proxy;
  this.protocols = builder.protocols;
  this.connectionSpecs = builder.connectionSpecs;
  this.interceptors = Util.immutableList(builder.interceptors);
  this.networkInterceptors = Util.immutableList(builder.networkInterceptors);
  this.eventListenerFactory = builder.eventListenerFactory;
  this.proxySelector = builder.proxySelector;
  this.cookieJar = builder.cookieJar;
  this.cache = builder.cache;
  this.internalCache = builder.internalCache;
  this.socketFactory = builder.socketFactory;

  boolean isTLS = false;
  for (ConnectionSpec spec : connectionSpecs) {
    isTLS = isTLS || spec.isTls();
  }

  if (builder.sslSocketFactory != null || !isTLS) {
    this.sslSocketFactory = builder.sslSocketFactory;
    this.certificateChainCleaner = builder.certificateChainCleaner;
  } else {
    X509TrustManager trustManager = systemDefaultTrustManager();
    this.sslSocketFactory = systemDefaultSslSocketFactory(trustManager);
    this.certificateChainCleaner = CertificateChainCleaner.get(trustManager);
  }

  this.hostnameVerifier = builder.hostnameVerifier;
  this.certificatePinner = builder.certificatePinner.withCertificateChainCleaner(
      certificateChainCleaner);
  this.proxyAuthenticator = builder.proxyAuthenticator;
  this.authenticator = builder.authenticator;
  this.connectionPool = builder.connectionPool;
  this.dns = builder.dns;
  this.followSslRedirects = builder.followSslRedirects;
  this.followRedirects = builder.followRedirects;
  this.retryOnConnectionFailure = builder.retryOnConnectionFailure;
  this.connectTimeout = builder.connectTimeout;
  this.readTimeout = builder.readTimeout;
  this.writeTimeout = builder.writeTimeout;
  this.pingInterval = builder.pingInterval;
}
```

在OkHttpClient.Builder的构造器中有很多默认的值，如下注释：

```java
public Builder() {
  dispatcher = new Dispatcher();    // 分发器，另有一个带线程池参数的构造器
  protocols = DEFAULT_PROTOCOLS;    // 支持的协议，默认为HTTP_2、HTTP_1_1
  connectionSpecs = DEFAULT_CONNECTION_SPECS;  // 传输层版本、连接协议
  // 事件监听器，3.8版本set方法还是package级别的，暂时不能设置
  eventListenerFactory = EventListener.factory(EventListener.NONE);
  proxySelector = ProxySelector.getDefault();   // 代理选择器
  cookieJar = CookieJar.NO_COOKIES;             // 读写Cookie的容器
  socketFactory = SocketFactory.getDefault();   // Socket工厂
  hostnameVerifier = OkHostnameVerifier.INSTANCE;// 主机名验证器
  certificatePinner = CertificatePinner.DEFAULT;
  proxyAuthenticator = Authenticator.NONE;      // 代理认证器
  authenticator = Authenticator.NONE;           // 本地认证器
  connectionPool = new ConnectionPool();        // 连接池
  dns = Dns.SYSTEM;                             // 域名
  followSslRedirects = true;                    // SSL重定向
  followRedirects = true;                       // 普通重定向
  retryOnConnectionFailure = true;              // 连接失败重试
  connectTimeout = 10_000;                      // 连接超时时间
  readTimeout = 10_000;                         // 读超时时间
  writeTimeout = 10_000;                        // 写超时时间
  pingInterval = 0;
}

```

详细讲一下每个 filed 的定义

- `Dispatcher dispatcher` :调度器，⽤于调度后台发起的网络请求，有后台总请求数和单主机总请求数的控制
- `List<Protocol> protocols` :支持的应⽤层协议，即 HTTP/1.1、 HTTP/2 等。
- `List<ConnectionSpec> connectionSpecs` :应⽤层⽀持的 Socket 设置，即使⽤明文传输(HTTP)还是某个版本的 TLS(HTTPS)
- `List<Interceptor> interceptors` :⼤多数时候使用的 Interceptor 都应该配置到这里。
- `List<Interceptor> networkInterceptors` :直接和⽹络请求交互的 Interceptor 配置到这⾥，如果你想查看返回的 301 报⽂文或者未解压 的 Response Body，需要在这⾥看。
- `CookieJar cookieJar` :管理 `Cookie` 的控制器。`OkHttp` 提供了 `Cookie` 存取的判⽀持(即什么时候需要存 Cookie，什么时候需要读取 Cookie，但没有给出具体的存取实现。如果需要存取 Cookie，你得⾃己写实现，例如用 Map 存在内存里，或者⽤别的⽅式存在本地存储或者数据库。
- `Cache cache` :Cache 存储的配置。默认是没有，如果需要用，得自己配置出 Cache 存储的⽂件位置以及存储空间上限。
- `HostnameVerifier hostnameVerifier` :⽤于验证 HTTPS 握⼿过程中下载到的证书所属者是否和⾃己要访问的主机名一致。
- `CertificatePinner certificatePinner` :⽤于设置 HTTPS 握手过程中针对某个 Host 的 Certificate Public Key Pinner，即把网站证书链中 的每一个证书公钥直接拿来提前配置进 OkHttpClient ⾥去，以跳过本地根证书，直接从代码⾥进⾏认证。这种用法比较少见，一般用于防止网站证书被人仿制。
- `Authenticator authenticator` :⽤于⾃动重新认证。配置之后，在请求收到 401 状态码的响应，会直接调⽤ authenticator ，⼿动加入 Authorization header 之后⾃动重新发起请求。
- `boolean followRedirects` :遇到重定向的要求，是否⾃动 follow。
- `boolean followSslRedirects`在重定向时，如果原先请求的是 http ⽽重定向的⽬标是 https，或者原先请求的是 https ⽽重定向的目标是
http，是否依然⾃动 follow。(记得，不是「是否⾃自动 follow HTTPS URL 重定向的意思，而是是否⾃动 follow 在 HTTP 和 HTTPS 之间切换的重定向)
- `boolean retryOnConnectionFailure`:在请求失败的时候是否⾃自动重试。注意，⼤多数的请求失败并不属于 OkHttp 所定义的「需要重试」， 这种重试只适⽤于「同⼀个域名的多个 IP 切换重试」「Socket 失效重试」 等情况。
- `int connectTimeout` :建立连接(TCP 或 TLS)的超时时间。
- `int readTimeout` :发起请求到读到响应数据的超时时间。
- `int writeTimeout` :发起请求并被⽬标服务器接受的超时时间。(为什么?因为有时候对方服务器可能由于某种原因而不读取你的 Request)

此外，`Request`的构造也很简单，字段如下：

```java
final HttpUrl url;                // 请求的url
final String method;              // 请求方式
final Headers headers;            // 请求头
final @Nullable RequestBody body; // 请求体
final Object tag;                 // 请求的tag
```

接下来看一下 OkHttpClient 的 newCall(Request) 方法

## 3.  OkHttpClient#newCall(Request) 方法 

```java
/**
  * Prepares the {@code request} to be executed at some point in the future.
  */
@Override public Call newCall(Request request) {
  return new RealCall(this, request, false /* for web socket */);
}
```

`newCall(Request)` ⽅法会返回⼀个 `RealCall` 对象，它是 `Call` 接口的实现。

```java
public interface Call extends Cloneable {
  /** 获得原始请求 */
  Request request();

  /** 同步执行请求 */
  Response execute() throws IOException;

  /** 异步执行请求 */
  void enqueue(Callback responseCallback);

  /** 尽可能取消请求。已经完成了的请求不能被取消 */
  void cancel();

  /**
   * 调用了execute()或者enqueue(Callback)后都是true
   */
  boolean isExecuted();

  boolean isCanceled();

  /** 创建一个新的、完全一样的Call对象，即使原对象状态为enqueued或者executed */
  Call clone();

  interface Factory {
    Call newCall(Request request);
  }
}
```

当调用 `RealCall.execute()` 的时候，`RealCall.getResponseWithInterceptorChain()` 会被调用，它会发起⽹络请求并拿到返回的响应，装进一个 `Response` 对象并作为返回值返回;

`RealCall.enqueue()` 被调⽤的时候大同小异，区别在于
`enqueue()` 会使⽤ `Dispatcher` 的线程池来把请求放在后台线程进行，但实质上使用的同样也是 `getResponseWithInterceptorChain()` 方法。

## 4.getResponseWithInterceptorChain() 

`getResponseWithInterceptorChain()` ⽅法做的事:把所有配置好的 `Interceptor` 放在⼀个 `List` ⾥，然后作为参数，创建⼀个 `RealInterceptorChain` 对象，并调用 `chain.proceed(request)` 来发起请求和获取响应。

## 5.RealInterceptorChain

在 `RealInterceptorChain` 中，多个 Interceptor 会依次调用⾃己的intercept() ⽅法。这个方法会做三件事:

1.对请求进⾏预处理
 
2.预处理之后，重新调用 `RealIntercepterChain.proceed()` 把请求交给下一个 `Interceptor`

3.在下⼀个 `Interceptor` 处理完成并返回之后，拿到 `Response` 进⾏后续处理

>当然了，最后⼀个 `Interceptor` 的任务只有⼀个:做真正的⽹络请求并拿到响应。

## 6.从上到下，每级 `Interceptor` 做的事

首先是开发者使用 `addInterceptor(Interceptor)` 所设置的，它们会按照开发者的要求，在所有其他 `Interceptor` 处理之前，进行最早的预处理⼯作，以及在收到 Response 之后，做最后的善后⼯作。如果你有统一的 `header` 要添加，可以在这⾥设置;

然后是 `RetryAndFollowUpInterceptor` :它负责在请求失败时的重试，以及重定向的⾃动后续请求。它的存在，可以让重试和重定向对于开发者是无感知的;

`BridgeInterceptor` :它负责⼀些不影响开发者开发，但影响 HTTP 交互的一些额外预处理。例如，`Content-Length` 的计算和添加、`gzip` 的⽀持 (Accept-Encoding: gzip)、gzip 压缩数据的解包，都是发生在这⾥;

`CacheInterceptor` :它负责 `Cache` 的处理。把它放在后⾯的网络交互相关 Interceptor 的前⾯的好处是，如果本地有了了可⽤的 Cache，⼀个请求可以在没有发⽣生实质⽹网络交互的情况下就返回缓存结果，⽽而完全不不需要 开发者做出任何的额外⼯工作，让 Cache 更更加⽆无感知;

`ConnectInterceptor` :它负责建⽴连接。在这里，OkHttp 会创建出网络请求所需要的 TCP 连接(如果是 HTTP)，或者是建⽴在 TCP 连接之上 的 TLS 连接(如果是 HTTPS)，并且会创建出对应的 HttpCodec 对象 (⽤用于编码解码 HTTP 请求);

然后是开发者使⽤ `addNetworkInterceptor(Interceptor)` 所设置的，它们的行为逻辑和使⽤ `addInterceptor(Interceptor)` 创建的一样，但由于位置不同，所以这⾥创建的 `Interceptor` 会看到每个请求和响应的数据(包括重定向以及重试的⼀些中间请求和响应)，并且看到的 是完整原始数据，⽽不是没有加 `Content-Length` 的请求数据，或者 `Body` 还没有被 `gzip` 解压的响应数据。多数情况，这个方法不需要被使⽤;

`CallServerInterceptor` :它负责实质的请求与响应的 I/O 操作，即 往 Socket ⾥写⼊入请求数据，和从 Socket 里读取响应数据。



