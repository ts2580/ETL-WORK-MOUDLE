package com.apache.sfdc.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class SalesforcePhotoChange {

    private static final String INSTANCE_URL = "https://posco--scrum4.sandbox.my.salesforce.com";
    public static void main(String[] args) throws Exception {

        // 참고 https://developer.salesforce.com/docs/atlas.en-us.chatterapi.meta/chatterapi/intro_input.htm#
        // https://developer.salesforce.com/docs/atlas.en-us.chatterapi.meta/chatterapi/connect_resources_users_files_general.htm

        // 사용 라이브러리 org.apache.httpcomponents:httpclient:4.5.14
        // 사용 라이브러리 org.apache.httpcomponents:httpmime:4.5.14
        // 사용 라이브러리 com.fasterxml.jackson.core:jackson-core:2.17.2

        String filePath = "C:\\Users\\sungjin\\Pictures\\주석 2024-05-31 141805.png";
        String imageName = "SungJin";
        String recordId = "005JP000000QESLYA4";

        // OAuth로 알아서 Access 토큰 가져오는것 만들기
        String accessToken = SalesforceOAuth.getAccessToken();

        CloseableHttpClient client = HttpClients.createDefault();

        // 파일 업로드. 아파치 http client 사용. 난 아파치가 좋아. Connect REST API 사용
        HttpPost post = new HttpPost(INSTANCE_URL + "/services/data/v61.0/connect/files/users/me");
        post.setHeader("Authorization", "Bearer " + accessToken);

        // 편하게 멀티파트로 담아준다.
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("title", imageName, ContentType.TEXT_PLAIN);
        builder.addBinaryBody("fileData", Files.readAllBytes(Paths.get(filePath)), ContentType.DEFAULT_BINARY, "image.jpg");

        HttpEntity multipart = builder.build();
        post.setEntity(multipart);

        CloseableHttpResponse response = client.execute(post);

        if (response.getStatusLine().getStatusCode() != 201) {
            throw new RuntimeException("파일 업로드 실패 : " + response.getStatusLine().getReasonPhrase());
        }else{
            // ContentDocument에 파일 생성 성공.

            // 데이터 직렬화, 역직렬화는 모두 잭슨으로 한다. 잭슨이 편해
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(EntityUtils.toString(response.getEntity()));
            String fileId = rootNode.get("id").asText();

            System.out.println("파일 Id : " + fileId);

            // ContentDocument에 올라간 파일을 record에 옮기자
            post = new HttpPost(INSTANCE_URL + "/services/data/v61.0/connect/user-profiles/" + recordId + "/photo");
            post.setHeader("Authorization", "Bearer " + accessToken);
            post.setHeader("Content-Type", "application/json");

            Map<String, Object> mapContent = new HashMap<>();
            mapContent.put("cropSize", 55);
            mapContent.put("cropX", 0);
            mapContent.put("cropY", 0);
            mapContent.put("fileId", fileId);

            post.setEntity(new StringEntity(objectMapper.writeValueAsString(mapContent)));

            response = client.execute(post);

            if (response.getStatusLine().getStatusCode() != 201) {
                System.out.println(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("파일 업로드 까진 갔는데... : " + response.getStatusLine().getReasonPhrase());
            }else{
                rootNode = objectMapper.readTree(EntityUtils.toString(response.getEntity()));
                String isSuccess = rootNode.get("success").asText();

                System.out.println("성공 여부 : " + isSuccess);
            }
        }

        client.close();
    }

}