package com.leekyoungil.cachemem.model;

import java.awt.*;
import java.io.Serializable;

/**
 * Created by kyoungil_lee on 2014. 7. 24..
 */
public class Test implements Serializable {
    private String test1;
    private String test2;
    private String test3;
    private String test4;
    private String test5;

    private List test;

    public String getTest1() {
        return test1;
    }

    public void setTest1(String test1) {
        this.test1 = test1;
    }

    public String getTest2() {
        return test2;
    }

    public void setTest2(String test2) {
        this.test2 = test2;
    }

    public String getTest3() {
        return test3;
    }

    public void setTest3(String test3) {
        this.test3 = test3;
    }

    public String getTest4() {
        return test4;
    }

    public void setTest4(String test4) {
        this.test4 = test4;
    }

    public String getTest5() {
        return test5;
    }

    public void setTest5(String test5) {
        this.test5 = test5;
    }

    public List getTest() {
        return test;
    }

    public void setTest(List test) {
        this.test = test;
    }
}
