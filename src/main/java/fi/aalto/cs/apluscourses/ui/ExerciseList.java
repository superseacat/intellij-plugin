package fi.aalto.cs.apluscourses.ui;

import javax.swing.JList;
import javax.swing.JPanel;

public class ExerciseList {

  @Binding
  private JList exercises;
  private JPanel basePanel;

  public JPanel getBasePanel() {
    return basePanel;
  }
}