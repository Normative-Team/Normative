package modeling;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import data.DataReader;
import data.DataReader.ProjectData;
import slp.core.lexing.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.modeling.mix.InverseMixModel;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.NGramCache;
import slp.core.modeling.ngram.NGramModel;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;
import slp.core.util.Pair;
import util.Setup;

public class BasicModeling {

	private static final boolean LOCAL = false;
	private static final boolean OPEN_VOCABULARY = false;
	private static final boolean CACHE = false;
	
	public static void main(String[] args) {
		String root = args.length > 0 ? args[0] : "E:/CP/Kavaler/";
		Setup.setupParameters(!OPEN_VOCABULARY);
		Map<String, ProjectData> data = DataReader.read(new File(root + "issue_comments_REPLACECODE_TOKENIZEDBODY_FLAT.csv"));
		String outFile = root + "entropies" + (OPEN_VOCABULARY ? "OV" : "CV") + (LOCAL ? "WU" : "NU") + (CACHE ? "WC" : "NC") + ".csv";
		try (FileWriter fw = new FileWriter(new File(outFile))) {
			double total = 0;
			int totalCount = 0;
			for (Entry<String, ProjectData> entry : data.entrySet()) {
				String project = entry.getKey();
				initVocabulary(data, project);
				MixModel model = initModel(data, project);
				Map<Pair<String, String>, String> issues = entry.getValue().issues;
				Map<Pair<String, String>, List<List<Double>>> entropiesGlobal = modelProject(project, model, issues, fw);
				DoubleSummaryStatistics stats = stats(entropiesGlobal);
				total += stats.getSum();
				totalCount += stats.getCount();
				System.out.println(project + "\t" + stats.getAverage() + "\t" + stats.getCount() + "\t" + Vocabulary.size());
			}
			System.out.println("Total\t" + total / totalCount + "\t" + totalCount);			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void initVocabulary(Map<String, ProjectData> data, String project) {
		Vocabulary.reset();
		VocabularyRunner.build(getTrainTokens(data, project).flatMap(l -> l));
		if (!OPEN_VOCABULARY) Vocabulary.close();
	}

	private static MixModel initModel(Map<String, ProjectData> data, String project) {
		Model global = NGramModel.standard();
		Model local = NGramModel.standard();
		if (CACHE) local = new InverseMixModel(local, new NGramCache());
		MixModel model = new InverseMixModel(global, local);
		ModelRunner.learnTokens(model.getLeft(), getTrainTokens(data, project));
		return model;
	}

	private static Stream<Stream<String>> getTrainTokens(Map<String, ProjectData> data, String project) {
		Stream<Stream<String>> tokens = Stream.empty();
		for (String other : data.keySet()) {
			if (other.equals(project)) continue;
			ProjectData otherData = data.get(other);
			Stream<Stream<String>> projectTokens = otherData.issues.values().stream()
					.map(l -> Stream.of(l))
					.flatMap(LexerRunner::lex);
			tokens = Stream.concat(tokens, projectTokens);
		}
		return tokens;
	}

	private static Map<Pair<String, String>, List<List<Double>>> modelProject(String project, MixModel model, Map<Pair<String, String>, String> issues, FileWriter fw) throws IOException {
		Map<Pair<String, String>, List<List<Double>>> entropies = new HashMap<>();
		for (Entry<Pair<String, String>, String> issue : issues.entrySet()) {
			Pair<String, String> key = issue.getKey();
			String content = issue.getValue();
			List<List<Double>> modeled = ModelRunner.modelLines(model, Stream.of(content));
			entropies.put(key, modeled);
			DoubleSummaryStatistics stats = modeled.stream().flatMap(l -> l.stream()).mapToDouble(p -> p).summaryStatistics();
			fw.append(project + "," + key.left + "," + key.right + "," + stats.getAverage() + "," + stats.getCount() + "\n");
			if (LOCAL) ModelRunner.learnLines(model.getRight(), Stream.of(content));
		}
		return entropies;
	}

	private static DoubleSummaryStatistics stats(Map<Pair<String, String>, List<List<Double>>> entropies) {
		return entropies.values().stream()
				.flatMap(l -> l.stream().flatMap(l2 -> l2.stream()))
				.mapToDouble(l -> l).summaryStatistics();
	}
}
