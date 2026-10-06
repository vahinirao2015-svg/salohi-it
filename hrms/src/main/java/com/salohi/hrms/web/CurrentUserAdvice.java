package com.salohi.hrms.web;

import com.salohi.hrms.security.HrmsUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute("currentUser")
    public HrmsUser currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof HrmsUser user)) {
            return null;
        }
        return user;
    }
}
