package fi.aalto.cs.apluscourses.intellij.utils;

import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.updateSettings.impl.UpdateSettings;
import com.intellij.openapi.util.io.FileUtilRt;
import fi.aalto.cs.apluscourses.model.UnexpectedResponseException;
import fi.aalto.cs.apluscourses.utils.CoursesClient;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.jetbrains.annotations.NotNull;

public class SettingsUtil {

  private SettingsUtil() {

  }

  /**
   * Adds startup actions, which unzip the given ZIP file in the IntelliJ IDEA configuration
   * directory and delete the given ZIP file. The IDE is also configured to check for configuration
   * updates on restart. The IDE must be restarted for the given IDE settings to take effect. It's
   * important to ensure that the file exists when the IDE is restarted, since that's when the file
   * gets unzipped.
   * @param file A ZIP file containing the IDE settings. Such a file can be created using IDEA's
   *             "Export Settings" feature.
   * @throws IOException If an IO error occurs.
   */
  public static void importIdeSettings(@NotNull File file) throws IOException {
    String configPath = FileUtilRt.toSystemIndependentName(PathManager.getConfigPath());
    StartupActionScriptManager.addActionCommands(
        Arrays.asList(
            new StartupActionScriptManager.UnzipCommand(file, new File(configPath)),
            new StartupActionScriptManager.DeleteCommand(file)
        )
    );
    UpdateSettings.getInstance().forceCheckForUpdateAfterRestart();
  }

  /**
   * Creates a temporary file and downloads the settings ZIP file from the given URL to the
   * temporary file. The IDE settings are then imported from the temporary file using {@link
   * SettingsUtil#importIdeSettings(File)}.
   * @param settingsUrl The URL from which the IDE settings ZIP file is downloaded.
   * @throws IOException                 If an IO error occurs.
   * @throws UnexpectedResponseException If the request made to the URL results in an unexpected
   *                                     response. This usually indicates an error in the course
   *                                     configuration.
   */
  public static void importIdeSettings(@NotNull URL settingsUrl)
      throws IOException, UnexpectedResponseException {
    File tempFile = FileUtilRt.createTempFile("course-ide-settings", ".zip");
    CoursesClient.fetchZip(settingsUrl, tempFile);
    importIdeSettings(tempFile);
  }

}
