package fi.aalto.cs.apluscourses.intellij.model;

import com.intellij.ProjectTopics;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.Library;
import fi.aalto.cs.apluscourses.model.ModelFactory;
import fi.aalto.cs.apluscourses.model.Module;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntelliJModelFactory implements ModelFactory {

  private static final Logger logger = LoggerFactory.getLogger(IntelliJModelFactory.class);

  @NotNull
  private final APlusProject project;

  public IntelliJModelFactory(@NotNull Project project) {
    this.project = new APlusProject(project);
  }

  @Override
  public Course createCourse(@NotNull String name,
                             @NotNull List<Module> modules,
                             @NotNull List<Library> libraries,
                             @NotNull Map<String, String> requiredPlugins,
                             @NotNull Map<String, URL> resourceUrls) {
    IntelliJCourse course =
        new IntelliJCourse(name, modules, libraries, requiredPlugins, resourceUrls, project);
    // Add a module change listener with the created course instance to the project
    // TODO: These should be handled in those classes; also, renaming issue?
    project.getMessageBus().connect().subscribe(ProjectTopics.MODULES, new ModuleListener() {
      @Override
      public void moduleRemoved(@NotNull Project project,
                                @NotNull com.intellij.openapi.module.Module projectModule) {
          course.onComponentRemove(course.getComponentIfExists(projectModule.getName()));
      }
    });
    project.getLibraryTable().addListener(new LibraryTable.Listener() {
      @Override
      public void afterLibraryRemoved(
          @NotNull com.intellij.openapi.roots.libraries.Library library) {
        course.onComponentRemove(course.getComponentIfExists(name));
      }
    });
    project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES,
        new BulkFileListener() {
          @Override
          public void after(@NotNull List<? extends VFileEvent> events) {
            for (VFileEvent event : events) {
              if (event instanceof VFileDeleteEvent) {
                VirtualFile deletedFile = Objects.requireNonNull(event.getFile());
                course.onComponentFilesDeleted(course.getComponentIfExists(deletedFile));
              }
            }
          }
        }
    );
    return course;
  }

  @Override
  public Module createModule(@NotNull String name, @NotNull URL url) {
    // IntelliJ modules may already be present in the project or file system, so we determine the
    // state at module creation here.
    return new IntelliJModule(name, url, project, project.resolveModuleState(name));
  }

  @Override
  public Library createLibrary(@NotNull String name) {
    throw new UnsupportedOperationException(
        "Only common libraries like Scala SDK are currently supported.");
  }

  private void markDependentModulesInvalid(@NotNull IntelliJCourse course,
      @NotNull String removedModuleName) {
    Project project = course.getProject();
    com.intellij.openapi.module.Module[] projectModules = ModuleManager.getInstance(project)
        .getModules();

    for (com.intellij.openapi.module.Module module : projectModules) {
      Module courseModule = getCourseModule(course, module);
      if (courseModule != null) {
        List<String> courseModuleDependencies = getCourseModuleDependencies(courseModule);
        if (courseModuleDependencies != null
            && courseModuleDependencies.contains(removedModuleName)) {
          courseModule.stateMonitor.set(StateMonitor.ERROR);
        }
      }
    }
  }

  @Nullable
  private Module getCourseModule(@NotNull IntelliJCourse course,
      @NotNull com.intellij.openapi.module.Module module) {
    Module courseModule = null;
    try {
      courseModule = course.getModule(module.getName());
    } catch (NoSuchModuleException e) {
      logger.error(e.getMessage(), e);
    }
    return courseModule;
  }

  @Nullable
  private List<String> getCourseModuleDependencies(@NotNull Module courseModule) {
    List<String> courseModuleDependencies = null;
    try {
      courseModuleDependencies = courseModule.getDependencies();
    } catch (ModuleLoadException e) {
      logger.error(e.getMessage(), e);
    }
    return courseModuleDependencies;
  }
}
