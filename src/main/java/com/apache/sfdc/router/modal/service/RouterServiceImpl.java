package com.apache.sfdc.router.modal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.salesforce.SalesforceComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RouterServiceImpl implements RouterService{

    @Value("${camel.component.salesforce-ecology.login-url}")
    private String ecologyLoginUrl;

    @Value("${camel.component.salesforce-ecology.client-id}")
    private String ecologyClientId;

    @Value("${camel.component.salesforce-ecology.client-secret}")
    private String ecologyClientSecret;

    @Value("${camel.component.salesforce-ecology.user-name}")
    private String ecologyUserName;

    @Value("${camel.component.salesforce-ecology.password}")
    private String ecologyPassword;

    @Value("${camel.component.salesforce-sj.login-url}")
    private String SungJinDevLoginUrl;

    @Value("${camel.component.salesforce-sj..client-id}")
    private String SungJinDevClientId;

    @Value("${camel.component.salesforce-sj.client-secret}")
    private String SungJinDevClientSecret;

    @Value("${camel.component.salesforce-sj.user-name}")
    private String SungJinDevUserName;

    @Value("${camel.component.salesforce-sj.password}")
    private String SungJinDevPassword;

    public void SetSubCDC(String changeEvent) throws Exception {

        SalesforceComponent myDev = new SalesforceComponent();
        myDev.setLoginUrl(ecologyLoginUrl);
        myDev.setClientId(ecologyClientId);
        myDev.setClientSecret(ecologyClientSecret);
        myDev.setUserName(ecologyUserName);
        myDev.setPassword(ecologyPassword);
        myDev.setPackages("com.apache.sfdc.router.dto");

        SalesforceComponent scrumDev = new SalesforceComponent();
        scrumDev.setLoginUrl(SungJinDevLoginUrl);
        scrumDev.setClientId(SungJinDevClientId);
        scrumDev.setClientSecret(SungJinDevClientSecret);
        scrumDev.setUserName(SungJinDevUserName);
        scrumDev.setPassword(SungJinDevPassword);
        scrumDev.setPackages("com.apache.sfdc.router.dto");

        RouteBuilder rb = new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                System.out.println("실행");

                from("myDev:subscribe:" + changeEvent)
                        .process(exchange -> {
                            Message message = exchange.getIn();
                            System.out.println(message.getBody());

                            ObjectMapper objectMapper = new ObjectMapper();
                            Map<String, Object> map = objectMapper.convertValue(message.getBody(), Map.class);
                            map.remove("Id");

                            exchange.getIn().setBody(new Gson().toJson(map));
                        })
                        .to("scrumDev:createSObject?sObjectName=" + changeEvent)
                        .log("Created new Account with ID ${body.id}");
            }
        };

        CamelContext myCamelContext = new DefaultCamelContext();
        myCamelContext.addRoutes(rb);
        myCamelContext.addComponent("myDev", myDev);
        myCamelContext.addComponent("scrumDev", scrumDev);

        try{
            myCamelContext.start();
        }catch (Exception e){
            System.out.println(e.getMessage());
            myCamelContext.close();
        }
    }
}