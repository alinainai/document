```Kotlin
    private int currentRetryCount = 0;
    private static final int maxConnectCount = 6;
    private int waitRetryTime = 0;

    @Override
    public void updateRegionConfig() {
        mCompositeDisposable.add(regionBusiness.getRegionConfigFromServer().
                retryWhen(throwableObservable -> throwableObservable.flatMap((Function<Throwable, ObservableSource<?>>) throwable -> {
                    if (throwable instanceof IOException) {
                        if (currentRetryCount < maxConnectCount) {
                            currentRetryCount++;
                            waitRetryTime = 1000 + currentRetryCount * 1000;
                            CLog.e("getRegionConfig", "currentRetryCount="+currentRetryCount );
                            return Observable.just(1).delay(waitRetryTime, TimeUnit.MILLISECONDS);
                        } else {
                            return Observable.error(new Throwable("重试次数已超过设置次数 = " + currentRetryCount + "，即 不再重试"));
                        }
                    } else {
                        CLog.e("getRegionConfig", "error=非I/O异常" );
                        return Observable.error(new Throwable("发生了非网络异常（非I/O异常）"));
                    }
                })).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.getRegionConfig() != null) {
                        CLog.e("getRegionConfig", "result="+result );
                        currentRetryCount=0;
                        waitRetryTime= 0;
                        regionBusiness.saveRegionConfigCacheAndSp(result.getRegionConfig());

                    }
                }, throwable -> {
                    currentRetryCount=0;
                    waitRetryTime= 0;

                    CLog.e("getRegionConfig", "error=" + throwable);
                }));
    }
```
