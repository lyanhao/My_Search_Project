package task;

import app.FileMeta;
import util.DBUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FileSearch {

    public static List<FileMeta> search(String dir, String content) {
        List<FileMeta> metas = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "select name, path, is_directory, size, last_modified" +
                    " from file_meta where" +
                    " path=? or path like ?";
            if (content != null && content.trim().length() != 0) {
                sql += " and (name like ? or pinyin like ? or pinyin_first like ?)";
            }
            preparedStatement = connection.prepareStatement(sql);
            //占位符设值
            preparedStatement.setString(1, dir);    //匹配子文件和子文件夹
            preparedStatement.setString(2, dir + File.separator + "%"); //匹配孙子辈
            if (connection != null && content.trim().length() != 0) {
                preparedStatement.setString(3, "%" + content + "%");
                preparedStatement.setString(4, "%" + content + "%");
                preparedStatement.setString(5, "%" + content + "%");
            }
            //执行sql语句
            resultSet = preparedStatement.executeQuery();
            //处理结果集
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String path = resultSet.getString("path");
                Boolean isDirectory = resultSet.getBoolean("is_directory");
                Long size = resultSet.getLong("size");
                Timestamp lastModified = resultSet.getTimestamp("last_modified");
                FileMeta meta = new FileMeta(name, path, isDirectory, size,
                        new java.util.Date(lastModified.getTime()));
                metas.add(meta);
            }
        } catch (Exception e) {
            throw new RuntimeException("数据库文件查询失败，路径：" + dir + "，搜索内容：" + content, e);
        } finally {
            DBUtil.close(connection, preparedStatement, resultSet);
        }
        return metas;
    }

}
