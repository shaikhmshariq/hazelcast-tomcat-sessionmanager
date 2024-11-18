package com.hazelcast.session.springboot;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
public class TestController {

    @RequestMapping("/set")
    public void defaultMapping(HttpSession session){
        session.setAttribute("testAttr", 1);
    }
}
