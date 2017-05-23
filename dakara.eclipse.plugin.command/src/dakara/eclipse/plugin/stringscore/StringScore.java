package dakara.eclipse.plugin.stringscore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * scoring strategies:
 * - rank by distance found from beginning of string
 * - rank by size of word match
 * - rank by multiple word matches
 * - rank by matching first letters of words
 * - rank by contiguous characters found
 * - single characters only match word start?
 *
 */
public class StringScore {
	private static final Score EMPTY_SCORE = new Score(0, Collections.emptyList());
	private static final Score NOT_FOUND_SCORE = new Score(-1, Collections.emptyList());
	
	// TODO run all permutations and keep highest score
	public static Score containsAnyOrderWords(String match, String target) {
		int totalRank = 0;
		List<Integer> matches = new ArrayList<>();
		for (String word : splitWords(match)) {
			Score score = contains(word, maskRegions(target, matches));
			if ( score.rank <= 0) {
				totalRank = 0;
				break;  // all words must be found
			}
			totalRank += score.rank;
			matches.addAll(score.matches);
		}
		
		Score acronymScore = scoreAsAcronym(match.toLowerCase(), target.toLowerCase());
		if (acronymScore.rank > totalRank) {
			return acronymScore;
		}
		
		return new Score(totalRank, matches);
	}
	
	
	
	private static Score scoreAsAcronym(String searchInput, String text) {
		StringCursor matchesCursor = new StringCursor(text);
		StringCursor acronymCursor = addMarkersForAcronym(new StringCursor(text));
		StringCursor inputCursor = new StringCursor(searchInput);
		
		while (!acronymCursor.markerPositionTerminal() && !inputCursor.cursorPositionTerminal()) {
			if (acronymCursor.currentMarker() == inputCursor.currentChar()) {
				inputCursor.moveCursorForward();
				matchesCursor.addMarker(acronymCursor.indexOfMarker());
			}
			acronymCursor.advanceMarker();
		}
		
		// did we complete all matches from the input
		if (inputCursor.cursorPositionTerminal()) {
			return new Score(100, matchesCursor.markers);
		}
		
		return EMPTY_SCORE;
	}
	
	private static StringCursor addMarkersForAcronym(StringCursor text) {
		while (!text.cursorPositionTerminal()) {
			if (Character.isAlphabetic(text.currentChar()) && !Character.isAlphabetic(text.peekPreviousChar())) {
				text.addMarker(text.indexOfCursor());
			}
			text.moveCursorForward();
		}
		return text;
	}
	
	
	/*
	 * replace matched regions with space so we don't match them again
	 */
	private static String maskRegions(String text, List<Integer> maskIndexes) {
		if (maskIndexes.size() == 0) return text;
		StringBuilder builder = new StringBuilder(text);
		maskIndexes.stream().forEach(index -> builder.setCharAt(index, ' '));
		return builder.toString();
	}
	
	// TODO don't have infinite scoring.  Need to have ranges (0-10) for each algorithm
	// score whole word matching higher vs partial includes
	public static Score contains(String match, String target) {
		if ((match == null) || (match.length() == 0)) return EMPTY_SCORE;
		
		match = match.toLowerCase();
		target = target.toLowerCase();
		StringCursor targetCursor = new StringCursor(target);
		boolean fullMatch = targetCursor.moveCursorIndexOf(match).wordAtCursor().equals(match);  // did we match full word
		//boolean fullMatch = targetCursor.moveCursorIndexOf(match).markWordAtCursor().textOfMarkers().equals(match);  // did we match full word
		if ( fullMatch ) {
			return new Score(2, targetCursor.markRangeForward(match.length()).markers());
		} else if (!targetCursor.cursorPositionTerminal()) {
			return new Score(1, targetCursor.markRangeForward(match.length()).markers());
		}
		return NOT_FOUND_SCORE;
	}
	
	private static String[] splitWords(String text) {
		String[] words = text.split(" ");
		return words;
	}
	
	public static class StringCursor {
		public final String text;
		private int indexOfCursor = 0;
		private int currentMarker = 0;
		
		private List<Integer> markers = new ArrayList<>();
		public StringCursor(String text) {
			this.text = text;
		}
		
		public StringCursor setMarkers(List<Integer> markers) {
			this.markers = markers;
			return this;
		}
		
		public boolean markerPositionTerminal() {
			if (currentMarker == markers.size() || currentMarker == -1) return true;
			return false;
		}
		
		public char currentMarker() {
			if (currentMarker < markers.size())
				return text.charAt(markers.get(currentMarker));
			return 0;
		}	
		
		public char previousMarker() {
			if (currentMarker > 0) {
				return text.charAt(markers.get(currentMarker - 1));
			}
			return 0;
		}
		
		public StringCursor addMarker(int index) {
			if (index >= text.length()) throw new IllegalArgumentException("Index is greater than text length " +index);
			markers.add(index);
			return this;
		}
		
		public StringCursor markRangeForward(int charsForward) {
			if (charsForward + indexOfCursor >= text.length()) throw new IllegalArgumentException("Index is greater than text length " +charsForward + indexOfCursor);
			for (int index = indexOfCursor; index < indexOfCursor + charsForward; index++) {
				markers.add(index);
			}
			return this;
		}
		
		public List<Integer> markers() {
			return markers;
		}
		
		public String wordAtCursor() {
			if (cursorPositionTerminal()) return "";
			int currentIndex = indexOfCursor;
			int indexStart = moveCursorPreviousAlphaBoundary().indexOfCursor();
			int indexEnd   = moveCursorNextAlphaBoundary().indexOfCursor();
			indexOfCursor = currentIndex;
			return text.substring(indexStart, indexEnd);
		}
		
		public boolean cursorPositionTerminal() {
			if (indexOfCursor == text.length() || indexOfCursor == -1) return true;
			return false;
		}
		
		public char currentChar() {
			if (indexOfCursor < text.length())
				return text.charAt(indexOfCursor);
			return 0;
		}
		
		public char peekPreviousChar() {
			if (indexOfCursor > 0) {
				return text.charAt(indexOfCursor - 1);
			}
			return 0;
		}
		
		public char peekNextChar() {
			if (indexOfCursor < text.length() - 1) {
				return text.charAt(indexOfCursor + 1);
			}
			return 0;
		}
		
		public StringCursor moveCursorPreviousAlphaBoundary() {
			while(Character.isAlphabetic(peekPreviousChar())) {
				moveCursorBackward();
			}
			return this;
		}
		
		public StringCursor moveCursorNextAlphaBoundary() {
			while(Character.isAlphabetic(peekNextChar())) {
				moveCursorForward();
			}
			return this;
		}
		
		public StringCursor moveCursorIndexOf(String match) {
			 indexOfCursor = text.indexOf(match);
			 return this;
		}
		
		public StringCursor moveCursorForward() {
			 indexOfCursor++;
			 return this;
		}
		
		public StringCursor moveCursorBackward() {
			 indexOfCursor--;
			 return this;
		}		
		
		public StringCursor advanceMarker() {
			 currentMarker++;
			 return this;
		}
		
		public int indexOfCursor() {
			return indexOfCursor;
		}
		
		public int indexOfMarker() {
			if (!markers.isEmpty())
				return markers.get(currentMarker);
			return 0;
		}
		
	}
	
	public static class Score {
		public final int rank;
		public final List<Integer> matches;
		public Score(int rank, List<Integer> matches) {
			this.rank = rank;
			this.matches = matches;
		}
	}
	
	
}