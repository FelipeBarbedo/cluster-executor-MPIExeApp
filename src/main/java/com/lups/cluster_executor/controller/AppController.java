package com.lups.cluster_executor.controller;

import com.lups.cluster_executor.service.AppExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/execute")
public class AppController {

    @Autowired
    private final AppExecutorService appExecutorService;

    @Autowired
    public AppController(AppExecutorService appExecutorService) {

        this.appExecutorService = appExecutorService;
    }

    @PostMapping("/publish")
    public String executeFiles(@RequestParam("file") List<MultipartFile> files) {

        return appExecutorService.codeExecutor(files);
    }
}
