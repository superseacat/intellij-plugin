package fi.aalto.cs.intellij;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;

/**
 * A helper class to simplify testing plugin manipulation logics.
 */
@Ignore
public class PluginsTestHelper extends BasePlatformTestCase {

  /**
   * A helper method that creates a sample {@link List} of {@link IdeaPluginDescriptor} based on the
   * test data.
   *
   * @return a {@link List} of two valid {@link IdeaPluginDescriptor}s.
   */
  @NotNull
  public static List<IdeaPluginDescriptor> getDummyPluginsListOfTwo() {
    String[] paths = {"src/test/resources/plugins/dummy_a+_plugin.xml",
        "src/test/resources/plugins/dummy_scala_plugin.xml"};
    return getDummyPluginsListOfTwo(paths);
  }

  /**
   * A helper method that creates a sample {@link List} of {@link IdeaPluginDescriptor} based on the
   * provided data.
   *
   * @param paths an array of {@link String} pointing to plugin.xml files of plugins to load.
   * @return a {@link List} of two valid {@link IdeaPluginDescriptor}s.
   */
  @NotNull
  public static List<IdeaPluginDescriptor> getDummyPluginsListOfTwo(@NotNull String[] paths) {
    return Arrays.stream(paths).map(path -> {
      try {
        return getIdeaPluginDescriptor(path);
      } catch (IOException | JDOMException e) {
        e.printStackTrace();
        return null;
      }
    }).collect(Collectors.toList());
  }

  /**
   * A helper method that creates a sample {@link IdeaPluginDescriptor} from the plugin.xml file.
   * provided data.
   *
   * @param path a {@link String} pointing to plugin.xml file of plugins to load.
   * @return a {@link List} of two valid {@link IdeaPluginDescriptor}s.
   */
  @NotNull
  public static IdeaPluginDescriptorImpl getIdeaPluginDescriptor(@NotNull String path)
      throws IOException, JDOMException {
    File filePath = new File(path);
    IdeaPluginDescriptorImpl ideaPluginDescriptor =
        new IdeaPluginDescriptorImpl(filePath, false);
    ideaPluginDescriptor.loadFromFile(filePath, null, true);
    return ideaPluginDescriptor;
  }

}
