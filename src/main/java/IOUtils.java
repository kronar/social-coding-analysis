import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import net.rcarz.jiraclient.Issue;
import org.eclipse.jgit.diff.DiffEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public final class IOUtils {
    private static final Splitter SPLITTER = Splitter.on(":");

    private IOUtils() {
    }

    public static Cache<String, String> loadCache(String cacheFilePath) throws IOException {
        File cacheFile = new File(cacheFilePath);
        if (cacheFile.exists()) {
            Cache<String, String> cache = CacheBuilder.<String, String>newBuilder().build();
            List<String> strings = Files.readLines(cacheFile, Charsets.UTF_8);
            for (String string : strings) {
                Iterable<String> split = SPLITTER.split(string);
                String key = Iterables.get(split, 0);
                String priority = Iterables.get(split, 1);
                cache.put(key, priority);
            }
            return cache;
        } else {
            return CacheBuilder.newBuilder().build();
        }
    }

    public static void saveJiraIssuesCache(List<Issue> bugsOnly, String cacheFilePath) throws IOException {
        FileWriter fw = new FileWriter(new File(cacheFilePath), false);
        for (Issue issue : bugsOnly) {
            fw.write(issue.getKey() + ":" + issue.getPriority().getName() + System.lineSeparator());
        }
        fw.flush();
        fw.close();
    }

    public static void saveToFile(ArrayListMultimap<String, String> r) throws IOException {

        FileWriter fileWriter = new FileWriter("out.log");

        r.asMap().entrySet().stream().sorted((o1, o2) -> -1 * Integer.compare(o1.getValue().size(), o2.getValue().size())).forEach(e -> {
            try {
                fileWriter.write(e.getKey() + " >> " + Joiner.on(',').join(e.getValue()) + "\n");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        fileWriter.flush();
        fileWriter.close();
    }


    public static String getPath(DiffEntry diff) {
        String oldPath = diff.getOldPath();
        String newPath = diff.getNewPath();
        return oldPath.equals(newPath) ? oldPath : newPath;
    }

}
