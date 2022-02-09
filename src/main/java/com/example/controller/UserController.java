package com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin
public class UserController {


    @PostMapping (value = "/user/{userName}")
    public String getUserByName(
            HttpServletRequest httpServletRequest
            ,
            @PathVariable String userName
            ,
            @RequestBody String userName3
            ,
            @RequestParam String userName2
    ) {

        return "111";

    }
}