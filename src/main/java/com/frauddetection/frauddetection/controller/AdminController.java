package com.frauddetection.frauddetection.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;

@Controller
public class AdminController {

    private final FraudAlertRepository fraudAlertRepository;

    public AdminController(FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @GetMapping("/admin/alerts")
    public String showAlerts(Model model) {

        List<FraudAlert> alerts =
                fraudAlertRepository.findAllByOrderByCreatedAtDesc();

        model.addAttribute("alerts", alerts);

        return "admin/alerts";
    }
}
