import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.internal.data.CRC64;

/**
 * Reports which candidate class directory the classes in a .ec file came from.
 *
 * JaCoCo identifies a class by CRC64 of its bytes. When a report is built against
 * a directory holding different bytes for the same class name - the wrong product
 * flavour, or output taken from before a bytecode-rewriting plugin such as Hilt -
 * jacococli prints "Execution data for class X does not match" and silently drops
 * that class's coverage.
 *
 * Guessing which directory is correct wastes a rebuild per attempt. The .ec is
 * authoritative: it records the exact id of every class that ran. This compares
 * those ids against each candidate directory and reports the hit rate, so the
 * right directory can be picked from evidence.
 *
 * Usage: MatchClasses <file.ec> <classdir> [more classdirs...]
 */
public final class MatchClasses {

    public static void main(final String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: MatchClasses <file.ec> <classdir>...");
            System.exit(2);
        }

        // class id -> vm name, as recorded at runtime
        final Map<Long, String> runtime = new HashMap<>();
        try (FileInputStream in = new FileInputStream(args[0])) {
            final ExecutionDataReader reader = new ExecutionDataReader(in);
            reader.setSessionInfoVisitor(new ISessionInfoVisitor() {
                public void visitSessionInfo(final SessionInfo info) {
                    // sessions are not needed, but a visitor is mandatory
                }
            });
            reader.setExecutionDataVisitor(new IExecutionDataVisitor() {
                public void visitClassExecution(final ExecutionData data) {
                    runtime.put(data.getId(), data.getName());
                }
            });
            while (reader.read()) {
                continue;
            }
        }
        System.out.printf("runtime classes in exec: %d%n", runtime.size());

        final Set<String> runtimeNames = new HashSet<>(runtime.values());

        for (int i = 1; i < args.length; i++) {
            final Path root = Paths.get(args[i]);
            if (!Files.isDirectory(root)) {
                System.out.printf("  %-100s (missing)%n", args[i]);
                continue;
            }
            final List<Path> files = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".class"))
                        .forEach(files::add);
            }
            int idMatch = 0;
            int nameOnly = 0;
            for (final Path p : files) {
                final byte[] bytes = Files.readAllBytes(p);
                final long id = CRC64.classId(bytes);
                if (runtime.containsKey(id)) {
                    idMatch++;
                    continue;
                }
                // same class name, different bytes: this is what produces the
                // "does not match" warning
                final String rel = root.relativize(p).toString()
                        .replace('\\', '/');
                final String vmName = rel.substring(0, rel.length() - ".class".length());
                if (runtimeNames.contains(vmName)) {
                    nameOnly++;
                }
            }
            System.out.printf("  classes=%-6d id_match=%-6d name_only_mismatch=%-5d  %s%n",
                    files.size(), idMatch, nameOnly, args[i]);
        }
    }
}
