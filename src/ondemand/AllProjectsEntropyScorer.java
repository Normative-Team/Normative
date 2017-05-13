package ondemand;

import java.io.File;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import data.DataReader;
import data.DataReader.ProjectData;
import slp.core.lexing.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.modeling.mix.InverseMixModel;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.NGramModel;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;
import slp.core.util.Pair;
import util.Setup;

public class AllProjectsEntropyScorer implements Runnable {
	
	private MixModel model;

	public static void main(String[] args) {
		new Thread(new AllProjectsEntropyScorer(args[0])).start();
	}

	public AllProjectsEntropyScorer(String root) {
		Setup.setupParameters();
		System.out.println("Reading in data");
		Map<String, ProjectData> data = DataReader.read(new File(root));
		System.out.println("Commencing training on " + data.values().stream().mapToInt(l -> l.issues.size()).sum() + " issues");
		initVocabulary(data);
		this.model = train(data);
		System.out.println(getAllTokens(data).flatMap(l -> l).count());
		System.out.println("Training complete (" + getAllTokens(data).flatMap(l -> l).count() + " tokens). Type a valid file name in the console to run the model");
	}

	private static MixModel train(Map<String, ProjectData> data) {
		Model global = NGramModel.standard();
		Model local = NGramModel.standard();
		MixModel model = new InverseMixModel(global, local);
		ModelRunner.learnTokens(model.getLeft(), getAllTokens(data));
		return model;
	}

	private static void initVocabulary(Map<String, ProjectData> data) {
		Vocabulary.reset();
		VocabularyRunner.build(getAllTokens(data).flatMap(l -> l));
		Vocabulary.close();
	}
	
	private static Stream<Stream<String>> getAllTokens(Map<String, ProjectData> data) {
		return data.values().stream().flatMap(p -> p.issues.values().stream())
				.map(l -> Stream.of(l))
				.flatMap(LexerRunner::lex);
	}

	@Override
	public void run() {
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				String line = scanner.nextLine();
				File f = new File(line);
				if (!f.exists())
					System.err.println("File not found: " + f.getAbsolutePath());
				else {
					Pair<File, List<List<Double>>> modeled = ModelRunner.model(this.model, f).findAny().get();
					// Tentatively, just write average entropy to std:out
//					EntropyOutput.write(f, modeled);
					DoubleSummaryStatistics stats = ModelRunner.getStats(modeled.right);
					System.out.println(stats.getAverage());
				}
			}
		}
	}
}
