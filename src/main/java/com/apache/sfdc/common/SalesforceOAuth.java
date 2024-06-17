package com.apache.sfdc.common;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class SalesforceOAuth {

    private static final String LOGIN_URL = "https://login.salesforce.com/services/oauth2/token";
    private static final String CLIENT_ID = "3MVG9q4K8Dm94dAwF6D70zsfWDZO2vEz0CCf0bQtywOlbgdghIYL0JLpyG2HP5bvUkV5Lm1B.4bZE0z6pkVu3";
    private static final String CLIENT_SECRET = "A532570413C1AB3952E7CEDEBDBFF0735651F2F8011B093C8CE03A646FF9AD0A";
    private static final String USERNAME = "admin@ecologysyncmanagementco.kr.dev";
    private static final String PASSWORD = "qwer1234!!NLIi077LCt55mBjVM5AJKyTH";

    public static String getAccessToken() throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(LOGIN_URL);
        StringEntity params = new StringEntity(
                "grant_type=password&client_id=" + CLIENT_ID +
                        "&client_secret=" + CLIENT_SECRET +
                        "&username=" + USERNAME +
                        "&password=" + PASSWORD);
        post.addHeader("content-type", "application/x-www-form-urlencoded");
        post.setEntity(params);

        CloseableHttpResponse response = httpClient.execute(post);
        String responseString = EntityUtils.toString(response.getEntity());
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(responseString);
        return String.valueOf(json.get("access_token"));
    }
}
