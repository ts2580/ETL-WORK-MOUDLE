package com.apache.sfdc.router.service;

import java.util.Map;

public interface RouterService {

    Map<String,Object> setTable(Map<String, String> mapProperty, String token);

    String setPushTopic(Map<String, String> mapProperty, String token, String soql);

    void subscribePushTopic(Map<String, String> mapProperty, String token, Map<String, Object> mapType) throws Exception;
}
