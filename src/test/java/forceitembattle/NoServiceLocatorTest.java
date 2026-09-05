package forceitembattle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Nothing outside {@link ForceItemBattle} may hold a {@code ForceItemBattle}.
 *
 * <p>The plugin used to be a service locator: 84 classes took it and reached through 141
 * {@code getXManager()} calls to find their collaborators. Every one of those is now a named
 * constructor parameter, which is what makes {@code onEnable()}'s construction block a real
 * dependency order, and what lets a manager, command or menu be tested without a plugin graph
 * standing behind it.
 *
 * <p>That property is invisible: adding one {@code ForceItemBattle plugin} field compiles, passes
 * every other test, and quietly reopens the door. So it is pinned here, the same way
 * {@link CrossRepoContractTest} pins parsers it cannot import -- by reading the source, because the
 * fact under test is about what the code is allowed to say rather than about what it does.
 *
 * <p>If this fails, take the collaborator by name. If it genuinely is not built yet, take a
 * {@code Supplier} of it and say in a comment which cycle that breaks -- there are three, and
 * CLAUDE.md lists them.
 */
class NoServiceLocatorTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java/forceitembattle");

    /** A field, parameter or local of the plugin type. */
    private static final Pattern HOLDS_PLUGIN =
            Pattern.compile("\\bForceItemBattle\\s+[a-z]\\w*\\s*[;,)=]");

    private static final Pattern BLOCK_COMMENT = Pattern.compile("(?s)/\\*.*?\\*/");
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");
    private static final Pattern STRING_LITERAL = Pattern.compile("\"(\\\\.|[^\"])*\"");

    @Test
    void noClassOutsideThePluginHoldsThePlugin() throws IOException {
        List<String> offenders = new ArrayList<>();

        try (var paths = Files.walk(SOURCE_ROOT)) {
            for (Path file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals("ForceItemBattle.java")) {
                    continue;
                }
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = HOLDS_PLUGIN.matcher(stripComments(source));
                if (matcher.find()) {
                    offenders.add(SOURCE_ROOT.relativize(file) + " -> " + matcher.group().trim());
                }
            }
        }

        assertTrue(offenders.isEmpty(),
                "These hold the plugin to reach a collaborator; take it by name instead: " + offenders);
    }

    /**
     * Comments only. String literals are deliberately left in: a literal matching "the plugin type
     * followed by an identifier" does not occur, and the obvious regex for stripping them backtracks
     * catastrophically on the 1,845-line item registry.
     */
    private static String stripComments(String source) {
        String stripped = BLOCK_COMMENT.matcher(source).replaceAll("");
        return LINE_COMMENT.matcher(stripped).replaceAll("");
    }
}
