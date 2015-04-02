package com.leekyoungil.cachemem.memcached;

import com.leekyoungil.cachemem.CacheMem;
import com.leekyoungil.cachemem.CacheMemInterface;
import com.leekyoungil.cachemem.model.CacheMemLog;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.Map.Entry;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 3/31/15.
 * github : https://github.com/LeeKyoungIl/cachemem
 */
public class MemcachedControl extends MemcachedInfo {

    /**
     * define MemcachedControl singleton instance.
     */
    private volatile static MemcachedControl INSTANCE = null;

    private MemcachedControl () { }

    /**
     * Gets instance.
     *
     * Singleton object.
     *
     * @return the instance
     */
    public static MemcachedControl getInstance () {
        if (INSTANCE == null) {
            synchronized (MemcachedControl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MemcachedControl();
                }
            }
        }

        return INSTANCE;
    }

    public MemcachedResult set (String key, Object value, int ttl, String siteName, String ipAddress, String itemName, String originKey) {
        MemcachedResult memcachedResult = new MemcachedResult();

        memcachedResult.setResult(true);

        if (!connectCheck(0)) {
            memcachedResult.setResultText(ERROR_001);
            memcachedResult.setResult(false);

            return memcachedResult;
        }

        boolean result = false;

        try {
            int addTTLTime = 30;

            try {
                addTTLTime = (CacheMemInterface.addTTLTime.containsKey(siteName) ? CacheMemInterface.addTTLTime.get(siteName) : 30);
            } catch (Exception e) {
                e.printStackTrace();
                addTTLTime = 30;
            }

            int serverNo = 0;

            // 처음은 무조건 localhost 로 set 시도
            result = this.memcachedClient.get(serverNo).set(key, ((ttl > 0) ? ttl+addTTLTime : this.timeout), value);

            CacheMemLog log = new CacheMemLog();

            if (result == false) {
                serverNo = getServerNo(-1, 0);

                this.confuseServerNo.clear();

                if (serverNo < 0) {
                    memcachedResult.setResult(false);
                    memcachedResult.setResultText(ERROR_006);

                    value = null;
                    ttl = serverNo;
                    result = false;
                } else {
                    result = this.memcachedClient.get(serverNo).set(key, ((ttl > 0) ? ttl+addTTLTime : this.timeout), value);
                }
            }

            if (result) {
                memcachedResult.setResultText(SUCCESS_001);
                memcachedResult.setResult(result);

                try {
                    if (this.metaMemcachedClient != null && this.metaMemcachedClient.getAvailableServers().size() > 0) {
                        this.metaMemcachedClient.set(key, ((ttl > 0) ? ttl+1+addTTLTime : this.timeout), serverNoMap.get(serverNo));
                    } else {
                        this.metaMemcachedClient = null;
                        metaConnect();
                    }
                } catch (Exception me) {
                    me.printStackTrace();
                }
            }

            log.setSiteName(siteName);
            log.setKey(key);
            log.setObjectData(value);
            log.setTtl(ttl);
            log.setTtlM(ttl+addTTLTime);
            log.setSetTime((int) (System.currentTimeMillis() / 1000));
            log.setIpAddress(ipAddress);
            log.setItemName(itemName);
            log.setOriginKey(originKey);

            LinkedBlockingQueue<CacheMemLog> cacheMemsLog = CacheMem.LOG_QUEUE_MAP.get(CacheMem.QUEUE_NO);

            cacheMemsLog.add(log);
        } catch (TimeoutException|InterruptedException|MemcachedException|NullPointerException e) {
            memcachedResult = handleToException(e, memcachedResult);
        }

        return memcachedResult;
    }

    public MemcachedResult get (String key) {
        MemcachedResult memcachedResult = new MemcachedResult();

        memcachedResult.setResult(true);

        if (!connectCheck(0)) {
            memcachedResult.setResultText(ERROR_001);
            memcachedResult.setResult(false);

            return memcachedResult;
        }

        Object tmpResult = null;

        boolean checkMetaServer = false;

        // 일단 메타 서버 뒤진다.
        try {
            if (this.metaMemcachedClient != null && this.metaMemcachedClient.getAvailableServers().size() > 0) {
                Object serverMetaAddress = this.metaMemcachedClient.get(key);

                if (serverMetaAddress != null) {
                    checkMetaServer = true;
                    String flashDbAddress = (String) serverMetaAddress;

                    Optional<Entry<Integer, String>> result = this.serverNoMap.entrySet().stream().filter(entry -> entry.getValue().equals(flashDbAddress)).findFirst();

                    if (result.isPresent()) {
                        tmpResult = this.memcachedClient.get(result.get().getKey()).get(key);
                    }
                }
            } else {
                this.metaMemcachedClient = null;
                metaConnect();
            }
        } catch (TimeoutException|InterruptedException|MemcachedException e) {
            memcachedResult = handleToException(e, memcachedResult);
            tmpResult = null;
        }

        if (tmpResult == null && !checkMetaServer) {
            // 무조건 localhost 부터 뒤진다
            for (MemcachedClient memcachedClientTmp : this.memcachedClient) {
                try {
                    if (tmpResult == null) {
                        tmpResult = memcachedClientTmp.get(key);

                        if (tmpResult != null) {
                            break;
                        }
                    }
                } catch (TimeoutException|InterruptedException|MemcachedException e) {
                    memcachedResult = handleToException(e, memcachedResult);
                    tmpResult = null;
                }
            }
        }

        memcachedResult.setResultText(SUCCESS_002);

        if (tmpResult != null) {
            memcachedResult.setResultObject(tmpResult);
            memcachedResult.setResultText(SUCCESS_001);
        }

        memcachedResult.setResult(true);


        return memcachedResult;
    }

    public MemcachedResult purge (String key) {
        MemcachedResult memcachedResult = new MemcachedResult();

        memcachedResult.setResult(true);

        if (!connectCheck(0)) {
            memcachedResult.setResultText(ERROR_001);
            memcachedResult.setResult(false);

            return memcachedResult;
        }
        // 무조건 localhost 부터 뒤진다
        for (MemcachedClient memcachedClientTmp : this.memcachedClient) {
            try {
                memcachedClientTmp.delete(key);
                memcachedResult.setResultText(SUCCESS_001);
                memcachedResult.setResult(true);
            } catch (TimeoutException|InterruptedException|MemcachedException e) {
                memcachedResult = handleToException(e, memcachedResult);
            }
        }

        return memcachedResult;
    }

    /**
     * Handle to exception.
     *
     * @param e the e
     * @param memcachedResult the memcached result
     * @return the memcached result
     */
    private MemcachedResult handleToException (Exception e, MemcachedResult memcachedResult) {
        String errorType = e.toString().toLowerCase();

        if (errorType.indexOf("timeoutexception") > -1) {
            memcachedResult.setResultText(ERROR_002);
        } else if (errorType.indexOf("interruptedexception") > -1) {
            memcachedResult.setResultText(ERROR_003);
        } else if (errorType.indexOf("memcachedexception") > -1) {
            memcachedResult.setResultText(ERROR_004);
        } else if (errorType.indexOf("exception") > -1) {
            memcachedResult.setResultText(ERROR_005);
        }

        memcachedResult.setResult(false);
        e.printStackTrace();

        return memcachedResult;
    }
}
