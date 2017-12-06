import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.collect.*;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by nikita on 10.07.17.
 */
public class App {
    private static final int COUNT = 100_000;
    private static final Pattern PATTERN = Pattern.compile("KAFKA-\\d+");
    private static final String ROOT_FOLDER = "where you checked out Kafka";
    private static final String JIRA_URL = "https://issues.apache.org/jira";
    private static final String ISSUES_CACHE_FILE = "issues.txt";


    public static void main(String[] args) throws IOException, GitAPIException, JiraException {
        Cache<String, String> issueCache = IOUtils.loadCache(ISSUES_CACHE_FILE);
        Stopwatch timer = Stopwatch.createStarted();

        ArrayListMultimap<String, String> r = getPerFileIssuesMap();
        IOUtils.saveToFile(r);
        HashSet<String> allIds = Sets.newHashSet(r.values());
        Set<String> strings = Sets.difference(allIds, issueCache.asMap().keySet()).immutableCopy();


        //ONLINE
//        List<Issue> bug = pullJiraData(strings)
//                .stream()
//                .filter(issue -> issue.getIssueType().getName().equalsIgnoreCase("bug")).collect(Collectors.toList());
//        Save cache for offline
//        IOUtils.saveJiraIssuesCache(bug,ISSUES_CACHE_FILE);
//        List<String> bugsFromJira = bug.stream()
//                .map(issue -> issue.getKey()).collect(Collectors.toList());


        //OFFLINE
        List<String> bugsFromJira = issueCache.asMap().keySet().stream().collect(Collectors.toList());


        Set<String> allBugIds = Sets.newHashSet(issueCache.asMap().keySet());
        allBugIds.addAll(bugsFromJira);


        List<Map.Entry<String, IssueRatio>> ranked = transformData(r, allBugIds);
        ranked.stream().filter(e -> e.getValue().getBugCount() > 10).forEach(e -> System.out.println(e.getKey() + " >> " + e.getValue().toString()));
        long time = timer.stop().elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Done! " + String.valueOf(time) + " millis");

    }

    private static List<Map.Entry<String, IssueRatio>> transformData(ArrayListMultimap<String, String> r, Set<String> allBugIds) {
        Map<String, IssueRatio> zzz = Maps.transformValues(r.asMap(), input -> {
            HashSet<String> perFileIssues = Sets.newHashSet(input);
            int bugsInFile = Sets.intersection(allBugIds, perFileIssues).size();
            return new IssueRatio(perFileIssues.size(), bugsInFile);
        });


        return zzz.entrySet().stream().sorted((o1, o2) -> -1*(Double.compare(o1.getValue().getBugCount(), o2.getValue().getBugCount()))).collect(Collectors.toList());
    }

    private static ArrayListMultimap<String, String> getPerFileIssuesMap() throws IOException, GitAPIException {
        Git gr = Git.open(new File(ROOT_FOLDER));
        Repository repo = gr.getRepository();
        Iterable<RevCommit> commits = gr.log().setMaxCount(COUNT).call();
        List<RevCommit> cc = new ArrayList<>();
        Iterables.addAll(cc, commits);
        System.out.println("Commits total " + cc.size());
        // Map <File - List of Issues >
        return groupCommitsByFiles(gr, repo, cc);
    }


    private static List<Issue> pullJiraData(Set<String> uniqueIssueIds) throws JiraException {
        JiraClient jiraClient = new JiraClient(JIRA_URL);
        List<Issue> all = Lists.newArrayList();
        List<List<String>> partition = Lists.partition(Lists.newArrayList(uniqueIssueIds), 50);
        int pSize = partition.size();
        System.out.println("Partitions : " + pSize);
        int counter = 0;
        for (List<String> batch : partition) {
            List<Issue> searchResult = JiraHelper.getSearchResult(jiraClient, batch);
            all.addAll(searchResult);
            System.out.println("Partition " + String.valueOf(++counter) + " of " + pSize);
        }
        return all;

    }


    private static Optional<String> findIssueId(String sm) {
        Matcher matcher = PATTERN.matcher(sm);
        boolean b = matcher.find();
        if (b) return Optional.ofNullable(matcher.group());
        return Optional.empty();
    }


    private static ArrayListMultimap<String, String> groupCommitsByFiles(Git gr, Repository repo, List<RevCommit> cc) throws IOException, GitAPIException {
        int notFoundInComments = 0;
        ArrayListMultimap<String, String> perFile = ArrayListMultimap.<String, String>create();
        for (int i = 0; i < cc.size() - 1; i++) {
            RevCommit revCommit = cc.get(i);
            ObjectId head = revCommit.getTree().getId();
            RevCommit pCommit = cc.get(i + 1);
            ObjectId previousHead = pCommit.getTree().getId();
            ObjectReader reader = repo.newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, previousHead);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, head);

            Optional<String> s = findIssueId(pCommit.getShortMessage());


            if (s.isPresent()) {
                List<DiffEntry> call = gr.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call();
                for (DiffEntry diff : call) {
                    String path = IOUtils.getPath(diff);
                    File file = new File(ROOT_FOLDER, path);
                    String absolutePath = file.getAbsolutePath();
                    boolean targetFileExtensionDetected = absolutePath.endsWith(".scala") || absolutePath.endsWith(".java");
                    boolean isTest = absolutePath.contains("/src/test/");
                    boolean exists = file.exists();
                    boolean criteria = exists && targetFileExtensionDetected && !isTest;
                    if (criteria) {
                        perFile.put(path, s.get());

                    }
                }
            } else {
                notFoundInComments++;

            }
        }
        System.out.println("Commits without issues " + notFoundInComments);
        return perFile;
    }


}
