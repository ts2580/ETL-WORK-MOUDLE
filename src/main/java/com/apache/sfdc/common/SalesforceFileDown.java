package com.apache.sfdc.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class SalesforceFileDown {

    private static final String INSTANCE_URL = "https://마이 도메인 참조 ㅎ";
    public static void main(String[] args) throws Exception {

        // 참고 https://developer.salesforce.com/docs/atlas.en-us.chatterapi.meta/chatterapi/intro_input.htm#
        // https://developer.salesforce.com/docs/atlas.en-us.chatterapi.meta/chatterapi/connect_resources_users_files_general.htm
        // https://developer.salesforce.com/docs/atlas.en-us.object_reference.meta/object_reference/sforce_api_objects_contentdocumentlink.htm

        // 사용 라이브러리 org.apache.httpcomponents:httpclient:4.5.14
        // 사용 라이브러리 org.apache.httpcomponents:httpmime:4.5.14
        // 사용 라이브러리 com.fasterxml.jackson.core:jackson-core:2.17.2

        // OAuth로 알아서 Access 토큰 가져오는것 만들기. 이정도는 할 수 있자너
        String accessToken = SalesforceOAuth.getAccessToken();

        // 맨 처음 필요한것. ContentDocument(File)에 있는 파일의 sfid
        String contentDocumentId = "069JP000000CFuXYAW";

        String query = "SELECT Id, LatestPublishedVersionId FROM ContentDocument WHERE Id = '" + contentDocumentId + "'";

        // 아파치 http client 사용. 난 아파치가 좋아. 멀티파트 담기도 편하고. 여기선 안쓰지만~
        HttpGet get = new HttpGet(INSTANCE_URL + "/services/data/v61.0/query?q=" +  URLEncoder.encode(query, StandardCharsets.UTF_8));
        get.setHeader("Authorization", "Bearer " + accessToken);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(get);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(EntityUtils.toString(response.getEntity()));

        JsonNode records = rootNode.get("records");

        // 그 파일의 마지막 버전 Id
        String latestPublishedVersionId = records.get(0).get("LatestPublishedVersionId").asText();

        System.out.println("파일의 마지막 버전 Id : " + latestPublishedVersionId);

        query = "SELECT Id, Title, FileExtension, VersionData FROM ContentVersion WHERE Id = '" + latestPublishedVersionId + "'";

        // 아, 짜피 여러번쓸꺼 메서드로 뺼껄 ㅎ
        get = new HttpGet(INSTANCE_URL + "/services/data/v61.0/query?q=" +  URLEncoder.encode(query, StandardCharsets.UTF_8));
        get.setHeader("Authorization", "Bearer " + accessToken);

        response = httpClient.execute(get);

        rootNode = objectMapper.readTree(EntityUtils.toString(response.getEntity()));
        records = rootNode.get("records");

        // 그 파일의 마지막 버전의 다운로드 경로, 이름, 확장자
        String versionData = records.get(0).get("VersionData").asText();
        String title = records.get(0).get("Title").asText();
        String fileExtension = records.get(0).get("FileExtension").asText();

        System.out.println("그 파일의 경로 : " + versionData);
        System.out.println("그 파일의 이름 : " + title);
        System.out.println("그 파일의 확장자 : " + fileExtension);

        System.out.println("다운중....기달 ㅎ");

        String fullNm = title + "." + fileExtension;

        String downloadLink = INSTANCE_URL + versionData;

        // 다운받을 위치 ㅎ
        String filePath = "C:/Users/HSJ/Downloads/" + fullNm;

        // 봐봐 또 쓰잖아 ㅡㅡ. 못하는 개발자 특 : 한 메서드에 때려박음
        get = new HttpGet(downloadLink);
        get.setHeader("Authorization", "Bearer " + accessToken);

        response = httpClient.execute(get);
        InputStream inputStream = response.getEntity().getContent();
        OutputStream outputStream = new FileOutputStream(filePath);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        outputStream.close();

        httpClient.close();

        System.out.println("다운로드 완료");

    }

}