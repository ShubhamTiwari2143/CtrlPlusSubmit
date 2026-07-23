package in.ctrlplussubmit.model;

public class SubmissionHistory {

    private String filePath;
    private String fileName;
    
	public SubmissionHistory(String filePath, String fileName) {
		super();
		this.filePath = filePath;
		this.fileName = fileName;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}    
}
