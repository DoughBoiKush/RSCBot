package org.rsbot.util;

import org.rsbot.Configuration;
import org.rsbot.gui.BotGUI;
import org.rsbot.util.io.HttpClient;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author Xel
 * @author Paris
 */
public final class UpdateChecker {
	private static final Logger log = Logger.getLogger(UpdateChecker.class.getName());
	private static int latest = -1;
	private static boolean error;

	public static boolean isError() {
		if (latest == -1) {
			getLatestVersion();
		}
		return error;
	}

	public static void notify(final BotGUI instance) {
		if (Configuration.getVersion() >= getLatestVersion()) {
			return;
		}
		log.info("New version available");
		final int update = JOptionPane.showConfirmDialog(instance, "RSCBot is outdated, would you like to update?", "Update Found", JOptionPane.YES_NO_OPTION);
		if (update != 0) {
			return;
		}
		try {
			update(instance);
		} catch (final Exception e) {
			log.warning("Unable to apply update");
		}
	}

	public static boolean isDeprecatedVersion() {
		final int kill;
		try {
			kill = Integer.parseInt(HttpClient.downloadAsString(new URL(Configuration.Paths.URLs.VERSION_KILL)));
		} catch (final Exception ignored) {
			return false;
		}
		return kill > Configuration.getVersion();
	}

	public static int getLatestVersion() {
		if (latest != -1 || error) {
			return latest;
		}
		final File cache = new File(Configuration.Paths.getCacheDirectory(), "0.23.txt");
		BufferedReader reader = null;
		try {
			HttpClient.download(new URL(Configuration.Paths.URLs.VERSION), cache);
			reader = new BufferedReader(new FileReader(cache));
			final String s = reader.readLine().trim();
			reader.close();
			latest = Integer.parseInt(s);
			return latest;
		} catch (final Exception ignored) {
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (final IOException ignored) {
			}
		}
		log.warning("Unable to obtain latest version information");
		error = true;
		return -1;
	}

	private static void update(final BotGUI instance) throws IOException {
		log.info("Downloading update...");
		final File jarNew = new File(Configuration.NAME + "-" + "0.23" + ".jar");
		HttpClient.download(new URL(Configuration.Paths.URLs.DOWNLOAD), jarNew);
		final String jarOld = Configuration.Paths.getRunningJarPath();
		Runtime.getRuntime().exec("java -jar \"" + jarNew + "\" --delete \"" + jarOld + "\"");
		instance.cleanExit(true);
	}
}
