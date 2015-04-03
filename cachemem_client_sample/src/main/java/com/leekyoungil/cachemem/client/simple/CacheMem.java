package com.leekyoungil.cachemem.client.simple;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Kyoungil_Lee on 4/2/15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheMem {

    /**
     * Site name.
     *
     * 사이트 명
     *
     * @return the string
     */
    String siteName () default "empty";

    /**
     * Item name. (Can be utilized in various kind.)
     *
     * 아이템 명 (다양한 용도로 활용 가능)
     *
     * @return the string
     */
    String itemName () default "empty";

    /**
     * Ttl int. (Time to live)
     *
     * 캐쉬 생명주기
     *
     * @return the int
     */
    int ttl () default 0;
}
