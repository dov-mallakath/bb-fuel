package com.backbase.ct.bbfuel.healthcheck;

import com.backbase.ct.bbfuel.client.common.RestClient;
import com.backbase.ct.bbfuel.client.productsummary.ArrangementsIntegrationRestClient;
import com.backbase.ct.bbfuel.client.productsummary.ProductSummaryPresentationRestClient;
import com.backbase.ct.bbfuel.data.CommonConstants;
import com.backbase.ct.bbfuel.util.GlobalProperties;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductSummaryHealthCheck {

    private final ArrangementsIntegrationRestClient arrangementsIntegrationRestClient;

    private final ProductSummaryPresentationRestClient productSummaryPresentationRestClient;

    private GlobalProperties globalProperties = GlobalProperties.getInstance();

    public void checkProductSummaryServicesHealth() {
        HealthCheck healthCheck = new HealthCheck();
        long healthCheckTimeOutInMinutes = globalProperties
            .getLong(CommonConstants.PROPERTY_HEALTH_CHECK_TIMEOUT_IN_MINUTES);

        if (healthCheckTimeOutInMinutes > 0) {
            List<RestClient> restClients = Arrays.asList(
                arrangementsIntegrationRestClient,
                productSummaryPresentationRestClient);

            healthCheck.checkServicesHealth(restClients);
        }
    }
}
