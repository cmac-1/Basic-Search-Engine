package informationRetrieval;

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
		File outputDir = new File("..\\Outputs\\DocumentRelevanceDecisions\\");
		
		ArrayList<Topic> topicList = generateTopics(topics);
		ArrayList<String> stoppingWords = generateStoppingWordsList(stoppingWordsList);
		HashMap<Topic, ArrayList<Document>> topicDocMap = createDocuments(datasetFolder, topicList, stoppingWords);
		
		HashMap<Topic, HashMap<Document, Double>> finalScores = relevanceModelRankings(topicDocMap, stoppingWords);
		Iterator<Topic> finalTopicItr = finalScores.keySet().iterator();
		while (finalTopicItr.hasNext()) {
			Topic currentTopic = finalTopicItr.next();
			FileWriter outputWriter = new FileWriter((outputDir.getAbsolutePath() + "\\Training" + currentTopic.getTopicID() + ".txt"));
			HashMap<Document, Double> mapForTopic = finalScores.get(currentTopic);
			mapForTopic = sortHashMap(mapForTopic);
			Iterator<Document> docItr = mapForTopic.keySet().iterator();
			int counter = 0;
			while (docItr.hasNext()) {
				Document currentDocument = docItr.next();
				if (counter < 5) {
					outputWriter.write("R" + currentTopic.getTopicID() + " " + currentDocument.getDocumentID() + " " + "1\n");
				}
				if (counter > mapForTopic.keySet().size() - 6) {
					outputWriter.write("R" + currentTopic.getTopicID() + " " + currentDocument.getDocumentID() + " " + "0\n");
				}
				counter++;
			}
			outputWriter.close();
		}

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
	
	public static HashMap<Topic, HashMap<Document, Double>> relevanceModelRankings(HashMap<Topic, ArrayList<Document>> topicDocMap, ArrayList<String> stoppingList) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		HashMap<Topic, HashMap<Document, Double>> finalRankings = new HashMap<Topic, HashMap<Document, Double>>();
		Class<?> stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");
        SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();
		Iterator<Topic> topicItr = topicDocMap.keySet().iterator();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			String query = (currentTopic.getTitle() + currentTopic.getDescription());
			query = query.replaceAll("&quot;", "");
			query = query.replaceAll(",&quot;", "");
			query = query.replaceAll("\\p{Punct}", "");
			query = query.replaceAll("[^a-zA-Z ]","");
			String[] termsInQuery = query.split(" ");
			ArrayList<String> queryTerms = new ArrayList<String>();
			for (int i = 0; i < termsInQuery.length; i++) {
				String currentTerm = termsInQuery[i];
				stemmer.setCurrent(currentTerm.toLowerCase());
				stemmer.stem();
				currentTerm = stemmer.getCurrent();
				if (stoppingList.contains(currentTerm) != true && currentTerm.equals("african") == false) {
					queryTerms.add(currentTerm);
				}
				if (stoppingList.contains(currentTerm) != true && currentTerm.equals("african")) {
					queryTerms.add("africa");
				}
			}
			HashMap<String, Integer> queryInCollectionCount = new HashMap<String, Integer>();
			for (int j = 0; j < queryTerms.size(); j++) {
				String queryTerm = queryTerms.get(j);
				queryInCollectionCount.put(queryTerm, 0);
			}
			for (int l = 0; l < queryTerms.size(); l++) {
				ArrayList<Document> documentList = topicDocMap.get(currentTopic);
				String currentTerm = queryTerms.get(l);
				for (int m = 0; m < documentList.size(); m++) {
					Document currentDocument = documentList.get(m);
					int termCountAcrossCollection = queryInCollectionCount.get(currentTerm);
					if (currentDocument.getTermCount(currentTerm) > 0) {
						queryInCollectionCount.put(currentTerm, (termCountAcrossCollection + currentDocument.getTermCount(currentTerm)));
					}
				}
			}
			ArrayList<Document> topicDocuments = topicDocMap.get(currentTopic);
			double collectionWordCount = 0.0;
			for (int n = 0; n < topicDocuments.size(); n++) {
				Document currentDocument = topicDocuments.get(n);
				collectionWordCount += (currentDocument.getWordCount());
			}
			HashMap<Document, Double> queryLikelihoodScores = new HashMap<Document, Double>();
			for (int n = 0; n < topicDocuments.size(); n++) {
				double queryLikelihoodScore = 0.0;
				Document currentDocument = topicDocuments.get(n);
				Iterator<String> queryTermItr = queryInCollectionCount.keySet().iterator();
				while (queryTermItr.hasNext()) {
					String currentTerm = queryTermItr.next();
					double queryTermLikelihoodScore = 0.0;
					double termCountInDocument = currentDocument.getTermCount(currentTerm);
					double termCountInCollection = queryInCollectionCount.get(currentTerm);
					double documentWordCount = (currentDocument.getWordCount());
					if (termCountInCollection > 0) {
						queryTermLikelihoodScore = Math.log((termCountInDocument + 250.0 * (termCountInCollection / collectionWordCount)) / (documentWordCount + 250.0));
						queryLikelihoodScore += queryTermLikelihoodScore;
					}
				}
				queryLikelihoodScores.put(currentDocument, queryLikelihoodScore);
			}
			queryLikelihoodScores = sortHashMap(queryLikelihoodScores);
			HashMap<String, Integer> vocabulary = new HashMap<String, Integer>();
			Iterator<Document> docItr = queryLikelihoodScores.keySet().iterator();
			int counter = 0;
			while (docItr.hasNext() && counter != 6) {
				Document currentDocument = docItr.next();
				ArrayList<String> termList = currentDocument.getSortedTermList();
				for (int p = 0; p < termList.size(); p++) {
					vocabulary.put(termList.get(p), 0);
				}
				counter++;
			}
			Iterator<String> vocabItr = vocabulary.keySet().iterator();
			while (vocabItr.hasNext()) {
				String currentTerm = vocabItr.next();
				int counter2 = 0;
				Iterator<Document> docItr2 = queryLikelihoodScores.keySet().iterator();
				while (docItr2.hasNext() && counter2 != 8) {
					Document currentDocument = docItr2.next();
					if (currentDocument.getTermCount(currentTerm) > 0) {
						int currentCount = vocabulary.get(currentTerm);
						int countInDoc = currentDocument.getTermCount(currentTerm);
						vocabulary.put(currentTerm, (currentCount + countInDoc));
					}
					counter++;
				}
			}
			double normalisingRelevanceScore = 0.0;
			Iterator<String> termItr = vocabulary.keySet().iterator();
			while (termItr.hasNext()) {
				double scoreForTerm = 0.0;
				String currentTerm = termItr.next();
				int counter2 = 0;
				Iterator<Document> docItr2 = queryLikelihoodScores.keySet().iterator();
				while (docItr2.hasNext() && counter2 != 8) {
					Document currentDocument = docItr2.next();
					double queryLikelihoodScore = queryLikelihoodScores.get(currentDocument);
					double frequencyInDoc = currentDocument.getTermCount(currentTerm);
					double frequencyInCollection = vocabulary.get(currentTerm);
					double documentWordCount = currentDocument.getWordCount();
					if (frequencyInCollection > 0) {
						double scoreForDoc = (1.0 * (frequencyInDoc + 250.0 * (frequencyInCollection / collectionWordCount)) / (documentWordCount + 250.0) * queryLikelihoodScore);
						scoreForTerm += scoreForDoc;
					}
					counter2++;
				}
				normalisingRelevanceScore += scoreForTerm;
			}
			HashMap<String, Double> wordInRelevanceModelScores = new HashMap<String, Double>();
			Iterator<String> finalTermItr = vocabulary.keySet().iterator();
			while (finalTermItr.hasNext()) {
				String currentTerm = finalTermItr.next();
				double scoreForTerm = 0.0;
				int counter2 = 0;
				Iterator<Document> finalDocItr = queryLikelihoodScores.keySet().iterator();
				while (finalDocItr.hasNext() && counter2 != 8) {
					Document currentDocument = finalDocItr.next();
					double queryLikelihoodScore = queryLikelihoodScores.get(currentDocument);
					double frequencyInDoc = currentDocument.getTermCount(currentTerm);
					double frequencyInCollection = vocabulary.get(currentTerm);
					double documentWordCount = currentDocument.getWordCount();
					if (frequencyInCollection > 0) {
						double scoreForDoc = (1.0 * (frequencyInDoc + 250.0 * (frequencyInCollection / collectionWordCount)) / (documentWordCount + 250.0) * queryLikelihoodScore);
						scoreForTerm += scoreForDoc;
					}
					counter2++;
				}
				scoreForTerm = (scoreForTerm / normalisingRelevanceScore);
				wordInRelevanceModelScores.put(currentTerm, scoreForTerm);
			}
			wordInRelevanceModelScores = sortStringHashMap(wordInRelevanceModelScores);
			HashMap<String, Integer> newVocab = new HashMap<String, Integer>();
			Iterator<String> termItr2 = wordInRelevanceModelScores.keySet().iterator();
			int counter3 = 0;
			while (termItr2.hasNext() && counter3 != 15) {
				String currentTerm = termItr2.next();
				newVocab.put(currentTerm, 0);
				counter3++;
			}
			Iterator<String> vocabItr2 = newVocab.keySet().iterator();
			while (vocabItr2.hasNext()) {
				String currentTerm = vocabItr2.next();
				for (int s = 0; s < topicDocuments.size(); s++) {
					Document currentDocument  = topicDocuments.get(s);
					if (currentDocument.getTermCount(currentTerm) > 0) {
						int countInDoc = currentDocument.getTermCount(currentTerm);
						int currentValue = newVocab.get(currentTerm);
						newVocab.put(currentTerm, (countInDoc + currentValue));
					}					
				}
			}
			HashMap<Document, Double> finalDocumentScores = new HashMap<Document, Double>();
			for (int t = 0; t < topicDocuments.size(); t++) {
				Document currentDocument = topicDocuments.get(t);
				double documentScore = 0.0;
				Iterator<String> termInVocabItr = newVocab.keySet().iterator();
				while (termInVocabItr.hasNext()) {
					String currentTerm = termInVocabItr.next();
					double relevanceModelScore = wordInRelevanceModelScores.get(currentTerm);
					double termCountInDoc = currentDocument.getTermCount(currentTerm);
					double termCountInCollection = newVocab.get(currentTerm);
					double docWordCount = currentDocument.getWordCount();
					double initalScore = Math.log((termCountInDoc + 250.0 * (termCountInCollection / collectionWordCount)) / (docWordCount + 250.0));
					double scoreForTerm = (relevanceModelScore * initalScore);
					documentScore += scoreForTerm;
				}
				finalDocumentScores.put(currentDocument, documentScore);
			}
			finalRankings.put(currentTopic, finalDocumentScores);
		}
		return finalRankings;
	}
}
