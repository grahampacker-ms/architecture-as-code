package integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bson.Document;
import org.eclipse.microprofile.config.ConfigProvider;
import org.finos.calm.domain.Adr;
import org.finos.calm.domain.AdrBuilder;
import org.finos.calm.domain.AdrContent;
import org.finos.calm.domain.AdrDecision;
import org.finos.calm.domain.AdrDecisionBuilder;
import org.finos.calm.domain.AdrLinkBuilder;
import org.finos.calm.domain.AdrOption;
import org.finos.calm.domain.AdrOptionBuilder;
import org.finos.calm.domain.AdrStatus;
import org.finos.calm.domain.NewAdr;
import org.finos.calm.domain.NewAdrBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import static integration.MongoSetup.counterSetup;
import static integration.MongoSetup.namespaceSetup;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MongoAdrIntegration {

    ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(MongoAdrIntegration.class);

    private final String TITLE = "My ADR";
    private final String PROBLEM_STATEMENT = "My problem is...";
    private final List<String> DECISION_DRIVERS = List.of("a", "b", "c");
    private final AdrOption OPTION_A = AdrOptionBuilder.builder().name("Option 1").description("optionDescription")
            .positiveConsequences(List.of("a")).negativeConsequences(List.of("b")).build();
    private final AdrOption OPTION_B = AdrOptionBuilder.builder().name("Option 2").description("optionDescription")
            .positiveConsequences(List.of("c")).negativeConsequences(List.of("d")).build();
    private final List<AdrOption> CONSIDERED_OPTIONS = List.of(OPTION_A, OPTION_B);
    private final String RATIONALE = "This is the best option";
    private final AdrDecision DECISION_OUTCOME = AdrDecisionBuilder.builder()
            .rationale(RATIONALE)
            .chosenOption(OPTION_A)
            .build();

    private final NewAdr newAdr = NewAdrBuilder.builder()
            .title(TITLE)
            .contextAndProblemStatement(PROBLEM_STATEMENT)
            .decisionDrivers(DECISION_DRIVERS)
            .consideredOptions(CONSIDERED_OPTIONS)
            .decisionOutcome(DECISION_OUTCOME)
            .links(List.of(AdrLinkBuilder.builder().rel("abc").href("http://abc.com").build()))
            .build();

    private final AdrContent adrContent = AdrContent.builderFromNewAdr(newAdr).status(AdrStatus.DRAFT).build();

    @BeforeEach
    public void setupAdrs() {
        String mongoUri = ConfigProvider.getConfig().getValue("quarkus.mongodb.connection-string", String.class);

        // Safeguard: Fail fast if URI is not set
        if(mongoUri == null || mongoUri.isBlank()) {
            logger.error("MongoDB URI is not set. Check the EndToEndResource configuration.");
            throw new IllegalStateException("MongoDB URI is not set. Check the EndToEndResource configuration.");
        }

        try(MongoClient mongoClient = MongoClients.create(mongoUri)) {
            MongoDatabase database = mongoClient.getDatabase("calmSchemas");

            if(!database.listCollectionNames().into(new ArrayList<>()).contains("adrs")) {
                database.createCollection("adrs");
                database.getCollection("adrs").insertOne(
                        new Document("namespace", "finos").append("adrs", new ArrayList<>())
                );
            }

            counterSetup(database);
            namespaceSetup(database);
        }
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @Order(1)
    void end_to_end_verify_get_with_no_architecture() {
        given()
                .when().get("/calm/namespaces/finos/adrs")
                .then()
                .statusCode(200)
                .body("values", empty());
    }

    @Test
    @Order(2)
    void end_to_end_verify_create_an_adr() throws JsonProcessingException {
        given()
                .body(objectMapper.writeValueAsString(newAdr))
                .header("Content-Type", "application/json")
                .when().post("/calm/namespaces/finos/adrs")
                .then()
                .statusCode(201)
                .header("Location", containsString("calm/namespaces/finos/adrs/1"));
    }

    @Test
    @Order(3)
    void end_to_end_verify_get_adr_revision() throws JsonProcessingException {
        Adr expectedAdr = AdrBuilder.builder()
                .namespace("finos")
                .id(1)
                .revision(1)
                .adrContent(adrContent)
                .build();

        Adr actualAdr = given()
                .when().get("/calm/namespaces/finos/adrs/1/revisions/1")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Adr.class);
        assertEquals(expectedAdr, actualAdr);
    }

    @Test
    @Order(4)
    void end_to_end_verify_get_adr() {
        Adr result = given()
                .when().get("/calm/namespaces/finos/adrs/1")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Adr.class);

        Adr expectedAdr = AdrBuilder.builder()
                .namespace("finos")
                .id(1)
                .revision(1)
                .adrContent(adrContent)
                .build();
        assertEquals(expectedAdr, result);
    }

    @Test
    @Order(5)
    void end_to_end_verify_update_an_adr() throws JsonProcessingException {
        given()
                .body(objectMapper.writeValueAsString(newAdr))
                .header("Content-Type", "application/json")
                .when().post("/calm/namespaces/finos/adrs/1")
                .then()
                .statusCode(201)
                .header("Location", containsString("calm/namespaces/finos/adrs/1"));
    }

    @Test
    @Order(6)
    void end_to_end_verify_get_revisions() {
        given()
                .when().get("/calm/namespaces/finos/adrs/1/revisions")
                .then()
                .statusCode(200)
                .body("values", hasSize(2))
                .body("values[0]", equalTo(1))
                .body("values[1]", equalTo(2));

    }

    @Test
    @Order(7)
    void end_to_end_verify_update_an_adr_status() throws JsonProcessingException {
        given()
                .when().post("/calm/namespaces/finos/adrs/1/status/PROPOSED")
                .then()
                .statusCode(201)
                .header("Location", containsString("calm/namespaces/finos/adrs/1"));
    }

    @Test
    @Order(8)
    void end_to_end_verify_status_changed() throws JsonProcessingException {

        given()
                .when().get("/calm/namespaces/finos/adrs/1/revisions/3")
                .then()
                .statusCode(200)
                .body("adrContent.status", equalTo("PROPOSED"));
    }

}