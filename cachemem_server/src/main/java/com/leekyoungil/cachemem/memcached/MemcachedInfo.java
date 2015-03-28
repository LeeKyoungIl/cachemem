package com.leekyoungil.cachemem.memcached;

import net.rubyeye.xmemcached.MemcachedClient;

/**
 * Created by kyoungil_lee on 2014. 7. 24..
 */
public interface MemcachedInfo {
    // memcached run script
    // ./memcached -p 11211 -d -u root -m 2045 -c 10240 -P /dev/shm/memcached.pid -t 3

    public int timeout = 300;

    public String[] serverAddress = {"127.0.0.1:11211"};

    public String metaServerAddress = "127.0.0.1:11311";

    public final String SUCCESS_001 = "success";
    public final String SUCCESS_002 = "empty";
    public final String ERROR_001 = "flashDb connect exception";
    public final String ERROR_002 = "timeout exception";
    public final String ERROR_003 = "interrupted exception";
    public final String ERROR_004 = "server exception";
    public final String ERROR_005 = "known exception";
    public final String ERROR_006 = "server has gone";

    public abstract boolean connect ();

    public abstract MemcachedClient getMemcachedServerConnection (String serverAddress);

    public abstract boolean close ();

    public abstract MemcachedResult set (String key, Object value, int ttl, String siteName, String ipAddress, String itemName, String originKey);

    public abstract MemcachedResult get (String key);

    public abstract MemcachedResult purge (String key);


}
