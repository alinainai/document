
## 一、简介

一个用于帮助 Android App 进行组件化改造的框架；

支持模块间的路由（ARouter的主要功能）、通信（使用依赖注入原理实现）、解耦（使用依赖注入原理通过面向接口编程实现）

Arouter的github地址: [https://github.com/alibaba/ARouter](https://github.com/alibaba/ARouter)

demo地址:[https://github.com/alibaba/ARouter.git](https://github.com/alibaba/ARouter.git)

中文文档:[https://github.com/alibaba/ARouter/blob/master/README_CN.md](https://github.com/alibaba/ARouter/blob/master/README_CN.md)

可以先 clone 到本地，编译之前先把 project 中 settings.gradle 的
```groovy
//include ':arouter-idea-plugin' 注释掉。
```
## 二、使用

依据文档集成最新版本即可，注意打印日志和ARouter功能的开启

```java
ARouter.openDebug();
ARouter.init(getApplication());
```
最基本路由功能
```java
ARouter.getInstance().build(path).navigation();
```

普通跳转：可以传 Object 以及基本数据类型

```java
private void goToShareActivityNormal() {
    Author author = new Author();
    author.setName("Margaret Mitchell");
    author.setCounty("USA");
    ARouter.getInstance().build("/share/shareBook")
            .withString("bookName", "Gone with the Wind")
            .withObject("author", author)
            .navigation();
}
```

startActivityForResult 跳转

```java
 private void goToShareActivityForResult() {
    Author author = new Author();
    author.setName("Margaret Mitchell");
    author.setCounty("USA");
    ARouter.getInstance().build("/share/shareMagazine")
            .withString("bookName", "Gone with the Wind")
            .withObject("author", author)
            .navigation(getActivity(), REQUEST_CODE);
}
```

跳转这块和原生的 startActivity 基本没有区别，具体可以参考 ARouter 的 demo。在跳转之后的页面使用 getIntent 获取基本数据类型，推荐使用 @Autowired 配合 ARouter.getInstance().inject(this) 获取参数。

详情参见 demo的 Test1Activit y测试类。

#### Path 跳转和提供实体类的标识符

使用 @Route(path = "/test/activity1") 配置，此参数为跳转的标识。

也就是上面 ARouter.getInstance().build(path).navigation() 里面的Path

使用如下：

```java
@Route(path = "/test/activity1", name = "测试用 Activity")
public class Test1Activity extends AppCompatActivity {}；
```
所有跨组件进行跳转的Activity尽量配置在 ComBiz中，
还有一些通用的实体类尽量也配置在ComBiz中。
对外提供实体类，如Fragment

#### Fragment的配置
```java
@Route(path = "/test/fragment")
public class BlankFragment extends Fragment {}
```
#### 获取Fragment 对象
```java
Fragment fragment = (Fragment) ARouter.getInstance().build("/test/fragment").navigation();
```
#### 组件化中用到的功能

在业务模块中常常需要定义一些各个模块对外暴露的接口供其他模块使用。
这里可以在业务模块中定义各个模块对外暴露的接口

如：

```java
public interface HelloService extends IProvider { 
    void sayHello(String name);
    String getName();
    boolean isLogin();
}
```
#### 此时要继承ARouter的IProvider，在各个功能模块中实现该接口
如：
```java
@Route(path = "/yourservicegroupname/hello")
public class HelloServiceImpl implements HelloService {
    Context mContext;
    @Override    
    public void sayHello(String name) {        
        Toast.makeText(mContext, "Hello " + name, Toast.LENGTH_SHORT).show();     
    }      
    @Override   
    public void init(Context context) {      
        mContext = context; 
    }
}
```

#### 其他业务模块可以通过注解的方式进行调用

```java
@Autowired(name = “/yourservicegroupname/hello“)
ZhihuInfoService mZhihuInfoService;
```

#### 在OnCreate方法中进行注入注册
```java
ARouter.getInstance().inject(this);
```
#### 也可通过下面方式调用
```java
((HelloService) ARouter.getInstance().build("/yourservicegroupname/hello").navigation()).sayHello("mike");
```
#### 目前我开源的一个组件化项目
https://github.com/alinainai/Base
