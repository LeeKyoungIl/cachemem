package com.leekyoungil.cachemem.memcached;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 3/31/15.
 * github : https://github.com/LeeKyoungIl/cachemem
 */
public class MemcachedResult {
    private boolean result;
    private String resultText;
    private Object resultObject;
    private boolean objectGetReady = false;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }

    public Object getResultObject() {
        return resultObject;
    }

    public void setResultObject(Object resultObject) {
        this.resultObject = resultObject;
    }

    public boolean isObjectGetReady() {
        return objectGetReady;
    }

    public void setObjectGetReady(boolean objectGetReady) {
        this.objectGetReady = objectGetReady;
    }
}
