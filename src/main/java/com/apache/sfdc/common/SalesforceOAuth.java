package com.apache.sfdc.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.Map;
import java.util.Objects;

public class SalesforceOAuth {

    private static final String LOGIN_URL = "";
    private static final String CLIENT_ID = "";
    private static final String CLIENT_SECRET = "";
    private static final String USERNAME = "";
    private static final String PASSWORD = "";

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

    public static String getAccessToken(Map<String, String> mapProperty) throws Exception {

        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .addEncoded("grant_type", "password")
                .addEncoded("client_id", mapProperty.get("client_id"))
                .addEncoded("client_secret", mapProperty.get("client_secret"))
                .addEncoded("username", mapProperty.get("username"))
                .addEncoded("password", mapProperty.get("password"))
                .build();

        Request request = new Request.Builder()
                .url(mapProperty.get("loginUrl") + "/services/oauth2/token")
                .method("POST", formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        String token = "";

        try(Response response = client.newCall(request).execute()) {
            // 잭슨
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode rootNode = objectMapper.readTree(Objects.requireNonNull(response.body()).string());
            token = rootNode.get("access_token").asText();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

        return token;
    }
}
