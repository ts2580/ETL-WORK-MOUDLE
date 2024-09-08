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
@RequestMapping("router")
public class RouterController {
    private final RouterService routerService;

    @PostMapping("/streaming")
    public String setPushTopic(@RequestBody String strJson) throws Exception {

        // Streaming API로는 PushTopic events, generic events, platform events, Change Data Capture events 다 받음
        // PushTopic event는 streaming으로 받자.
        // Bayeux 프로토콜과 CometD 클라이언트 기반.

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

    @PostMapping("/pubsub")
    public String setCDC(@RequestBody String strJson) throws Exception {

        // platform events, Change Data Capture events 받을 수 있음
        // gRPC API 및 HTTP/2 기반.
        // HTTP/2를 통해 압축을 사용하여 실시간, 고성능 데이터 스트리밍을 제공
        // Apache Avro 형식으로 바이너리 이벤트 메시지를 효율적으로 게시하고 전달

        ObjectMapper objectMapper = new ObjectMapper();

        // strJson을 Map으로 변환 , mapProperty에는 아파치 카멜로 salesforce component 접속정보
        Map<String, String> mapProperty = objectMapper.readValue(strJson, Map.class);

        // 토큰 생성
        String token = SalesforceOAuth.getAccessToken(mapProperty);

        // 테이블 생성 후 데이터 넣기
        Map<String, Object> mapReturn = routerService.setTable(mapProperty, token);

        // CDC 이벤트는 Point & Click으로 가능. 설정에서 미리 세팅 했다고 가이드 라인 줄것.
        routerService.subscribeCDC(mapProperty);


        return null;
    }

}