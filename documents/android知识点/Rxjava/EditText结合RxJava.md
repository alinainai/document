    

    #### Presenter类代码
    
    ```Kotlin
    //新建一个数据发射类
    private val publishSubject: PublishSubject<String> = PublishSubject.create()

    //init 方法中进行注册
    init {
        addDispose(publishSubject.debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .switchMap { t -> Observable.just(mModel.getMapsByFilter(t)) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ t -> mView.setMapData(t) }, { e -> e.printStackTrace() }))
    }
    
    //当 EditText 发生变化回调这个方法
    fun startSearch(str: String) {
        publishSubject.onNext(str)
    }
    ```
    
    #### debounce 操作符
    
    去抖动 
    
     ```Kotlin
    debounce(200, TimeUnit.MILLISECONDS)
     ```
    当 200 MILLISECONDS 内无新事件产生才会发射数据。
    
    #### switchMap 操作符
    
    只重新发送操作符接收的最后一个收到的数据
    
    
    
    
    
    
