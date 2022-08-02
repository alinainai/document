## 一、从 Activity 出发 

先从持有 Window 对象的 Activity 开始，首先看下  Activity 的 dispatchTouchEvent 方法

### 1.1 先看下 Activity 中 dispatchTouchEvent 方法

```java
public boolean dispatchTouchEvent(MotionEvent ev) {   
    if (ev.getAction() == MotionEvent.ACTION_DOWN) {// 一般事件列开始都是 DOWN 事件，故此处基本是true
        onUserInteraction(); 
     }
    if (getWindow().superDispatchTouchEvent(ev)) {// 若 getWindow().superDispatchTouchEvent(ev) 的返回 true，不会回调 Activity 的 onTouchEvent 方法
        return true;
    }
    return onTouchEvent(ev);    
}
```

如果 getWindow().superDispatchTouchEvent(ev) 处理了事件，就不会回调 Activity 的 onTouchEvent 方法。我们继续跟进看下 Window 中的 superDispatchTouchEvent(ev) 方法

### 1.2 Window 中的 superDispatchTouchEvent(ev) 方法

PhoneWindow 是 Window 类的唯一子类。Activity 的成员变量 mWindow 就是 PhoneWindow 类型。

```java
public class PhoneWindow extends Window implements MenuBuilder.Callback {
    @Override
    public boolean superDispatchTouchEvent(MotionEvent event) {
        return mDecor.superDispatchTouchEvent(event); //mDecor（DecorView类型） 是顶层 View 的实例对象
    } 
}
```
### 1.3 DecorView 中 superDispatchTouchEvent(ev) 方法
```java
public class DecorView extends FrameLayout implements RootViewSurfaceTaker, WindowCallbacks
    public boolean superDispatchTouchEvent(MotionEvent event) {
	//直接调用父类（ViewGroup）的的dispatchTouchEvent()
        return super.dispatchTouchEvent(event);
    }
}
```
事件的传递流程 Activity -> Window -> DecorView

## 2. ViewGroup 的 dispatchTouchEvent

```java
@Override
public boolean dispatchTouchEvent(MotionEvent ev) {
	
    boolean handled = false; // 是否进行过操作的记录
    if (onFilterTouchEventForSecurity(ev)) {
        final int action = ev.getAction();
        final int actionMasked = action & MotionEvent.ACTION_MASK;

        if (actionMasked == MotionEvent.ACTION_DOWN) {     
            cancelAndClearTouchTargets(ev); // 清空TouchTargets
            resetTouchState(); // 重新设置Touch的状态，经过这两个方法 mFirstTouchTarget会被置为空
        } 
        final boolean intercepted; // 是否拦截
        if (actionMasked == MotionEvent.ACTION_DOWN || mFirstTouchTarget != null) {   
            // View 是否充许当前 ViewGroup 对事件拦截，默认为false（允许 ViewGroup 拦截）。View 通过重写 requestDisallowIntercept() 
            final boolean disallowIntercept = (mGroupFlags & FLAG_DISALLOW_INTERCEPT) != 0;            
            if (!disallowIntercept) { 
                intercepted = onInterceptTouchEvent(ev); // 获取 ViewGroup 的 onInterceptTouchEvent 的结果
                ev.setAction(action); // restore action in case it was changed
            } else {
                intercepted = false;
            }
        } else {
            intercepted = true;
        }
        // 检查事件是否取消
        final boolean canceled = resetCancelNextUpFlag(this) || actionMasked == MotionEvent.ACTION_CANCEL;
        // Update list of touch targets for pointer down, if needed. 多点触控的参数
        final boolean split = (mGroupFlags & FLAG_SPLIT_MOTION_EVENTS) != 0;
        TouchTarget newTouchTarget = null;
        boolean alreadyDispatchedToNewTouchTarget = false;
        if (!canceled && !intercepted) {
            if (actionMasked == MotionEvent.ACTION_DOWN
                    || (split && actionMasked == MotionEvent.ACTION_POINTER_DOWN)
                    || actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
                final int actionIndex = ev.getActionIndex(); // always 0 for down
                final int idBitsToAssign = split ? 1 << ev.getPointerId(actionIndex)
                        : TouchTarget.ALL_POINTER_IDS;
                final int childrenCount = mChildrenCount;
                if (newTouchTarget == null && childrenCount != 0) {//下面这一大坨代码是根据点击区域找到可以接收事件的View，并赋值给 mFirstTouchTarget。
                    final float x = ev.getX(actionIndex);
                    final float y = ev.getY(actionIndex);
                    // Find a child that can receive the event. Scan children from front to back. 
                    final ArrayList<View> preorderedList = buildTouchDispatchChildList();
                    final boolean customOrder = preorderedList == null
                            && isChildrenDrawingOrderEnabled();
                    final View[] children = mChildren;
                    for (int i = childrenCount - 1; i >= 0; i--) {
                        final int childIndex = getAndVerifyPreorderedIndex(
                                childrenCount, i, customOrder);
                        final View child = getAndVerifyPreorderedView(preorderedList, children, childIndex);
                        if (!canViewReceivePointerEvents(child)
                                || !isTransformedTouchPointInView(x, y, child, null)) {
                            ev.setTargetAccessibilityFocus(false);
                            continue;
                        }                                                             
                        if (dispatchTransformedTouchEvent(ev, false, child, idBitsToAssign)) {  //将事件传递给子 View    
                            newTouchTarget = addTouchTarget(child, idBitsToAssign);
                            alreadyDispatchedToNewTouchTarget = true;
                            break;
                        }                         
                    }   
                }
            }
        }
        if (mFirstTouchTarget == null) { // 如果没有子View消费事件，就自己消费掉
            handled = dispatchTransformedTouchEvent(ev, canceled, null, TouchTarget.ALL_POINTER_IDS);//注意这里的 null           
        } else {  // TouchTarget是手指和被摸到的子View的绑定，是一个链表结构
            TouchTarget predecessor = null;
            TouchTarget target = mFirstTouchTarget;
            while (target != null) {
                final TouchTarget next = target.next;
                if (alreadyDispatchedToNewTouchTarget && target == newTouchTarget) {
                    handled = true;
                } else {
                    final boolean cancelChild = resetCancelNextUpFlag(target.child)
                            || intercepted;
                    if (dispatchTransformedTouchEvent(ev, cancelChild,
                            target.child, target.pointerIdBits)) {//如果 ViewGroup intercepted 改为 true，给子View 发送 cancel 事件。
                        handled = true;
                    }
                    if (cancelChild) { 
                        if (predecessor == null) {
                            mFirstTouchTarget = next;
                        } else {
                            predecessor.next = next;
                        }
                        target.recycle();
                        target = next;
                        continue;
                    }
                }
                predecessor = target;
                target = next;
            }
        }  
    }
    return handled;
}
```

1.touch 事件是如何从驱动层传递给 Framework 层的 InputManagerService；

2.WMS 是如何通过 ViewRootImpl 将事件传递到目标窗口；

3.touch 事件到达 DecorView 后，是如何一步步传递到内部的子 View 中的。


