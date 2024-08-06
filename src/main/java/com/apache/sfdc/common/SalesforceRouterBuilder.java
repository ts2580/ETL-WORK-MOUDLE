package com.apache.sfdc.common;

import com.apache.sfdc.router.repository.ETLRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 왜 클래스로 뺐냐 :: instance 직접 선언하면 모듈화가 안됨.. 구조화가 안돼서 보기 힘들다
// 게터세터 / 기본생성자 => 생성자 주입으로 선택 .. 게터세터는 뭔가 빠질수가 있어서 생성자에서 넣어주는걸로 선택함 -> 타입과 순서 맞춰서 넣게 강제할 수 있음
// todo 숙제: 차주 화욜까지 리스트 처리 (Thread.sleep 는 무식한 방법임.. Aggregate
public class SalesforceRouterBuilder extends RouteBuilder {
    private final String selectedObject;
    private final Map<String, Object> mapType;
    private final ETLRepository etlRepository;
    public SalesforceRouterBuilder(String selectedObject, Map<String, Object> mapType, ETLRepository etlRepository) {
        this.selectedObject = selectedObject;
        this.mapType = mapType;
        this.etlRepository = etlRepository;
    }

    @Override
    public void configure() throws Exception {
        from("sf:subscribe:" + selectedObject)
                .process((exchange) -> {
                    // 메세지는 단건으로 옴
                    Message message = exchange.getIn();

                    ObjectMapper objectMapper = new ObjectMapper();

                    // 오는 message의 형식이 일단 JSON이 아님. 그리고 Id도 바꿔줄 필요가 있고
                    Map<String, Object> mapParam = objectMapper.convertValue(message.getBody(), Map.class);
                    mapParam.put("sfid", mapParam.get("Id"));
                    mapParam.remove("Id");

                    // JsonNode로 처리해야 쌍따옴표로 처리하기 편함. 위에 데이터 부을때 코드랑 리팩토링해서 합치기도 편하고
                    JsonNode rootNode = objectMapper.valueToTree(mapParam);

                    StringBuilder soql = new StringBuilder();
                    // 단건으로 오지만 repo 재활용하려고 + 숙제임
                    // Array 처리할 때 쓰임
                    List<String> listUnderQuery = new ArrayList<>();

                    StringBuilder underQuery = new StringBuilder("(");

                    // 이미 mapType이 있으므로 여기선 한번에 처리. 한번에 처리 하므로 순서 맞음
                    // forEachRemaining =>  더이상 key value 뽑을 수 없을 때까지 forEach 돌아감
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

                    // 하나밖에 없지만 담아줌
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
                });
    }
}
