

- 1. 对于Serializable，类只需要实现 `Serializable` 接口，并提供一个序列化版本 `id(serialVersionUID)` 即可。

- 2.而Parcelable则需要实现 `writeToParcel`、`describeContents` 函数以及静态的 `CREATOR` 变量**，实际上就是将如何打包和解包的工作自己来定义，而序列化的这些操作完全由底层实现。

```java

public class User implements Parcelable{

    // 当前实体类的三个属性
    public int userId;
    public String userName;
    public boolean isMale;
    // 另外的实体类对象
    public Book book;

    // 添加一个有参的构造方法
    public User(int userId, String userName, boolean isMale) {
        super();
        this.userId = userId;
        this.userName = userName;
        this.isMale = isMale;
    }

    /**
     * 内容描述功能的实现，如果含有文件描述符，返回 1。
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * 序列化功能实现，flags 为1时标识当前对象需要作为返回值返回，基本不会是1。
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // TODO Auto-generated method stub
        dest.writeInt(userId);
        dest.writeString(userName);
        dest.writeByte((byte)(isMale?1:0)); 
        dest.writeParcelable(book, 0);
    }

    /**
     * 反序列化功能的实现
     */
    public static final Parcelable.Creator<User> CREATOR = new Creator<User>() {

        // 创建指定长度的原始对象数组
        @Override
        public User[] newArray(int size) {
            return new User[size];
        }

        // 从序列化后的对象中创建原始对象
        @Override
        public User createFromParcel(Parcel source) {
            return new User(source);
        }
    };

    private User(Parcel source){
        userId = source.readInt();
        userName = source.readString();
        isMale = source.readByte()!= 0;
        // 由于book是一个可序列化的对象，所以它的反序列化过程需要传递当前线程的上下文加载器，否则会报无法找到类的错误。
        book = source.readParcelable(Thread.currentThread().getContextClassLoader());
    }

}

```


