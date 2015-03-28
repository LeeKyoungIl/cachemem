package com.leekyoungil.cachemem.model;

/**
 * Created by kyoungil_lee on 10/5/14.
 */
public class CacheMemLog {
    private String siteName;
    private String key;
    private Object objectData;
    private int ttl;
    private int ttlM;
    private int setTime;
    private String ipAddress;
    private String itemName;
    private String originKey;

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getObjectData() {
        return objectData;
    }

    public void setObjectData(Object objectData) {
        this.objectData = objectData;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public int getTtlM() {
        return ttlM;
    }

    public void setTtlM(int ttlM) {
        this.ttlM = ttlM;
    }

    public int getSetTime() {
        return setTime;
    }

    public void setSetTime(int setTime) {
        this.setTime = setTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getOriginKey() {
        return originKey;
    }

    public void setOriginKey(String originKey) {
        this.originKey = originKey;
    }
}

