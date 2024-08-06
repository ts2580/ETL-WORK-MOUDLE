package com.apache.sfdc.router.controller;

import com.apache.sfdc.common.SalesforceOAuth;
import com.apache.sfdc.router.service.RouterService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    public String setPushTopic(@RequestBody String strJson) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        // strJson을 Map으로 변환 , mapProperty에는 아파치 카멜로 salesforce component 접속정보
        Map<String, String> mapProperty = objectMapper.readValue(strJson, Map.class);

        // 토큰 생성
        String token = SalesforceOAuth.getAccessToken(mapProperty);

        // 테이블 생성 후 데이터 넣기
        Map<String, Object> mapReturn = routerService.setTable(mapProperty, token);

        // 푸시토픽 넣어주기
        String pushTopic = routerService.setPushTopic(mapProperty, mapReturn, token);

        // 푸시토픽 구독 및 DB 삽입
        routerService.subscribePushTopic(mapProperty, token, (Map<String, Object>) mapReturn.get("mapType"));

        return "모든 시퀸스 성공";
    }
}