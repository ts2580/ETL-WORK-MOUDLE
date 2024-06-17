package com.apache.sfdc.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

@SpringBootApplication
public class SalesforceDTOGenerator {

    private static final String INSTANCE_URL = "https://ecologysyncmanagement-dev-ed.develop.my.salesforce.com/";

    public static void main(String[] args) throws Exception {

        OkHttpClient client = new OkHttpClient();

        String sObjectName = "Account";  // SObject 이름
        Request request = new Request.Builder()
                .url(INSTANCE_URL + "/services/data/v60.0/sobjects/" + sObjectName + "/describe")
                .addHeader("Authorization", "Bearer " + SalesforceOAuth.getAccessToken())
                .build();

        Response response = client.newCall(request).execute();

        System.out.println(response.body());

        if (response.isSuccessful()) {
            String responseBody = response.body().string();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            generateDTOClass(rootNode, sObjectName);
        } else {
            System.err.println("Failed to retrieve metadata: " + response.message());
        }
    }

    private static void generateDTOClass(JsonNode rootNode, String sObjectName) throws FileNotFoundException {
        StringBuilder classBuilder = new StringBuilder();
        classBuilder.append("package com.apache.sfdc.router.dto;\n\n");
        classBuilder.append("import lombok.Data;\n\n");
        classBuilder.append("@Data \n");
        classBuilder.append("public class ").append(sObjectName).append(" {\n");

        JsonNode fields = rootNode.get("fields");
        for (JsonNode field : fields) {
            String fieldName = field.get("name").asText();
            String fieldType = field.get("type").asText();
            String javaType = mapToJavaType(fieldType);

            classBuilder.append("    private ").append(javaType).append(" ").append(fieldName).append(";\n");
        }

        classBuilder.append("}\n");

        // 출력 디렉토리 생성
        File dir = new File("src/main/java/com/apache/sfdc/router/dto");
        boolean result;
        if (!dir.exists()) {
            result = dir.mkdirs();
            System.out.println("기존 경로 존재 여부 : " + result);
        }

        // 파일 생성
        File file = new File(dir, sObjectName + ".java");
        try (PrintWriter out = new PrintWriter(file)) {
            out.print(classBuilder);
            System.out.println("DTO class 경로: " + file.getAbsolutePath());
        }
    }

    private static String mapToJavaType(String fieldType) {
        
        // 타입 더 쪼개기
        return switch (fieldType) {
            case "string" -> "String";
            case "boolean" -> "Boolean";
            case "int" -> "Integer";
            case "double" -> "Double";
            // 추가적인 필드 타입 매핑
            default -> "String";
        };
    }

}
