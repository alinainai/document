## 1.基本使用

OkHttp是一个默认有效的HTTP客户端：

- HTTP/2支持允许对同一主机的所有请求共享套接字。
- 连接池减少了请求延迟（如果HTTP/2不可用）。
- transparent GZIP 压缩了下载大小。
- Response缓存可以完全避免网络的重复请求。

当网络故障时，OkHttp会自动重试：它将从常见的连接问题中静默地恢复。如果您的服务有多个IP地址，如果第一次连接失败，OkHttp将尝试备用地址；这对于IPv4 + IPv6以及在冗余数据中心中托管的服务是必需的。

OkHttp还支持现代TLS功能（TLS 1.3，ALPN，certificate pinning）。It can be configured to fall back for broad connectivity.

本章主要介绍OkHttp的实现，代码基于 [okhttp-4.9.0](https://github.com/square/okhttp/tree/parent-4.9.0)

```kotlin
//1、用建造者模式初始化一个 OkHttpClient 对象
val builder = OkHttpClient.Builder()
builder.connectTimeout(10, TimeUnit.SECONDS)

builder.addInterceptor(Interceptor { chain ->
    Log.d(TAG, "Interceptor url:" + chain.request().url.toString())
    chain.proceed(chain.request())
})
builder.addNetworkInterceptor(Interceptor { chain ->
    Log.d(TAG, "NetworkInterceptor url:" + chain.request().url.toString())
    chain.proceed(chain.request())
})

val client: OkHttpClient = builder.build()

//2、初始化一个 Request 对象
val request: Request = Request.Builder()
    .url("https://www.baidu.com")
    .build()

//3、调用 OkHttpClient#newCall(Request) 方法生成一个 Call 对象
val call: Call = client.newCall(request)
```

调用 Call.enqueue(Callback) 或者 Call.execute() 方法实现请求

```kotlin
// 1.enqueue(Callback) 异步请求
call.enqueue(object : Callback {
    override fun onFailure(call: Call, e: IOException) {
        Log.d(TAG, "onFailure: " + e.message)
    }
    override fun onResponse(call: Call, response: Response) {
        Log.d(TAG, "response:" + response.body?.string())
    }
})
//2.execute() 同步请求，返回一个 Response 对象    
val response = call.execute()
```

使用步骤总结：

- 1.使用 OkHttpClient.Builder 新建一个 OkHttpClient 对象，可以在 OkHttpClient.Builder 设置一些参数
- 2.使用 Request.Builder 新建一个 Request 对象
- 3.使用 OkHttpClient.newCall(Request) 生成一个 Call 对象
- 4.调用 Call.enqueue(Callback) 或者 Call.execute() 实现网络请求。

## 2.OkHttpClient 的建造者参数

`OkHttpClient` 相当于配置中心，所有的请求都会共享这些配置。比如：出错是否重试、共享连接池。

在 `OkHttpClient.Builder` 的构造器中有很多默认的值：

```kotlin
  class Builder constructor() {
    internal var dispatcher: Dispatcher = Dispatcher() //调度器，⽤于调度后台发起的网络请求，有后台总请求数和单主机总请求数的控制，还可以使用自定义线程池
    internal var connectionPool: ConnectionPool = ConnectionPool() // 应⽤层⽀持的 Socket 设置，即使⽤明文传输(HTTP)还是某个版本的 TLS(HTTPS)
    internal val interceptors: MutableList<Interceptor> = mutableListOf() // ⼤多数时候使用的 Interceptor 都应该配置到这里。
    internal val networkInterceptors: MutableList<Interceptor> = mutableListOf() // 直接和⽹络请求交互的 Interceptor 配置到这⾥，如果你想查看返回的 301 报⽂文或者未解压 的 Response Body，需要在这⾥看。
    internal var eventListenerFactory: EventListener.Factory = EventListener.NONE.asFactory() //
    internal var retryOnConnectionFailure = true // 在请求失败的时候是否⾃自动重试。注意，⼤多数的请求失败并不属于 OkHttp 所定义的「需要重试」，这种重试只适⽤于「同⼀个域名的多个 IP 切换重试」「Socket 失效重试」 等情况。
    internal var authenticator: Authenticator = Authenticator.NONE // ⽤于⾃动重新认证。配置之后，在请求收到 401 状态码的响应，会直接调⽤ authenticator ，⼿动加入 Authorization header 之后⾃动重新发起请求。
    internal var followRedirects = true // 遇到重定向的要求，是否⾃动 follow。
    internal var followSslRedirects = true // 在重定向时，如果原先请求的是 http ⽽重定向的⽬标是 https，或者原先请求的是 https ⽽重定向的目标是
http，是否依然⾃动 follow。(记得，不是「是否⾃自动 follow HTTPS URL 重定向的意思，而是是否⾃动 follow 在 HTTP 和 HTTPS 之间切换的重定向)
    internal var cookieJar: CookieJar = CookieJar.NO_COOKIES //管理 `Cookie` 的控制器。`OkHttp` 提供了 `Cookie` 存取的判⽀持(即什么时候需要存 Cookie，什么时候需要读取 Cookie，但没有给出具体的存取实现。如果需要存取 Cookie，你得⾃己写实现，例如用 Map 存在内存里，或者⽤别的⽅式存在本地存储或者数据库。
    internal var cache: Cache? = null  // Cache 存储的配置。默认是没有，如果需要用，得自己配置出 Cache 存储的⽂件位置以及存储空间上限。
    internal var dns: Dns = Dns.SYSTEM
    internal var proxy: Proxy? = null
    internal var proxySelector: ProxySelector? = null
    internal var proxyAuthenticator: Authenticator = Authenticator.NONE
    internal var socketFactory: SocketFactory = SocketFactory.getDefault()
    internal var sslSocketFactoryOrNull: SSLSocketFactory? = null
    internal var x509TrustManagerOrNull: X509TrustManager? = null
    internal var connectionSpecs: List<ConnectionSpec> = DEFAULT_CONNECTION_SPECS
    internal var protocols: List<Protocol> = DEFAULT_PROTOCOLS // 支持的应⽤层协议，即 HTTP/1.1、 HTTP/2 等。
    internal var hostnameVerifier: HostnameVerifier = OkHostnameVerifier // ⽤于验证 HTTPS 握⼿过程中下载到的证书所属者是否和⾃己要访问的主机名一致。
    internal var certificatePinner: CertificatePinner = CertificatePinner.DEFAULT // ⽤于设置 HTTPS 握手过程中针对某个 Host 的 Certificate Public Key Pinner，即把网站证书链中 的每一个证书公钥直接拿来提前配置进 OkHttpClient ⾥去，以跳过本地根证书，直接从代码⾥进⾏认证。这种用法比较少见，一般用于防止网站证书被人仿制。
    internal var certificateChainCleaner: CertificateChainCleaner? = null
    internal var callTimeout = 0
    internal var connectTimeout = 10_000 // 建立连接(TCP 或 TLS)的超时时间。
    internal var readTimeout = 10_000 // 发起请求到读到响应数据的超时时间。
    internal var writeTimeout = 10_000 // 发起请求并被⽬标服务器接受的超时时间。(为什么?因为有时候对方服务器可能由于某种原因而不读取你的 Request)
    internal var pingInterval = 0
    internal var minWebSocketMessageToCompress = RealWebSocket.DEFAULT_MINIMUM_DEFLATE_SIZE
    internal var routeDatabase: RouteDatabase? = null
```

此外，`Request`的构造也很简单，字段如下：

```kotlin
class Request internal constructor(
  @get:JvmName("url") val url: HttpUrl,
  @get:JvmName("method") val method: String,
  @get:JvmName("headers") val headers: Headers,
  @get:JvmName("body") val body: RequestBody?,
  internal val tags: Map<Class<*>, Any>
){...}
```

接下来看一下 OkHttpClient 的 newCall(Request) 方法

## 3.Call 和 RealCall 

按照调用步骤，首先看下`OkHttpCient.newCall(Request)`方法：

```kotlin
override fun newCall(request: Request): Call = RealCall(this, request, forWebSocket = false)
```
`newCall(Request)` ⽅法会返回⼀个 `RealCall` 对象，它是 `Call` 接口的实现。

```kotlin
interface Call : Cloneable {
  /** 获得原始请求 */
  fun request(): Request
  
  /** 同步执行请求 */
  @Throws(IOException::class)
  fun execute(): Response

  /** 尽可能取消请求。已经完成了的请求不能被取消 
  fun cancel()

  /** 调用了execute()或者enqueue(Callback)后都是true */
  fun isExecuted(): Boolean

  fun isCanceled(): Boolean

  fun timeout(): Timeout
  
  /** 创建一个新的、完全一样的Call对象，即使原对象状态为enqueued或者executed */
  public override fun clone(): Call

  fun interface Factory {
    fun newCall(request: Request): Call
  }
}
```

在看一下 `RealCall` 类，它的构造器以及成员变量：

```kotlin
class RealCall(
  val client: OkHttpClient,
  /** The application's original request unadulterated by redirects or auth headers. */
  val originalRequest: Request,
  val forWebSocket: Boolean
) : Call {
  private val connectionPool: RealConnectionPool = client.connectionPool.delegate

  internal val eventListener: EventListener = client.eventListenerFactory.create(this)

  private val timeout = object : AsyncTimeout() {
    override fun timedOut() {
      cancel()
    }
  }.apply {
    timeout(client.callTimeoutMillis.toLong(), MILLISECONDS)
  }
  //标记是否已经执行
  private val executed = AtomicBoolean()
```

## 4、同步请求 RealCall.execute()

同步请求的代码如下：

```kotlin
override fun execute(): Response {
  check(executed.compareAndSet(false, true)) { "Already Executed" }
  
  timeout.enter()
  callStart()
  try {
    // 1、将 RealCall 添加到同步执行队列
    client.dispatcher.executed(this)
    // 2、通过该方法获取 Response 对象
    return getResponseWithInterceptorChain()
  } finally {
    // 3、执行完成后将 RealCall 从同步执行队列中移除
    client.dispatcher.finished(this)
  }
}
```
该方法最终会调用 `RealCall.getResponseWithInterceptorChain()` 方法，它会发起⽹络请求并拿到返回的响应，装进一个 `Response` 对象并作为返回值返回;

## 5.异步请求 RealCall.enqueue(Callback)

`RealCall.enqueue()` 被调⽤的时候大同小异，区别在于`enqueue()` 会使⽤ `Dispatcher` 的线程池来把请求放在后台线程进行，但最终还是会调用 `RealCall.getResponseWithInterceptorChain()` 方法。

`AsyncCall` 是一个 `Runnable` 对象，它是 `RealCall` 的内部类，部分代码如下:

```kotlin
 internal inner class AsyncCall(
    private val responseCallback: Callback
  ) : Runnable {
    @Volatile var callsPerHost = AtomicInteger(0) 
      private set

    fun reuseCallsPerHostFrom(other: AsyncCall) {// 复用
      this.callsPerHost = other.callsPerHost
    }

    val host: String
      get() = originalRequest.url.host

    val request: Request
        get() = originalRequest

    val call: RealCall
        get() = this@RealCall
```

接来下让我们跟踪下 `RealCall.enqueue(Callback)` 方法

```kotlin
// 通过 RealCall.enqueue(Callback) 方法执行异步请求
override fun enqueue(responseCallback: Callback) {
  check(executed.compareAndSet(false, true)) { "Already Executed" }
  callStart()
  //调用 OkHttpClient 的 Dispatcher.enqueue(AsyncCall) 方法
  client.dispatcher.enqueue(AsyncCall(responseCallback))
}
```

该方法新建一个 `AsyncCall` 对象，并把 `responseCallback` 当做参数传入到 `AsyncCall` 中。然后使用 `client.dispatcher` 去处理 `AsyncCall` 对象。
我们继续追踪 `Dispatcher.enqueue(AsyncCall)` 方法：

```kotlin
// Dispatcher.enqueue(AsyncCall) 方法
internal fun enqueue(call: AsyncCall) {
  synchronized(this) {
    //将 AsyncCall 添加到 readyAsyncCalls 队列中
    readyAsyncCalls.add(call)
    // Mutate the AsyncCall so that it shares the AtomicInteger of an existing running call to
    // the same host.
    if (!call.call.forWebSocket) {// 看看统一域名下有没有可以复用的 AsyncCall
      val existingCall = findExistingCallWithHost(call.host)
      if (existingCall != null) call.reuseCallsPerHostFrom(existingCall)
    }
  }
  //调用 Dispatcher.promoteAndExecute 方法 
  promoteAndExecute()
}
```

这个方法的主要功能是将 `AsyncCall` 添加到异步的预备队列中，然后调用 `Dispatcher.promoteAndExecute()` 方法。
我们来看一下 `Dispatcher.promoteAndExecute()` 的代码:

```kotlin
private fun promoteAndExecute(): Boolean {
  this.assertThreadDoesntHoldLock()
  
  val executableCalls = mutableListOf<AsyncCall>()
  val isRunning: Boolean
  synchronized(this) {
    val i = readyAsyncCalls.iterator()
    while (i.hasNext()) {
      val asyncCall = i.next()
      // 1、如果正在运行的 AsyncCalls 的 size 不符合条件就停止循环，默认异步执行的 runningAsyncCalls 的数量为64 个
      if (runningAsyncCalls.size >= this.maxRequests) break // Max capacity.
      if (asyncCall.callsPerHost.get() >= this.maxRequestsPerHost) continue // Host max capacity. 每个域名下的最大数量是5个
      i.remove()
      asyncCall.callsPerHost.incrementAndGet()
      // 将 asyncCall 添加到可运行List集合
      executableCalls.add(asyncCall)
      // 2、将 asyncCall 添加到执行中的List集合
      runningAsyncCalls.add(asyncCall)
    }
    isRunning = runningCallsCount() > 0
  }
  // 3、将 executableCalls中的 AsyncCall 添加到 Dispatcher 的线程池中
  for (i in 0 until executableCalls.size) {
    val asyncCall = executableCalls[i]
    asyncCall.executeOn(executorService)
  }
  
  return isRunning
}
```

该方法的主要作用是用 `Dispatcher` 中的线程池来执行 `AsyncCall`，`AsyncCall.executeOn(ExecutorService)` 就是使用线程池来执行 `AsyncCall(Runnable的子类)`

```kotlin
/**
  * Attempt to enqueue this async call on [executorService]. This will attempt to clean up
  * if the executor has been shut down by reporting the call as failed.
  */
fun executeOn(executorService: ExecutorService) {
  client.dispatcher.assertThreadDoesntHoldLock()
  
  var success = false
  try {
    //使用线程池执行 AsyncCall 对象，也就是调用 AsyncCall 的 run 方法
    executorService.execute(this)
    success = true
  } catch (e: RejectedExecutionException) {
    val ioException = InterruptedIOException("executor rejected")
    ioException.initCause(e)
    noMoreExchanges(ioException)
    responseCallback.onFailure(this@RealCall, ioException)
  } finally {
    if (!success) {
      client.dispatcher.finished(this) // This call is no longer running!
    }
  }
}
```
继续看 AsyncCall.run() 方法，最终还是调用 RealCall.getResponseWithInterceptorChain() 方法
```kotlin
override fun run() {
  threadName("OkHttp ${redactedUrl()}") {
    var signalledCallback = false
    timeout.enter()
    try {
      //核心代码，通过 getResponseWithInterceptorChain() 获取 response 对象
      val response = getResponseWithInterceptorChain()
      signalledCallback = true
      responseCallback.onResponse(this@RealCall, response)
    } catch (e: IOException) {
      if (signalledCallback) {
        // Do not signal the callback twice!
        Platform.get().log("Callback failure for ${toLoggableString()}", Platform.INFO, e)
      } else {
        responseCallback.onFailure(this@RealCall, e)
      }
    } catch (t: Throwable) {
      cancel()
      if (!signalledCallback) {
        val canceledException = IOException("canceled due to $t")
        canceledException.addSuppressed(t)
        responseCallback.onFailure(this@RealCall, canceledException)
      }
      throw t
    } finally {
      client.dispatcher.finished(this)
    }
  }
}
```

## 6.RealCall.getResponseWithInterceptorChain() 方法

`getResponseWithInterceptorChain()` ⽅法把所有配置好的 `Interceptor` 放在⼀个 `List` ⾥，然后作为参数，创建⼀个 `RealInterceptorChain` 对象，并调用 `chain.proceed(request)` 来发起请求和获取响应。
```kotlin
  @Throws(IOException::class)
  internal fun getResponseWithInterceptorChain(): Response {
    // Build a full stack of interceptors.
    val interceptors = mutableListOf<Interceptor>()
    interceptors += client.interceptors
    interceptors += RetryAndFollowUpInterceptor(client)
    interceptors += BridgeInterceptor(client.cookieJar)
    interceptors += CacheInterceptor(client.cache)
    interceptors += ConnectInterceptor
    if (!forWebSocket) {
      interceptors += client.networkInterceptors
    }
    interceptors += CallServerInterceptor(forWebSocket)

    // 创建⼀个 RealInterceptorChain 对象
    val chain = RealInterceptorChain(
        call = this,
        interceptors = interceptors,
        index = 0, //初始 index = 0
        exchange = null,
        request = originalRequest,
        connectTimeoutMillis = client.connectTimeoutMillis,
        readTimeoutMillis = client.readTimeoutMillis,
        writeTimeoutMillis = client.writeTimeoutMillis
    )

    var calledNoMoreExchanges = false
    try {
      // 调用 chain.proceed(request) 来发起请求和获取响应
      val response = chain.proceed(originalRequest)
      if (isCanceled()) {
        response.closeQuietly()
        throw IOException("Canceled")
      }
      return response
    } catch (e: IOException) {
      calledNoMoreExchanges = true
      throw noMoreExchanges(e) as Throwable
    } finally {
      if (!calledNoMoreExchanges) {
        noMoreExchanges(null)
      }
    }
  }
```
再看下`RealInterceptorChain.proceed(Request)`方法

## 5.RealInterceptorChain 

```kotlin
  @Throws(IOException::class)
  override fun proceed(request: Request): Response {
    ...
    // Call the next interceptor in the chain.
    val next = copy(index = index + 1, request = request)
    val interceptor = interceptors[index]

    @Suppress("USELESS_ELVIS")
    val response = interceptor.intercept(next) ?: throw NullPointerException(
        "interceptor $interceptor returned null")
    ...
    return response
  }
```

在 `RealInterceptorChain` 中，多个 Interceptor 会依次调用⾃己的intercept() ⽅法。

intercept()方法会做三件事:

- 1.对请求进⾏预处理
- 2.预处理之后，重新调用 `RealIntercepterChain.proceed()` 把请求交给下一个 `Interceptor`
- 3.在下⼀个 `Interceptor` 处理完成并返回之后，拿到 `Response` 进⾏后续处理

>当然了，最后⼀个 `Interceptor` 的任务只有⼀个:做真正的⽹络请求并拿到响应。

## 6.从上到下，每级 `Interceptor` 做的事

- 1.首先是开发者使用 `addInterceptor(Interceptor)` 所设置的，它们会按照开发者的要求，在所有其他 `Interceptor` 处理之前，进行最早的预处理⼯作，以及在收到 Response 之后，做最后的善后⼯作。如果你有统一的 `header` 要添加，可以在这⾥设置;
- 2.然后是 `RetryAndFollowUpInterceptor` :它负责在请求失败时的重试，以及重定向的⾃动后续请求。它的存在，可以让重试和重定向对于开发者是无感知的;
- 3.`BridgeInterceptor`:它负责⼀些不影响开发者开发，但影响 HTTP 交互的一些额外预处理。例如，`Content-Length` 的计算和添加、`gzip` 的⽀持 (Accept-Encoding: gzip)、gzip 压缩数据的解包，都是发生在这⾥;
- 4.`CacheInterceptor` :它负责 `Cache` 的处理。把它放在后⾯的网络交互相关 Interceptor 的前⾯的好处是，如果本地有了了可⽤的 Cache，⼀个请求可以在没有发⽣生实质⽹网络交互的情况下就返回缓存结果，⽽而完全不不需要 开发者做出任何的额外⼯工作，让 Cache 更更加⽆无感知;
- 5.`ConnectInterceptor` :它负责建⽴连接。在这里，OkHttp 会创建出网络请求所需要的 TCP 连接(如果是 HTTP)，或者是建⽴在 TCP 连接之上 的 TLS 连接(如果是 HTTPS)，并且会创建出对应的 HttpCodec 对象 (⽤用于编码解码 HTTP 请求);
- 6.然后是开发者使⽤ `addNetworkInterceptor(Interceptor)` 所设置的，它们的行为逻辑和使⽤ `addInterceptor(Interceptor)` 创建的一样，但由于位置不同，所以这⾥创建的 `Interceptor` 会看到每个请求和响应的数据(包括重定向以及重试的⼀些中间请求和响应)，并且看到的 是完整原始数据，⽽不是没有加 `Content-Length` 的请求数据，或者 `Body` 还没有被 `gzip` 解压的响应数据。多数情况，这个方法不需要被使⽤;
- 7.`CallServerInterceptor` :它负责实质的请求与响应的 `I/O` 操作，即往 `Socket` ⾥写入请求数据，和从 `Socket` 里读取响应数据。

## 



