package com.jorchestrator.controller;

import com.jorchestrator.facade.SystemMonitorFacade;
import com.jorchestrator.repository.JobExecutionRepository;
import com.jorchestrator.service.NodeRegistryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UIController {

    private final SystemMonitorFacade monitorFacade;
    private final JobExecutionRepository executionRepo;
    private final NodeRegistryService nodeService;

    public UIController(SystemMonitorFacade monitorFacade, JobExecutionRepository executionRepo, NodeRegistryService nodeService) {
        this.monitorFacade = monitorFacade;
        this.executionRepo = executionRepo;
        this.nodeService = nodeService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("health", monitorFacade.getClusterHealth());
        model.addAttribute("executions", executionRepo.findAll()); // Pass executions instead of jobs
        model.addAttribute("nodes", nodeService.getAllNodes());
        return "dashboard"; 
    }
}