package com.apache.sfdc.router.controller;

import com.apache.sfdc.common.SalesforceOAuth;
import com.apache.sfdc.router.service.RouterService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("apache")
public class RouterController {

    private final RouterService routerService;

    @PostMapping("/pushtopic")
    public String setTopic(@RequestParam Map<String, String> mapParam) throws Exception {

        // x-www-form-urlencoded로 보냈으므로 @RequestParam으로 받음.

        String mapPropertyJson = mapParam.get("mapProperty");
        Map<String, String> mapProperty = new ObjectMapper().readValue(mapPropertyJson, new TypeReference<Map<String, String>>(){});

        // access token 가져오기
        String token = SalesforceOAuth.getAccessToken(mapProperty);

        // 테이블 만들고 데이터 부어넣기
        Map<String, Object> returnMap = routerService.setTable(mapProperty, token);

        // pushTopic 만들기
        String isSet = routerService.setPushTopic(mapProperty, token, String.valueOf(returnMap.get("soqlForPushTopic")));
        System.out.println("PushTopic 설정 : " + isSet);

        // 토픽 구독 설정
        routerService.subscribePushTopic(mapProperty, token, (Map<String, Object>) returnMap.get("mapType"));

        return "모든 시퀸스 성공";
    }
}