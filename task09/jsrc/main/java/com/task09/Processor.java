package com.task09;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.epam.OpenMeteoAPIClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.LambdaUrlConfig;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.TracingMode;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "processor",
        roleName = "processor-role",
        layers = {"sdk-layer"},
        tracingMode = TracingMode.Active
)
@LambdaLayer(
        layerName = "sdk-layer",
        libraries = {"lib/OpenMetio-1.0.jar"},
        runtime = DeploymentRuntime.JAVA8,
        artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
        authType = AuthType.NONE,
        invokeMode = InvokeMode.BUFFERED
)
@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
        @EnvironmentVariable(key = "target_table", value = "${target_table}")
})
public class Processor implements RequestHandler<Object, Map<String, Object>> {

    private final OpenMeteoAPIClient apiClient = new OpenMeteoAPIClient();
    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> handleRequest(Object request, Context context) {
        try {
            String tableName = System.getenv("target_table");
            Table eventsTable = dynamoDB.getTable(tableName);
            String rawJsonForecast = apiClient.getWeatherForecast();
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
            };

            Object forecastMap = objectMapper.readValue(rawJsonForecast, typeRef);

            Item item = new Item().withPrimaryKey("id", UUID.randomUUID().toString())
                    .with("forecast", forecastMap);
            eventsTable.putItem(item);
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("statusCode", 200);
            responseMap.put("body", item.toJSONPretty());
            return responseMap;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
