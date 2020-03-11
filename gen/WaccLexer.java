// Generated from /homes/zy7218/wacc_51/antlr_config/WaccLexer.g4 by ANTLR 4.8
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class WaccLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		ARROW=1, DOUBLE_ARROW=2, COLON=3, DOT=4, ADD=5, SUB=6, MUL=7, DIV=8, MOD=9, 
		GTE=10, GT=11, LTE=12, LT=13, EQ=14, NEQ=15, AND=16, OR=17, DOTDOT=18, 
		NOT=19, LEN=20, ORD=21, CHR=22, FST=23, SND=24, SKIP_STAT=25, LPAR=26, 
		RPAR=27, LBRA=28, RBRA=29, LCUR=30, RCUR=31, SEMICOLON=32, BEGIN=33, IS=34, 
		END=35, NULL=36, TRUE=37, FALSE=38, IF=39, THEN=40, ELSE=41, FI=42, WHILE=43, 
		FOR=44, IN=45, DO=46, DONE=47, NEWPAIR=48, READ=49, FREE=50, RETURN=51, 
		EXIT=52, PRINT=53, PRINTLN=54, CALL=55, PAIR=56, VAR=57, CONST=58, NEWTYPE=59, 
		UNION=60, OF=61, WHEN=62, WHERE=63, FORALL=64, TRAIT=65, REQUIRED=66, 
		INSTANCE=67, BASE_TYPE=68, INTEGER=69, STRLIT=70, CHARLIT=71, ASSIGN=72, 
		COMMA=73, IDENT=74, CAP_IDENT=75, COMMENT=76, WS=77;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"ARROW", "DOUBLE_ARROW", "COLON", "DOT", "ADD", "SUB", "MUL", "DIV", 
			"MOD", "GTE", "GT", "LTE", "LT", "EQ", "NEQ", "AND", "OR", "DOTDOT", 
			"NOT", "LEN", "ORD", "CHR", "FST", "SND", "SKIP_STAT", "LPAR", "RPAR", 
			"LBRA", "RBRA", "LCUR", "RCUR", "SEMICOLON", "BEGIN", "IS", "END", "NULL", 
			"TRUE", "FALSE", "IF", "THEN", "ELSE", "FI", "WHILE", "FOR", "IN", "DO", 
			"DONE", "NEWPAIR", "READ", "FREE", "RETURN", "EXIT", "PRINT", "PRINTLN", 
			"CALL", "PAIR", "VAR", "CONST", "NEWTYPE", "UNION", "OF", "WHEN", "WHERE", 
			"FORALL", "TRAIT", "REQUIRED", "INSTANCE", "BASE_TYPE", "DIGIT", "INTEGER", 
			"STRLIT", "CHARLIT", "NORMAL_CHAR", "ESC_CHAR", "ASSIGN", "COMMA", "IDENT_HEAD", 
			"IDENT_TAIL", "IDENT", "CAP_IDENT", "COMMENT", "WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'->'", "'=>'", "':'", "'.'", "'+'", "'-'", "'*'", "'/'", "'%'", 
			"'>='", "'>'", "'<='", "'<'", "'=='", "'!='", "'&&'", "'||'", "'..'", 
			"'!'", "'len'", "'ord'", "'chr'", "'fst'", "'snd'", "'skip'", "'('", 
			"')'", "'['", "']'", "'{'", "'}'", "';'", "'begin'", "'is'", "'end'", 
			"'null'", "'true'", "'false'", "'if'", "'then'", "'else'", "'fi'", "'while'", 
			"'for'", "'in'", "'do'", "'done'", "'newpair'", "'read'", "'free'", "'return'", 
			"'exit'", "'print'", "'println'", "'call'", "'pair'", "'var'", "'const'", 
			"'newtype'", "'union'", "'of'", "'when'", "'where'", "'forall'", "'trait'", 
			"'required'", "'instance'", null, null, null, null, "'='", "','"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "ARROW", "DOUBLE_ARROW", "COLON", "DOT", "ADD", "SUB", "MUL", "DIV", 
			"MOD", "GTE", "GT", "LTE", "LT", "EQ", "NEQ", "AND", "OR", "DOTDOT", 
			"NOT", "LEN", "ORD", "CHR", "FST", "SND", "SKIP_STAT", "LPAR", "RPAR", 
			"LBRA", "RBRA", "LCUR", "RCUR", "SEMICOLON", "BEGIN", "IS", "END", "NULL", 
			"TRUE", "FALSE", "IF", "THEN", "ELSE", "FI", "WHILE", "FOR", "IN", "DO", 
			"DONE", "NEWPAIR", "READ", "FREE", "RETURN", "EXIT", "PRINT", "PRINTLN", 
			"CALL", "PAIR", "VAR", "CONST", "NEWTYPE", "UNION", "OF", "WHEN", "WHERE", 
			"FORALL", "TRAIT", "REQUIRED", "INSTANCE", "BASE_TYPE", "INTEGER", "STRLIT", 
			"CHARLIT", "ASSIGN", "COMMA", "IDENT", "CAP_IDENT", "COMMENT", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public WaccLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "WaccLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2O\u0213\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\3\2\3"+
		"\2\3\2\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n"+
		"\3\n\3\13\3\13\3\13\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\17\3\17\3\17\3\20"+
		"\3\20\3\20\3\21\3\21\3\21\3\22\3\22\3\22\3\23\3\23\3\23\3\24\3\24\3\25"+
		"\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\30\3\30\3\30"+
		"\3\30\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\3\33\3\33\3\34\3\34"+
		"\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3\"\3\"\3\"\3\"\3\"\3\"\3#"+
		"\3#\3#\3$\3$\3$\3$\3%\3%\3%\3%\3%\3&\3&\3&\3&\3&\3\'\3\'\3\'\3\'\3\'\3"+
		"\'\3(\3(\3(\3)\3)\3)\3)\3)\3*\3*\3*\3*\3*\3+\3+\3+\3,\3,\3,\3,\3,\3,\3"+
		"-\3-\3-\3-\3.\3.\3.\3/\3/\3/\3\60\3\60\3\60\3\60\3\60\3\61\3\61\3\61\3"+
		"\61\3\61\3\61\3\61\3\61\3\62\3\62\3\62\3\62\3\62\3\63\3\63\3\63\3\63\3"+
		"\63\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\65\3\65\3\65\3\65\3\65\3\66\3"+
		"\66\3\66\3\66\3\66\3\66\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\38\38"+
		"\38\38\38\39\39\39\39\39\3:\3:\3:\3:\3;\3;\3;\3;\3;\3;\3<\3<\3<\3<\3<"+
		"\3<\3<\3<\3=\3=\3=\3=\3=\3=\3>\3>\3>\3?\3?\3?\3?\3?\3@\3@\3@\3@\3@\3@"+
		"\3A\3A\3A\3A\3A\3A\3A\3B\3B\3B\3B\3B\3B\3C\3C\3C\3C\3C\3C\3C\3C\3C\3D"+
		"\3D\3D\3D\3D\3D\3D\3D\3D\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E"+
		"\3E\3E\5E\u01cc\nE\3F\3F\3G\6G\u01d1\nG\rG\16G\u01d2\3H\3H\3H\7H\u01d8"+
		"\nH\fH\16H\u01db\13H\3H\3H\3I\3I\3I\5I\u01e2\nI\3I\3I\3J\3J\3K\3K\3K\3"+
		"L\3L\3M\3M\3N\3N\3O\3O\3O\5O\u01f4\nO\3P\3P\7P\u01f8\nP\fP\16P\u01fb\13"+
		"P\3Q\3Q\7Q\u01ff\nQ\fQ\16Q\u0202\13Q\3R\3R\7R\u0206\nR\fR\16R\u0209\13"+
		"R\3R\3R\3S\6S\u020e\nS\rS\16S\u020f\3S\3S\2\2T\3\3\5\4\7\5\t\6\13\7\r"+
		"\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25"+
		")\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O"+
		")Q*S+U,W-Y.[/]\60_\61a\62c\63e\64g\65i\66k\67m8o9q:s;u<w=y>{?}@\177A\u0081"+
		"B\u0083C\u0085D\u0087E\u0089F\u008b\2\u008dG\u008fH\u0091I\u0093\2\u0095"+
		"\2\u0097J\u0099K\u009b\2\u009d\2\u009fL\u00a1M\u00a3N\u00a5O\3\2\7\5\2"+
		"$$))^^\13\2$$))\62\62^^ddhhppttvv\4\2aac|\4\2\f\f\17\17\5\2\13\f\17\17"+
		"\"\"\2\u021a\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2"+
		"\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2"+
		"\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2"+
		"\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2"+
		"\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3"+
		"\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2"+
		"\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2"+
		"S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3"+
		"\2\2\2\2a\3\2\2\2\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2"+
		"\2\2m\3\2\2\2\2o\3\2\2\2\2q\3\2\2\2\2s\3\2\2\2\2u\3\2\2\2\2w\3\2\2\2\2"+
		"y\3\2\2\2\2{\3\2\2\2\2}\3\2\2\2\2\177\3\2\2\2\2\u0081\3\2\2\2\2\u0083"+
		"\3\2\2\2\2\u0085\3\2\2\2\2\u0087\3\2\2\2\2\u0089\3\2\2\2\2\u008d\3\2\2"+
		"\2\2\u008f\3\2\2\2\2\u0091\3\2\2\2\2\u0097\3\2\2\2\2\u0099\3\2\2\2\2\u009f"+
		"\3\2\2\2\2\u00a1\3\2\2\2\2\u00a3\3\2\2\2\2\u00a5\3\2\2\2\3\u00a7\3\2\2"+
		"\2\5\u00aa\3\2\2\2\7\u00ad\3\2\2\2\t\u00af\3\2\2\2\13\u00b1\3\2\2\2\r"+
		"\u00b3\3\2\2\2\17\u00b5\3\2\2\2\21\u00b7\3\2\2\2\23\u00b9\3\2\2\2\25\u00bb"+
		"\3\2\2\2\27\u00be\3\2\2\2\31\u00c0\3\2\2\2\33\u00c3\3\2\2\2\35\u00c5\3"+
		"\2\2\2\37\u00c8\3\2\2\2!\u00cb\3\2\2\2#\u00ce\3\2\2\2%\u00d1\3\2\2\2\'"+
		"\u00d4\3\2\2\2)\u00d6\3\2\2\2+\u00da\3\2\2\2-\u00de\3\2\2\2/\u00e2\3\2"+
		"\2\2\61\u00e6\3\2\2\2\63\u00ea\3\2\2\2\65\u00ef\3\2\2\2\67\u00f1\3\2\2"+
		"\29\u00f3\3\2\2\2;\u00f5\3\2\2\2=\u00f7\3\2\2\2?\u00f9\3\2\2\2A\u00fb"+
		"\3\2\2\2C\u00fd\3\2\2\2E\u0103\3\2\2\2G\u0106\3\2\2\2I\u010a\3\2\2\2K"+
		"\u010f\3\2\2\2M\u0114\3\2\2\2O\u011a\3\2\2\2Q\u011d\3\2\2\2S\u0122\3\2"+
		"\2\2U\u0127\3\2\2\2W\u012a\3\2\2\2Y\u0130\3\2\2\2[\u0134\3\2\2\2]\u0137"+
		"\3\2\2\2_\u013a\3\2\2\2a\u013f\3\2\2\2c\u0147\3\2\2\2e\u014c\3\2\2\2g"+
		"\u0151\3\2\2\2i\u0158\3\2\2\2k\u015d\3\2\2\2m\u0163\3\2\2\2o\u016b\3\2"+
		"\2\2q\u0170\3\2\2\2s\u0175\3\2\2\2u\u0179\3\2\2\2w\u017f\3\2\2\2y\u0187"+
		"\3\2\2\2{\u018d\3\2\2\2}\u0190\3\2\2\2\177\u0195\3\2\2\2\u0081\u019b\3"+
		"\2\2\2\u0083\u01a2\3\2\2\2\u0085\u01a8\3\2\2\2\u0087\u01b1\3\2\2\2\u0089"+
		"\u01cb\3\2\2\2\u008b\u01cd\3\2\2\2\u008d\u01d0\3\2\2\2\u008f\u01d4\3\2"+
		"\2\2\u0091\u01de\3\2\2\2\u0093\u01e5\3\2\2\2\u0095\u01e7\3\2\2\2\u0097"+
		"\u01ea\3\2\2\2\u0099\u01ec\3\2\2\2\u009b\u01ee\3\2\2\2\u009d\u01f3\3\2"+
		"\2\2\u009f\u01f5\3\2\2\2\u00a1\u01fc\3\2\2\2\u00a3\u0203\3\2\2\2\u00a5"+
		"\u020d\3\2\2\2\u00a7\u00a8\7/\2\2\u00a8\u00a9\7@\2\2\u00a9\4\3\2\2\2\u00aa"+
		"\u00ab\7?\2\2\u00ab\u00ac\7@\2\2\u00ac\6\3\2\2\2\u00ad\u00ae\7<\2\2\u00ae"+
		"\b\3\2\2\2\u00af\u00b0\7\60\2\2\u00b0\n\3\2\2\2\u00b1\u00b2\7-\2\2\u00b2"+
		"\f\3\2\2\2\u00b3\u00b4\7/\2\2\u00b4\16\3\2\2\2\u00b5\u00b6\7,\2\2\u00b6"+
		"\20\3\2\2\2\u00b7\u00b8\7\61\2\2\u00b8\22\3\2\2\2\u00b9\u00ba\7\'\2\2"+
		"\u00ba\24\3\2\2\2\u00bb\u00bc\7@\2\2\u00bc\u00bd\7?\2\2\u00bd\26\3\2\2"+
		"\2\u00be\u00bf\7@\2\2\u00bf\30\3\2\2\2\u00c0\u00c1\7>\2\2\u00c1\u00c2"+
		"\7?\2\2\u00c2\32\3\2\2\2\u00c3\u00c4\7>\2\2\u00c4\34\3\2\2\2\u00c5\u00c6"+
		"\7?\2\2\u00c6\u00c7\7?\2\2\u00c7\36\3\2\2\2\u00c8\u00c9\7#\2\2\u00c9\u00ca"+
		"\7?\2\2\u00ca \3\2\2\2\u00cb\u00cc\7(\2\2\u00cc\u00cd\7(\2\2\u00cd\"\3"+
		"\2\2\2\u00ce\u00cf\7~\2\2\u00cf\u00d0\7~\2\2\u00d0$\3\2\2\2\u00d1\u00d2"+
		"\7\60\2\2\u00d2\u00d3\7\60\2\2\u00d3&\3\2\2\2\u00d4\u00d5\7#\2\2\u00d5"+
		"(\3\2\2\2\u00d6\u00d7\7n\2\2\u00d7\u00d8\7g\2\2\u00d8\u00d9\7p\2\2\u00d9"+
		"*\3\2\2\2\u00da\u00db\7q\2\2\u00db\u00dc\7t\2\2\u00dc\u00dd\7f\2\2\u00dd"+
		",\3\2\2\2\u00de\u00df\7e\2\2\u00df\u00e0\7j\2\2\u00e0\u00e1\7t\2\2\u00e1"+
		".\3\2\2\2\u00e2\u00e3\7h\2\2\u00e3\u00e4\7u\2\2\u00e4\u00e5\7v\2\2\u00e5"+
		"\60\3\2\2\2\u00e6\u00e7\7u\2\2\u00e7\u00e8\7p\2\2\u00e8\u00e9\7f\2\2\u00e9"+
		"\62\3\2\2\2\u00ea\u00eb\7u\2\2\u00eb\u00ec\7m\2\2\u00ec\u00ed\7k\2\2\u00ed"+
		"\u00ee\7r\2\2\u00ee\64\3\2\2\2\u00ef\u00f0\7*\2\2\u00f0\66\3\2\2\2\u00f1"+
		"\u00f2\7+\2\2\u00f28\3\2\2\2\u00f3\u00f4\7]\2\2\u00f4:\3\2\2\2\u00f5\u00f6"+
		"\7_\2\2\u00f6<\3\2\2\2\u00f7\u00f8\7}\2\2\u00f8>\3\2\2\2\u00f9\u00fa\7"+
		"\177\2\2\u00fa@\3\2\2\2\u00fb\u00fc\7=\2\2\u00fcB\3\2\2\2\u00fd\u00fe"+
		"\7d\2\2\u00fe\u00ff\7g\2\2\u00ff\u0100\7i\2\2\u0100\u0101\7k\2\2\u0101"+
		"\u0102\7p\2\2\u0102D\3\2\2\2\u0103\u0104\7k\2\2\u0104\u0105\7u\2\2\u0105"+
		"F\3\2\2\2\u0106\u0107\7g\2\2\u0107\u0108\7p\2\2\u0108\u0109\7f\2\2\u0109"+
		"H\3\2\2\2\u010a\u010b\7p\2\2\u010b\u010c\7w\2\2\u010c\u010d\7n\2\2\u010d"+
		"\u010e\7n\2\2\u010eJ\3\2\2\2\u010f\u0110\7v\2\2\u0110\u0111\7t\2\2\u0111"+
		"\u0112\7w\2\2\u0112\u0113\7g\2\2\u0113L\3\2\2\2\u0114\u0115\7h\2\2\u0115"+
		"\u0116\7c\2\2\u0116\u0117\7n\2\2\u0117\u0118\7u\2\2\u0118\u0119\7g\2\2"+
		"\u0119N\3\2\2\2\u011a\u011b\7k\2\2\u011b\u011c\7h\2\2\u011cP\3\2\2\2\u011d"+
		"\u011e\7v\2\2\u011e\u011f\7j\2\2\u011f\u0120\7g\2\2\u0120\u0121\7p\2\2"+
		"\u0121R\3\2\2\2\u0122\u0123\7g\2\2\u0123\u0124\7n\2\2\u0124\u0125\7u\2"+
		"\2\u0125\u0126\7g\2\2\u0126T\3\2\2\2\u0127\u0128\7h\2\2\u0128\u0129\7"+
		"k\2\2\u0129V\3\2\2\2\u012a\u012b\7y\2\2\u012b\u012c\7j\2\2\u012c\u012d"+
		"\7k\2\2\u012d\u012e\7n\2\2\u012e\u012f\7g\2\2\u012fX\3\2\2\2\u0130\u0131"+
		"\7h\2\2\u0131\u0132\7q\2\2\u0132\u0133\7t\2\2\u0133Z\3\2\2\2\u0134\u0135"+
		"\7k\2\2\u0135\u0136\7p\2\2\u0136\\\3\2\2\2\u0137\u0138\7f\2\2\u0138\u0139"+
		"\7q\2\2\u0139^\3\2\2\2\u013a\u013b\7f\2\2\u013b\u013c\7q\2\2\u013c\u013d"+
		"\7p\2\2\u013d\u013e\7g\2\2\u013e`\3\2\2\2\u013f\u0140\7p\2\2\u0140\u0141"+
		"\7g\2\2\u0141\u0142\7y\2\2\u0142\u0143\7r\2\2\u0143\u0144\7c\2\2\u0144"+
		"\u0145\7k\2\2\u0145\u0146\7t\2\2\u0146b\3\2\2\2\u0147\u0148\7t\2\2\u0148"+
		"\u0149\7g\2\2\u0149\u014a\7c\2\2\u014a\u014b\7f\2\2\u014bd\3\2\2\2\u014c"+
		"\u014d\7h\2\2\u014d\u014e\7t\2\2\u014e\u014f\7g\2\2\u014f\u0150\7g\2\2"+
		"\u0150f\3\2\2\2\u0151\u0152\7t\2\2\u0152\u0153\7g\2\2\u0153\u0154\7v\2"+
		"\2\u0154\u0155\7w\2\2\u0155\u0156\7t\2\2\u0156\u0157\7p\2\2\u0157h\3\2"+
		"\2\2\u0158\u0159\7g\2\2\u0159\u015a\7z\2\2\u015a\u015b\7k\2\2\u015b\u015c"+
		"\7v\2\2\u015cj\3\2\2\2\u015d\u015e\7r\2\2\u015e\u015f\7t\2\2\u015f\u0160"+
		"\7k\2\2\u0160\u0161\7p\2\2\u0161\u0162\7v\2\2\u0162l\3\2\2\2\u0163\u0164"+
		"\7r\2\2\u0164\u0165\7t\2\2\u0165\u0166\7k\2\2\u0166\u0167\7p\2\2\u0167"+
		"\u0168\7v\2\2\u0168\u0169\7n\2\2\u0169\u016a\7p\2\2\u016an\3\2\2\2\u016b"+
		"\u016c\7e\2\2\u016c\u016d\7c\2\2\u016d\u016e\7n\2\2\u016e\u016f\7n\2\2"+
		"\u016fp\3\2\2\2\u0170\u0171\7r\2\2\u0171\u0172\7c\2\2\u0172\u0173\7k\2"+
		"\2\u0173\u0174\7t\2\2\u0174r\3\2\2\2\u0175\u0176\7x\2\2\u0176\u0177\7"+
		"c\2\2\u0177\u0178\7t\2\2\u0178t\3\2\2\2\u0179\u017a\7e\2\2\u017a\u017b"+
		"\7q\2\2\u017b\u017c\7p\2\2\u017c\u017d\7u\2\2\u017d\u017e\7v\2\2\u017e"+
		"v\3\2\2\2\u017f\u0180\7p\2\2\u0180\u0181\7g\2\2\u0181\u0182\7y\2\2\u0182"+
		"\u0183\7v\2\2\u0183\u0184\7{\2\2\u0184\u0185\7r\2\2\u0185\u0186\7g\2\2"+
		"\u0186x\3\2\2\2\u0187\u0188\7w\2\2\u0188\u0189\7p\2\2\u0189\u018a\7k\2"+
		"\2\u018a\u018b\7q\2\2\u018b\u018c\7p\2\2\u018cz\3\2\2\2\u018d\u018e\7"+
		"q\2\2\u018e\u018f\7h\2\2\u018f|\3\2\2\2\u0190\u0191\7y\2\2\u0191\u0192"+
		"\7j\2\2\u0192\u0193\7g\2\2\u0193\u0194\7p\2\2\u0194~\3\2\2\2\u0195\u0196"+
		"\7y\2\2\u0196\u0197\7j\2\2\u0197\u0198\7g\2\2\u0198\u0199\7t\2\2\u0199"+
		"\u019a\7g\2\2\u019a\u0080\3\2\2\2\u019b\u019c\7h\2\2\u019c\u019d\7q\2"+
		"\2\u019d\u019e\7t\2\2\u019e\u019f\7c\2\2\u019f\u01a0\7n\2\2\u01a0\u01a1"+
		"\7n\2\2\u01a1\u0082\3\2\2\2\u01a2\u01a3\7v\2\2\u01a3\u01a4\7t\2\2\u01a4"+
		"\u01a5\7c\2\2\u01a5\u01a6\7k\2\2\u01a6\u01a7\7v\2\2\u01a7\u0084\3\2\2"+
		"\2\u01a8\u01a9\7t\2\2\u01a9\u01aa\7g\2\2\u01aa\u01ab\7s\2\2\u01ab\u01ac"+
		"\7w\2\2\u01ac\u01ad\7k\2\2\u01ad\u01ae\7t\2\2\u01ae\u01af\7g\2\2\u01af"+
		"\u01b0\7f\2\2\u01b0\u0086\3\2\2\2\u01b1\u01b2\7k\2\2\u01b2\u01b3\7p\2"+
		"\2\u01b3\u01b4\7u\2\2\u01b4\u01b5\7v\2\2\u01b5\u01b6\7c\2\2\u01b6\u01b7"+
		"\7p\2\2\u01b7\u01b8\7e\2\2\u01b8\u01b9\7g\2\2\u01b9\u0088\3\2\2\2\u01ba"+
		"\u01bb\7k\2\2\u01bb\u01bc\7p\2\2\u01bc\u01cc\7v\2\2\u01bd\u01be\7u\2\2"+
		"\u01be\u01bf\7v\2\2\u01bf\u01c0\7t\2\2\u01c0\u01c1\7k\2\2\u01c1\u01c2"+
		"\7p\2\2\u01c2\u01cc\7i\2\2\u01c3\u01c4\7d\2\2\u01c4\u01c5\7q\2\2\u01c5"+
		"\u01c6\7q\2\2\u01c6\u01cc\7n\2\2\u01c7\u01c8\7e\2\2\u01c8\u01c9\7j\2\2"+
		"\u01c9\u01ca\7c\2\2\u01ca\u01cc\7t\2\2\u01cb\u01ba\3\2\2\2\u01cb\u01bd"+
		"\3\2\2\2\u01cb\u01c3\3\2\2\2\u01cb\u01c7\3\2\2\2\u01cc\u008a\3\2\2\2\u01cd"+
		"\u01ce\4\62;\2\u01ce\u008c\3\2\2\2\u01cf\u01d1\5\u008bF\2\u01d0\u01cf"+
		"\3\2\2\2\u01d1\u01d2\3\2\2\2\u01d2\u01d0\3\2\2\2\u01d2\u01d3\3\2\2\2\u01d3"+
		"\u008e\3\2\2\2\u01d4\u01d9\7$\2\2\u01d5\u01d8\5\u0093J\2\u01d6\u01d8\5"+
		"\u0095K\2\u01d7\u01d5\3\2\2\2\u01d7\u01d6\3\2\2\2\u01d8\u01db\3\2\2\2"+
		"\u01d9\u01d7\3\2\2\2\u01d9\u01da\3\2\2\2\u01da\u01dc\3\2\2\2\u01db\u01d9"+
		"\3\2\2\2\u01dc\u01dd\7$\2\2\u01dd\u0090\3\2\2\2\u01de\u01e1\7)\2\2\u01df"+
		"\u01e2\5\u0093J\2\u01e0\u01e2\5\u0095K\2\u01e1\u01df\3\2\2\2\u01e1\u01e0"+
		"\3\2\2\2\u01e2\u01e3\3\2\2\2\u01e3\u01e4\7)\2\2\u01e4\u0092\3\2\2\2\u01e5"+
		"\u01e6\n\2\2\2\u01e6\u0094\3\2\2\2\u01e7\u01e8\7^\2\2\u01e8\u01e9\t\3"+
		"\2\2\u01e9\u0096\3\2\2\2\u01ea\u01eb\7?\2\2\u01eb\u0098\3\2\2\2\u01ec"+
		"\u01ed\7.\2\2\u01ed\u009a\3\2\2\2\u01ee\u01ef\t\4\2\2\u01ef\u009c\3\2"+
		"\2\2\u01f0\u01f4\5\u009bN\2\u01f1\u01f4\4C\\\2\u01f2\u01f4\5\u008bF\2"+
		"\u01f3\u01f0\3\2\2\2\u01f3\u01f1\3\2\2\2\u01f3\u01f2\3\2\2\2\u01f4\u009e"+
		"\3\2\2\2\u01f5\u01f9\5\u009bN\2\u01f6\u01f8\5\u009dO\2\u01f7\u01f6\3\2"+
		"\2\2\u01f8\u01fb\3\2\2\2\u01f9\u01f7\3\2\2\2\u01f9\u01fa\3\2\2\2\u01fa"+
		"\u00a0\3\2\2\2\u01fb\u01f9\3\2\2\2\u01fc\u0200\4C\\\2\u01fd\u01ff\5\u009d"+
		"O\2\u01fe\u01fd\3\2\2\2\u01ff\u0202\3\2\2\2\u0200\u01fe\3\2\2\2\u0200"+
		"\u0201\3\2\2\2\u0201\u00a2\3\2\2\2\u0202\u0200\3\2\2\2\u0203\u0207\7%"+
		"\2\2\u0204\u0206\n\5\2\2\u0205\u0204\3\2\2\2\u0206\u0209\3\2\2\2\u0207"+
		"\u0205\3\2\2\2\u0207\u0208\3\2\2\2\u0208\u020a\3\2\2\2\u0209\u0207\3\2"+
		"\2\2\u020a\u020b\bR\2\2\u020b\u00a4\3\2\2\2\u020c\u020e\t\6\2\2\u020d"+
		"\u020c\3\2\2\2\u020e\u020f\3\2\2\2\u020f\u020d\3\2\2\2\u020f\u0210\3\2"+
		"\2\2\u0210\u0211\3\2\2\2\u0211\u0212\bS\2\2\u0212\u00a6\3\2\2\2\r\2\u01cb"+
		"\u01d2\u01d7\u01d9\u01e1\u01f3\u01f9\u0200\u0207\u020f\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}