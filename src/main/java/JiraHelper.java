import com.google.common.collect.Lists;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class JiraHelper {
    private static final int BATCH_SIZE = 50;
    private static final String JQL_BATCH_TEMPLATE = "project=Kafka and key in (%s)";
    private static final String DELIMITER = ",";
    private static final String BAD_STATUS_CODE = "400";

    private JiraHelper() {
    }

    public static List<Issue> getSearchResult(JiraClient jiraClient, List<String> batch) {
        List<Issue> result = Lists.newArrayList();
        try {
            String concatenatedKeys = batch.stream().collect(Collectors.joining(DELIMITER));
            String jql = String.format(JQL_BATCH_TEMPLATE, concatenatedKeys);
            Issue.SearchResult searchResult = jiraClient.searchIssues(jql, BATCH_SIZE);
            result.addAll(searchResult.issues);
            return result;
        } catch (JiraException e) {
            // Some issue was deleted
            if (e.getMessage().contains(BAD_STATUS_CODE)) {
                System.out.println("Single-ticket query mode. Please wait! ");
                for (String s : batch) {
                    Optional<Issue> opt = execJqlSingleTicketRequest(jiraClient, s);
                    if (opt.isPresent()) {
                        result.add(opt.get());
                    } else {
                        System.err.println("Cant find issue with key " + s);
                    }
                }


            }
        }
        return result;
    }

    public static Optional<Issue> execJqlSingleTicketRequest(JiraClient jiraClient, String s) {
        try {
            String jql = String.format("project=Kafka and key=%s", s);
            Issue.SearchResult searchResult = jiraClient.searchIssues(jql);
            return Optional.of(searchResult.issues.get(0));
        } catch (JiraException e) {
            return Optional.empty();
        }
    }
}
