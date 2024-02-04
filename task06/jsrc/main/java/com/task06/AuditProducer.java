package com.task06;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.xspec.M;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.LambdaSnapStart;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@LambdaHandler(lambdaName = "audit_producer",
        roleName = "audit_producer-role",
        runtime = DeploymentRuntime.JAVA11,
        snapStart = LambdaSnapStart.PublishedVersions)

@DynamoDbTriggerEventSource(targetTable = "Configuration", batchSize = 1)

public class AuditProducer implements RequestHandler<DynamodbEvent, Void> {
    private static final String AUDIT_TABLE_NAME = "cmtr-a655d43a-Audit-test";

    @Override
    public Void handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        for (DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord : dynamodbEvent.getRecords()) {
            if ("INSERT".equals(dynamodbStreamRecord.getEventName()) || "MODIFY".equals(dynamodbStreamRecord.getEventName())) {
                Map<String, AttributeValue> newImage = dynamodbStreamRecord.getDynamodb().getNewImage();

                if (newImage != null && newImage.containsKey("key") && newImage.containsKey("value")) {
                    String itemKey = newImage.get("key").getS();
                    String newValue = newImage.get("value").getS();

                    if ("INSERT".equals(dynamodbStreamRecord.getEventName())) {
                        createAuditEntry(itemKey, newValue, null);
                    } else if ("MODIFY".equals(dynamodbStreamRecord.getEventName())) {
                        Map<String, AttributeValue> oldImage = dynamodbStreamRecord.getDynamodb().getOldImage();
                        String oldValue = oldImage.get("value").getS();

                        if (!oldValue.equals(newValue)) {
                            createAuditEntry(itemKey, newValue, oldValue);
                        }
                    }
                }
            }
        }
        return null;
    }

    private void createAuditEntry(String itemKey, String newValue, String oldValue) {
        DynamoDB dynamoDBClient = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());

        Table auditTable = dynamoDBClient.getTable(AUDIT_TABLE_NAME);

        Item auditItem = new Item()
                .withPrimaryKey("id", java.util.UUID.randomUUID().toString())
                .withString("itemKey", itemKey)
                .withString("modificationTime", Instant.now().toString());

        if (oldValue != null) {
            auditItem.withString("updatedAttribute", "value")
                    .with("oldValue", oldValue)
                    .with("newValue", newValue);
        } else {
            Map<String, String> map = new HashMap<>();
            map.put("key", itemKey);
            map.put("value", newValue);
            auditItem.withMap("newValue", map);
        }
        auditTable.putItem(auditItem);
    }
}
