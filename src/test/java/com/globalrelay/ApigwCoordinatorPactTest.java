package com.globalrelay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({
        PactConsumerTestExt.class})
public class ApigwCoordinatorPactTest {
    @Pact(consumer = "IaParseCoordinator", provider = "ApiGW")
    RequestResponsePact getMessagesFromAPIGW(PactDslWithProvider builder) {
        return builder.given("messages exist")
                      .uponReceiving("get all messages")
                      .method("POST")
                      //.headers(headers())
                      .path("/getMessages")
                      .willRespondWith()
                      .status(200)
                      .body("This is the getMessages pact test")
                      .toPact();
    }

    @Pact(consumer = "IaParseCoordinator", provider = "ApiGW")
    RequestResponsePact saveParsedMessagesInAPIGW(PactDslWithProvider builder) {
        return builder.given("message has been parsed")
                      .uponReceiving("get parsed message")
                      .method("POST")
                      //.headers(headers())
                      .path("/saveParsedMessage")
                      .willRespondWith()
                      .status(200)
                      .headers(responseHeaders())
                      .body(new PactDslJsonBody()
                              .stringType("type", "ok")
                              .stringType("Message", "DONE")
                           )
                      .toPact();
    }

    @Pact(consumer = "IaParseCoordinator", provider = "Parser")
    RequestResponsePact parseMessageInParser(PactDslWithProvider builder) {
        JSONObject jsonObject = new JSONObject();

        JSONArray array = new JSONArray();

        JSONObject arrayElementOne = new JSONObject();
        arrayElementOne.put("name", "Received");
        arrayElementOne.put("value", "from ex1.office.globalrelay.net ([10.6.60.10]) by ex1.office.globalrelay.net ([10.6.60.10]) with mapi; Thu, 1 Dec 2011 16:16:21 -0800");

        array.put(arrayElementOne);

        JSONObject rootObject = new JSONObject().put("serialNumber", 0)
                                                .put("from", "mang.user3 <mang.user3@globalrelay.net>")
                                                .put("to", "mang.user2 <mang.user2@globalrelay.net>")
                                                .put("errorExtractingBodyText", false)
                                                .put("headers", array);

        jsonObject.put("commonData", rootObject);


        return builder.given("message has been received")
                      .uponReceiving("get bundle messages")
                      .method("POST")
                      //.headers(headers())
                      .path("/parse")
                      .willRespondWith()
                      .status(200)
                      .headers(responseHeaders())
                      .body(jsonObject)
                      .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getMessagesFromAPIGW")
    void getAllMessages_whenMessagesExist(MockServer mockServer) {
        String baseUri = mockServer.getUrl();
        RestAssured.baseURI = baseUri;
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "multipart/form-data");
        headerMap.put("Accept-Encoding", "gzip");
        RequestSpecification httpRequest = RestAssured.given();
        ValidatableResponse response = httpRequest
                .headers(headerMap)
                .multiPart("achiveId", UUID.randomUUID().toString())
                .multiPart("bundleId", 1)
                .multiPart("maxSerialId", 1)
                .request(Method.POST, "/getMessages")
                .prettyPeek()
                .then();

        response.assertThat()
                .statusCode(200);
    }

    @Test
    @PactTestFor(pactMethod = "saveParsedMessagesInAPIGW")
    void saveMessage_whenMessageParsed(MockServer mockServer) {
        String baseUri = mockServer.getUrl();
        RestAssured.baseURI = baseUri;
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "multipart/form-data");
        headerMap.put("Accept-Encoding", "gzip,deflate");
        RequestSpecification httpRequest = RestAssured.given();
        ValidatableResponse response = httpRequest
                .headers(headerMap)
                .multiPart("achiveId", UUID.randomUUID().toString())
                .multiPart("serialId", 1)
                .multiPart("message", "test message")
                .request(Method.POST, "/saveParsedMessage")
                .prettyPeek()
                .then();

        response.assertThat()
                .statusCode(200);
    }

    @Test
    @PactTestFor(pactMethod = "parseMessageInParser")
    void parseMessage_whenMessageReceived(MockServer mockServer) {
        String baseUri = mockServer.getUrl();
        RestAssured.baseURI = baseUri;
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "multipart/form-data");
        headerMap.put("Accept-Encoding", "gzip, deflate, br");
        RequestSpecification httpRequest = RestAssured.given();
        ValidatableResponse response = httpRequest
                .headers(headerMap)
                .multiPart("message", "This is the getMessages pact test")
                .request(Method.POST, "/parse")
                .prettyPeek()
                .then();

        response.assertThat()
                .statusCode(200);
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "multipart/form-data");
        headers.put("Accept-Encoding", "gzip");
        return headers;
    }

    private Map<String, String> responseHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=UTF-8");
        headers.put("Accept-Encoding", "gzip");
        return headers;
    }

}
