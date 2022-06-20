### 1. 在 Manifest 设置 Activity 的启动模式

#### 1.1 standard 默认的启动模式

默认启动模式，每次激活 Activity 时都会创建 Activity 实例，并放入任务栈中。Activity A 启动了 Activity B (standard), 则 B 运行在 A 所在的栈中（即发送 intent 的 task 栈的顶部）。

注意：用 ApplicationContext 去启动 standard 模式的 Activity 时，要设置 FLAG_ACTIVITY_NEW_TASK flag。

适用场景：一般大多数 Activity

#### 1.2 singleTop 栈顶复用

如果在当前任务的栈顶正好存在该 Activity 的实例，就重用该实例，会调用实例的 onNewIntent()。

适用场景：阅读页面，视频播放页面。

#### 1.3 singleTask 栈内复用

在系统中查找属性值 affinity 等于它的属性值 taskAffinity 的任务栈。如果存在则在该任务栈中启动，否则先新建 affinity 为 taskAffinity 新任务栈。

如果任务栈中已经有该 Activity 的实例，会重用该实例并调用实例的 onNewIntent()，重用时，会让该实例回到栈顶，并将改实例上面的其他 Activity 出栈。如果栈中不存在该实例，将会创建新实例并入栈。

适用场景：项目首页。

注意：会使 flagFLAG_ACTIVITY_RESET_TASK_IF_NEEDED 失效

例子：

目前任务栈 S1 中情况为 ABC，此时 Activity D 以 singleTask 请求启动，需要的任务栈 S2，由于 S2, D 均不存在，则系统先创建任务栈 S2，再创建 D 放入 S2 中。

若 D 需要的是 S1，其它情况同上述，则系统直接创建 D，放入栈 S1 中

若 D 需要的是 S1，且 S1 的情况 ADBC，此时 D 不会重新创建，而是移除 BC，D 即为栈顶，调用 onNewIntent

#### 1.4 singleInstance 单实例模式

系统不会在 singleInstance activity 的 task 栈中放入任何其他的 activity 实例，它单独位于一个 task 中。

singleInstance 不要用于中间页面，如果用于中间页面，跳转会有问题，比如：A -> B (singleInstance) -> C，由于 B 是自己一个任务栈，而 AC 默认情况下是同一个栈，这样回退的时候就是 C -> A -> B 可能会让用户懵逼。

应用场景：来电界面、Launcher 页面

### 2. flag 设置

通过 flag 方式设置启动模式优先级要高于 Manifest 中配置。

无法通过设置 flag 设置 singleInstance 模式。

FLAG_ACTIVITY_NEW_TASK : 如果 taskAffinity 一样则与标准模式一样新启动一个 Activity；如果不一样则新建一个 task 放该 Activity。

FLAG_ACTIVITY_SINGLE_TOP : 与 SingleTop 效果一致

FLAG_ACTIVITY_CLEAR_TOP : 销毁目标 Activity 和它之上的所有 Activity，重新创建目标 Activity，+ FLAG_ACTIVITY_SINGLE_TOP 效果与 SingleTask 效果一致

### 3. 设置 taskAffinity

启动模式为 singleTask 或 singleInstance 才能生效。

```html
android:taskAffinity="task.name" //值为字符串，必须包含 "."
```
不同的 TaskStack, Affinity 可以一样。

如果在 xml 设未设置 launchMode，只设置了 taskAffinity 则需要在 intent 时指定相关的 launchMode 的 flag。

```java
Intent intent = new Intent(aAvtivity.this, bActivity.class);
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
startActivity(intent);
```

与 allowTaskReparenting 混用

在应用 A 中启动了应用 B 的 ActivityC，若 allowTaskReparenting 为 true，则 C 会从 A 的栈中移到 B 的任务栈。此时从 home，打开应用 B 显示的不是主界面，而是 ActivityC

### 4. Ex：查看当前任务栈的 adb 命令 

```shell
adb shell dumpsys activity activities
```
