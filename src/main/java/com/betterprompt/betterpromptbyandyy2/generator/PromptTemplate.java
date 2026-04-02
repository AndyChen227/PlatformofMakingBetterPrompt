package com.betterprompt.betterpromptbyandyy2.generator;

/**
 * Immutable value object representing a single generated prompt
 * together with its classification metadata.
 */
public class PromptTemplate {

    private final String prompt;
    private final String taskType;
    private final String verbosity;

    public PromptTemplate(String prompt, String taskType, String verbosity) {
        this.prompt    = prompt;
        this.taskType  = taskType;
        this.verbosity = verbosity;
    }

    public String getPrompt()    { return prompt; }
    public String getTaskType()  { return taskType; }
    public String getVerbosity() { return verbosity; }
}
