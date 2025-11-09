package org.example;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import dev.langchain4j.service.AiServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static chat.giga.model.Scope.GIGACHAT_API_PERS;

public class AppGigaResponseFormat {
    private static Logger log = LogManager.getLogger(AppGigaResponseFormat.class);

    public record Person(String name, int age, double height, boolean married) {
    }

    interface PersonExtractor {

        Person extractPersonFrom(String text);
    }

    public static void main(String[] args) {

//        JsonSchema jsonSchema = JsonSchema.builder()
//                .name("Person") // OpenAI requires specifying the name for the schema
//                .rootElement(JsonObjectSchema.builder() // see [1] below
//                        .addStringProperty("name")
//                        .addIntegerProperty("age")
//                        .addNumberProperty("height")
//                        .addBooleanProperty("married")
//                        .required("name", "age", "height", "married") // see [2] below
//                        .build())
//                .build();

        GigaChatChatModel model = GigaChatChatModel.builder()
                .verifySslCerts(false)
                .authClient(AuthClient.builder().withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .authKey("MmZhNTA2OTYtNzUzZC00NWY1LWFkMGItYmY0YjczZjI1MzBjOmM0ZmRhNjFlLWI5YmYtNDVmZS1iOGRmLWZhYzU3MThmYTUyNQ==")
                                .scope(GIGACHAT_API_PERS)
                                .build())
                        .build())
                .logRequests(true)
                .logResponses(true)
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        //.responseFormat(ResponseFormat.JSON)
                        //.responseFormat(jsonSchema)
                        .modelName(ModelName.GIGA_CHAT_2)
                        .build())
                .build();

//        ResponseFormat responseFormat = ResponseFormat.builder()
//                .type(JSON) // type can be either TEXT (default) or JSON
//                .jsonSchema(JsonSchema.builder()
//                        .name("Person") // OpenAI requires specifying the name for the schema
//                        .rootElement(JsonObjectSchema.builder() // see [1] below
//                                .addStringProperty("name")
//                                .addIntegerProperty("age")
//                                .addNumberProperty("height")
//                                .addBooleanProperty("married")
//                                .required("name", "age", "height", "married") // see [2] below
//                                .build())
//                        .build())
//                .build();

        String message = """
                Джону 42 года, и он живет независимой жизнью.
                                Его рост составляет 1,75 метра, и он держится уверенно.
                                В настоящее время он не женат и может свободно сосредоточиться на своих личных целях и интересах.
                """;

        PersonExtractor personExtractor = AiServices.create(PersonExtractor.class, model);
        Person person = personExtractor.extractPersonFrom(message);
        log.info(person.toString());

    }

}
