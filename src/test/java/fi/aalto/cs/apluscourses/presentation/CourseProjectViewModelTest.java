package fi.aalto.cs.apluscourses.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.ModelExtensions;
import java.util.Collections;
import org.junit.Test;

public class CourseProjectViewModelTest {

  private final Course emptyCourse = new ModelExtensions.TestCourse("123",
      "NiceCourse",
      //  modules
      Collections.emptyList(),
      //  libraries
      Collections.emptyList(),
      //  exerciseModules
      Collections.emptyMap(),
      //  resourceUrls
      Collections.emptyMap(),
      //  autoInstallComponentNames
      Collections.emptyList(),
      //  replInitialCommands
      Collections.emptyMap());

  @Test
  public void testInformationTextIncludesCourseName() {
    CourseProjectViewModel courseProjectViewModel = new CourseProjectViewModel(emptyCourse, "");
    assertEquals("The information text contains the course name",
        "NiceCourse", courseProjectViewModel.getCourseName());
  }

  @Test
  public void testIdeSettingsNotPreviouslyImported() {
    CourseProjectViewModel courseProjectViewModel
        = new CourseProjectViewModel(emptyCourse, "different");

    assertFalse("By default the user should not want to opt out",
        courseProjectViewModel.userOptsOutOfSettings());

    assertTrue("Settings opt out should be available",
        courseProjectViewModel.canUserOptOutSettings());

    assertTrue("The settings text should mention that IDEA settings will be adjusted",
        courseProjectViewModel.shouldShowSettingsInfo());
  }

  @Test
  public void testIdeSettingsAlreadyImported() {
    CourseProjectViewModel courseProjectViewModel
        = new CourseProjectViewModel(emptyCourse, "123");

    assertTrue(courseProjectViewModel.userOptsOutOfSettings());

    assertFalse("Settings opt out should not be available",
        courseProjectViewModel.canUserOptOutSettings());

    assertTrue("The settings text should mention that settings are already imported",
        courseProjectViewModel.shouldShowCurrentSettings());
  }
}
