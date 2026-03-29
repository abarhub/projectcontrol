package org.projectcontrol.core.service;

import org.projectcontrol.core.vo.projet.GitRepositoryInfo;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitInfoService {

    private RunService runService = new RunService();

    /**
     * Analyse un répertoire Git et retourne les informations sous forme d'objet.
     *
     * @param repoPath Chemin absolu vers le répertoire Git
     * @return GitRepositoryInfo contenant toutes les infos du dépôt
     * @throws GitAnalysisException en cas d'erreur lors de l'analyse
     */
    public GitRepositoryInfo analyzeRepository(String repoPath) throws GitAnalysisException, IOException, InterruptedException {
        File repoDir = new File(repoPath);

        if (!repoDir.exists() || !repoDir.isDirectory()) {
            return null;
        }
        if (!new File(repoDir, ".git").exists()) {
            return null;
        }

        // 1. Fetch
        runCommand(repoDir, "git", "fetch", "--all");

        GitRepositoryInfo info = new GitRepositoryInfo();

        // 2. Hash court et long du dernier commit
        info.setLongHash(runCommand(repoDir, "git", "rev-parse", "HEAD"));
        info.setShortHash(runCommand(repoDir, "git", "rev-parse", "--short", "HEAD"));

        // 3. Message du dernier commit
        info.setCommitMessage(runCommand(repoDir, "git", "log", "-1", "--pretty=%s"));

        // 4. Date du dernier commit (format ISO 8601)
        String rawDate = runCommand(repoDir, "git", "log", "-1", "--pretty=%ci");
        info.setCommitDate(parseCommitDate(rawDate));

        // 5. Branche actuelle
        info.setCurrentBranch(runCommand(repoDir, "git", "rev-parse", "--abbrev-ref", "HEAD"));

        // 6. Liste de toutes les branches (locales + remote)
        List<String> branches = new ArrayList<>();
        String branchOutput = runCommand(repoDir, "git", "branch", "-a");
        for (String line : branchOutput.split("\n")) {
            String branch = line.replaceAll("^[*\\s]+", "").trim();
            if (!branch.isEmpty()) {
                branches.add(branch);
            }
        }
        info.setBranches(branches);

        // 7. Fichiers non commités (staged + unstaged + untracked)
        List<String> uncommittedFiles = new ArrayList<>();
        String statusOutput = runCommand(repoDir, "git", "status", "--porcelain");
        if (!statusOutput.isEmpty()) {
            for (String line : statusOutput.split("\n")) {
                if (!line.trim().isEmpty()) {
                    uncommittedFiles.add(line.trim());
                }
            }
        }
        info.setUncommittedFiles(uncommittedFiles);
        info.setHasUncommittedFiles(!uncommittedFiles.isEmpty());

        // logs
        List<String> logs = new ArrayList<>();
        int nbCommit = 10;
        String logsOutput = runCommand(repoDir, "git", "log", "-n " + nbCommit,
                "--graph", "--decorate", "--date=format:'%Y-%m-%d %H:%M'",
                "--pretty=format:'%h (%ad) <%an> %d% %s'");
        if (!logsOutput.isEmpty()) {
            for (String line : logsOutput.split("\n")) {
                if (!line.trim().isEmpty()) {
                    logs.add(line.trim());
                }
            }
        }
        info.setLogs(logs);

        return info;
    }

    /**
     * Exécute une commande Git dans le répertoire donné et retourne la sortie.
     */
    private String runCommand(File workingDir, String... command) throws GitAnalysisException, IOException, InterruptedException {
        List<String> logs = new ArrayList<>();
        int res = runService.runCommand(x -> logs.add(x.line()), null, workingDir.toPath(), command);
        if (res != 0) {
            throw new GitAnalysisException("Git command failed with exit code " + res);
        }
        return String.join("\n", logs);
    }


    private String runCommand0(File workingDir, String... command) throws GitAnalysisException {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDir);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!output.isEmpty()) output.append("\n");
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // Lire stderr pour un message d'erreur utile
                StringBuilder error = new StringBuilder();
                try (BufferedReader errReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errReader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                }
                throw new GitAnalysisException(
                        "Commande échouée (exit " + exitCode + ") : " + String.join(" ", command)
                                + "\n" + error
                );
            }

            return output.toString().trim();

        } catch (GitAnalysisException e) {
            throw e;
        } catch (Exception e) {
            throw new GitAnalysisException("Erreur lors de l'exécution de la commande Git", e);
        }
    }

    /**
     * Parse la date retournée par git log --pretty=%ci
     * Format attendu : "2024-03-15 14:32:10 +0200"
     */
    private static LocalDateTime parseCommitDate(String rawDate) {
        try {
            // Supprime le fuseau horaire pour simplifier (ex: "+0200")
            String normalized = rawDate.replaceAll("\\s[+-]\\d{4}$", "").trim();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(normalized, formatter);
        } catch (Exception e) {
            return null; // Date non parsable, on retourne null plutôt que de planter
        }
    }

    // Exception dédiée
    public static class GitAnalysisException extends Exception {
        public GitAnalysisException(String message) {
            super(message);
        }

        public GitAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
