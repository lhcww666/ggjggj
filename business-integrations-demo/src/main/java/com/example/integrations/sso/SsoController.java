package com.example.integrations.sso;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sso")
public class SsoController {

    private final SsoService ssoService;

    public SsoController(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    @PostMapping("/authorize")
    public Map<String, Object> authorize(@RequestBody SsoAuthorizeRequest request) {
        return ssoService.authorize(request);
    }

    @GetMapping("/exchange")
    public Map<String, Object> exchange(@RequestParam String code) {
        return ssoService.exchange(code);
    }
}
