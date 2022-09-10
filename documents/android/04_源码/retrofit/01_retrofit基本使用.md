### 1. 要点

1.1 GET 请求不能使用 @Body 注解

1.2 baseUrl 网址末尾一定加 "/" 斜杠

1.3 @Body 注解一定要加 .addConverterFactory(GsonConverterFactory.create())

### 2. GET 请求

2.1 请求样式：baseUrl + getUserInfo?id=1234

```java
@GET("api/getUserInfo")
Call<UserInfo> getUserInfo(@Query("id") String userId);
```

2.2 请求样式是通过 "?" 形式拼接多个参数时就使用 @QueryMap

```java
@GET("api/getArticalInfo")
Call<ArticalInfo> getArticalInfo(@QueryMap Map<String, String> params);
```

2.3 要访问的地址由某个参数动态拼接而成时，使用 @Path 注解

```java
@GET("api/getDynamicInfo/{param}/data")
Call<ResponseBody> getDynamicInfo(@Path("param")int param);
```

2.4 当要访问的地址不只是动态的变几个参数，而是整个地址都要变化，甚至是基类地址也要变化时，这种动态地址就要用到 @Url 注解

```java
@GET
Call<ResponseBody> getDynamicUrl(@Url String url);

String url = "http://mock-api.com/2vKVbXK8.mock/api/getDynamicUrlData"
getApi.getDynamicUrl(url).enqueue(callback)
```

### 3. 添加 Header 

3.1 Headers 注解

```java
@Headers("version:1.1")
@GET("api/staticHeaderInfo")
Call<GetBean> getStaticHeadersInfo();
```

3.2 Header 注解

```java
@GET("api/dynamicHeadersInfo")
Call<ResponseBody> getDynamicHeaderInfo(@Header("version") String version);
```

3.3 HeaderMap 注解

```java
@GET("api/dynamicHeadersInfo")
Call<ResponseBody> getDynamicHeadersInfo(@HeaderMap Map<String, String> headers);
```

### 4. POST 请求

4.1 参数较少时使用 @Field

```java
@FormUrlEncoded
@POST("api/fieldParam")
Call<ResponseBody> postFieldFun(@Field("key") String key);
```

4.2 用 @FieldMap 传递多个参数

```java
@FormUrlEncoded
@POST("api/fieldMapParam")
Call<ResponseBody> postFildMapFun(@FieldMap Map<String, String> params);
```

4.3 用 @Body 注解，直接传入一个对象过去

```java
@POST("api/bodyParam")
Call<ResponseBody> postBodyFun(@Body PostBodyBean postBodyBean);
```
### 5. @Part/@PartMap 文件上传

5.1 单个文件上传

```java
@Multipart
@POST("upload")
Call<ResponseBody> uploadOneFile(@Part MultipartBody.Part body);

File file = new File(path);
RequestBody fileRQ = RequestBody.create(MediaType.parse("image/*"), file);
MultipartBody.Part part MultipartBody.Part.createFormData("picture", file.getName(), fileRQ);

Call<ResponseBody> uploadCall = uploadService.uploadOneFile(part);
```

5.2 使用@PartMap实现多文件上传

```java
@Multipart
@POST("upload")
Call<ResponseBody> uploadFiles(@PartMap Map<String, RequestBody> map);
```


