package org.modelix.model.mpsplugin.plugin;

import jetbrains.mps.plugins.tool.GeneratedTool;
import javax.swing.Icon;
import org.modelix.model.mpsplugin.history.CloudView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowAnchor;
import javax.swing.JComponent;

public class CloudTool_Tool extends GeneratedTool {
  private static final Icon ICON = IconContainer.ICON_a0;
  private CloudView component;
  public CloudTool_Tool(Project project) {
    super(project, "Cloud", null, ICON, ToolWindowAnchor.BOTTOM, false);
  }
  public void init(Project project) {
    super.init(project);
    CloudTool_Tool.this.makeAvailableLater();
  }
  public JComponent getComponent() {
    if (CloudTool_Tool.this.component == null) {
      CloudTool_Tool.this.component = new CloudView();
    }
    return CloudTool_Tool.this.component;
  }
}