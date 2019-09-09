package informationFiltering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.tartarus.snowball.SnowballStemmer;

public class Document {
	HashMap<String, Integer> termMap;
	ArrayList<String> stoppingWords;
	String documentID;
	int counter;
	public Document(String documentID, ArrayList<String> stoppingWords) {
		termMap = new HashMap<String, Integer>();
		this.stoppingWords = stoppingWords;
		this.documentID = documentID;
		counter = 0;
	}
	
	public String getDocumentID() {
		return documentID;
	}
		
	public void addTerm(String term) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		if (term.equals("US")) {
			if (termMap.containsKey(term)) {
				termMap.replace(term, (termMap.get(term) + 1));
			}
			else {
				termMap.put(term, 1);
			}
		}
		term = stemWordBySnowball(term);
		if (stoppingWords.contains(term) == false && term.equals("") == false && term.length() >= 2) {
			if (termMap.containsKey(term)) {
				termMap.replace(term, (termMap.get(term) + 1));
			}
			else {
				termMap.put(term, 1);
			}
		}
		counter++;
	}
	
	public HashMap<String, Integer> getTermMap() {
		return termMap;
	}
	
	public String stemWordBySnowball(String word) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		if (word.equals("US")) {
			return word;
		}
		Class<?> stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");
        SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();
		stemmer.setCurrent(word.toLowerCase());
		stemmer.stem();
		word = stemmer.getCurrent();
		if (word.equals("african") == true) {
			word = "africa";
		}
		return word;
	}
	
	public int getTermCount(String term) {
		//Returns the corresponding frequency for a term in the hash map
		if (termMap.containsKey(term)) {
			return termMap.get(term);
		}
		// If it is not in the hash map, return 0
		return 0;
	}
	
	public int getWordCount() {
		return counter;
	}
	
	public ArrayList<String> getSortedTermList() {
		ArrayList<String> termList = new ArrayList<>(termMap.keySet());
		Collections.sort(termList);
		return termList;
	}

}
