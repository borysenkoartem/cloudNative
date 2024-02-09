package com.task09;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.epam.OpenMeteoAPIClient;
import com.syndicate.deployment.annotations.LambdaUrlConfig;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.TracingMode;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

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
public class Processor implements RequestHandler<Object, Map<String, Object>> {

	private static final String WEATHER_TABLE = "cmtr-a655d43a-Weather-test";
	private final OpenMeteoAPIClient apiClient = new OpenMeteoAPIClient();

	public Map<String, Object> handleRequest(Object request, Context context) {
		DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
		Table eventsTable = dynamoDB.getTable(WEATHER_TABLE);
		Item item = new Item().withPrimaryKey("id", UUID.randomUUID().toString())
				.with("forecast", apiClient.getWeatherForecast());
		eventsTable.putItem(item);
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("statusCode", 200);
		responseMap.put("forecast", item.toJSONPretty());
		return responseMap;
	}
}
