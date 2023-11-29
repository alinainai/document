
<img width="600" alt="git 分区" src="https://user-images.githubusercontent.com/17560388/185092541-b3ff0020-9c93-41ef-bfe7-14afabb135d7.png">

- Workspace：工作区
- Index / Stage：暂存区
- Repository：仓库区（或本地仓库）
- Remote：远程仓库


### 一、初始化
```shell
# 下载一个项目和它的整个代码历史
$ git clone [url]
```
### 二、配置
```shell
# 显示当前的Git配置
$ git config --list
```
```shell
# 编辑Git配置文件
$ git config -e [--global]
```
```shell
# 设置提交代码时的用户信息
$ git config [--global] user.name "[name]"
$ git config [--global] user.email "[email address]"
```
### 三、增加/删除文件
```shell
# 添加指定文件到暂存区
$ git add [file1] [file2] ...
```
```shell
# 添加指定目录到暂存区，包括子目录
$ git add [dir]
```
```shell
# 添加当前目录的所有文件到暂存区
$ git add .
```
```shell
# 删除工作区文件，并且将这次删除放入暂存区
$ git rm [file1] [file2] ...
```
```shell
# 停止追踪指定文件，但该文件会保留在工作区
$ git rm --cached [file]
```
```shell
# 改名文件，并且将这个改名放入暂存区
$ git mv [file-original] [file-renamed]
```
### 四、代码提交
```shell
# 提交暂存区到仓库区
$ git commit -m [message]
```
```shell
# 提交暂存区的指定文件到仓库区
$ git commit [file1] [file2] ... -m [message]
```
```shell
# 提交工作区自上次commit之后的变化，直接到仓库区
$ git commit -a
```
```shell
# 提交时显示所有diff信息
$ git commit -v
```
```shell
# 使用一次新的commit，替代上一次提交
# 如果代码没有任何新变化，则用来改写上一次commit的提交信息
$ git commit --amend -m [message]
```
```shell
# 重做上一次commit，并包括指定文件的新变化
$ git commit --amend [file1] [file2] ...
```

### 五、分支
```shell
# 列出所有本地分支
$ git branch
```
```shell
# 列出所有远程分支
$ git branch -r
```
```shell
# 列出所有本地分支和远程分支
$ git branch -a
```
```shell
# 新建一个分支，但依然停留在当前分支
$ git branch [branch-name]
```
```shell
# 新建一个分支，并切换到该分支
$ git checkout -b [branch]
```
```shell
# 新建一个分支，指向指定commit
$ git branch [branch] [commit]
```
```shell
# 新建一个分支，与指定的远程分支建立追踪关系
$ git branch --track [branch] [remote-branch]
```
```shell
# 切换到指定分支，并更新工作区
$ git checkout [branch-name]
```
```shell
# 切换到上一个分支
$ git checkout -
```
```shell
# 建立追踪关系，在现有分支与指定的远程分支之间
$ git branch --set-upstream [branch] [remote-branch]
```
```shell
# 合并指定分支到当前分支
$ git merge [branch]
```
```shell
# 选择一个commit，合并进当前分支
$ git cherry-pick [commit]
```
```shell
# 删除分支
$ git branch -d [branch-name]
```
```shell
# 删除远程分支
$ git push origin --delete [branch-name]
$ git branch -dr [remote/branch]
```

### 六、标签
```shell
# 列出所有tag
$ git tag
```
```shell
# 新建一个tag在当前commit
$ git tag [tag]
```
```shell
# 新建一个tag在指定commit
$ git tag [tag] [commit]
```
```shell
# 删除本地tag
$ git tag -d [tag]
```
```shell
# 删除远程tag
$ git push origin :refs/tags/[tagName]
```
```shell
# 查看tag信息
$ git show [tag]
```
```shell
# 提交指定tag
$ git push [remote] [tag]
```
```shell
# 提交所有tag
$ git push [remote] --tags
```
```shell
# 新建一个分支，指向某个tag
$ git checkout -b [branch] [tag]
```

### 七、查看信息
```shell
# 显示有变更的文件
$ git status
```
```shell
# 显示当前分支的版本历史
$ git log
```
```shell
# 显示commit历史，以及每次commit发生变更的文件
$ git log --stat
```
```shell
# 搜索提交历史，根据关键词
$ git log -S [keyword]
```
```shell
# 显示某个commit之后的所有变动，每个commit占据一行
$ git log [tag] HEAD --pretty=format:%s
```
```shell
# 显示某个commit之后的所有变动，其"提交说明"必须符合搜索条件
$ git log [tag] HEAD --grep feature
```
```shell
# 显示某个文件的版本历史，包括文件改名
$ git log --follow [file]
$ git whatchanged [file]
```
```shell
# 显示指定文件相关的每一次diff
$ git log -p [file]
```
```shell
# 显示过去5次提交
$ git log -5 --pretty --oneline
```
```shell
# 显示所有提交过的用户，按提交次数排序
$ git shortlog -sn
```
```shell
# 简单commit 的 hash 和 提交信息
 git log --pretty=format:"%h %s"
```

```shell
# 显示指定文件是什么人在什么时间修改过
$ git blame [file]
```
```shell
# 显示暂存区和工作区的差异
$ git diff
```
```shell
# 显示暂存区和上一个commit的差异
$ git diff --cached [file]
```
```shell
# 显示工作区与当前分支最新commit之间的差异
$ git diff HEAD
```
```shell
# 显示两次提交之间的差异
$ git diff [first-branch]...[second-branch]
```
```shell
# 显示今天你写了多少行代码
$ git diff --shortstat "@{0 day ago}"
```
```shell
# 显示某次提交的元数据和内容变化
$ git show [commit]
```
```shell
# 显示某次提交发生变化的文件
$ git show --name-only [commit]
```
```shell
# 显示某次提交时，某个文件的内容
$ git show [commit]:[filename]
```
```shell
# 显示当前分支的最近几次提交
$ git reflog
```

### 八、远程同步
```shell
# 下载远程仓库的所有变动
$ git fetch [remote]
```
```shell
# 显示所有远程仓库
$ git remote -v
```
```shell
# 显示某个远程仓库的信息
$ git remote show [remote]
```
```shell
# 增加一个新的远程仓库，并命名
$ git remote add [shortname] [url]
```
```shell
# 取回远程仓库的变化，并与本地分支合并
$ git pull [remote] [branch]
```
```shell
# 上传本地指定分支到远程仓库
$ git push [remote] [branch]
```
```shell
# 强行推送当前分支到远程仓库，即使有冲突
$ git push [remote] --force
```
```shell
# 推送所有分支到远程仓库
$ git push [remote] --all
```

### 九、撤销
```shell
# 恢复暂存区的指定文件到工作区
$ git checkout [file]
```
```shell
# 恢复某个commit的指定文件到暂存区和工作区
$ git checkout [commit] [file]
```
```shell
# 恢复暂存区的所有文件到工作区
$ git checkout .
```
```shell
# 重置暂存区的指定文件，与上一次commit保持一致，但工作区不变
$ git reset [file]
```
```shell
# 重置暂存区与工作区，与上一次commit保持一致
$ git reset --hard
```
```shell
# 重置当前分支的指针为指定commit，同时重置暂存区，但工作区不变
$ git reset [commit]
```
```shell
# 重置当前分支的HEAD为指定commit，同时重置暂存区和工作区，与指定commit一致
$ git reset --hard [commit]
```
```shell
# 重置当前HEAD为指定commit，但保持暂存区和工作区不变
$ git reset --keep [commit]
```
```shell
# 新建一个commit，用来撤销指定commit
# 后者的所有变化都将被前者抵消，并且应用到当前分支
$ git revert [commit]
```
```shell
# 暂时将未提交的变化移除，稍后再移入
$ git stash
$ git stash pop
```

### 其他

```shell
git rebase –continue
```
```shell
git reset --hard c8310ce08d5b66bd6e067541e035f70da505f57d
```
```shell
git push origin head
```
```shell
git stash -u
```

```shell
git reflog //包含被删除的commit记录
```


```shell
# 比较两个版本差异
git diff branch1 branch2 --stat //显示出所有有差异的文件列表
git diff branch1 branch2 [具体文件路径] //显示指定文件的详细差异
```

```shell
git pull origin feature_quick_publish_20200213 –rebase
```
```shell
git pull origin master –rebase
```
```shell
git push origin head
```

