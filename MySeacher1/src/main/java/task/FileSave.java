package task;

import app.FileMeta;
import util.DBUtil;
import util.Util;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileSave implements ScanCallBack {
    @Override
    public void callBack(File dir) {
        //文件夹下一级子文件和子文件夹保存到数据库
        //获取本地目录下一级子文件和子文件夹
        //集合框架中使用自定义类型，判断是否某个对象在集合存在：比对两个集合中的元素
        File[] children = dir.listFiles();
        List<FileMeta> locals = new ArrayList<>();
        if (children != null) {
            for (File child : children) {
                locals.add(new FileMeta(child));
            }
        }
        //获取数据库保存的dir目录的下一级子文件和子文件夹(select)
        List<FileMeta> metas = query(dir);

        //数据库有，本地没有，做删除(delete)
        for (FileMeta meta : metas) {
            if (!locals.contains(meta)) {
                delete(meta);
            }
        }
        //本地有，数据库没有，做插入(insert)
        for (FileMeta meta : locals) {
            if (!metas.contains(meta)) {
                save(meta);
            }
        }
    }

    private void delete(FileMeta meta) {
        //meta的删除：
        //1.删除meta信息本身
        //2.如果meta是目录，还要将meta所有的子文件，子文件夹都删除
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "delete from file_meta where" +
                    " (name=? and path=? and is_directory=?)"; //删除文件自身
            //如果是文件夹，还要删除文件夹的子文件和子文件夹
            if (meta.getDirectory()) {
                sql += " or path=?"   //匹配数据库文件夹的儿子辈
                        + " or path like ?";    //匹配数据库文件夹的孙后辈
            }
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, meta.getName());
            preparedStatement.setString(2, meta.getPath());
            preparedStatement.setBoolean(3, meta.getDirectory());
            if (meta.getDirectory()) {
                preparedStatement.setString(4, meta.getPath() +
                        File.separator + meta.getName());
                System.out.println(sql);
                preparedStatement.setString(5, meta.getPath() +
                        File.separator + meta.getName() + File.separator);
                System.out.println(sql);
            }
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("删除文件信息出错", e);
        } finally {
            DBUtil.close(connection, preparedStatement);
        }
    }

    /**
     * 查询文件信息
     *
     * @param dir
     * @return
     */
    private List<FileMeta> query(File dir) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<FileMeta> metas = new ArrayList<>();
        try {
            //1.创建数据库连接
            connection = DBUtil.getConnection();
            String sql = "select name, path, is_directory, size, last_modified" +
                    " from file_meta where path=?";
            //2.创建JDBC操作命令对象
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, dir.getPath());
            //3.执行sql语句
            resultSet = preparedStatement.executeQuery();
            //4.处理结果集
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String path = resultSet.getString("path");
                Boolean isDirectory = resultSet.getBoolean("is_directory");
                Long size = resultSet.getLong("size");
                Timestamp lastModified = resultSet.getTimestamp("last_modified");
                FileMeta meta = new FileMeta(name, path, isDirectory, size, new java.util.Date(lastModified.getTime()));
                System.out.printf("查询文件信息：name=%s, path=%s, is_directory=%s, size=%s, last_modified=%s\n",
                        name, path, String.valueOf(isDirectory), String.valueOf(size), Util.parseDate(new java.util.Date(lastModified.getTime())));
                metas.add(meta);
            }
            return metas;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("查询文件信息出错", e);
        } finally {
            DBUtil.close(connection, preparedStatement, resultSet);
        }
    }

    /**
     * 文件信息保存到数据库
     *
     * @param meta
     */
    private void save(FileMeta meta) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            //1.获取数据库连接
            connection = DBUtil.getConnection();
            String sql = "insert into file_meta" +
                    "(name,path,is_directory,size,last_modified,pinyin,pinyin_first)" +
                    " values (?, ?, ?, ?, ?, ?, ?)";
            //2.获取sql操作命令对象
            statement = connection.prepareStatement(sql);
            statement.setString(1, meta.getName());
            statement.setString(2, meta.getPath());
            statement.setBoolean(3, meta.getDirectory());
            statement.setLong(4, meta.getSize());
            //数据库保存日期类型，可以按使用数据库设置的日期格式，以字符串传入
            statement.setString(5, meta.getLastModifiedText());
            statement.setString(6, meta.getPinyin());
            statement.setString(7, meta.getPinyinFirst());

            System.out.println("执行文件保存操作：" + sql);
            //3.执行sql
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("文件保存失败，请检查sql语句", e);
        } finally {
            //4.释放资源
            DBUtil.close(connection, statement);
        }
    }
}
