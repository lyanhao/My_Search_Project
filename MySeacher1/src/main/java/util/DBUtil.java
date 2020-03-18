package util;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import task.DBInit;

import javax.sql.DataSource;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBUtil {

    private static volatile DataSource DATA_SOURCE;

    /**
     * 提供获取数据库连接池的功能：
     * 使用单例模式（多线程 安全版本）
     * <p>
     * 问题：（回顾多线程安全版本的单例模式）
     * 1.为什么在外层判断是否等于null？                    答：目的是为了提高效率
     * 2.synchronized加锁以后，为什么还要判断等于null？    答：确保单例
     * 3.为什么DataSource类变量要使用volatile关键字修饰？  答：volatile能够禁止指令重排序，建立内存屏障
     * <p>
     * 多线程操作：原子性，可见性（主内存拷贝到工作内存），有序性
     * synchronized 保证三个特性，volatile 保证可见性，有序性
     *
     * @return
     */
    private static DataSource getDataSource() {
        //双重校验锁
        if (DATA_SOURCE == null) {  //提高效率
            //刚开始所有进入这行代码的线程，DATA_SOURCE对象都是null
            //可能是第一个进去的线程，也可能是第一个线程之后的线程进入并执行
            synchronized (DBUtil.class) {   //
                if (DATA_SOURCE == null) {
                    //初始化操作
                    SQLiteConfig config = new SQLiteConfig();
                    config.setDateStringFormat(Util.DATE_PATTERN);
                    DATA_SOURCE = new SQLiteDataSource(config);
                    ((SQLiteDataSource) DATA_SOURCE).setUrl(getUrl());
                }
            }
        }
        return DATA_SOURCE;
    }

    /**
     * 获取sqlite数据库文件url的方法
     *
     * @return
     */
    private static String getUrl() {
        //获取target编译文件夹的路径
        //通过classLoader.getResource() / classLoader.getResourceAsStream()
        //默认的根路径为编译文件夹路径(target / classes)
        URL classesURL = DBInit.class.getClassLoader().getResource("./");
        //获取target/classes文件夹的父目录路径
        String dir = new File(classesURL.getPath()).getParent();
        String url = "jdbc:sqlite://" + dir + File.separator + "everything-like.db";
        //new SqliteDateSource()，把这个对象的url设置进去，才会创建这个文件。
        //如果文件已经存在，就会读取文件。
        //url = URLDecoder.decode(url,"UTF-8");
        System.out.println("获取数据库文件路径：" + url);
        return url;
    }

    /**
     * 提供获取数据库连接的方法：
     * 从数据库连接池DataSource.getConnection()来获取数据库连接
     *
     * @return
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static void main(String[] args) throws SQLException {
        System.out.println(getConnection());
    }

    public static void close(Connection connection, Statement statement) {
        close(connection, statement, null);
    }

    /**
     * 释放数据库资源：
     *
     * @param connection 数据库连接
     * @param statement  sql执行对象
     * @param resultSet  结果集
     */
    public static void close(Connection connection, Statement statement, ResultSet resultSet) {
        try {
            if (connection != null) {
                connection.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("释放数据库资源错误", e);
        }
    }
}
