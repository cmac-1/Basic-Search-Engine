package baseline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.tartarus.snowball.SnowballStemmer;
public class Main {

	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		File topics = new File("..\\TextFiles\\TopicStatements101-150.txt");
		File datasetFolder = new File("..\\TextFiles\\dataset101-150");
		File stoppingWordsList = new File("..\\TextFiles\\common-english-words.txt");
		File outputDir = new File("..\\Outputs\\BaselineResults");
		ArrayList<Topic> topicList = generateTopics(topics);
		ArrayList<String> stoppingWords = generateStoppingWordsList(stoppingWordsList);
		HashMap<Topic, ArrayList<Document>> topicDocMap = createDocuments(datasetFolder, topicList, stoppingWords);
		HashMap<Topic, HashMap<Document, Double>> topicDocScoreMap = generateBM25Scores(topicDocMap);
		
		Iterator<Topic> topicItr = topicDocScoreMap.keySet().iterator();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			FileWriter outputWriter = new FileWriter((outputDir.getAbsolutePath() + "\\BaselineResult" +currentTopic.getTopicID() + ".dat"));
			HashMap<Document, Double> docScores = topicDocScoreMap.get(currentTopic);
			docScores = sortHashMap(docScores);
			Iterator<Document> docItr = docScores.keySet().iterator();
			while (docItr.hasNext()) {
				Document currentDocument = docItr.next();
				double bm25score = docScores.get(currentDocument);
				outputWriter.write("R" + currentTopic.getTopicID() + " " + currentDocument.getDocumentID() + " " + bm25score + "\n");
			}
			outputWriter.close();
		}

	}
	
	public static HashMap<Topic, HashMap<Document, Double>> generateBM25Scores(HashMap<Topic, ArrayList<Document>> topicDocMap) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		HashMap<Topic, HashMap<Document, Double>> finalBM25Scores = new HashMap<Topic, HashMap<Document, Double>>();
		Class<?> stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");
        SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();
		Iterator<Topic> topicItr = topicDocMap.keySet().iterator();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			ArrayList<Document> topicDocumentList = topicDocMap.get(currentTopic);
			String query = currentTopic.getTitle();
			query = query.replaceAll("&quot;", "");
			query = query.replaceAll(",&quot;", "");
			query = query.replaceAll("\\p{Punct}", "");
			query = query.replaceAll("[^a-zA-Z ]","");
			String[] termsInQuery = query.split(" ");
			HashMap<String, Integer> queryWithFrequency = new HashMap<String, Integer>();
			for (int i = 0; i < termsInQuery.length; i++) {
				String currentTerm = termsInQuery[i];
				stemmer.setCurrent(currentTerm.toLowerCase());
				stemmer.stem();
				currentTerm = stemmer.getCurrent();
				if (queryWithFrequency.containsKey(currentTerm)) {
					int currentFrequency = queryWithFrequency.get(currentTerm);
					queryWithFrequency.put(currentTerm, (currentFrequency + 1));
				}
				else {
					queryWithFrequency.put(currentTerm, 1);
				}
			}
			HashMap<String, Integer> termDocumentFrequencies = new HashMap<String, Integer>();
			Iterator<String> termItr = queryWithFrequency.keySet().iterator();
			while (termItr.hasNext()) {
				String currentTerm = termItr.next();
				for (int j = 0; j < topicDocumentList.size(); j++) {
					Document currentDocument = topicDocumentList.get(j);
					if (currentDocument.getTermCount(currentTerm) > 0) {
						if (termDocumentFrequencies.containsKey(currentTerm)) {
							int currentValue = termDocumentFrequencies.get(currentTerm);
							termDocumentFrequencies.put(currentTerm, (currentValue + 1));
						}
						else {
							termDocumentFrequencies.put(currentTerm, 1);
						}
					}
				}
			}
			Iterator<String> termItr2 = queryWithFrequency.keySet().iterator();
			while(termItr2.hasNext()) {
				String currentTerm = termItr2.next();
				if (termDocumentFrequencies.containsKey(currentTerm) != true) {
					termDocumentFrequencies.put(currentTerm, 0);
				}
			}
			double totalDocWordCount = 0.0;
			for (int k = 0; k < topicDocumentList.size(); k++) {
				Document currentDocument = topicDocumentList.get(k);
				double wordCount = currentDocument.getWordCount();
				totalDocWordCount += wordCount;
			}
			double totalNoOfDocs = topicDocumentList.size();
			double averageWordCount = (totalDocWordCount / totalNoOfDocs);
			HashMap<Document, Double> BM25Scores = new HashMap<Document, Double>();
			for (int l = 0; l < topicDocumentList.size(); l++) {
				Document currentDocument = topicDocumentList.get(l);
				double bm25Score = calculateBM25(currentDocument, query, averageWordCount, topicDocumentList.size(), termDocumentFrequencies);
				BM25Scores.put(currentDocument, bm25Score);
			}
			finalBM25Scores.put(currentTopic, BM25Scores);
		}
		return finalBM25Scores;
	}
	
	public static double calculateBM25(Document aDoc, String aQuery, double avgDocLen, int docNo, HashMap<String, Integer> dfs) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		//Gets every term in the query by splitting it on spaces
		String[] termsInQuery = aQuery.split(" ");
		//Stems each term in the query using snowball stemmer
		Class<?> stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");
        SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();
        for (int k = 0; k < termsInQuery.length; k++) {
    		stemmer.setCurrent(termsInQuery[k].toLowerCase());
    		stemmer.stem();
    		termsInQuery[k] = stemmer.getCurrent();
        }
        //Calculates the K used in BM25 calculations
		double K = (1.2 * (1 - 0.75) + 0.75 * (aDoc.getWordCount() / avgDocLen));
		//Intialises the BM25 score variable
		double bm25score = 0;
		//Loops through each term in the query
		for (int i = 0; i < termsInQuery.length; i++) {
			//Gets the number of documents the term from the query appears in
			double termFrequency = 0;
			if (dfs.containsKey(termsInQuery[i]) == true)  {
				termFrequency = dfs.get(termsInQuery[i]);
			}
			//The number of times the term from the query appears in the document
			double termFrequencyInDoc = aDoc.getTermCount(termsInQuery[i]);
			int termInQueryCount = 0;
			double termBM25Score;
			//Calculates the number of times the term appears in the query
			for (int j = 0; j < termsInQuery.length; j++) {
				if (termsInQuery[i].equals(termsInQuery[j])) {
					termInQueryCount++;
				}
			}
			//Calculates the final BM25 score for this term using the previously calculated variables
			//The BM25 score calculation is split up to make it easier to read and code
			double leftSide = (1 / ((termFrequency + 0.5) / (docNo - termFrequency + 0.5)));
			double middleSide = ((2.2 * termFrequencyInDoc) / (K + termFrequencyInDoc));
			double rideSide = ((101 * termInQueryCount) / (100 + termInQueryCount));
			//The final score
			termBM25Score = (Math.log(leftSide) * middleSide * rideSide);
			//Adds the score for that term to the total query score 
			bm25score += termBM25Score;
		}
		return bm25score;
	}
	
	public static ArrayList<Topic> generateTopics(File topicFile) throws IOException {
		String line = null;
		FileReader topicReader;
		ArrayList<Topic> topicList = new ArrayList<Topic>();
		topicReader = new FileReader(topicFile);
        BufferedReader bufferedReader = new BufferedReader(topicReader);
    	Boolean start = false;
    	String currentTopic = "";
    	ArrayList<String> importedTopics = new ArrayList<String>();
        while((line = bufferedReader.readLine()) != null) {
        	if (line.contains("</top>")) {
        		start = false;
        		importedTopics.add(currentTopic);
        		currentTopic = "";
        	}
        	if (line.contains("<top>")) {
        		start = true;
        	}     	
        	if (start == true) {
        		currentTopic += line;
        	}
        	currentTopic += "\n";
        }    
		topicReader.close();
		Iterator<String> topicIterator = importedTopics.iterator();
		while (topicIterator.hasNext() == true) {
			String[] newTopic = topicIterator.next().split("\n");
			String topicID = "";
			String topicDesc = "";
			String topicTitle = "";
			Boolean searchingDesc = false;
			for (int i = 0; i < newTopic.length; i++) {
				if (i <= newTopic.length - 2) {
					if (newTopic[i  + 1].contains("<narr>")) {
						searchingDesc = false;
					}
					if (newTopic[i  + 1].contains("<narr>") == false && searchingDesc == true && topicDesc.equals("") == false) {
						topicDesc += " ";
					}
				}
				if (searchingDesc == true) {
					topicDesc += newTopic[i];
				}
				if (newTopic[i].contains("<num>")) {
					topicID = newTopic[i].replaceAll("[<>:a-zA-Z ]", "");
				}
				if (newTopic[i].contains("<title>")) {
					topicTitle = newTopic[i].replaceAll("<title>  ", "");
					topicTitle = topicTitle.replaceAll("<title> ", "");
					topicTitle = topicTitle.replaceAll("<title>", "");
				}
				if (newTopic[i].contains("<desc>")) {
					searchingDesc = true;
				}
			}
			Topic finalTopic = new Topic(topicID, topicTitle, topicDesc);
			topicList.add(finalTopic);
		}
		return topicList;
	}
	
	public static HashMap<Topic, ArrayList<Document>> createDocuments(File datasetFolder, ArrayList<Topic> topicList, ArrayList<String> stoppingWordsList) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		HashMap<Topic, ArrayList<Document>> topicDocMap = new HashMap<Topic, ArrayList<Document>>();
		for (int i = 0; i < topicList.size(); i++) {
			Topic currentTopic = topicList.get(i);
			String line = "";
			String topicDatasetFolderPath = (datasetFolder.getPath() + "\\Training" + currentTopic.getTopicID());
			File topicDatasetFolder = new File(topicDatasetFolderPath);
			ArrayList<Document> topicDocumentList = new ArrayList<Document>();
			for (File file : topicDatasetFolder.listFiles()) {
	            FileReader fileReader;
				fileReader = new FileReader(file);
	            BufferedReader bufferedReader = new BufferedReader(fileReader);
	            String current_id = "";
	            Document new_document = null;
	            Boolean start_end = false;
	            while((line = bufferedReader.readLine()) != null) {
	            	if (line.contains("newsitem")) 
	            	{
						String[] documentID = line.split(" ");
						for (int j = 0; j < documentID.length; j++) 
						{
							if (documentID[j].contains("itemid")) 
							{
								String[] newString = documentID[j].split("\"");
								current_id = newString[1];
								new_document = new Document(current_id, stoppingWordsList);
							}
						}	            	
 	            	}
	            	if (line.contains("</text>")) 
	            	{
	            		start_end = false;
	            	}
	            	if (line.contains("<title>")) {
		            	line = line.replaceAll("<title>", "");
		            	line = line.replaceAll("</title>", "");
	            		line = line.replaceAll("&quot;", "");
	            		line = line.replaceAll(",&quot;", "");
	            		line = line.replaceAll("\\p{Punct}", "");
	            		line = line.replaceAll("[^a-zA-Z ]","");
	            		String[] brokenLine = line.split(" ");
	            		for (int k = 0; k < brokenLine.length; k++) 
	            		{
	            			brokenLine[k] = brokenLine[k].replaceAll("\\s+", "");
	            			new_document.addTerm(brokenLine[k]);
	            		}
	            	}
	            	if (line.contains("<headline>")) {
		            	line = line.replaceAll("<headline>", "");
		            	line = line.replaceAll("</headline>", "");
	            		line = line.replaceAll("&quot;", "");
	            		line = line.replaceAll(",&quot;", "");
	            		line = line.replaceAll("\\p{Punct}", "");
	            		line = line.replaceAll("[^a-zA-Z ]","");
	            		String[] brokenLine = line.split(" ");
	            		for (int k = 0; k < brokenLine.length; k++) 
	            		{
	            			brokenLine[k] = brokenLine[k].replaceAll("\\s+", "");
	            			new_document.addTerm(brokenLine[k].toLowerCase());
	            		}
	            	}
	            	if (start_end == true) {
		            	line = line.replaceAll("<p>", "");
		            	line = line.replaceAll("</p>", "");
	            		line = line.replaceAll("&quot;", "");
	            		line = line.replaceAll(",&quot;", "");
	            		line = line.replaceAll("\\p{Punct}", "");
	            		line = line.replaceAll("[^a-zA-Z ]","");
	            		String[] brokenLine = line.split(" ");
	            		for (int k = 0; k < brokenLine.length; k++) 
	            		{
	            			brokenLine[k] = brokenLine[k].replaceAll("\\s+", "");
	            			new_document.addTerm(brokenLine[k].toLowerCase());
	            		}

	            	}
	            	if (line.contains("<text>")) 
	            	{
	            		start_end = true;
	            	}

	            }
	            bufferedReader.close();
	            topicDocumentList.add(new_document);
			}
			topicDocMap.put(topicList.get(i), topicDocumentList);
		}
		return topicDocMap;
	}
	
	public static ArrayList<String> generateStoppingWordsList(File stoppingWordsFile) throws IOException {
		ArrayList<String> stoppingWords = new ArrayList<String>();
		FileReader fileReader = new FileReader(stoppingWordsFile);
		String line = "";
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        while((line = bufferedReader.readLine()) != null) {
        	for (String word : line.split(",")) {
        		stoppingWords.add(word);
        	}
        }
        bufferedReader.close();
        return stoppingWords;
	}
	
	public static HashMap<Document, Double> sortHashMap(HashMap<Document, Double> docList) {

	    LinkedHashMap<Document, Double> sortedMap = new LinkedHashMap<>();
	    List<Document> mapKeys = new ArrayList<>(docList.keySet());
	    List<Double> mapValues = new ArrayList<>(docList.values());
	    Collections.sort(mapValues);
	    Collections.reverse(mapValues);
	    Iterator<Double> valueIt = mapValues.iterator();
	    while (valueIt.hasNext()) {
	        Double val = valueIt.next();
	        Iterator<Document> keyIt = mapKeys.iterator();

	        while (keyIt.hasNext()) {
	        	Document nextKey = keyIt.next();
	            Double comp1 = docList.get(nextKey);
	            Double comp2 = val;

	            if (comp1.equals(comp2)) {
	                keyIt.remove();
	                sortedMap.put(nextKey, val);
	                break;
	            }
	        }
	    }
		return sortedMap;
	}
	
	public static HashMap<String, Double> sortStringHashMap(HashMap<String, Double> wordList) {

	    LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
	    List<String> mapKeys = new ArrayList<>(wordList.keySet());
	    List<Double> mapValues = new ArrayList<>(wordList.values());
	    Collections.sort(mapValues);
	    Collections.reverse(mapValues);
	    Iterator<Double> valueIt = mapValues.iterator();
	    while (valueIt.hasNext()) {
	        Double val = valueIt.next();
	        Iterator<String> keyIt = mapKeys.iterator();

	        while (keyIt.hasNext()) {
	        	String nextKey = keyIt.next();
	            Double comp1 = wordList.get(nextKey);
	            Double comp2 = val;

	            if (comp1.equals(comp2)) {
	                keyIt.remove();
	                sortedMap.put(nextKey, val);
	                break;
	            }
	        }
	    }
		return sortedMap;
	}

}
