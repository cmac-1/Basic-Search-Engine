package informationFiltering;

public class Topic {
	String topicID;
	String title;
	String description;
	public Topic(String topicID, String topicTitle, String topicDescription) {
		this.topicID = topicID;
		this.title = topicTitle;
		this.description = topicDescription;
	}
	
	public String getTopicID() {
		return topicID;
	}
	public String getTitle() {
		return title;
	}
	public String getDescription() {
		return description;
	}
}
