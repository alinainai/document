
Subject：它既是Observable，又是observer。也就是既可以发送事件，也可以接收事件。

```java
PublishSubject<Integer> source = PublishSubject.create();　　//PublicSubject：接收到订阅之后的所有数据。
// It will get 1, 2, 3, 4 and onComplete
source.subscribe(getFirstObserver()); 
source.onNext(1);
source.onNext(2);
source.onNext(3);
// It will get 4 and onComplete for second observer also.
source.subscribe(getSecondObserver());
source.onNext(4);
source.onComplete();
```
```java
ReplaySubject<Integer> source = ReplaySubject.create();　　//ReplaySubject：接收到所有的数据，包括订阅之前的所有数据和订阅之后的所有数据。
// It will get 1, 2, 3, 4
source.subscribe(getFirstObserver());
source.onNext(1);
source.onNext(2);
source.onNext(3);
source.onNext(4);
source.onComplete();
// It will also get 1, 2, 3, 4 as we have used replay Subject
source.subscribe(getSecondObserver());
```
```java
BehaviorSubject<Integer> source = BehaviorSubject.create();　　//BehaviorSubject：接收到订阅前的最后一条数据和订阅后的所有数据。
// It will get 1, 2, 3, 4 and onComplete
source.subscribe(getFirstObserver());
source.onNext(1);
source.onNext(2);
source.onNext(3);
// It will get 3(last emitted)and 4(subsequent item) and onComplete
source.subscribe(getSecondObserver());
source.onNext(4);
source.onComplete();
```
```java
AsyncSubject<Integer> source = AsyncSubject.create();　　//AsyncSubject：不管在什么位置订阅，都只接接收到最后一条数据
// It will get only 4 and onComplete
source.subscribe(getFirstObserver());
source.onNext(1);
source.onNext(2);
source.onNext(3);
// It will also get only get 4 and onComplete
source.subscribe(getSecondObserver());
source.onNext(4);
source.onComplete();
```
