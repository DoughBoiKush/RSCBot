package org.rsbot.script.provider;

import org.rsbot.Configuration;
import org.rsbot.util.io.HttpClient;
import org.rsbot.util.io.IniParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * @author Paris
 */
public class ScriptDeliveryNetwork extends FileScriptSource {
	private static final Logger log = Logger.getLogger("ScriptDelivery");
	private static ScriptDeliveryNetwork instance;
	private String key;
	private final String defaultKey = "0000000000000000000000000000000000000000";
	private final int version = 1;
	private URL base = null;
	private boolean forceUpdate = false;

	private ScriptDeliveryNetwork() {
		super(new File(Configuration.Paths.getScriptsNetworkDirectory()));
		key = defaultKey;
	}

	public void start() {
		if (load()) {
			try {
				init();
			} catch (final Exception e) {
				e.printStackTrace();
				log.severe("Could not download scripts from the network!");
			}
		}
	}

	public static ScriptDeliveryNetwork getInstance() {
		if (instance == null) {
			instance = new ScriptDeliveryNetwork();
		}
		return instance;
	}

	public void forceUpdate() {
		forceUpdate = true;
		try {
			init();
		} catch (final IOException ignored) {
		}
	}

	private boolean load() {
		HashMap<String, String> keys = null;
		boolean enabled = true;
		String error = "could not load control file";

		try {
			final URL source = new URL(Configuration.Paths.URLs.SDN_CONTROL);
			final File cache = getChachedFile("control.txt");
			HttpClient.download(source, cache);
			final BufferedReader reader = new BufferedReader(new FileReader(cache));
			keys = IniParser.deserialise(reader).get(IniParser.emptySection);
			reader.close();
		} catch (final Exception e) {
			enabled = false;
		}

		if (keys == null || keys.isEmpty() || keys.containsKey("enabled") && !IniParser.parseBool(keys.get("enabled"))) {
			enabled = false;
		} else {
			if (keys.containsKey("error")) {
				error = keys.get("error");
			}
			if (keys.containsKey("version")) {
				final int remoteVersion = Integer.parseInt(keys.get("version"));
				if (version != remoteVersion) {
					enabled = false;
					error = "please update your version of the bot";
				}
			}
			if (keys.containsKey("url")) {
				try {
					base = new URL(keys.get("url").replace("%key", getKey()));
				} catch (final MalformedURLException e) {
				}
			}
		}

		if (base == null) {
			enabled = false;
		}

		if (!enabled) {
			log.warning("Service disabled: " + error);
		}

		return enabled;
	}

	private void init() throws IOException {
		final File cache = new File(Configuration.Paths.getScriptsNetworkDirectory());

		if (!cache.exists()) {
			cache.mkdirs();
		}

		if (Configuration.getCurrentOperatingSystem() == Configuration.OperatingSystem.WINDOWS) {
			final String path = "\"" + cache.getAbsolutePath() + "\"";
			try {
				Runtime.getRuntime().exec("attrib +H " + path);
			} catch (final IOException e) {
			}
		}

		BufferedReader br, br1;
		String line, line1;
		final HashMap<String, URL> scripts = new HashMap<String, URL>(64);

		final File manifest = getChachedFile("manifest.txt");
		final HttpURLConnection con = HttpClient.download(base, manifest);
		base = con.getURL();
		br = new BufferedReader(new FileReader(manifest));

		while ((line = br.readLine()) != null) {
			final URL packUrl = new URL(base, line);
			long mod = 0;
			final File pack = getChachedFile("pack-" + getFileName(packUrl));
			if (pack.exists()) {
				mod = pack.lastModified();
			}
			final HttpURLConnection packCon = HttpClient.download(packUrl, pack);
			if (pack.lastModified() == mod && !forceUpdate) {
				continue;
			}
			br1 = new BufferedReader(new FileReader(pack));
			while ((line1 = br1.readLine()) != null) {
				final URL scriptUrl = new URL(packCon.getURL(), line1);
				scripts.put(getFileName(scriptUrl), scriptUrl);
			}
			br1.close();
		}

		br.close();

		if (!scripts.isEmpty()) {
			sync(scripts);
		}

		forceUpdate = false;
	}

	private void sync(final HashMap<String, URL> scripts) {
		int n = 0;
		for (final String name : scripts.keySet()) {
			if (!name.contains("$")) {
				n++;
			}
		}
		if (n > 0) {
			log.info("Loading " + Integer.toString(n) + " scripts from the network");
		}

		int created = 0, deleted = 0, updated = 0;
		final File dir = new File(Configuration.Paths.getScriptsNetworkDirectory());
		final ArrayList<File> delete = new ArrayList<File>(64);

		for (final File f : dir.listFiles()) {
			if (f.getName().endsWith(".class")) {
				delete.add(f);
			}
		}

		final ArrayList<Callable<Collection<Object>>> tasks = new ArrayList<Callable<Collection<Object>>>();

		for (final Entry<String, URL> key : scripts.entrySet()) {
			final File path = new File(dir, key.getKey());
			if (!path.getName().contains("$")) {
				if (delete.contains(path)) {
					updated++;
				} else {
					created++;
				}
			}
			delete.remove(path);
			tasks.add(new Callable<Collection<Object>>() {
				public Collection<Object> call() throws Exception {
					log.fine("Downloading: " + path.getName());
					HttpClient.download(key.getValue(), path);
					return null;
				}

				;
			});
		}

		int threads = Runtime.getRuntime().availableProcessors();
		final ExecutorService executorService = Executors.newFixedThreadPool(threads);
		try {
			executorService.invokeAll(tasks);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		for (final File f : delete) {
			if (!f.delete()) {
				f.deleteOnExit();
			}
			if (!f.getName().contains("$")) {
				deleted++;
			}
		}

		log.fine(String.format("Downloaded %1$d new scripts, updated %2$d and deleted %3$d", created, deleted, updated));
	}

	private String getFileName(final URL url) {
		final String path = url.getPath();
		return path.substring(path.lastIndexOf('/') + 1);
	}

	private File getChachedFile(final String name) {
		return new File(Configuration.Paths.getCacheDirectory(), "sdn-" + name);
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}
}
