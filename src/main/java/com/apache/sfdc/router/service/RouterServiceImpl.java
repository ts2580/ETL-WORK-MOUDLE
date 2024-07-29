package com.apache.sfdc.router.service;

import com.apache.sfdc.common.SalesforceOAuth;
import com.apache.sfdc.router.dto.FieldDefinition;
import com.apache.sfdc.router.repository.ETLRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.salesforce.SalesforceComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RouterServiceImpl implements RouterService{

    private final ETLRepository etlRepository;

    @Override
    public Map<String,Object> setTable(Map<String, String> mapProperty, String token) {

        String selectedObject = mapProperty.get("selectedObject");
        String instanceUrl = mapProperty.get("instanceUrl");

        Map<String,Object> returnMap = new HashMap<>();

        List<FieldDefinition> listDef = new ArrayList<>();

        OkHttpClient client = new OkHttpClient();

        // 잭슨으로 역직렬화
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode rootNode;

        Request request = new Request.Builder()
                .url(instanceUrl + "/services/data/v61.0/sobjects/" + selectedObject + "/describe")
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        // DDL 테이블 생성용
        StringBuilder ddl = new StringBuilder();

        // 순차적 values 구문 구성용
        List<String> listFields = new ArrayList<>();

        // soql로 받아온 값 변환용 맵
        Map<String,String> mapType = new HashMap<>();

        try(Response response = client.newCall(request).execute()) {
            String responseBody = Objects.requireNonNull(response.body()).string();

            // 세일즈 포스로 따지면 JSON.deserializeUntyped();
            rootNode = objectMapper.readTree(responseBody);

            JsonNode fields = rootNode.get("fields");

            listDef = objectMapper.convertValue(fields, new TypeReference<List<FieldDefinition>>() {});

            ddl.append("CREATE OR REPLACE table config.").append(selectedObject).append("(");

            for(FieldDefinition obj : listDef){

                mapType.put(obj.name, obj.type);

                // 데이터 타입 확인용
                // System.out.println(obj.name + " : " + obj.type);

                // 세일즈포스에서 만드는 모든 필드타입들은 하단의 Type으로 모인다. 특정 Object 타입(Address, Name 등)은 빼주자
                switch (obj.type) {
                    case "id" -> {
                        ddl.append("sfid VARCHAR(18) primary key not null comment '").append(obj.label).append("',");
                    }case "textarea" -> {
                        if (obj.length > 4000) {
                            ddl.append(obj.name).append(" TEXT comment '").append(obj.label).append("',");
                        } else {
                            ddl.append(obj.name).append(" VARCHAR(").append(obj.length).append(") comment '").append(obj.label).append("',");
                        }
                        listFields.add(obj.name);
                    }case "reference" ->{
                        ddl.append(obj.name).append(" VARCHAR(18) comment '").append(obj.label).append("',");
                        listFields.add(obj.name);
                    }case "string", "picklist", "multipicklist", "phone", "url" ->{
                        ddl.append(obj.name).append(" VARCHAR(").append(obj.length).append(") comment '").append(obj.label).append("',");
                        listFields.add(obj.name);
                    }case "boolean" -> {
                        ddl.append(obj.name).append(" boolean comment '").append(obj.label).append("',");
                        listFields.add(obj.name);
                    }case "datetime" ->{
                        ddl.append(obj.name).append(" TIMESTAMP comment '").append(obj.label).append("',");
                        listFields.add(obj.name);
                    }case "date" -> {
                        ddl.append(obj.name).append(" date comment '").append(obj.label).append("',");
                        listFields.add(obj.name);
                    }case "time" -> {
                        ddl.append(obj.name).append(" time comment '").append(obj.label).append("',");
                        listFields.add(obj.name);
                    }case "double", "percent", "currency" -> {
                        ddl.append(obj.name).append(" double precision comment '").append(obj.label).append("',");
                        listFields.add(obj.name);
                    }case "int" -> {
                        ddl.append(obj.name).append(" int comment '").append(obj.label).append("',");
                        listFields.add(obj.name);
                    }
                }
            }

            ddl.deleteCharAt(ddl.length() - 1);
            ddl.append("); ");

        }catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }

        returnMap.put("mapType", mapType);

        // 테이블 만들기
        etlRepository.setFieldDef(ddl.toString());

        // 데이터 쿼리 및 동적 Insert into 구성용
        StringBuilder soql = new StringBuilder();

        // PushTopic은 large TextArea 지원 안함 ㅡㅡ
        StringBuilder soqlForPushTopic = new StringBuilder();

        for (String field : listFields) {
            soql.append(field).append(",");

            if(!mapType.get(field).equals("textarea")){
                soqlForPushTopic.append(field).append(",");
            }
        }

        soqlForPushTopic.deleteCharAt(soqlForPushTopic.length() - 1);
        soql.deleteCharAt(soql.length() - 1);

        returnMap.put("soqlForPushTopic", soqlForPushTopic);

        String query = "SELECT Id, " + soql + " FROM " + selectedObject;

        request = new Request.Builder()
                .url(instanceUrl + "/services/data/v61.0/query/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8))
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        try(Response response = client.newCall(request).execute()) {

            rootNode = objectMapper.readTree(Objects.requireNonNull(response.body()).string());
            JsonNode records = rootNode.get("records");

            String upperQuery = "Insert Into config." + selectedObject + "(sfid, " + soql + ") " + "values";

            // 객체로 넣을려면 dto가 필요한데. 문자열로 풀어서 넣는것 말고 다른 방법 없을까.

            List<String> listUnderQuery = new ArrayList<>();

            StringBuilder underQuery;
            for (JsonNode record : records) {

                underQuery = new StringBuilder();
                underQuery.append("(").append(record.get("Id")).append(",");

                for (String field : listFields) {

                    if(mapType.get(field).equals("datetime")){
                        underQuery.append(record.get(field).toString().replace(".000+0000","").replace("T"," ")).append(",");
                    }else if(mapType.get(field).equals("time")){
                        underQuery.append(record.get(field).toString().replace("Z","")).append(",");
                    }else{
                        underQuery.append(record.get(field)).append(",");
                    }

                }

                underQuery.deleteCharAt(underQuery.length() - 1);
                underQuery.append(")");

                listUnderQuery.add(String.valueOf(underQuery));
            }

            Instant start = Instant.now();

            int insertedData = etlRepository.insertObject(upperQuery, listUnderQuery);

            Instant end = Instant.now();
            Duration interval = Duration.between(start, end);

            long hours = interval.toHours();
            long minutes = interval.toMinutesPart();
            long seconds = interval.toSecondsPart();

            System.out.println("테이블 : " + selectedObject + ". 삽입된 데이터 수 : " + insertedData + ". 소요시간 : " + hours + "시간 " + minutes + "분 " + seconds + "초");

        }catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return returnMap;
    }

    @SneakyThrows
    @Override
    public String setPushTopic(Map<String, String> mapProperty, String token, String soql) {

        String selectedObject = mapProperty.get("selectedObject");
        String instanceUrl = mapProperty.get("instanceUrl");

        // 하나의 String만 만들면 되므로 StringBuilder 안쓸꺼
        String pushTopic = "PushTopic pushTopic = new PushTopic();" + "pushTopic.Query = 'SELECT Id, " +
                soql + " FROM " + selectedObject + "';" +
                "pushTopic.Name = '" + selectedObject + "';" +
                "pushTopic.ApiVersion = " + 61.0 + ";" +
                "pushTopic.NotifyForOperationCreate = " + true + ";" +
                "pushTopic.NotifyForOperationUpdate = " + true + ";" +
                "pushTopic.NotifyForOperationUndelete = " + true + ";" +
                "pushTopic.NotifyForOperationDelete = " + true + ";" +
                "insert pushTopic;";

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(instanceUrl + "/services/data/v61.0/tooling/executeAnonymous/?anonymousBody=" + URLEncoder.encode(pushTopic,StandardCharsets.UTF_8))
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String returnMsg = "";
        try(Response response = client.newCall(request).execute()) {
            JsonNode rootNode = objectMapper.readTree(Objects.requireNonNull(response.body()).string());
            returnMsg = rootNode.get("success").asText();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

        return returnMsg;
    }

    @Override
    public void subscribePushTopic(
            Map<String, String> mapProperty,
            String token,
            Map<String, Object> mapType
    ) throws Exception {

        String selectedObject = mapProperty.get("selectedObject");

        // access token 을 직접 넣을수가 없군
        SalesforceComponent sfEcology = new SalesforceComponent();
        sfEcology.setLoginUrl(mapProperty.get("loginUrl"));
        sfEcology.setClientId(mapProperty.get("client_id"));
        sfEcology.setClientSecret(mapProperty.get("client_secret"));
        sfEcology.setUserName(mapProperty.get("username"));
        sfEcology.setPassword(mapProperty.get("password"));
        sfEcology.setPackages("com.apache.sfdc.router.dto");

        ObjectMapper objectMapper = new ObjectMapper();

        RouteBuilder rb = new RouteBuilder() {
            @Override
            public void configure() {
                System.out.println("실행");

                from("sf:subscribe:" + selectedObject)
                        .process(exchange -> {
                            Message message = exchange.getIn();

                            // 오는 message의 형식이 일단 JSON이 아님. 그리고 Id도 바꿔줄 핑교가 있고
                            Map<String, Object> mapParam = objectMapper.convertValue(message.getBody(), Map.class);
                            mapParam.put("sfid", mapParam.get("Id"));
                            mapParam.remove("Id");

                            mapType.put("sfid","Id");

                            // JsonNode로 처리해야 쌍따옴표로 처리하기 편함. 위에 데이터 부을때 코드랑 리팩토링해서 합치기도 편하고
                            JsonNode rootNode = objectMapper.valueToTree(mapParam);

                            StringBuilder soql = new StringBuilder();

                            List<String> listUnderQuery = new ArrayList<>();

                            StringBuilder underQuery = new StringBuilder("(");

                            // 이미 mapType이 있으므로 여기선 한번에 처리. 한번에 처리 하므로 순서 맞음
                            rootNode.fields().forEachRemaining(field -> {
                                String fieldName = field.getKey();
                                JsonNode fieldValue = field.getValue();

                                soql.append(fieldName).append(",");

                                if(mapType.get(fieldName).equals("datetime") && fieldValue != null){
                                    // 애는 왜 soql 갈길떄랑 다르게오냐 ㅡㅡ
                                    underQuery.append(fieldValue.toString().replace(".000Z","").replace("T"," ")).append(",");
                                }else if(mapType.get(fieldName).equals("time") && fieldValue != null){
                                    underQuery.append(fieldValue.toString().replace("Z","")).append(",");
                                }else{
                                    underQuery.append(fieldValue).append(",");
                                }
                            });

                            underQuery.deleteCharAt(underQuery.length() - 1);
                            underQuery.append(")");

                            listUnderQuery.add(String.valueOf(underQuery));

                            soql.deleteCharAt(soql.length() - 1);

                            String upperQuery = "Insert Into config." + selectedObject + "(" + soql + ") " + "values";

                            Instant start = Instant.now();

                            int insertedData = etlRepository.insertObject(upperQuery, listUnderQuery);

                            Instant end = Instant.now();
                            Duration interval = Duration.between(start, end);

                            long hours = interval.toHours();
                            long minutes = interval.toMinutesPart();
                            long seconds = interval.toSecondsPart();

                            System.out.println("테이블 : " + selectedObject + ". 삽입된 데이터 수 : " + insertedData + ". 소요시간 : " + hours + "시간 " + minutes + "분 " + seconds + "초");

                        })
                        .log("${body}");
            }
        };

        CamelContext myCamelContext = new DefaultCamelContext();
        myCamelContext.addRoutes(rb);
        myCamelContext.addComponent("sf", sfEcology);

        try{
            myCamelContext.start();
        }catch (Exception e){
            System.out.println(e.getMessage());
            myCamelContext.close();
        }
    }
}