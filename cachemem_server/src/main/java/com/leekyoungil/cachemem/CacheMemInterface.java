package com.leekyoungil.cachemem;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by kyoungil_lee on 2014. 8. 5..
 */
public interface CacheMemInterface {
    public static final ConcurrentHashMap<String, Integer> addTTLTime = new ConcurrentHashMap<>();

    public static final boolean nonblocking = false;

    public final int threadSocketGetPort1 = 22017;
    public final int threadSocketSetPort1 = 22018;
    public final int threadSocketGetPort2 = 22027;
    public final int threadSocketSetPort2 = 22028;
    public final int threadSocketGetPort3 = 22037;
    public final int threadSocketSetPort3 = 22038;
    public final int threadSocketGetPort4 = 22047;
    public final int threadSocketSetPort4 = 22048;
    public final int threadSocketGetPort5 = 22057;
    public final int threadSocketSetPort5 = 22058;

    public final int serverSockets[] = {threadSocketSetPort1, threadSocketSetPort2, threadSocketSetPort3, threadSocketSetPort4, threadSocketSetPort5, threadSocketGetPort1, threadSocketGetPort2, threadSocketGetPort3, threadSocketGetPort4, threadSocketGetPort5};

    public final int threadSocketMaxConnectionQueue = 2048;
    public final int basicPoolSize = 50;
    public final int maximumPoolSize = 200;
    public final int keepPoolAliveTime = 10 * 1000;
    public final int BUFF_SIZE = 30 * 1024 * 1024;

    public final int vertxSocketPort = 23017;

    public final String objectByteHeader = "Application/octet-stream";
    public final String objectHtmlHeader = "text/html";

    public final String errorMsg001 = "error_code_001"; // Object is null
    public final String errorMsg002 = "error_code_002"; // Object to Byte error
    public final String errorMsg003 = "error_code_003"; // Exception error
}
