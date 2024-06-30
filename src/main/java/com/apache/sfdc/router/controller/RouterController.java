package com.apache.sfdc.router.controller;

import com.apache.sfdc.router.modal.service.RouterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("apache")
public class RouterController {

    private final RouterService routerService;

    @GetMapping("/cdc")
    public void getConfig(@RequestParam(value = "changeEvent") String changeEvent) throws Exception {
        routerService.SetSubCDC(changeEvent);
    }
}
