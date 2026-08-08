package com.sqlprofiler.model;

public class Finding {
	private String ruleName;
    private String severity;     // CRITICAL, HIGH, MEDIUM, LOW
    private String confidence;   // HIGH, MEDIUM, LOW
    private String explanation;  // what is wrong, in plain English
    private String evidence;     // where exactly was it detected
    private String fixSql;       // the SQL fix to apply
	public Finding(String ruleName, String severity, String confidence, String explanation, String evidence,
			String fixSql) {
		super();
		this.ruleName = ruleName;
		this.severity = severity;
		this.confidence = confidence;
		this.explanation = explanation;
		this.evidence = evidence;
		this.fixSql = fixSql;
	}
	public String getRuleName() {
		return ruleName;
	}
	public String getSeverity() {
		return severity;
	}
	public String getConfidence() {
		return confidence;
	}
	public String getExplanation() {
		return explanation;
	}
	public String getEvidence() {
		return evidence;
	}
	public String getFixSql() {
		return fixSql;
	}
    
    
}
