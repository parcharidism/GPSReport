import java.io.File;
import java.io.IOException;

public class Orchestrator {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Orchestrator <folder_path>");
            return;
        }

        String folderPath = args[0];
        File folder = new File(folderPath);

        if (!folder.isDirectory()) {
            System.err.println("The specified path is not a directory.");
            return;
        }

        // Step 1: Preprocess all CSV files (rename and prepare for processing)
        System.out.println("Preprocessing CSV files...");
        preprocessCSVFiles(folder);

        // Step 2: Process preprocessed CSV files and generate reports
        System.out.println("Generating reports...");
        generateReports(folder);

        // Step 3: Clean up intermediate processed files
        System.out.println("Cleaning up intermediate files...");
        cleanupProcessedFiles(folder);

        System.out.println("Process completed. Only HTML reports remain in the folder.");
    }

    private static void preprocessCSVFiles(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv") && !name.startsWith("processed_"));

        if (files == null || files.length == 0) {
            System.out.println("No CSV files to preprocess.");
            return;
        }

        for (File file : files) {
            File processedFile = new File(file.getParent(), "processed_" + file.getName());
            if (file.renameTo(processedFile)) {
                System.out.println("Renamed: " + file.getName() + " -> " + processedFile.getName());
            } else {
                System.err.println("Failed to rename: " + file.getName());
            }
        }
    }

    private static void generateReports(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().startsWith("processed_") && name.toLowerCase().endsWith(".csv"));

        if (files == null || files.length == 0) {
            System.out.println("No preprocessed CSV files found.");
            return;
        }

        for (File file : files) {
            System.out.println("Processing file: " + file.getName());
            try {
                GPSRouteProcessor.processCSVFile(file); // Call the first Java class
            } catch (IOException | InterruptedException e) {
                System.err.println("Error processing file: " + file.getName() + ". " + e.getMessage());
            }
        }
    }

    private static void cleanupProcessedFiles(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().startsWith("processed_") && name.toLowerCase().endsWith(".csv"));

        if (files == null || files.length == 0) {
            System.out.println("No processed files to clean up.");
            return;
        }

        for (File file : files) {
            if (file.delete()) {
                System.out.println("Deleted: " + file.getName());
            } else {
                System.err.println("Failed to delete: " + file.getName());
            }
        }
    }
}
