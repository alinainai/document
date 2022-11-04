## 一、简单介绍
## Migration 操作
### 1、修改数据库版本号
```kotlin
@Database(entities = {User.class}, version = 2)
public abstract class UsersDatabase extends RoomDatabase
```
### 2、创建Migration，1和2分别代表上一个版本和新的版本
```kotlin
static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(SupportSQLiteDatabase database) {
      //此处对于数据库中的所有更新都需要写下面的代码
        database.execSQL("ALTER TABLE users "
                + " ADD COLUMN last_update INTEGER");
    }
};
```
### 3、把migration 添加到 Room database builder
```kotlin
database = Room.databaseBuilder(context.getApplicationContext(),
        UsersDatabase.class, "Sample.db")
         //增加下面这一行
        .addMigrations(MIGRATION_1_2)
        .build();
```
说明：SQLite的ALTER TABLE命令非常局限，只支持重命名表以及添加新的字段。

####4、官方文档
https://developer.android.com/training/data-storage/room/migrating-db-versions.html

## 参考
