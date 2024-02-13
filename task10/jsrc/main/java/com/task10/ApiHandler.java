package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role"
)
@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
        @EnvironmentVariable(key = "tables_table", value = "${tables_table}"),
        @EnvironmentVariable(key = "reservations_table", value = "${reservations_table}"),
        @EnvironmentVariable(key = "booking_userpool", value = "${booking_userpool}")
})

public class ApiHandler extends CommonTools implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final SignUpHandler signUpHandler;
    private final SignInHandler signInHandler;

    public ApiHandler() {
        final String userPoolName = System.getenv("booking_userpool");
        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder().region(Region.of(System.getenv("region"))).build();
        signUpHandler = new SignUpHandler(cognitoClient, userPoolName);
        signInHandler = new SignInHandler(cognitoClient, userPoolName);
    }


    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String httpMethod = request.getHttpMethod();
        switch (httpMethod) {
            case "POST":
                return handlePostRequest(request, context);
            case "GET":
                return handleGetRequest(request);
            default:
                return get400InvalidResponse();
        }
    }

    private APIGatewayProxyResponseEvent handleGetRequest(APIGatewayProxyRequestEvent request) {
        String resource = request.getResource();
        switch (resource) {
            case "/tables/{tableId}":
                return getTable(request);
            case "/tables":
                return getTablesValues(System.getenv("tables_table"));
            case "/reservations":
                return getTablesValues(System.getenv("reservations_table"));
            default:
                return get400InvalidResponse();
        }
    }

    private APIGatewayProxyResponseEvent getTable(APIGatewayProxyRequestEvent request) {
        String tableName = System.getenv("tables_table");
        Table tablesTable = dynamoDB.getTable(tableName);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            String pathParameters = request.getPathParameters().get("tableId");
            int tableId = Integer.parseInt(pathParameters);
            Item tableItem = tablesTable.getItem("id", tableId);
            if (tableItem == null) {
                response.setStatusCode(404);
                response.setBody("Table not found");
                return response;
            }
            String responseBody = objectMapper.writeValueAsString(tableItem.asMap());

            response.setStatusCode(200);
            response.setBody(responseBody);
        } catch (NumberFormatException e) {
            response.setStatusCode(400);
            response.setBody("Invalid tableId format");
        } catch (Exception e) {
            set500Error(response);
        }
        return response;

    }

    private APIGatewayProxyResponseEvent getTablesValues(String tableName) {
        Table tablesTable = dynamoDB.getTable(tableName);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        List<Map<String, Object>> tablesList = new ArrayList<>();

        ItemCollection<ScanOutcome> items = tablesTable.scan();
        for (Item item : items) {
            Map<String, Object> tableData = item.asMap();
            tablesList.add(tableData);
        }
        try {
            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(tablesList));
        } catch (JsonProcessingException e) {
            set500Error(response);
        }
        return response;
    }

    private APIGatewayProxyResponseEvent handlePostRequest(APIGatewayProxyRequestEvent request, Context context) {
        String resource = request.getResource();
        switch (resource) {
            case "/tables":
                return createTable(request);
            case "/reservations":
                return createReservation(request);
            case "/signup":
                return signupUser(request, context);
            case "/signin":
                return signinUser(request, context);
            default:
                return get400InvalidResponse();
        }
    }

    private APIGatewayProxyResponseEvent signupUser(APIGatewayProxyRequestEvent request, Context context) {
        return signUpHandler.handleRequest(request, context);
    }

    private APIGatewayProxyResponseEvent signinUser(APIGatewayProxyRequestEvent request, Context context) {
        return signInHandler.handleRequest(request, context);
    }

    private APIGatewayProxyResponseEvent createTable(APIGatewayProxyRequestEvent request) {
        String tableName = System.getenv("tables_table");
        Table tablesTable = dynamoDB.getTable(tableName);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            String requestBody = request.getBody();
            JsonNode requestJson = objectMapper.readTree(requestBody);
            Item table = new Item().withPrimaryKey("id", requestJson.get("id").asInt())
                    .with("number", requestJson.get("number").asInt())
                    .with("places", requestJson.get("places").asInt())
                    .with("isVip", requestJson.get("isVip").asBoolean())
                    .with("minOrder", requestJson.has("minOrder") ? requestJson.get("minOrder").asInt() : 0);
            tablesTable.putItem(table);
            response.setStatusCode(200);
            response.setBody(String.format(SIMPLE_MESSAGE_JSON_TEMPLATE, "id", requestJson.get("id").asInt()));
        } catch (Exception e) {
            set500Error(response);
        }
        return response;
    }

    private APIGatewayProxyResponseEvent createReservation(APIGatewayProxyRequestEvent request) {
        String tableName = System.getenv("reservations_table");
        Table reservationTable = dynamoDB.getTable(tableName);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            String requestBody = request.getBody();
            JsonNode requestJson = objectMapper.readTree(requestBody);
            String uid = UUID.randomUUID().toString();
            Item resertvation = new Item().withPrimaryKey("reservationId", uid)
                    .withInt("tableNumber", requestJson.get("tableNumber").asInt())
                    .withString("clientName", requestJson.get("clientName").asText())
                    .withString("phoneNumber", requestJson.get("phoneNumber").asText())
                    .withString("date", requestJson.get("date").asText())
                    .withString("slotTimeStart", requestJson.get("slotTimeStart").asText())
                    .withString("slotTimeEnd", requestJson.get("slotTimeEnd").asText());
            reservationTable.putItem(resertvation);
            response.setStatusCode(200);
            response.setBody(String.format(SIMPLE_MESSAGE_JSON_TEMPLATE, "reservationId", uid));
        } catch (Exception e) {
            set500Error(response);
        }
        return response;
    }

    private void set500Error(APIGatewayProxyResponseEvent response) {
        response.setStatusCode(500);
        response.setBody("Internal Server Error");
    }

    private APIGatewayProxyResponseEvent get400InvalidResponse() {
        System.out.println("CATCH INVALID REQUEST ------->");
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid request");
    }
}
