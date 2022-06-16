package com.wisdge.cloud.fs.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.LocaleResolver;

import javax.annotation.Resource;

@Controller
@Slf4j
public class StartController extends BaseController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
