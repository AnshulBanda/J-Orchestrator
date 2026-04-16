package com.jorchestrator.controller;

import com.jorchestrator.facade.SystemMonitorFacade;
import com.jorchestrator.service.JobSubmissionService;
import com.jorchestrator.service.NodeRegistryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UIController {

    private final SystemMonitorFacade monitorFacade;
    private final JobSubmissionService jobService;
    private final NodeRegistryService nodeService;

    public UIController(SystemMonitorFacade monitorFacade, JobSubmissionService jobService, NodeRegistryService nodeService) {
        this.monitorFacade = monitorFacade;
        this.jobService = jobService;
        this.nodeService = nodeService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("health", monitorFacade.getClusterHealth());
        model.addAttribute("jobs", jobService.getAllJobs());
        model.addAttribute("nodes", nodeService.getAllNodes());
        return "dashboard"; // Maps to src/main/resources/templates/dashboard.html
    }
}