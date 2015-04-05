package com.leekyoungil.cachemem.client.simple;

import com.leekyoungil.cachemem.client.simple.model.TmpString;
import com.leekyoungil.cachemem.client.socket.CacheMemSocketClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by Kyoungil_Lee on 4/2/15.
 */
@Aspect
@Component
public class CacheMemAnnotation {

    @Resource(name = "cacheMemManager")
    CacheMemManager cacheMemManager;

    @Pointcut("@annotation(com.leekyoungil.cachemem.client.simple.CacheMem)")
    public void pointcut () {}

    @Around("pointcut()")
    public Object dataHandler (ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();

        int ttl = 0;
        String siteName = null;
        String itemName = null;
        String serverType = null;

        /**
         * 필수 값으 확인함.
         * check to a requirement value.
         */
        try {
            ttl = signature.getMethod().getAnnotation(CacheMem.class).ttl();
            siteName = signature.getMethod().getAnnotation(CacheMem.class).siteName();
            itemName = signature.getMethod().getAnnotation(CacheMem.class).itemName();
            serverType = signature.getMethod().getAnnotation(CacheMem.class).serverType();
        } catch (Exception ex) {
            ex.printStackTrace();

            ttl = 0;
            siteName = "empty";
            itemName = "empty";
            serverType = null;
        }

        /**
         * 만약 필수값중 하나라도 문제가 생기면 원래 메소드를 실행한다.
         * if there is problems in requirement values. execute a original method.
         */
        if (ttl == 0 || "empty".equals(siteName) || "empty".equals(itemName) || serverType == null) {
            return excuteOriginMethod(pjp);
        }

        /**
         * 데이타를 CacheMem 서버로 부터 가지고 온다.
         * get data from CacheMem server.
         */
        Object result = cacheMemManager.getDataFromCacheMem(pjp.getArgs(), serverType, siteName, signature.getMethod().getName());

        if (result == null) {
            /**
             * 원래 메소드를 실행한다.
             * execute a original method.
             */
            result = excuteOriginMethod(pjp);

            byte dataBytes[] = null;

            if ((result instanceof String)) {
                TmpString tmpString = new TmpString();
                tmpString.setTmpString((String) result);

                dataBytes = CacheMemSocketClient.toByteArray(tmpString);
            } else {
                dataBytes = CacheMemSocketClient.toByteArray(result);
            }

            /**
             * 데이타를 CacheMem 서버에 저장한다.
             * set data to the CacheMem server.
             */
            cacheMemManager.setDataToCacheMem(pjp.getArgs(), serverType, siteName, signature.getMethod().getName(), itemName, dataBytes, ttl);
        }

        return result;
    }

    /**
     * Excute origin method.
     *
     * 원래의 메소드를 실행한다.
     *
     * @param pjp the pjp
     * @return the object
     */
    private Object excuteOriginMethod (ProceedingJoinPoint pjp) {
        Object resultObject = null;

        try {
            resultObject = pjp.proceed();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return resultObject;
    }
}
