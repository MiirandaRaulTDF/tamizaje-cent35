package cent35.controllers;

import cent35.dtos.DashboardResumenDTO;
import cent35.services.DashboardResumenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardResumenService resumenService;

    public DashboardController(DashboardResumenService resumenService) {
        this.resumenService = resumenService;
    }

    @GetMapping("/resumen")
    public DashboardResumenDTO obtenerResumen() {
        return resumenService.obtenerResumen();
    }
}