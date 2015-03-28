package com.leekyoungil.cachemem.util;

import com.leekyoungil.cachemem.model.CacheMemLog;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

/**
 * Created by kyoungil_lee on 10/6/14.
 */
public class CacheMemLogger {
    private volatile static CacheMemLogger instance = null;

    public volatile static Connection mysqlConn = null;

    private CacheMemLogger() { }

    public static CacheMemLogger getInstance () {
        if (instance == null) {
            synchronized (CacheMemLogger.class) {
                if (instance == null) {
                    instance = new CacheMemLogger();
                }
            }
        }

        return instance;
    }

    public void connMysql() {
        if (CacheMemLogger.mysqlConn == null) {
            //MySQL 접속
            try {
                String mysqlHost = "jdbc:mysql://127.0.0.1:3306/";
                String mysqlUser = "test";
                String mysqlPass = "test_password";
                String mysqlDb = "cachemem_log";

                Class.forName("com.mysql.jdbc.Driver").newInstance();
                CacheMemLogger.mysqlConn = DriverManager.getConnection(mysqlHost + mysqlDb + "?autoReconnect=true&failOverReadOnly=false&maxReconnects=10&useUnicode=true&characterEncoding=utf8&useServerPrepStmts=false&characterSetResults=utf8&zeroDateTimeBehavior=convertToNull", mysqlUser, mysqlPass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                closeConn();
            } catch (SQLException e) {
                e.printStackTrace();
                closeConn();
            } catch (InstantiationException e) {
                e.printStackTrace();
                closeConn();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                closeConn();
            }
        }
    }

    public boolean insertLog (CacheMemLog cacheMemLog) {
        connMysql();

        if (CacheMemLogger.mysqlConn == null) {
            return false;
        }

        String tableName = "flashdb_set_log";

        try {
            byte[] objectData = (byte[]) cacheMemLog.getObjectData();

            if (cacheMemLog.getObjectData() == null || objectData.length < 10) {
                tableName = "flashdb_set_log_error";
            }

            PreparedStatement statement = CacheMemLogger.mysqlConn.prepareStatement("INSERT INTO "+tableName+" (sitename, itemname, originKey, md5key, object, settime, ttl, ttl_m, ipaddress, objectSize) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            statement.setString(1, cacheMemLog.getSiteName());
            statement.setString(2, cacheMemLog.getItemName());
            statement.setString(3, cacheMemLog.getOriginKey());
            statement.setString(4, cacheMemLog.getKey());
            statement.setBytes(5, objectData);
            statement.setInt(6, cacheMemLog.getSetTime());
            statement.setInt(7, cacheMemLog.getTtl());
            statement.setInt(8, cacheMemLog.getTtlM());
            statement.setString(9, cacheMemLog.getIpAddress());
            statement.setInt(10, objectData.length);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            closeConn();

            try {
                String str = "INSERT INTO "+tableName+" (sitename, itemname, originKey, md5key, object, settime, ttl, ttl_m, ipaddress, objectSize) values("+ cacheMemLog.getSiteName()+", "+ cacheMemLog.getItemName()+", "+ cacheMemLog.getOriginKey()+", "+ cacheMemLog.getKey()+", , "+ cacheMemLog.getSetTime()+", "+ cacheMemLog.getTtl()+", "+ cacheMemLog.getTtlM()+", "+ cacheMemLog.getIpAddress()+", )";
                BufferedWriter file = new BufferedWriter(new FileWriter("/home/CacheMem/logError.txt", true));
                file.write(str, 0, str.length());
                file.newLine();
                file.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return true;
    }

    public void closeConn () {
        try {
            CacheMemLogger.mysqlConn.close();
            CacheMemLogger.mysqlConn = null;
        } catch (SQLException e) {
            e.printStackTrace();
            CacheMemLogger.mysqlConn = null;
        }
    }
}
