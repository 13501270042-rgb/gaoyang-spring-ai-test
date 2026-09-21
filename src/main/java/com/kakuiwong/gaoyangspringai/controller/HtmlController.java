package com.kakuiwong.gaoyangspringai.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * @author: gaoyang
 * @Description:
 */
@RestController
public class HtmlController {

    @RequestMapping("/html")
    public void chatPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/chat.html").forward(request, response);
    }
}
