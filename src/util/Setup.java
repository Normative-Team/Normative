package util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;
import slp.core.lexing.LexerRunner;
import slp.core.modeling.ModelRunner;
import slp.core.modeling.ngram.NGramModel;
import slp.core.modeling.ngram.WBModel;
import slp.core.translating.VocabularyRunner;

public class Setup {

	public static void setupParameters() {
		setupParameters(false);
	}
	
	public static void setupParameters(boolean closeVocabulary) {
		LexerRunner.addSentenceMarkers(true);
		LexerRunner.setLexer(new Lexer() {
			@Override
			public Stream<Stream<String>> lex(List<String> lines) {
				return lines.stream().map(l -> Arrays.stream(l.split(" +")));
			}
		});
		LexerRunner.perLine(true);
		ModelRunner.perLine(true);

		if (closeVocabulary) VocabularyRunner.cutOff(10);
		
		ModelRunner.setNGramOrder(4);
		NGramModel.setStandard(WBModel.class);
	}

}
