package com.backbase.ct.bbfuel.service;

import static com.backbase.ct.bbfuel.data.AccessGroupsDataGenerator.generateDataGroupPostRequestBody;
import static com.backbase.ct.bbfuel.data.AccessGroupsDataGenerator.generateFunctionGroupPostRequestBody;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;

import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.ct.bbfuel.client.accessgroup.AccessGroupIntegrationRestClient;
import com.backbase.ct.bbfuel.client.accessgroup.AccessGroupPresentationRestClient;
import com.backbase.ct.bbfuel.client.accessgroup.ServiceAgreementsIntegrationRestClient;
import com.backbase.integration.accessgroup.rest.spec.v2.accessgroups.datagroups.DataGroupPostResponseBody;
import com.backbase.integration.accessgroup.rest.spec.v2.accessgroups.function.Permission;
import com.backbase.presentation.accessgroup.rest.spec.v2.accessgroups.datagroups.DataGroupsGetResponseBody;
import com.backbase.presentation.accessgroup.rest.spec.v2.accessgroups.functiongroups.FunctionGroupsGetResponseBody;
import io.restassured.response.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessGroupService {

    private final static Logger LOGGER = LoggerFactory.getLogger(AccessGroupService.class);

    private final AccessGroupPresentationRestClient accessGroupPresentationRestClient;

    private final AccessGroupIntegrationRestClient accessGroupIntegrationRestClient;

    private final ServiceAgreementsIntegrationRestClient serviceAgreementsIntegrationRestClient;

    public String ingestFunctionGroup(String externalServiceAgreementId, String functionGroupName, List<Permission> permissions) {
        Response response = accessGroupIntegrationRestClient.ingestFunctionGroup(
            generateFunctionGroupPostRequestBody(externalServiceAgreementId, functionGroupName, permissions));

        if (response.statusCode() == SC_BAD_REQUEST &&
            response.then()
                .extract()
                .as(BadRequestException.class)
                .getErrors()
                .get(0)
                .getMessage()
                .equals("Function Group with given name already exists")) {

            String internalServiceAgreementId = serviceAgreementsIntegrationRestClient
                .retrieveServiceAgreementByExternalId(externalServiceAgreementId)
                .getId();

            // Combination of function group name and service agreement is unique in the system
            FunctionGroupsGetResponseBody existingFunctionGroup = accessGroupPresentationRestClient
                .retrieveFunctionGroupsByServiceAgreement(internalServiceAgreementId)
                .stream()
                .filter(functionGroupsGetResponseBody -> functionGroupName.equals(functionGroupsGetResponseBody.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("No existing function group found by service agreement [%s] and name [%s]", externalServiceAgreementId, functionGroupName)));

            LOGGER.info("Function group \"{}\" [{}] already exists, skipped ingesting this function group",
                existingFunctionGroup.getName(), existingFunctionGroup.getId());

            return existingFunctionGroup.getId();

        } else {
            String functionGroupId = response.then()
                .statusCode(SC_CREATED)
                .extract()
                .as(DataGroupPostResponseBody.class)
                .getId();

            LOGGER.info("Function group \"{}\" [{}] ingested under service agreement [{}])",
                functionGroupName, functionGroupId, externalServiceAgreementId);

            return functionGroupId;
        }
    }

    public String ingestDataGroup(String externalServiceAgreementId, String dataGroupName,
        String type, List<String> internalArrangementIds) {
        Response response = accessGroupIntegrationRestClient.ingestDataGroup(
            generateDataGroupPostRequestBody(externalServiceAgreementId, dataGroupName, type, internalArrangementIds));

        if (response.statusCode() == SC_BAD_REQUEST &&
            response.then()
                .extract()
                .as(BadRequestException.class)
                .getErrors()
                .get(0)
                .getMessage()
                .equals("Function Group with given name already exists")) {

            String internalServiceAgreementId = serviceAgreementsIntegrationRestClient
                .retrieveServiceAgreementByExternalId(externalServiceAgreementId)
                .getId();

            // Combination of data group name and service agreement is unique in the system
            DataGroupsGetResponseBody existingDataGroup = accessGroupPresentationRestClient
                .retrieveDataGroupsByServiceAgreement(internalServiceAgreementId)
                .stream()
                .filter(dataGroupsGetResponseBody -> dataGroupName.equals(dataGroupsGetResponseBody.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("No existing data group found by service agreement [%s] and name [%s]", externalServiceAgreementId, dataGroupName)));

            return existingDataGroup.getId();

        } else {
            String dataGroupId = response.then()
                .statusCode(SC_CREATED)
                .extract()
                .as(DataGroupPostResponseBody.class)
                .getId();

            LOGGER.info("Data group \"{}\" [{}] ingested under service agreement [{}]",
                dataGroupName, dataGroupId, externalServiceAgreementId);

            return dataGroupId;
        }
    }

}
