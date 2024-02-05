package com.task07;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static sun.plugin2.util.PojoUtil.toJson;

@LambdaHandler(lambdaName = "uuid_generator",
        roleName = "uuid_generator-role"
)

@RuleEventSource(targetRule = "uuid_trigger")

public class UuidGenerator implements RequestHandler<Object, String> {

    private static final String BUCKET_NAME = "cmtr-a655d43a-uuid-storage-test";

    @Override
    public String handleRequest(Object input, Context context) {
        String fileName = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("ids", generateRandomUuids(10));

        String content = toJson(jsonMap);

        uploadToS3(fileName, content);

        return "UUIDs uploaded to S3 successfully!";
    }

    private List<String> generateRandomUuids(int count) {
        List<String> uuids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            uuids.add(UUID.randomUUID().toString());
        }
        return uuids;
    }

    private void uploadToS3(String fileName, String content) {
        S3Client s3Client = S3Client.builder().build();

        // Upload the content to S3 bucket
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(fileName)
                        .build(),
                RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8)));

        // Close the S3 client
        s3Client.close();
    }
}
