package informationFiltering;

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

import informationRetrieval.Document;
import informationRetrieval.Topic;

public class Main {

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		File topics = new File("..\\TextFiles\\TopicStatements101-150.txt");
		File datasetFolder = new File("..\\TextFiles\\dataset101-150");
		File relevanceResults = new File("..\\Outputs\\DocumentRelevanceDecisions");
		File stoppingWordsList = new File("..\\TextFiles\\common-english-words.txt");
		File resultsDir = new File("..\\Outputs\\IF_Results\\");
		
		
		ArrayList<Topic> topicList = generateTopics(topics);
		ArrayList<String> stoppingWords = generateStoppingWordsList(stoppingWordsList);
		HashMap<Topic, ArrayList<Document>> topicDocMap = createDocuments(datasetFolder, topicList, stoppingWords);
		HashMap<Topic, ArrayList<Document>> relevantDocuments = generateRelevantDocuments(topicDocMap, relevanceResults);
		HashMap<Topic, ArrayList<Document>> nonRelevantDocuments = generateNonRelevantDocuments(topicDocMap, relevanceResults);
		HashMap<Topic, HashMap<String, Double>> topicTermWeights =  BM25TermWeighting(topicDocMap, relevantDocuments, nonRelevantDocuments);
		HashMap<Topic, HashMap<Document, Double>> finalRankings = rankDocuments(topicDocMap, topicTermWeights);
		Iterator<Topic> topicItr = finalRankings.keySet().iterator();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			FileWriter outputWriter = new FileWriter((resultsDir.getAbsolutePath() + "\\result" +currentTopic.getTopicID() + ".dat"));
			HashMap<Document, Double> docScores = finalRankings.get(currentTopic);
			docScores = sortHashMap(docScores);
			Iterator<Document> docItr = docScores.keySet().iterator();
			while (docItr.hasNext()) {
				Document currentDocument = docItr.next();
				double docScore = docScores.get(currentDocument);
				outputWriter.write(currentDocument.getDocumentID() + "\t" + docScore + "\n");
			}
			outputWriter.close();
		}
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

	public static HashMap<Topic, ArrayList<Document>> generateRelevantDocuments(HashMap<Topic, ArrayList<Document>> topicDocMap, File relevanceDecisions) throws IOException {
		HashMap<Topic, ArrayList<Document>> relevantDocuments = new HashMap<Topic, ArrayList<Document>>();
		Iterator<Topic> topicItr = topicDocMap.keySet().iterator();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			ArrayList<Document> releDocs = new ArrayList<Document>();
			ArrayList<Document> documentsForTopic = topicDocMap.get(currentTopic);
			String topicID = currentTopic.getTopicID();
			File topicRelevanceDecisions = new File(relevanceDecisions.getAbsolutePath() + "\\Training" + topicID + ".txt");
			String line = "";
            FileReader fileReader;
			fileReader = new FileReader(topicRelevanceDecisions);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
            	String[] splitLine = line.split(" ");
            	if (splitLine[2].equals("1")) {
            		for (int i = 0; i < documentsForTopic.size(); i++) {
            			Document currentDocument = documentsForTopic.get(i);
            			if (currentDocument.getDocumentID().equals(splitLine[1])) {
            				releDocs.add(currentDocument);
            			}
            		}
            	}
            }
            relevantDocuments.put(currentTopic, releDocs);
            bufferedReader.close();
		}
		return relevantDocuments;
	}

	public static HashMap<Topic, ArrayList<Document>> generateNonRelevantDocuments(HashMap<Topic, ArrayList<Document>> topicDocMap, File relevanceDecisions) throws IOException {
		HashMap<Topic, ArrayList<Document>> nonRelevantDocuments = new HashMap<Topic, ArrayList<Document>>();
		Iterator<Topic> topicItr = topicDocMap.keySet().iterator();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			ArrayList<Document> nonReleDocs = new ArrayList<Document>();
			ArrayList<Document> documentsForTopic = topicDocMap.get(currentTopic);
			String topicID = currentTopic.getTopicID();
			File topicRelevanceDecisions = new File(relevanceDecisions.getAbsolutePath() + "\\Training" + topicID + ".txt");
			String line = "";
            FileReader fileReader;
			fileReader = new FileReader(topicRelevanceDecisions);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
            	String[] splitLine = line.split(" ");
            	if (splitLine[2].equals("0")) {
            		for (int i = 0; i < documentsForTopic.size(); i++) {
            			Document currentDocument = documentsForTopic.get(i);
            			if (currentDocument.getDocumentID().equals(splitLine[1])) {
            				nonReleDocs.add(currentDocument);
            			}
            		}
            	}
            }
            nonRelevantDocuments.put(currentTopic, nonReleDocs);
            bufferedReader.close();
		}
		return nonRelevantDocuments;
	}

	public static HashMap<Topic, HashMap<String, Double>> BM25TermWeighting(HashMap<Topic, ArrayList<Document>> allDocuments, HashMap<Topic, ArrayList<Document>> relevantDocuments, HashMap<Topic, ArrayList<Document>> nonRelevantDocuments) {
		Iterator<Topic> topicItr = allDocuments.keySet().iterator();
		HashMap<Topic, HashMap<String, Double>> finalTermTopicWeights = new HashMap<Topic, HashMap<String, Double>>();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			ArrayList<Document> allTopicDocuments = allDocuments.get(currentTopic);
			ArrayList<Document> topicReleDocuments = relevantDocuments.get(currentTopic);
			int numberOfDocs = allTopicDocuments.size();
			int numberOfRelevantDocs = topicReleDocuments.size();
			ArrayList<String> relevantTerms = new ArrayList<String>();
			for (int i = 0; i < numberOfRelevantDocs; i++) {
				Document currentDocument = topicReleDocuments.get(i);
				ArrayList<String> termsInDoc = currentDocument.getSortedTermList();
				for (int j = 0; j < termsInDoc.size(); j++) {
					String currentTerm = termsInDoc.get(j);
					if (relevantTerms.contains(currentTerm) == false) {
						relevantTerms.add(currentTerm);
					}
				}
			}
			HashMap<String, Integer> termInDocsMap = new HashMap<String, Integer>();
			HashMap<String, Integer> termInRelevantDocsMap = new HashMap<String, Integer>();
			for (int k = 0; k < relevantTerms.size(); k++) {
				String currentTerm = relevantTerms.get(k);
				termInDocsMap.put(currentTerm, 0);
				termInRelevantDocsMap.put(currentTerm, 0);		
			}
			for (int l = 0; l < relevantTerms.size(); l++) {
				String currentTerm = relevantTerms.get(l);
				for (int m = 0; m < numberOfDocs; m++) {
					Document currentDocument = allTopicDocuments.get(m);
					if (currentDocument.getTermCount(currentTerm) > 0) {
						termInDocsMap.put(currentTerm, (termInDocsMap.get(currentTerm) + 1));
					}
				}
			}
			for (int n = 0; n < relevantTerms.size(); n++) {
				String currentTerm = relevantTerms.get(n);
				for (int o = 0; o < numberOfRelevantDocs; o++) {
					Document currentDocument = topicReleDocuments.get(o);
					if (currentDocument.getTermCount(currentTerm) > 0) {
						termInRelevantDocsMap.put(currentTerm, (termInRelevantDocsMap.get(currentTerm) + 1));
					}
				}
			}
			HashMap<String, Double> termWeights = new HashMap<String, Double>();
			for (int p = 0; p < relevantTerms.size(); p++) {
				String currentTerm = relevantTerms.get(p);
				double termInDocs = termInDocsMap.get(currentTerm);
				double termInRelevantDocs = termInRelevantDocsMap.get(currentTerm);
				double noOfDocs = numberOfDocs;
				double noOfReleDocs = numberOfRelevantDocs;
				double left = ((termInRelevantDocs + 2) / (noOfReleDocs - termInRelevantDocs + 2));
				double right = ((termInDocs - termInRelevantDocs + 2) / (((noOfDocs - termInDocs) - (noOfReleDocs - termInRelevantDocs)) + 2));
				double termWeight = (left / right);
				termWeights.put(currentTerm, termWeight);
			}
			finalTermTopicWeights.put(currentTopic, termWeights);
		}
		return finalTermTopicWeights;
	}

	public static HashMap<Topic, HashMap<Document, Double>> rankDocuments(HashMap<Topic, ArrayList<Document>> topicDocMap, HashMap<Topic, HashMap<String, Double>> rankedTerms) {
		HashMap<Topic, HashMap<Document, Double>> rankedDocuments = new HashMap<Topic, HashMap<Document, Double>>();
		Iterator<Topic> topicItr = topicDocMap.keySet().iterator();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			ArrayList<Document> documentList = topicDocMap.get(currentTopic);
			HashMap<Document, Double> finalDocScores = new HashMap<Document, Double>();
			HashMap<String, Double> termList = rankedTerms.get(currentTopic);
			for (int i = 0; i < documentList.size(); i++) {
				Document currentDocument = documentList.get(i);
				Iterator<String> termItr = termList.keySet().iterator();
				double documentScore = 0.0;
				while (termItr.hasNext()) {
					String currentTerm = termItr.next();
					double termScore = termList.get(currentTerm);
					if (currentDocument.getTermCount(currentTerm) > 0) {
						documentScore += termScore;
					}
				}
				finalDocScores.put(currentDocument, documentScore);
			}
			rankedDocuments.put(currentTopic, finalDocScores);
		}
		return rankedDocuments;
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

}

