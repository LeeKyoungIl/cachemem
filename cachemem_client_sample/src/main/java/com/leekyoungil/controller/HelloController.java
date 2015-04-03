package com.leekyoungil.controller;

import com.leekyoungil.bo.HelloBO;
import com.leekyoungil.model.Hello;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/")
public class HelloController {

    @Autowired
    HelloBO helloBO;

	@RequestMapping(method = RequestMethod.GET)
	public String printWelcome (ModelMap model) {
        String name = "Mr.Lee";
        int age = 33;
        String cellPhone = "+82-10-4059-5269";

        /**
         * CacheMem 을 사용하는 2가지 방법.
         * 2 ways to use the CacheMem server.
         *
         * 1. Annotation : sample is 'helloBO.getModel'
         * 2. Call a Method : sample is 'helloBO.getModelUsingSocket'
         */
        Hello returnData = helloBO.getModel(name, age, cellPhone);
        //Hello returnData = helloBO.getModelUsingSocket(name, age, cellPhone);

        model.addAttribute("message", "Hello CashMem!");

		return "hello";
	}
}