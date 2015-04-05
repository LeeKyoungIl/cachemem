package com.leekyoungil.cachemem.client.socket.model;

/**
 * Created by Kyoungil_Lee on 4/1/15.
 */
public class CacheMemResult {

    // 접속 서버 번호
    private int connectServerNo;
    // 접속 포트 번호
    private int connectPortNo;
    // 처리 결과
    private boolean result;
    // CacheMem 에서 받은 결과
    private Object resultData;
    // 오류 발생시 오류 내역
    private String errorText;

    public int getConnectServerNo() {
        return connectServerNo;
    }

    public void setConnectServerNo(int connectServerNo) {
        this.connectServerNo = connectServerNo;
    }

    public int getConnectPortNo() {
        return connectPortNo;
    }

    public void setConnectPortNo(int connectPortNo) {
        this.connectPortNo = connectPortNo;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public Object getResultData() {
        return resultData;
    }

    public void setResultData(Object resultData) {
        this.resultData = resultData;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }
}
