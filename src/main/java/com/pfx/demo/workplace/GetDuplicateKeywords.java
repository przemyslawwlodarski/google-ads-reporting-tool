package com.pfx.demo.workplace;

import com.beust.jcommander.Parameter;
import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v5.common.KeywordInfo;
import com.google.ads.googleads.v5.errors.GoogleAdsError;
import com.google.ads.googleads.v5.errors.GoogleAdsException;
import com.google.ads.googleads.v5.resources.AdGroup;
import com.google.ads.googleads.v5.resources.AdGroupCriterion;
import com.google.ads.googleads.v5.services.GoogleAdsRow;
import com.google.ads.googleads.v5.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v5.services.GoogleAdsServiceClient.SearchPagedResponse;
import com.google.ads.googleads.v5.services.SearchGoogleAdsRequest;
import com.pfx.demo.utils.ArgumentNames;
import com.pfx.demo.utils.CodeSampleParams;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;

public class GetDuplicateKeywords {

    private static final int PAGE_SIZE = 1_000;

    private static class GetKeywordsParams extends CodeSampleParams {

        @Parameter(names = ArgumentNames.CUSTOMER_ID, required = true)
        private Long customerId;

        @Parameter(names = ArgumentNames.AD_GROUP_ID)
        private Long adGroupId;
    }

    public static void main(String[] args) throws IOException {
        GetKeywordsParams params = new GetKeywordsParams();
        if (!params.parseArguments(args)) {

            // Either pass the required parameters for this example on the command line, or insert them
            // into the code here. See the parameter class definition above for descriptions.
            params.customerId = Long.parseLong("2323295773");

            // Optional: Specify an ad group ID to restrict search to only a given ad group.
            params.adGroupId = null;
        }

        GoogleAdsClient googleAdsClient = null;
        try {
            googleAdsClient = GoogleAdsClient.newBuilder().fromPropertiesFile().build();
        } catch (FileNotFoundException fnfe) {
            System.err.printf(
                    "Failed to load GoogleAdsClient configuration from file. Exception: %s%n", fnfe);
            System.exit(1);
        } catch (IOException ioe) {
            System.err.printf("Failed to create GoogleAdsClient. Exception: %s%n", ioe);
            System.exit(1);
        }

        try {
            new GetDuplicateKeywords().runExample(googleAdsClient, params.customerId, params.adGroupId);
        } catch (GoogleAdsException gae) {
            // GoogleAdsException is the base class for most exceptions thrown by an API request.
            // Instances of this exception have a message and a GoogleAdsFailure that contains a
            // collection of GoogleAdsErrors that indicate the underlying causes of the
            // GoogleAdsException.
            System.err.printf(
                    "Request ID %s failed due to GoogleAdsException. Underlying errors:%n",
                    gae.getRequestId());
            int i = 0;
            for (GoogleAdsError googleAdsError : gae.getGoogleAdsFailure().getErrorsList()) {
                System.err.printf("  Error %d: %s%n", i++, googleAdsError);
            }
            System.exit(1);
        }
    }

    /**
     * Runs the example.
     *
     * @param googleAdsClient the Google Ads API client.
     * @param customerId the client customer ID.
     * @param adGroupId the ad group ID for which keywords will be retrieved. If {@code null}, returns
     *     from all ad groups.
     * @throws GoogleAdsException if an API request failed with one or more service errors.
     * @throws Exception if the example failed due to other errors.
     */
    public void runExample(
            GoogleAdsClient googleAdsClient, long customerId, @Nullable Long adGroupId) {
        try (GoogleAdsServiceClient googleAdsServiceClient =
                     googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {
            String searchQuery =
                    "SELECT ad_group.id, "
                            + "ad_group_criterion.type, "
                            + "ad_group_criterion.criterion_id, "
                            + "ad_group_criterion.keyword.text, "
                            + "ad_group_criterion.keyword.match_type "
                            + "FROM ad_group_criterion "
                            + "WHERE ad_group_criterion.type = KEYWORD ";
            if (adGroupId != null) {
                searchQuery += String.format("AND ad_group.id = %d", adGroupId);
            }

            // Creates a request that will retrieve all keywords using pages of the specified page size.
            SearchGoogleAdsRequest request =
                    SearchGoogleAdsRequest.newBuilder()
                            .setCustomerId(Long.toString(customerId))
                            .setPageSize(PAGE_SIZE)
                            .setQuery(searchQuery)
                            .build();
            // Issues the search request.
            SearchPagedResponse searchPagedResponse = googleAdsServiceClient.search(request);
            // Iterates over all rows in all pages and prints the requested field values for the keyword
            // in each row.
            for (GoogleAdsRow googleAdsRow : searchPagedResponse.iterateAll()) {
                AdGroup adGroup = googleAdsRow.getAdGroup();
                AdGroupCriterion adGroupCriterion = googleAdsRow.getAdGroupCriterion();
                KeywordInfo keywordInfo = adGroupCriterion.getKeyword();
                System.out.printf(
                        "Keyword with text '%s', match type '%s', criteria type '%s', and ID %d "
                                + "was found in ad group with ID %d.%n",
                        keywordInfo.getText(),
                        keywordInfo.getMatchType(),
                        adGroupCriterion.getType(),
                        adGroupCriterion.getCriterionId(),
                        adGroup.getId());
            }
        }
    }
}