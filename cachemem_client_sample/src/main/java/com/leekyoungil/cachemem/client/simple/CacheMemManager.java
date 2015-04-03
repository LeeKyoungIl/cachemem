package com.leekyoungil.cachemem.client.simple;

import com.leekyoungil.cachemem.client.socket.CacheMemSocketClient;
import com.leekyoungil.cachemem.client.socket.model.CacheMemResult;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

/**
 * Created by Kyoungil_Lee on 4/2/15.
 */
@Component
public class CacheMemManager {

    /**
     * Connect to cache mem.
     *
     * @param params the params
     * @param siteName the site name
     * @param methodName the method name
     * @return the object
     */
    public Object getDataFromCacheMem (Object params[], String siteName, String methodName) {
        String key = DigestUtils.md5Hex(generateKey(params, siteName, methodName));

        CacheMemResult cacheMemResultGet = CacheMemSocketClient.get(0, key, 0);

        if (cacheMemResultGet.isResult() == false) {
            return null;
        } else {
            return cacheMemResultGet.getResultData();
        }
    }

    /**
     * Sets data to cache mem.
     *
     * 데이타를 CacheMem 서버에 저장한다. 단 thread 방식으로 실행시킨다.
     * set a data to CacheMem server using thread.
     *
     * @param params the params
     * @param siteName the site name
     * @param methodName the method name
     * @param itemName the item name
     * @param resultByte the result byte
     * @param ttl the ttl
     */
    public void setDataToCacheMem (Object params[], String siteName, String methodName, String itemName, byte[] resultByte, int ttl) {
        String originKey = generateKey(params, siteName, methodName);
        String key = DigestUtils.md5Hex(originKey);

        new Thread(() -> CacheMemSocketClient.set(0, siteName, originKey, itemName, key, resultByte, ttl, "0.0.0.0", 0)).start();
    }

    /**
     * Gets key.
     *
     * @param params the params
     * @param siteName the site name
     * @param methodName the method name
     * @return the key
     */
    private String generateKey (Object params[], String siteName, String methodName) {
        StringBuilder parameter = new StringBuilder();
        parameter.append(siteName);
        parameter.append("_");
        parameter.append(methodName);

        for (Object data : params) {
            parameter.append("_");
            parameter.append(data);
        }

        return parameter.toString();
    }
}
