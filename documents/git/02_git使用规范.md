## ⼀、规则

### 1. `commit`独立性

说明：基于任意commit为节点，进行的一次编译都能够成功，也就是说，每次 commit 都只依赖于之前的提交，且能够成功编译。

### 2. `commit` 原子性

与写代码类似，每一个 commit 只做一件事，这样更加清晰，提交信息也可以更加简洁、美观；别人看提交信息的时候，就能够一眼知道代码实现的思路和过程。


## 二、格式


### (一）`commit`的格式

每次提交，`Commit message` 都包括三个部分：`header`，`body` 和 `footer`。

```
<type>[<scope>]: <subject>
<blank line>
[<body>]
<blank line>
[<footer>]
```

**其中，`header` 是必需的，`body` 和 `footer` 可以省略。**

不管是哪一个部分，任何一行都不得超过72个字符（或100个字符）。这是为了避免自动换行影响美观。

### 说明：

#### 1. **Header**

Header部分只有一行，包括三个字段：`type（必需）`、`scope（可选）` 和 `subject（必需）`

#### 2. **type** 

用于说明 `commit` 的类别，只允许使用下面9个标识。：

    - feat : 新功能（feature）
    - fix : 修复bug (如果是奇效上的bug，建议贴上bug链接或者bugID)
    - docs : 文档（documentation），比如 README ,  ignore 等等
    - style : 格式，如修改了空格、格式缩进、逗号、删除无用的代码/导包等等，不改变代码逻辑
    - refactor : 重构（即不是新增功能，也不是修改bug的代码变动）
    - perf : 优化相关，比如提升性能、体验
    - test : 测试⽤用例例，包括单元测试、集成测试等
    - chore : 改变构建流程、或者增加依赖库、gradle工具等
    - revert : 回滚到上一个版本

#### 3. **scope** 

⽤于说明 `commit` 影响的范围，比如数据层、控制层、视图层等等，视项目不同⽽不同

#### 4. `subject` 

是 **commit** 目的，要求简短描述，不超过50个字符，**以动词开头**

#### 5. `body`

部分是对本次 **commit** 的详细描述，可以分成多行，应该说明代码变动的动机，以及与以前行为的对⽐

#### 6. `Footer`

Footer 部分只用于以下两种情况：

- 不兼容变动: 如果当前代码与上一个版本不兼容，则 Footer 部分以BREAKING CHANGE开头，后面是对变动的描述、以及变动理由和迁移方法。
- 关闭 Issue: 如果当前 commit 针对某个issue，那么可以在 Footer 部分关闭这个 issue 

要求：

```
# 标题行：50个字符以内，描述主要变更内容
#
# 主体内容：更详细的说明文本，建议72个字符以内。需要描述的信息包括:
#
# * 为什么这个变更更是必须的? 它可能是用来修复一个 bug ，增加一个 feature ，提升性能、可靠性、稳定性等等
# * 如何解决这个问题? 具体描述解决问题的步骤
# * 是否存在副作用、风险?
#
# 尾部：如果需要的话可以添加一个链接到 issue 地址或者其它文档，或者关闭某个 issue 。
```

### （二）revert格式
```
revert: <需要回滚的 commit 的 header 信息>

This reverts commit < commit 号>
```

### (三) 案例分析

标准的 `commit`:

```
feat: 增加列表排序页面
```

```
feat: 修改首页底部tab UI样式

这是一个临时需求，因为要赶节日时间临时加上，下个版本会上线换肤功能，支持切换首页tab。到时此commit可以直接revert掉。
```

```
style: 调整代码格式
```

```
test: 增加xUtils单元测试
```

```
fix: 修复排序页面看不到网关设备的bug

是由于recyclerView最后一条item没有显示出来，
根本原因是recyclerView的父布局是ConstraintLayout，
没有给recyclerView设置 app:layout_constraintBottom_toBottomOf="parent" 属性

close #1193
```

差评的 `commit`:

```
bug fix
```

```
ui调整
```

```
按产品要求调整
```


### （四）`git` 常用小技巧

笔者整理了一些常用的 `git` 命令，让大家更容易的遵循上面的 `commit` 规范

- #### 1.拉取远端代码时执行 rebase
推代码到远端时，git 提示有冲突，需要先 `pull` 一下远端的代码。
再
```
    // 常规操作
    git pull <remote> <branch>
    
    // 进阶操作
    // 自动把远端新提交的commit排列到本地还未推送到远端的commit之前
    //（遵循时间线，节省一个 merge 的 commit）
    git pull <remote> <branch> --rebase
```


- #### 2.调整`commit`顺序
一个功能提交了2个`commit` A、B后，提交了另一个不相关`commit` C，突然这时又追加了一个 和第一个功能相关的 `commit` D。

现在的`git log`顺序是,A B C D，其中ABD是相关的，需要调整顺序，然后ABD 3个commit一起提一个Merge Request。

```
    git rebase -i <A前一个commit号>
```

然后使用`vim`命令，dd,p 来调整commit的顺序，最后 :wq 保存退出

- #### 3.推本地代码到远程且指定截止的commit
本地提交了很多commit，不想一次全都推到远端，拆分成几次来Merge Request
```
    git push origin  <commit号>:<remote名>
```

- #### 4.快速新建分支同时切到新分支
```
    git checkout -b <新的分支名> <基准remote>/<基准分支名>
```

- #### 5.删除分支
```
    // 删除本地分支
    git branch -D <本地分支名>
    
    // 删除远端分支
    git push origin -d <远端分之名>
```

- #### 6.合并 `commit`

这个分为两种情形

当前改动需要追加到前一个 `commit`
    
```
    git commit --amend -m "<合并后的commit名>"
```

合并之前的多个commit

```
    git rebase -i <要合并的多个commit之前一个commit号>
```
然后把需要合并的commit 前面 的 `pick` 改为 `squash`，:wq 保存退出后按照提示重新修改 `commit` 名称

- #### 7.将所有未提交的修改（工作区和暂存区）保存至堆栈中
开发时经常遇到这种情况，写了一半代码，突然要去做别的事(查问题，紧急加功能, 给领导演示等)，但是当前的代码只写了一半，不能提`commit`又不好，又无法把所有改动丢掉。

这时候需要用到 `git stash`，能够将所有未提交的修改（工作区和暂存区）保存至堆栈中，用于后续恢复当前工作目录。
```
    // 缓存当前修改
    // 加了 -u 是包含所有新增的文件
    git stash -u
    
    // 将当前stash栈顶的内容弹出，并应用到当前分支对应的工作目录上。
    git stash pop
    
    // 将堆栈中的内容应用到当前目录，不同于git stash pop，该命令不会将内容从堆栈中删除
    git stash apply
    
    // 查看stah 列表
    git stash list
    
    // 弹出指定的 stash
    git stash pop <stash号>
    
    git stash apply <stash号>
```

- #### 8.在两个未合并的分支上同步代码
在不同的分支开发一个功能的不同模块，在自己分支有用到同事分支的一些实现。

```
    git cherry-pick <commit号>
```
