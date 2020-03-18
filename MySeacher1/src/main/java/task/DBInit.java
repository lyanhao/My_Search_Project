package task;

import util.DBUtil;

import java.io.*;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;

/**
 * 1.初始化数据库：数据库文件放在target/everything-like
 * 调用DBUtil.getConnection()完成数据库初始化
 * 2.读取sql文件
 * 3.执行sql语句来初始化表
 */
public class DBInit {

    public static String[] readSQL() {
        try {
            //通过ClassLoader获取流，或者通过FileInputStream获取字节流
            InputStream is = DBInit.class.getClassLoader().getResourceAsStream("init.sql");
            //字节流转换为字符流：需要通过字节字符转换流操作
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("--")) {  //去掉 -- 注释的代码
                    line = line.substring(0, line.indexOf("--"));
                }
                sb.append(line);
            }
            String[] sqls = sb.toString().split(";");
            return sqls;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("读取sql文件错误", e);
        }
    }

    public static void Init() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DBUtil.getConnection();
            statement = connection.createStatement();
            String[] sqls = readSQL();
            for (String sql : sqls) {
                System.out.println("执行sql操作：" + sql);
                statement.executeUpdate(sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("初始化数据库表操作失败", e);
        } finally {
            DBUtil.close(connection, statement);
        }
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(readSQL()));
        Init();
    }
}
