package cz.stabuilder.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Servíruje hlavní HTML stránku.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
