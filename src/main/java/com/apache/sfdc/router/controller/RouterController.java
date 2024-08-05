package com.apache.sfdc.router.controller;

import com.apache.sfdc.common.SalesforceOAuth;
import com.apache.sfdc.router.service.RouterService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("apache")
public class RouterController {

    private final RouterService routerService;

    @PostMapping("/pushtopic")
    public String setTopic(@RequestBody String json) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, String> mapProperty = objectMapper.readValue(json, Map.class);

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