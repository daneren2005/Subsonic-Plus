package net.sourceforge.subsonic.booter;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import net.sourceforge.subsonic.booter.agent.SettingsPanel;
import net.sourceforge.subsonic.booter.agent.StatusPanel;
import net.sourceforge.subsonic.booter.agent.SubsonicAgent;

/**
 * Application entry point for Subsonic booter.
 * <p/>
 * Use command line argument "-agent" to start the Windows service monitoring agent,
 * or "-mac" to start the Mac version of the deployer.
 *
 * @author Sindre Mehus
 */
public class Main {

    public Main(String contextName, List<String> args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext" + contextName + ".xml");

        SubsonicAgent agent  = (SubsonicAgent) context.getBean("agent");
        SettingsPanel settingsPanel = (SettingsPanel) context.getBean("settingsPanel");
        StatusPanel statusPanel = (StatusPanel) context.getBean("statusPanel");

        agent.setElevated(args.contains("-elevated"));

        if (args.contains("-stop")) {
            agent.startOrStopService(false);
            agent.showStatusPanel();
        }
        else if (args.contains("-start")) {
            agent.startOrStopService(true);
            agent.showStatusPanel();
        }

    }

    public static void main(String[] args) {
        System.err.println("args: " + Arrays.asList(args));

        String context = "-deployer";
        if (args.length > 0) {
            context = args[0];
        }
        new Main(context, Arrays.asList(args));
    }
}
