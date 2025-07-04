//package com.example.dream_stream_bot.controller;

//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.io.IOException;
//
//@RestController
//public class FileController {
//
//    @Autowired
//    private FileReadingService fileReadingService;
//
//    @GetMapping("/read-file")
//    public String readFile() {
//        try {
//            return fileReadingService.readFile("classpath:myfile.txt");
//        } catch (IOException e) {
//            return "Error reading file: " + e.getMessage();
//        }
//    }
//}
