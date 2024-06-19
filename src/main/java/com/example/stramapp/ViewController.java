package com.example.stramapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/upload")
    public String upload(){
        return "upload";
    }

    @GetMapping("/stream")
    public String stream(){
        return "stream";
    }
}
