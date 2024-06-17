package com.apache.sfdc.router.modal.service;

import lombok.RequiredArgsConstructor;
import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.salesforce.SalesforceComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class RouterServiceImpl implements RouterService{

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
    public void SetSubCDC(String changeEvent) throws Exception {

        SalesforceComponent salesforce = new SalesforceComponent();
        salesforce.setLoginUrl(loginUrl);
        salesforce.setClientId(clientId);
        salesforce.setClientSecret(clientSecret);
        salesforce.setUserName(userName);
        salesforce.setPassword(password);
        salesforce.setPackages(Arrays.toString(new String[]{"com.apache.sfdc.common"}));

        RouteBuilder rb = new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                System.out.println("실행");

                // Salesforce Pub/Sub API 구독 설정
                from("salesforce:getGlobalObjects?rawPayload=true")
                        .process(exchange -> {
                            Message in = exchange.getIn();
                            System.out.println(in.getBody());
                        });
            }
        };

        CamelContext myCamelContext = new DefaultCamelContext();
        myCamelContext.addRoutes(rb);
        myCamelContext.addComponent("salesforce", salesforce);

        try{
            myCamelContext.start();
        }catch (Exception e){
            System.out.println(e.getMessage());
            myCamelContext.close();
        }
    }
}
