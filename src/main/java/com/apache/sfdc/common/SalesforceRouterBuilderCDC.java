package com.apache.sfdc.common;

import com.apache.sfdc.router.repository.ETLRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

public class SalesforceRouterBuilderCDC extends RouteBuilder {
    private final String selectedObject;
    private final ETLRepository etlRepository;
    public SalesforceRouterBuilderCDC(String selectedObject, ETLRepository etlRepository) {
        this.selectedObject = selectedObject;
        this.etlRepository = etlRepository;
    }

    @Override
    public void configure() throws Exception {

        // std obj는 뒤에 곧바로 ChangeEvent 붙이고, custom은 __c를 __ChangeEvent로 교체.
        String eventName = selectedObject.contains("__c") ? selectedObject.replace("__c", "__ChangeEvent") : selectedObject + "ChangeEvent";

        // 장애시 ?replayPreset=CUSTOM 을 붙이고, 마지막 pubSubReplayId를 넣어주자
        // ?replayPreset=CUSTOM&pubSubReplayId=AAAAAAAEjdgAAA== 이런 방식으로. 그럼 그 후 데이터를 구독함.
        // PUB/SUB API일 때 장애조치는 pubSubReplayId를 Redis에 저장하는 방식으로.

        // 세일즈포스에서 List로 넣었을땐 Max sequenceNumber의 pubSubReplayId 저장!

        // https://developer.salesforce.com/docs/platform/pub-sub-api/references/methods/subscribe-rpc.html
        
        // Redis로 메시지 날릴것 아니고 Failover용 보조 DB로 쓸꺼니 Camel은 쓰지 말자

        from("sf:pubSubSubscribe:/data/" + eventName + "?replayPreset=CUSTOM&pubSubReplayId=AAAAAAAEjdgAAA==")
                .process((exchange) -> {
                    Message message = exchange.getIn();
                    System.out.println(message.getHeader("CamelSalesforcePubSubReplayId"));
                    System.out.println(message.getBody());

                });
    }
}
