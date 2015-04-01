package com.leekyoungil.cachemem.memcached;

import net.rubyeye.xmemcached.MemcachedClient;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 3/31/15.
 * github : https://github.com/LeeKyoungIl/cachemem
 */
public interface MemcachedInterface {
    //memcached run script
    //./memcached -p 11211 -d -u root -m 2045 -c 10240 -P /dev/shm/memcached.pid -t 3

    public final int timeout = 300;

    /**
     * memcached 서버들 주소를 배열에 포트 번호와 함께 설정한다.
     * define the memcached server address info with port number.
     */
    public final String serverAddress[] = {"127.0.0.1:11211"};
    /**
     * 메타서버 정보를 설정한다.
     * define meta server address info with port number.
     */
    public final String metaServerAddress = "127.0.0.1:11311";

    /**
     * 리턴 메시지들을 정의한다.
     * define return messages.
     */
    public final String SUCCESS_001 = "success";
    public final String SUCCESS_002 = "empty";
    public final String ERROR_001 = "cachemem server connect exception";
    public final String ERROR_002 = "timeout exception";
    public final String ERROR_003 = "interrupted exception";
    public final String ERROR_004 = "server exception";
    public final String ERROR_005 = "known exception";
    public final String ERROR_006 = "server has gone";

    /**
     * memcached 관련 메소드들을 정의한다.
     * define the memcached methods.
     */
    public abstract MemcachedClient getMemcachedServerConnection (String serverAddress);
    public abstract MemcachedResult set (String key, Object value, int ttl, String siteName, String ipAddress, String itemName, String originKey);
    public abstract MemcachedResult get (String key);
    public abstract MemcachedResult purge (String key);


}
