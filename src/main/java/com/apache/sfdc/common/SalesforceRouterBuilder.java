package com.apache.sfdc.common;

import com.apache.sfdc.router.repository.ETLRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
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
                /******리스트로 변경*******/
                // 메시지들을 5초 동안 모아서 리스트로 처리할거임..Thread.sleep 안쓰도록
                .aggregate(constant(true), new AggregationStrategy() {
                    @Override
                    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                        //Exception in thread "Camel (camel-2) thread #7 - Aggregator" java.lang.StackOverflowError
                        // 여기서 잘 처리해줘야댐 - 문서 : https://camel.apache.org/components/4.4.x/eips/aggregate-eip.html
                        List<Object> list;
                        if (oldExchange == null) {
                            list = new ArrayList<>();
                            list.add(newExchange.getIn().getBody());
                            newExchange.getIn().setBody(list);
                            return newExchange;
                        } else {
                            list = oldExchange.getIn().getBody(List.class);
                            list.add(newExchange.getIn().getBody());
                            return oldExchange;
                        }
                    }
                })
                .completionInterval(5000) // 5초 동안 메시지를 모음
                .process((exchange) -> {

                    // 메세지는 단건으로 옴
//                    Message message = exchange.getIn();

                    /******리스트로 변경*******/
                    // List<Message>로 받으면 에러남.. Object로 받자
                    List<Object> messageBodies = exchange.getIn().getBody(List.class);
                    List<String> listUnderQuery = new ArrayList<>();
                    StringBuilder soql = new StringBuilder();
                    // soql을 한번만 설정해주기 위한 변수
                    boolean isFirst = true;

                    ObjectMapper objectMapper = new ObjectMapper();
                    for (Object body : messageBodies) {
                        Map<String, Object> mapParam = objectMapper.convertValue(body, Map.class);
                        mapParam.put("sfid", mapParam.get("Id"));
                        mapParam.remove("Id");

                        JsonNode rootNode = objectMapper.valueToTree(mapParam);
                        StringBuilder underQuery = new StringBuilder("(");

                        // soql은 한번만 설정하기 (Insert에 들어갈 필드임)
                        if (isFirst) {
                            rootNode.fields().forEachRemaining(field -> {
                                String fieldName = field.getKey();
                                soql.append(fieldName).append(",");
                            });
                            isFirst = false;
                        }

                        rootNode.fields().forEachRemaining(field -> {
                            String fieldName = field.getKey();
                            JsonNode fieldValue = field.getValue();

                            if (mapType.get(fieldName).equals("datetime") && fieldValue != null) {
                                underQuery.append(fieldValue.toString().replace(".000Z", "").replace("T", " ")).append(",");
                            } else if (mapType.get(fieldName).equals("time") && fieldValue != null) {
                                underQuery.append(fieldValue.toString().replace("Z", "")).append(",");
                            } else {
                                underQuery.append(fieldValue).append(",");
                            }
                        });

                        underQuery.deleteCharAt(underQuery.length() - 1);
                        underQuery.append(")");
                        listUnderQuery.add(String.valueOf(underQuery));

                    }

                    soql.deleteCharAt(soql.length() - 1);

                    String upperQuery = "Insert Into config." + selectedObject + "(" + soql + ") " + "values";

                    Instant start = Instant.now();

                    int insertedData = etlRepository.insertObject(upperQuery, listUnderQuery);

                    Instant end = Instant.now();
                    Duration interval = Duration.between(start, end);

                    long hours = interval.toHours();
                    long minutes = interval.toMinutesPart();
                    long seconds = interval.toSecondsPart();

                    System.out.println("=====================================SalesforceRouterBuilder=====================================");
                    System.out.println("테이블 : " + selectedObject + ". 삽입된 데이터 수 : " + insertedData + ". 소요시간 : " + hours + "시간 " + minutes + "분 " + seconds + "초");
                });
    }
}
