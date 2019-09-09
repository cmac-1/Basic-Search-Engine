package evaluation;

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

	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		File topics = new File("..\\TextFiles\\TopicStatements101-150.txt");
		File datasetFolder = new File("..\\TextFiles\\dataset101-150");
		File baselineResults = new File("..\\Outputs\\BaselineResults\\");
		File stoppingWordsList = new File("..\\TextFiles\\common-english-words.txt");
		File IFResults = new File("..\\Outputs\\IF_Results\\");
		File evaluationResultsDir = new File("..\\Outputs\\EvaluationResults");
		File relevanceJudgements = new File("..\\TextFiles\\topicassignment101-150");
		
		ArrayList<Topic> topicList = generateTopics(topics);
		ArrayList<String> stoppingWords = generateStoppingWordsList(stoppingWordsList);
		HashMap<Topic, ArrayList<Document>> topicDocMap = createDocuments(datasetFolder, topicList, stoppingWords);
		HashMap<Topic, ArrayList<Document>> assignedRelevantDocuments = generateRelevantDocuments(topicDocMap, relevanceJudgements);
		HashMap<Topic, HashMap<Document, Double>> rankedDocuments = getRankedDocuments(topicDocMap, IFResults);
		HashMap<Topic, HashMap<Document, Double>> rankedBaselineDocuments = getBaseLineRankedDocuments(topicDocMap, baselineResults);
		HashMap<Topic, Double> baselineMAP = calculateMAP(rankedBaselineDocuments, assignedRelevantDocuments);
		HashMap<Topic, Double> modelMAP = calculateMAP(rankedDocuments, assignedRelevantDocuments);
		HashMap<Topic, Double> baselineFMeasure = getFMeasureScore(rankedBaselineDocuments, assignedRelevantDocuments);
		HashMap<Topic, Double> modelFMeasure = getFMeasureScore(rankedDocuments, assignedRelevantDocuments);
 		Iterator<Topic> topicItr = modelMAP.keySet().iterator();
 		Iterator<Topic> topicItr2 = baselineMAP.keySet().iterator();
 		while (topicItr.hasNext()) {
 			Topic baseTopic = topicItr2.next();
 			Topic modelTopic = topicItr.next();
			FileWriter outputWriter = new FileWriter((evaluationResultsDir.getAbsolutePath() + "\\EvaluationResult" + baseTopic.getTopicID() + ".dat"));
 			outputWriter.write("baselineModel, myLearningModel");
			double baseMAP = baselineMAP.get(baseTopic);
 			double modelMAPresult = modelMAP.get(modelTopic);
 			double baseFMeasure = baselineFMeasure.get(baseTopic);
 			double modelFMeasureResult = modelFMeasure.get(modelTopic);
 			outputWriter.write("\n" + baseMAP + ", " + modelMAPresult + "\t(MAP)");
 			outputWriter.write("\n" + baseFMeasure + ", " + modelFMeasureResult + "\t(F-Measure)");
 			HashMap<Document, Double> docScores = rankedDocuments.get(modelTopic);
 			Iterator<Document> docItr = docScores.keySet().iterator();
 			int counter = 1;
 			HashMap<Integer, String> topDocs = new HashMap<Integer, String>();
 			while (docItr.hasNext() && counter != 11) {
 				Document currentDoc = docItr.next();
 				topDocs.put(counter, currentDoc.getDocumentID());
 				counter++;
 			}
 			HashMap<Integer, ArrayList<Double>> resultsAtEachRankModel = calculateForRanks(topDocs, assignedRelevantDocuments.get(modelTopic));
 			Iterator<Integer> rankItr = resultsAtEachRankModel.keySet().iterator();
 			outputWriter.write("\nMy Learning Model results at each rank: \n");;
 			while (rankItr.hasNext()) {
 				int currentRank = rankItr.next();
 				ArrayList<Double> results = resultsAtEachRankModel.get(currentRank);
 				double recall = results.get(0);
 				double precision = results.get(1);
 				outputWriter.write("Rank: " + currentRank + ", precision = " + precision + ", recall = " + recall + "\n");
 			}
 			HashMap<Document, Double> baseDocScores = rankedDocuments.get(modelTopic);
 			Iterator<Document> baseDocItr = baseDocScores.keySet().iterator();
 			int counter2 = 1;
 			HashMap<Integer, String> baseTopDocs = new HashMap<Integer, String>();
 			while (baseDocItr.hasNext() && counter2 != 11) {
 				Document currentDoc = baseDocItr.next();
 				baseTopDocs.put(counter2, currentDoc.getDocumentID());
 				counter2++;
 			}
 			HashMap<Integer, ArrayList<Double>> resultsAtEachRankBase = calculateForRanks(baseTopDocs, assignedRelevantDocuments.get(modelTopic));
 			Iterator<Integer> baseRankItr = resultsAtEachRankBase.keySet().iterator();
 			outputWriter.write("\nBaseline results at each rank: \n");;
 			while (baseRankItr.hasNext()) {
 				int currentRank = baseRankItr.next();
 				ArrayList<Double> results = resultsAtEachRankBase.get(currentRank);
 				double recall = results.get(0);
 				double precision = results.get(1);
 				outputWriter.write("Rank: " + currentRank + ", precision = " + precision + ", recall = " + recall + "\n");
 			}

  			outputWriter.close();
 		}
	}
	
	public static HashMap<Topic, Double> calculateMAP(HashMap<Topic, HashMap<Document, Double>> topicDocMap, HashMap<Topic, ArrayList<Document>> relevantDocuments) {
		HashMap<Topic, Double> finalMAP = new HashMap<Topic, Double>();
		Iterator<Topic> topicItr = topicDocMap.keySet().iterator();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			HashMap<Document, Double> docScoreMap = topicDocMap.get(currentTopic);
			ArrayList<Document> relevantDocs = relevantDocuments.get(currentTopic);
			Object[] rankedDocuments = docScoreMap.keySet().toArray();
			double totalPrecision = 0.0;
			
			for (int i = 0; i < rankedDocuments.length; i++) {
				ArrayList<Document> retrievedDocuments = new ArrayList<Document>();
				ArrayList<Document> intersectedDocuments = new ArrayList<Document>();
				for (int j = 0; j <= i; j++) {
					Document currentDoc = (Document) rankedDocuments[j];
					retrievedDocuments.add(currentDoc);
				}
				for (int k = 0; k < retrievedDocuments.size(); k++) {
					Document currentDoc = retrievedDocuments.get(k);
					if (relevantDocs.contains(currentDoc)) {
						intersectedDocuments.add(currentDoc);
					}
				}
				double intersectSize = intersectedDocuments.size();
				double retrievedSize = retrievedDocuments.size();
				double currentPrecision = (intersectSize / retrievedSize);
				totalPrecision += currentPrecision;
			}
			double noOfDocs = rankedDocuments.length;
			double averagePrecision = (totalPrecision / noOfDocs);
			finalMAP.put(currentTopic, averagePrecision);
		}
		return finalMAP;
	}
	
	public static HashMap<Topic, Double> getFMeasureScore(HashMap<Topic, HashMap<Document, Double>> topicDocMap, HashMap<Topic, ArrayList<Document>> relevantDocuments) {
		HashMap<Topic, Double> finalFMeasure = new HashMap<Topic, Double>();
		Iterator<Topic> topicItr = topicDocMap.keySet().iterator();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			HashMap<Document, Double> docScoreMap = topicDocMap.get(currentTopic);
			ArrayList<Document> relevantDocs = relevantDocuments.get(currentTopic);
			Object[] rankedDocuments = docScoreMap.keySet().toArray();
			double totalfMeasure = 0.0;
			
			for (int i = 0; i < rankedDocuments.length; i++) {
				ArrayList<Document> retrievedDocuments = new ArrayList<Document>();
				for (int j = 0; j <= i; j++) {
					Document currentDoc = (Document) rankedDocuments[j];
					retrievedDocuments.add(currentDoc);
				}
				double recall = calculateRecall(relevantDocs, retrievedDocuments);
				double precision = calculatePrecision(relevantDocs, retrievedDocuments);
				double currentfMeasure = calculateFMeasure(recall, precision);
				if (Double.isNaN(currentfMeasure) != true) {
					totalfMeasure += currentfMeasure;
				}
			}
			double noOfDocs = rankedDocuments.length;
			double averagefMeasure = (totalfMeasure / noOfDocs);
			finalFMeasure.put(currentTopic, averagefMeasure);
		}
		return finalFMeasure;
	}
 	
	public static double calculateRecall(ArrayList<Document> relevantDocuments, ArrayList<Document> retrievedDocuments) {
		//Create new intersected documents array
		ArrayList<Document> intersectedDocuments = new ArrayList<Document>();
		//Loops through each relevant document
		for (int i = 0; i < relevantDocuments.size(); i++) {
			//If the relevant document has also been retrieved, add it to intersected documents array list
			if (retrievedDocuments.contains(relevantDocuments.get(i))) {
				intersectedDocuments.add(relevantDocuments.get(i));
			}
		}
		//Get size of intersected documents array list
		int intersectedDocSize = intersectedDocuments.size();
		//Get size of relevant documents array list
		int relevantDocSize = relevantDocuments.size();
		//Divide number of intersected documents by number of relevant documents, then convert to double
		double recallScore = (double)intersectedDocSize / relevantDocSize;	
		//Return the calculated recall
		return recallScore;		
	}
	
	public static double calculatePrecision(ArrayList<Document> relevantDocuments, ArrayList<Document> retrievedDocuments) {
		//Create new intersected documents array
		ArrayList<Document> intersectedDocuments = new ArrayList<Document>();
		//Loops through each relevant document
		for (int i = 0; i < relevantDocuments.size(); i++) {
			//If the relevant document has also been retrieved, add it intersected documents array list
			if (retrievedDocuments.contains(relevantDocuments.get(i))) {
				intersectedDocuments.add(relevantDocuments.get(i));
			}
		}
		//Get size of intersected documents array list
		int intersectedDocSize = intersectedDocuments.size();
		//Get size of retrieved documents array list
		int retrievedDocSize = retrievedDocuments.size();
		//Divide number of intersected documents by number of retrieved documents, then convert to double
		double precisionScore = (double)intersectedDocSize / retrievedDocSize;	
		//Return the calculated precision
		return precisionScore;		
	}

	public static double calculateFMeasure(double recall, double precision) {
		//Calculates fmeasure using given recall and precision
		double fMeasure = ((2 * recall * precision) / (recall + precision));
		//Return calculated fMeasure
		return fMeasure;
	}
	
	public static HashMap<Integer, ArrayList<Double>> calculateForRanks(HashMap<Integer, String> rankings, ArrayList<Document> relevantDocuments) {
		//Creates a hash map to store the final values
		HashMap<Integer, ArrayList<Double>> rankingRecallPrecision = new HashMap<Integer, ArrayList<Double>>();
		//Loops through each rank in the given hash map
		Iterator<Integer> rankItr = rankings.keySet().iterator();
		while (rankItr.hasNext()) {
			//Creates new array list to store intersected documents
			ArrayList<String> intersectedDocuments = new ArrayList<String>();
			//Gets current rank
			int currentRank = rankItr.next();
			//Creates an array list to store the recall and precision
			ArrayList<Double> recallAndPrecision = new ArrayList<Double>();
			//Loops through each ranking and checks to see if it is a relevant document. If it is, add it to intersected documents
			Iterator<Document> docItr = relevantDocuments.iterator();
			while (docItr.hasNext()) {
				Document currentDoc = docItr.next();
				String docId = currentDoc.getDocumentID();
				for (int i = 1; i <= currentRank; i++) {
					if(docId.equals(rankings.get(i))) {
						intersectedDocuments.add(rankings.get(i));
					}
				}
			}
			//Gets number of relevant docs
			double relevantDocSize = relevantDocuments.size();
			//Gets number of intersected docs
			double intersectedDocSize = intersectedDocuments.size();
			//Calculates recall and precision
			double currentRecall = intersectedDocSize / relevantDocSize;
			double currentPrecision = intersectedDocSize / currentRank;
			//Adds recall to first position, precsion to second precision
			recallAndPrecision.add(currentRecall);
			recallAndPrecision.add(currentPrecision);		
			//Adds rank and the corresponding recall and precision array list to the hash map
			rankingRecallPrecision.put(currentRank, recallAndPrecision);
		}
		//Returns the final hash map of ranking-recall/precision pairs
		return rankingRecallPrecision;		
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

	public static HashMap<Topic, HashMap<Document, Double>> getRankedDocuments(HashMap<Topic, ArrayList<Document>> topicDocMap, File resultsDir) throws IOException {
		HashMap<Topic, HashMap<Document, Double>> finalRankings = new HashMap<Topic, HashMap<Document, Double>>();
		Iterator<Topic> topicItr = topicDocMap.keySet().iterator();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			ArrayList<Document> docList = topicDocMap.get(currentTopic);
			HashMap<Document, Double> ranks = new HashMap<Document, Double>();
			for (int i = 0; i < docList.size(); i++) {
				Document currentDoc = docList.get(i);
				String docId = currentDoc.getDocumentID();
				File inputFile = new File(resultsDir.getAbsolutePath() + "\\result" + currentTopic.getTopicID() +".dat");
				FileReader inputReader = new FileReader(inputFile);
				BufferedReader buffer = new BufferedReader(inputReader);
				String line = "";
				while ((line = buffer.readLine()) != null) {
					String[] brokenLine = line.split("\t");
					if (brokenLine[0].equals(docId)) {
						ranks.put(currentDoc, Double.parseDouble(brokenLine[1]));
					}
				}
				buffer.close();
			}
			ranks = sortHashMap(ranks);
			finalRankings.put(currentTopic, ranks);
		}
		
		return finalRankings;
	}
	
	public static HashMap<Topic, HashMap<Document, Double>> getBaseLineRankedDocuments(HashMap<Topic, ArrayList<Document>> topicDocMap, File resultsDir) throws IOException {
		HashMap<Topic, HashMap<Document, Double>> finalRankings = new HashMap<Topic, HashMap<Document, Double>>();
		Iterator<Topic> topicItr = topicDocMap.keySet().iterator();
		while (topicItr.hasNext()) {
			Topic currentTopic = topicItr.next();
			ArrayList<Document> docList = topicDocMap.get(currentTopic);
			HashMap<Document, Double> ranks = new HashMap<Document, Double>();
			for (int i = 0; i < docList.size(); i++) {
				Document currentDoc = docList.get(i);
				String docId = currentDoc.getDocumentID();
				File inputFile = new File(resultsDir.getAbsolutePath() + "\\BaselineResult" + currentTopic.getTopicID() +".dat");
				FileReader inputReader = new FileReader(inputFile);
				BufferedReader buffer = new BufferedReader(inputReader);
				String line = "";
				while ((line = buffer.readLine()) != null) {
					String[] brokenLine = line.split(" ");
					if (brokenLine[1].equals(docId)) {
						ranks.put(currentDoc, Double.parseDouble(brokenLine[2]));
					}
				}
				buffer.close();
			}
			ranks = sortHashMap(ranks);
			finalRankings.put(currentTopic, ranks);
		}
		
		return finalRankings;
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
