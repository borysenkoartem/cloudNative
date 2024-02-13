package com.task10;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.http.HttpStatus;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;

import java.io.IOException;

public class SignInHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected SignInHandler(CognitoIdentityProviderClient cognitoIdentityProviderClient, String userPoolName) {
        super(cognitoIdentityProviderClient, userPoolName);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try {
            SignInRequest signInRequest = getSignInRequest(apiGatewayProxyRequestEvent);
            AdminInitiateAuthResponse adminInitiateAuthResponse = signIn(signInRequest.getEmail(), signInRequest.getPassword());
            String accessToken = adminInitiateAuthResponse.authenticationResult().idToken();
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpStatus.SC_OK)
                    .withBody(String.format(SIMPLE_MESSAGE_JSON_TEMPLATE, "accessToken", accessToken));
        } catch (Exception e) {
            e.printStackTrace();
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpStatus.SC_BAD_REQUEST)
                    .withBody(String.format(SIMPLE_MESSAGE_JSON_TEMPLATE, "error", e.getMessage()));
        }
    }

    private SignInRequest getSignInRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) throws IOException {
        String jsonPayload = apiGatewayProxyRequestEvent.getBody();
        return objectMapper.readValue(jsonPayload, SignInRequest.class);
    }
}
