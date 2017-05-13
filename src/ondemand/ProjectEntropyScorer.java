package ondemand;

import java.io.File;
import java.util.DoubleSummaryStatistics;
import java.util.List;
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

public class ProjectEntropyScorer implements Runnable {

	private MixModel model;

	public static void main(String[] args) {
		new Thread(new ProjectEntropyScorer(args[0], args[1])).start();
	}

	public ProjectEntropyScorer(String root, String project) {
		Setup.setupParameters();
		System.out.println("Reading in data");
		ProjectData projectData = DataReader.read(new File(root)).get(project);
		System.out.println("Commencing training on " + projectData.issues.size() + " issues");
		initVocabulary(projectData);
		this.model = train(projectData);
		System.out.println(getProjectTokens(projectData).flatMap(l -> l).count());
		System.out.println("Training complete (" + getProjectTokens(projectData).flatMap(l -> l).count() + " tokens). Type a valid file name in the console to run the model");
	}

	private static MixModel train(ProjectData data) {
		Model global = NGramModel.standard();
		Model local = NGramModel.standard();
		MixModel model = new InverseMixModel(global, local);
		ModelRunner.learnTokens(model.getLeft(), getProjectTokens(data));
		return model;
	}

	private static void initVocabulary(ProjectData data) {
		Vocabulary.reset();
		VocabularyRunner.build(getProjectTokens(data).flatMap(l -> l));
		Vocabulary.close();
	}

	private static Stream<Stream<String>> getProjectTokens(ProjectData data) {
		return data.issues.values().stream().map(l -> Stream.of(l)).flatMap(LexerRunner::lex);
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
