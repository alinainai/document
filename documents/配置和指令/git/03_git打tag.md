## 分支处理

### 1.首先切换到当前版本分支，以release_7.0.0.0为例
```shell
git checkout  release_7.0.0.0
```
### 2.查看当前分支状态，看看分支是否是已切换
```shell
git status
```
### 3.拉取对应的远端分支代码，本地分支同步远端分支
```shell
git pull origin release_7.0.0.0
```
### 4.和上一个版本tag做比较，生成差异文件列表，保存到 diff.txt （可以自己命名）
```shell
git diff --stat v6.9.9.0 >diff.txt
```
### 5.将diff文件粘贴到项目根目录的update.txt

全选 diff.txt 中的文本复制到 update.txt

并修改 对内功能说明 和 对外功能说明 使用git status查看状态
```shell
git status
```
删除diff文件
```shell
rm diff.txt
```
```shell
git status查看文件是否删除。
```
### 6.确认后提交 update.txt
```shell
git add .
git commit -m "docs: tag v7.0.0.0"
git push origin head
```
### 7.提交版本tag
```shell
git tag v7.0.0.0
```
查看仓库tag分支
```shell
git tag 
```
拉取remote仓库tag
```shell
git pull origin --tags
```
查看tag是否有冲突，用冲突解决，没有冲突直接提交
```shell
git status
```
查看仓库tag分支
```shell
git push origin --tags
```
在git仓库中查看tag分支是否提交

## 删除tag

删除本地tag 
```shell
 git tag -d <tag name>
```
删除远端tag 
```shell
git push --delete origin <tag name>
```

## 合并代码到master
在当前分支拉取master代码，并查看是否有冲突
```shell
git pull origin master
```
如有有提示信息需要填入commit的原因
输入 :wq 退出即可

git status

如果有冲突先把冲突解决，如果有不确定的代码可以找相关coder解决

然后提交merge request
