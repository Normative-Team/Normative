package data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import slp.core.util.Pair;

public class DataReader {
	
	public static Map<String, ProjectData> read(File file) {
		return read(file, null);
	}
		
	public static Map<String, ProjectData> read(File file, String filter) {
		Map<String, ProjectData> projects = new HashMap<>();
		String currProject = "";
		ProjectData currData = null;
		List<String> lines;
		try {
			lines = Files.lines(file.toPath(), StandardCharsets.UTF_8).collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		Pair<String, String> lastKey = null;
		for (int i = lines.size() - 1; i >= 0; i--) {
			String line = lines.get(i);
			String[] split = line.split(",", 6);
			if (filter != null && !split[0].equals(filter)) continue;
			if (split.length < 6 || !split[1].matches("[0-9]+")) {
				if (lastKey != null) currData.issues.put(lastKey, currData.issues.get(lastKey) + line);
				continue;
			}
			String project = split[0];
			if (!project.equals(currProject)) {
				currProject = project;
				currData = new ProjectData();
				projects.put(currProject, currData);
			}
			String content = split[split.length - 1];
			if (content.startsWith("\"")) {
				content = content.substring(1, content.length() - 1);
				content = content.replaceAll("\"\"", "\"");
			}
			Pair<String, String> key = Pair.of(split[2], split[3]);
			currData.issues.put(key, content);
			lastKey = key;
		}
		if (!currData.issues.isEmpty()) projects.put(currProject, currData);
		return projects;
	}
	
	public static class ProjectData {
		public final Map<Pair<String, String>, String> issues;
		
		private ProjectData() {
			this.issues = new LinkedHashMap<>();
		}
	}
}
