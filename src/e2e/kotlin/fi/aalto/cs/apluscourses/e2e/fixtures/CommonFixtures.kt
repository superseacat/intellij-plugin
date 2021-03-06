package fi.aalto.cs.apluscourses.e2e.fixtures

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.SearchContext
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import fi.aalto.cs.apluscourses.e2e.utils.LocatorBuilder
import java.time.Duration
import javax.swing.*

fun RemoteRobot.welcomeFrame() = find(WelcomeFrameFixture::class.java, Duration.ofSeconds(60))

fun RemoteRobot.ideFrame() = find(IdeFrameFixture::class.java, Duration.ofSeconds(20))

fun SearchContext.customComboBox(name: String) = find(CustomComboBoxFixture::class.java,
  LocatorBuilder()
    .withAttr("accessiblename", name)
    .withClass(JComboBox::class.java)
    .build())

fun SearchContext.dialog(title: String, timeout : Duration = Duration.ofSeconds(5)) =
    find(DialogFixture::class.java,
      LocatorBuilder()
        .withAttr("title", title)
        .withClass(JDialog::class.java)
        .build(),
      timeout)

fun SearchContext.heavyWeightWindow() = find(HeavyWeightWindowFixture::class.java, Duration.ofSeconds(5))

@FixtureName("Welcome Frame")
@DefaultXpath(by = "FlatWelcomeFrame type", xpath = "//div[@class='FlatWelcomeFrame']")
class WelcomeFrameFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent)
    : CommonContainerFixture(remoteRobot, remoteComponent) {
  fun newProjectButton() = button(byXpath("//div[(@class='MainButton' and @text='New Project') "
      + "or (@accessiblename='New Project' and @class='JButton')]"))
}

@FixtureName("IDE Frame")
@DefaultXpath("IdeFrameImpl type", "//div[@class='IdeFrameImpl']")
class IdeFrameFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent)
    : CommonContainerFixture(remoteRobot, remoteComponent) {
  fun menu() = find(MenuItemFixture::class.java,
    LocatorBuilder().withClass(JMenuBar::class.java).build())
    fun projectViewTree() = find(CommonContainerFixture::class.java, byXpath("//div[@class='ProjectViewTree']"))
    fun aPlusStripeButton() = find(
        CommonContainerFixture::class.java,
        byXpath("//div[@accessiblename='A+ Courses' and @class='StripeButton' and @text='A+ Courses']")
    )
    fun modules() = find(
        CommonContainerFixture::class.java,
        byXpath("//div[@class='ModuleListView']"),
        Duration.ofSeconds(20)
    )
}

@FixtureName("Menu Item")
class MenuItemFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent)
    : ContainerFixture(remoteRobot, remoteComponent) {
  fun item(text: String) = find(MenuItemFixture::class.java,
    LocatorBuilder()
      .withAttr("text", text)
      .withClass(JMenuItem::class.java)
      .build())
  fun select(text: String) : MenuItemFixture = with(item(text)) { click(); return@select this }
}

@FixtureName("Dialog")
class DialogFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent)
    : CommonContainerFixture(remoteRobot, remoteComponent) {
  fun ContainerFixture.sidePanel() = find(ContainerFixture::class.java,
      byXpath("//div[@class='SidePanel']"))
}

@FixtureName("Custom Combo Box")
class CustomComboBoxFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) :
  ComponentFixture(remoteRobot, remoteComponent) {
  fun dropdown() = click()
}

@FixtureName("Heavy Weight Window")
@DefaultXpath("HeavyWeightWindow type", "//div[@class='HeavyWeightWindow']")
class HeavyWeightWindowFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent)
  : CommonContainerFixture(remoteRobot, remoteComponent)
