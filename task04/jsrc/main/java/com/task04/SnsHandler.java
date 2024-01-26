package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.syndicate.deployment.annotations.events.SnsEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@LambdaHandler(lambdaName = "sns_handler",
        roleName = "sns_handler-role"
)
@SnsEventSource(targetTopic = "lambda_topic")
public class SnsHandler implements RequestHandler<SNSEvent, Map<String, Object>> {

    public Map<String, Object> handleRequest(SNSEvent snsEvent, Context context) {
        if (Objects.nonNull(snsEvent)) {
            for (SNSEvent.SNSRecord snsMessage : snsEvent.getRecords()) {
                String messageId = snsMessage.getSNS().getMessageId();
                String message = snsMessage.getSNS().getMessage();
                System.out.println("Received SNS message - MessageId: " + messageId + ", Message: " + message);
            }
        }
        return null;
    }
}
