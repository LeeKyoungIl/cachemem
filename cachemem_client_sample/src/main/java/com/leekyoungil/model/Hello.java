package com.leekyoungil.model;

import java.io.Serializable;

/**
 * Created by Kyoungil_Lee on 4/3/15.
 */
public class Hello implements Serializable {

    private String name;
    private int age;
    private String cellPhone;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getCellPhone() {
        return cellPhone;
    }

    public void setCellPhone(String cellPhone) {
        this.cellPhone = cellPhone;
    }
}
