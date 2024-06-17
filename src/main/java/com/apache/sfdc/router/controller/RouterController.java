package com.apache.sfdc.router.controller;

import com.apache.sfdc.router.modal.service.RouterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("apache")
public class RouterController {
    @Value("${camel.component.salesforce.client-id}")
    private String clientId;

    @Value("${camel.component.salesforce.client-secret}")
    private String clientSecret;

    @Value("${camel.component.salesforce.user-name}")
    private String userName;

    @Value("${camel.component.salesforce.password}")
    private String password;

    @Value("${camel.component.salesforce.login-url}")
    private String loginUrl;

    private final RouterService routerService;

    @GetMapping("/cdc")
    public void getConfig(@RequestParam(value = "changeEvent") String changeEvent) throws Exception {

        Map<String,String> MapParams = new HashMap<>();
        MapParams.put("clientId", clientId);
        MapParams.put("clientSecret", clientSecret);
        MapParams.put("userName", userName);
        MapParams.put("password", password);
        MapParams.put("loginUrl", loginUrl);

        routerService.SetSubCDC(changeEvent);
    }
}
