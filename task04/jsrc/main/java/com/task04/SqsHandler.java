package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.events.SqsTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import java.util.Map;
import java.util.Objects;

@LambdaHandler(lambdaName = "sqs_handler",
	roleName = "sqs_handler-role"
)
@SqsTriggerEventSource(targetQueue = "async_queue", batchSize = 1)

public class SqsHandler implements RequestHandler<SQSEvent, Map<String, Object>> {


	public Map<String, Object> handleRequest(SQSEvent sqsEvent, Context context) {
		if (Objects.nonNull(sqsEvent)) {
			for (SQSEvent.SQSMessage sqsMessage : sqsEvent.getRecords()) {
				String messageId = sqsMessage.getMessageId();
				String body = sqsMessage.getBody();
				System.out.println("Received SQS message - MessageId: " + messageId + ", Body: " + body);
			}
		}
		return null;
	}
}
