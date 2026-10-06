package com.salohi.hrms.web;

import com.salohi.hrms.service.DomainException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class DomainExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public String handle(DomainException exception, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", exception.getMessage());
        return "redirect:/";
    }
}
