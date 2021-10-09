### 流程

- 1.创建分支
- 2.功能开发
- 3.提交 `Merge Request`
- 4.组内成员 `Code Review`
- 5.合并分支

整个流程大致遵循github合并规范，如下图

<img src="https://user-images.githubusercontent.com/17560388/136642682-af12652c-0aa5-4d13-88e4-d6e80a097167.png" alt="github合并规范" width="600"  align="bottom" />

#### 1.创建分支
分支分为`master`、`feature`、`release` 三类类
- `master`分支: 唯一主分支，与当前线上最稳定版本保持同步，**禁止在该分支直接提交代码，只允许通过Merge Request的方式合并其它已经通过测试的分支**
- `feature`分支: 功能开发分支，用于开发新的需求，以 `feature_功能` 形式命名。 开发完成后，向 `release` 分支 发起  Merge Request 合并代码。
- `release`分支: 版本发布分支，对应一个将要发布的版本，以 `release_版本号` 形式命名。测试通过后发版后， 向 `master` 分支 发起  Merge Request 合并代码。

项目分支演进流程图如下：

<img src="https://user-images.githubusercontent.com/17560388/136642746-47ddfaf3-9582-41d7-a59c-f5548721ac17.png" alt="项目分支演进流程图" width="800"  align="bottom" />



#### 2.功能开发
开发过程中，每一个 `commit` 需保持独立性，原子性。

> 具体请查看 [git commit规范]()

#### 3.提交 `Merge Request`
创建 `Merge Request`, 可以在开发的任何过程中提交 `Merge Request`。

(建议尽早提交MR，可以留给其他人更多的review时间) 

未完成开发需要 添加 "WIP" 标记 (Working In Progress)。

开发完成后，移除 "WIP"，否者该 `Merge Request` 将不会被允许合并。

#### 4.组内成员 `Code Review`

`Code Review`包含

- 1). 组内成员进行 `Code Review`，并对有异议的地方添加 `Discussion`。主要针对以下几点:
    - 命名
    - typo
    - style (格式，缩进，无用导包、代码 等)
    - 代码有无明显漏洞 (空指针，内存泄漏等)
    - 代码是否有可以优化的地方 (内存，性能等)
    - 代码是否有更优雅的实现方式
    - 复杂逻辑处，注释是否够明确
    - 代码`commit` 是否符合 `git commit`规范
    - 包括但不限于以上
    
- 2). 开发者 `或阐述自己坚持己见的原因`，`或按照建议调整相应代码，重新提交`。 同时修改状态 `Resolve Discussion`，等候其他人员再次进行代码 `Review` 。

以上两个过程可能会 **反复执行**，直到所有 `Discussion` 均已经 `Resolved` ，同时组内人员对需要 Merge 的代码再无任何异议为止。

Review 后的同事需要 `点赞` 或 `在下方评论 "LGTM" (look good to me)` 来表示自己的认可。

#### 5.合并分支
执行完 **Code Review**（理论上至少一人进行过Review） 且不在 "WIP" 状态的Merge Request，便可以合并进目标分支。
