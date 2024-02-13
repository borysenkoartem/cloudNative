package com.task10;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.http.HttpStatus;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import java.io.IOException;


public class SignUpHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected SignUpHandler(CognitoIdentityProviderClient cognitoClient, String userPoolName) {
        super(cognitoClient, userPoolName);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try {
            SignUpRequest signUpRequest = getSignUpRequest(apiGatewayProxyRequestEvent);
            signUp(signUpRequest);
            confirmSignUp(signUpRequest);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpStatus.SC_OK)
                    .withBody(String.format(SIMPLE_MESSAGE_JSON_TEMPLATE, "message", "user has been signed up"));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpStatus.SC_BAD_REQUEST)
                    .withBody(String.format(SIMPLE_MESSAGE_JSON_TEMPLATE, "error", e.getMessage()));
        }
    }

    private SignUpRequest getSignUpRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) throws IOException {
        String jsonPayload = apiGatewayProxyRequestEvent.getBody();
        return objectMapper.readValue(jsonPayload, SignUpRequest.class);
    }
}
