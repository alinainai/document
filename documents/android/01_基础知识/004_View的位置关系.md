## 1、top、left、bottom、right 

`top`、`left`、`bottom`、`right` 这几个坐标是 `View` 与其所在的父容器的相对位置

## 2、x、y、translationX、translationY

`API 11（Android3.0）`之后，又增加了 `x`、`y`、`translationX`、`translationY`。

## 3、二者的关系

View 在平移的过程中，`top` 和 `left` 表示的是原始左上角的位置信息，其值不会发生改变。

发生改变的是`x、y、translationX、translationY`。

`x、y` 是 `View` 左上角相对于父布局的坐标，`translationX、translationY` 是平移的距离。关系如下

```java
x = left + translationX
y = top + translationY
```
## 4、验证

### 4.1 先看一种改变 `MarginLayoutParams` 的情况

```java
ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) button.getLayoutParams();
params.leftMargin +=300;
button.requestLayout();
```
采用上面的方式，`translationX` 和 `translationY` 依旧是 0，但是 `left` 会发生变化，这说明直接上面的做法是直接改变了 `view` 的位置，并非是平移

### 4.2 再看下使用属性动画改变 translationX 的结果

```java
ObjectAnimator.ofFloat(button, "translationX", 0, 100).setDuration(200).start();
```
`translationX` 平移了100，`x` 的值从 30 变为了 130，而原来的 `top, left, bottom, right` 坐标并没有变化。




