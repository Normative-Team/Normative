package ondemand;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

import slp.core.io.Writer;
import slp.core.lexing.LexerRunner;
import slp.core.modeling.ModelRunner;
import slp.core.util.Pair;

public class EntropyOutput {

	public static void write(File f, Pair<File, List<List<Double>>> modeled) {
		List<List<Double>> entropies = modeled.right;
		List<List<String>> lexed = LexerRunner.lex(f).map(l -> l.collect(Collectors.toList())).collect(Collectors.toList());
		List<List<String>> fused = new ArrayList<>();
		for (int i = 0; i < entropies.size(); i++) {
			fused.add(new ArrayList<>());
			for (int j = 0; j < entropies.get(i).size(); j++) {
				fused.get(fused.size() - 1).add(String.format("%.4f:%s", entropies.get(i).get(j), lexed.get(i).get(j)).replaceAll("\n", "\\n").replaceAll("\t", "\\t"));
			}
		}
		File entropiesOut = new File(f + "-entropies");
		File statsOut = new File(f + "-stats");
		DoubleSummaryStatistics stats = ModelRunner.getStats(entropies);
		try {
			Writer.writeAny(entropiesOut, fused);
			String content = String.format("Tokens:\t%d\nAvgEntropy:\t%.4f", stats.getCount(), stats.getAverage());
			Writer.writeContent(statsOut, content);
			System.out.println("Written summary statistics to file: " + entropiesOut);
			System.out.println("Written all tokens with entropies to file: " + entropiesOut);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error trying to write to: " + entropiesOut);
		}
	}
}
