package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.LambdaSnapStart;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
        runtime = DeploymentRuntime.JAVA11,
        snapStart = LambdaSnapStart.PublishedVersions
)
@DynamoDbTriggerEventSource(targetTable = "Events", batchSize = 1)

public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final String DYNAMO_DB_TABLE_NAME = "Events";

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        Table eventsTable = dynamoDB.getTable(DYNAMO_DB_TABLE_NAME);
        Item item = new Item().withPrimaryKey("id", UUID.randomUUID().toString())
                .with("principalId", Integer.parseInt(stringObjectMap.get("principalId").toString()))
                .with("createdAt", Instant.now().toString())
                .with("body", stringObjectMap.get("content"));
        eventsTable.putItem(item);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("statusCode", 201);
        responseMap.put("event", item.toJSONPretty());

        return responseMap;
    }
}
