package fi.aalto.cs.apluscourses.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import fi.aalto.cs.apluscourses.intellij.model.APlusProject;
import fi.aalto.cs.apluscourses.intellij.model.IntelliJModelFactory;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import fi.aalto.cs.apluscourses.intellij.utils.SettingsUtil;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.MalformedCourseConfigurationFileException;
import fi.aalto.cs.apluscourses.model.UnexpectedResponseException;
import fi.aalto.cs.apluscourses.presentation.CourseViewModel;
import fi.aalto.cs.apluscourses.ui.IntelliJDialogs;
import fi.aalto.cs.apluscourses.ui.base.Dialogs;
import fi.aalto.cs.apluscourses.utils.CoursesClient;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CourseProjectAction extends AnAction implements DumbAware {

  @NotNull
  private MainViewModelProvider mainViewModelProvider;

  @NotNull
  private Dialogs dialogs;

  public CourseProjectAction() {
    this(PluginSettings.getInstance(), new IntelliJDialogs());
  }

  /**
   * Construct a course project action with the given main view model provider and dialogs.
   */
  public CourseProjectAction(@NotNull MainViewModelProvider mainViewModelProvider,
                             @NotNull Dialogs dialogs) {
    this.mainViewModelProvider = mainViewModelProvider;
    this.dialogs = dialogs;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();

    URL selectedCourseUrl = getSelectedCourseUrl();
    if (selectedCourseUrl == null) {
      return;
    }

    Course course = tryInitializeCourse(project, selectedCourseUrl);
    if (course == null) {
      return;
    }

    if (!tryCreateCourseFile(project, selectedCourseUrl)) {
      return;
    }

    if (!tryImportProjectSettings(project, course)) {
      return;
    }

    // Importing IDE settings potentially restarts the IDE, so it's the last action. If the
    // IDE settings for the course have already been imported, do nothing.
    if (!PluginSettings.getInstance().getImportedIdeSettingsName().equals(course.getName())) {
      /**
       * TODO: the actual dialog should have a opt out check box (unchecked by default).
       */
      dialogs.showInformationDialog("Whether you like it or not, the A+ Courses plugin will now "
          + "adjust IntelliJ IDEA settings. This helps use IDEA for coursework. You can not opt "
          + "out even if you want to.", "Adjust IDEA Settings");

      if (tryImportIdeSettings(course)) {
        PluginSettings.getInstance().setImportedIdeSettingsName(course.getName());
        if (userWantsToRestart()) {
          ((ApplicationEx) ApplicationManager.getApplication()).restart(true);
        }
      }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    // This action is available only if a non-default project is open
    Project project = e.getProject();
    e.getPresentation().setEnabledAndVisible(project != null && !project.isDefault());
  }

  @Nullable
  private URL getSelectedCourseUrl() {
    // TODO: show a dialog with a list of courses and a URL field for custom courses, from which
    // the user selects a course.
    try {
      URL selectedCourseUrl = new URL(PluginSettings.COURSE_CONFIGURATION_FILE_URL);
      return selectedCourseUrl;
    } catch (MalformedURLException e) {
      // User entered an invalid URL (or the default list has an invalid URL, which would be a bug)
      return null;
    }
  }

  @Nullable
  private Course tryInitializeCourse(@NotNull Project project, @NotNull URL courseUrl) {
    Course course;
    try {
      InputStream inputStream = CoursesClient.fetchJson(courseUrl);
      course = Course.fromConfigurationData(
          new InputStreamReader(inputStream),
          courseUrl.toString(),
          new IntelliJModelFactory(project));
      mainViewModelProvider
          .getMainViewModel(project)
          .courseViewModel
          .set(new CourseViewModel(course));
      return course;
    } catch (UnexpectedResponseException | IOException e) {
      notifyNetworkError();
      return null;
    } catch (MalformedCourseConfigurationFileException e) {
      notifyMalformedCourseConfiguration();
      return null;
    }
  }

  /**
   * Creates a file in the project settings directory which contains the given course configuration
   * file URL.
   * @return True if the file was successfully created, false otherwise.
   */
  private boolean tryCreateCourseFile(@NotNull Project project, @NotNull URL courseUrl) {
    try {
      File file = new APlusProject(project).getCourseFilePath().toFile();
      FileUtils.writeStringToFile(file, courseUrl.toString(), StandardCharsets.UTF_8);
      return true;
    } catch (IOException e) {
      // TODO: what should we do if creating the file fails?
      return false;
    }
  }

  /**
   * Tries importing project settings from the given course. Shows an error dialog to the user if a
   * network error occurs.
   * @return True if project settings were successfully imported, false otherwise.
   */
  private boolean tryImportProjectSettings(@NotNull Project project, @NotNull Course course) {
    try {
      SettingsUtil.importProjectSettings(project, course);
      return true;
    } catch (IOException | UnexpectedResponseException e) {
      notifyNetworkError();
      return false;
    }
  }

  /**
   * Tries importing IDE settings from the given course. Shows an error dialog to the user if a
   * network error occurs.
   * @return True if IDE settings were successfully imported, false otherwise.
   */
  private boolean tryImportIdeSettings(@NotNull Course course) {
    try {
      SettingsUtil.importIdeSettings(course);
      return true;
    } catch (IOException | UnexpectedResponseException e) {
      notifyNetworkError();
      return false;
    }
  }

  private boolean userWantsToRestart() {
    return dialogs.showOkCancelDialog(
        "Settings were imported successfully. Restart IntelliJ IDEA now to reload the settings?",
        "Restart Needed", "Yes", "No");
  }

  private void notifyNetworkError() {
    dialogs.showErrorDialog("An error occurred while creating a course project. Please check your "
        + "network connection and try again, or contact the course staff if the issue persists.",
        "Network Error");
  }

  private void notifyMalformedCourseConfiguration() {
    dialogs.showErrorDialog("An error occurred while reading the course configuration. Please "
        + "contact the course staff.", "Course Configuration Error");
  }

}
