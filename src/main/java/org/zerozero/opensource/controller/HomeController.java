package org.zerozero.opensource.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("graphNodeCount", 133);
    model.addAttribute("graphEdgeCount", 354);
    model.addAttribute("documentCount", 40);
    model.addAttribute("mcpToolCount", 6);
    return "index";
  }
}
