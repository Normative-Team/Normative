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

public class CrossModeling {

	private static final boolean LOCAL = false;
	private static final boolean OPEN_VOCABULARY = false;
	private static final boolean CACHE = false;
	
	public static void main(String[] args) {
		String root = args.length > 0 ? args[0] : "";
		Setup.setupParameters(!OPEN_VOCABULARY);
		Map<String, ProjectData> data = DataReader.read(new File(root + "issue_comments_REPLACECODE_TOKENIZEDBODY_FLAT.csv"));
		String outFile = root + "cross-entropies" + (OPEN_VOCABULARY ? "OV" : "CV") + (LOCAL ? "WU" : "NU") + (CACHE ? "WC" : "NC") + ".csv";
		initVocabulary(data);
		try (FileWriter fw = new FileWriter(new File(outFile))) {
			double total = 0;
			int totalCount = 0;
			for (Entry<String, ProjectData> train : data.entrySet()) {
				String trainProject = train.getKey();
				MixModel model = train(train.getValue());
				for (Entry<String, ProjectData> test : data.entrySet()) {
					String testProject = test.getKey();
					Map<Pair<String, String>, List<List<Double>>> entropiesGlobal = test(model, test.getValue());
					DoubleSummaryStatistics stats = stats(entropiesGlobal);
					total += stats.getSum();
					totalCount += stats.getCount();
					fw.append(trainProject.split("_")[0] + "," + trainProject.split("_")[1]
							+ "," + testProject.split("_")[0] + "," + testProject.split("_")[1]
							+ "," + stats.getAverage() + "," + stats.getCount() + "\n");
					System.out.println(trainProject.split("_")[0] + "\t" + trainProject.split("_")[1]
							+ "\t" + testProject.split("_")[0] + "\t" + testProject.split("_")[1]
							+ "\t" + stats.getAverage() + "\t" + stats.getCount());
				}
			}
			System.out.println("Total\t" + total / totalCount + "\t" + totalCount);			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void initVocabulary(Map<String, ProjectData> data) {
		Vocabulary.reset();
		VocabularyRunner.build(data.values().stream().flatMap(l -> getProjectTokens(l)).flatMap(l -> l));
		if (!OPEN_VOCABULARY) Vocabulary.close();
	}

	private static MixModel train(ProjectData data) {
		Model global = NGramModel.standard();
		Model local = NGramModel.standard();
		if (CACHE) local = new InverseMixModel(local, new NGramCache());
		MixModel model = new InverseMixModel(global, local);
		ModelRunner.learnTokens(model.getLeft(), getProjectTokens(data));
		return model;
	}

	private static Stream<Stream<String>> getProjectTokens(ProjectData data) {
		return data.issues.values().stream()
				.map(l -> Stream.of(l))
				.flatMap(LexerRunner::lex);
	}

	private static Map<Pair<String, String>, List<List<Double>>> test(MixModel model, ProjectData data) throws IOException {
		Map<Pair<String, String>, List<List<Double>>> entropies = new HashMap<>();
		for (Entry<Pair<String, String>, String> issue : data.issues.entrySet()) {
			Pair<String, String> key = issue.getKey();
			String content = issue.getValue();
			List<List<Double>> modeled = ModelRunner.modelLines(model, Stream.of(content));
			entropies.put(key, modeled);
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
