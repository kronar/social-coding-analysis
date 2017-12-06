import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpringApp {


    private static final String SPRING = "where you checked out Spring Framework";
    private static final int COUNT = 100_000;
    private static final int THRESHOLD = 10;

    public static void main(String[] args) throws IOException, GitAPIException {
        Git gr = Git.open(new File(SPRING));
        Iterable<RevCommit> commits = gr.log().setMaxCount(COUNT).call();
        List<RevCommit> cc = new ArrayList<>();
        Iterables.addAll(cc, commits);
        int total = cc.size();
        System.out.println("Analyzed commits " + total);
        ArrayListMultimap<String, RevCommit> res = ArrayListMultimap.<String, RevCommit>create();


        for (RevCommit revCommit : cc) {
            String emailAddress = revCommit.getCommitterIdent().getName();
            res.put(emailAddress, revCommit);
        }


        for (String s : res.keySet()) {
            List<RevCommit> list = res.get(s);
            int size = list.size();
            int value = (size * 100 / total);
            if (value >= THRESHOLD) {
                System.out.println(s + " : " + value + " (" + size + ")");
            }
        }


    }
}
