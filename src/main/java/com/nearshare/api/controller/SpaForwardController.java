package com.nearshare.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardController {

    @RequestMapping(value = {
            "/",
            "/{path:^(?!api|ws|v3|swagger-ui|actuator|assets)[^\\.]*$}",
            "/{path:^(?!api|ws|v3|swagger-ui|actuator|assets)[^\\.]*$}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
