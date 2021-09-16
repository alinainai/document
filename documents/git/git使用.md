
#### Git用法
```shell
git branch 查看当前分支
```
```shell
git status 当前分支状态
```
```shell
git remote -v
```
```shell
git add . 添加修改文件
```
```shell
git rebase –continue
```
```shell
git reset --hard c8310ce08d5b66bd6e067541e035f70da505f57d
```
```shell
git commit -m “提交信息”
```
```shell
git diff --stat <上个版本tag名>
```
```shell
git cherry-pick
```
```shell
git push origin head
```
```shell
git stash -u
```
```shell
git stash pop
```

#### 查看log日志
```shell
git log 查看log日志
```
```shell
git reflog 包含被删除的commit记录
```

#### 比较两个版本差异
```shell
git diff branch1 branch2 --stat //显示出所有有差异的文件列表
git diff branch1 branch2 具体文件路径 //显示指定文件的详细差异
git diff branch1 branch2 //显示出所有有差异
```

```shell
git checkout -b release_6.9.3.0 origin/xxxxx
```
```shell
git pull origin feature_quick_publish_20200213 –rebase
```
```shell
git pull origin master –rebase
```
#### 切新分支
```shell
git checkout -b feature_xxxxx main/master
```
```shell
git push origin head
```
#### 删除本地分支
```shell
git branch –D feature_7.0_delete_camera_smart_home
```
#### 删除远程分支
```shell
git push origin -d feature_7.0_delete_camera_smart_home![image](https://user-images.githubusercontent.com/17560388/128830377-b62c4d16-21a6-4287-9877-a135ff259925.png)
```
