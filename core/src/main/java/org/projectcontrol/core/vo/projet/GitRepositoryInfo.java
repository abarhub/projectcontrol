package org.projectcontrol.core.vo.projet;

import java.time.LocalDateTime;
import java.util.List;

public class GitRepositoryInfo {
    private String shortHash;
    private String longHash;
    private String commitMessage;
    private LocalDateTime commitDate;
    private List<String> branches;
    private String currentBranch;
    private boolean hasUncommittedFiles;
    private List<String> uncommittedFiles;
    private List<String> logs;

    public String getShortHash() {
        return shortHash;
    }

    public void setShortHash(String shortHash) {
        this.shortHash = shortHash;
    }

    public String getLongHash() {
        return longHash;
    }

    public void setLongHash(String longHash) {
        this.longHash = longHash;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public LocalDateTime getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(LocalDateTime commitDate) {
        this.commitDate = commitDate;
    }

    public List<String> getBranches() {
        return branches;
    }

    public void setBranches(List<String> branches) {
        this.branches = branches;
    }

    public String getCurrentBranch() {
        return currentBranch;
    }

    public void setCurrentBranch(String currentBranch) {
        this.currentBranch = currentBranch;
    }

    public boolean isHasUncommittedFiles() {
        return hasUncommittedFiles;
    }

    public void setHasUncommittedFiles(boolean hasUncommittedFiles) {
        this.hasUncommittedFiles = hasUncommittedFiles;
    }

    public List<String> getUncommittedFiles() {
        return uncommittedFiles;
    }

    public void setUncommittedFiles(List<String> uncommittedFiles) {
        this.uncommittedFiles = uncommittedFiles;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }
}
