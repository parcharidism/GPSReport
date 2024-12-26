import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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

        // Step 1: Preprocess all CSV files
        System.out.println("Preprocessing CSV files...");
        preprocessCSVFiles(folder);

        // Step 2: Process preprocessed CSV files and generate reports
        System.out.println("Generating reports...");
        generateReports(folder);

        System.out.println("Process completed. Only HTML reports remain in the folder.");
    }

    private static void preprocessCSVFiles(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv") && !name.startsWith("processed_"));

        if (files == null || files.length == 0) {
            System.out.println("No CSV files to preprocess.");
            return;
        }

        // Create a "source_files" folder if it doesn't exist
        File sourceFilesFolder = new File(folder, "source_files");
        if (!sourceFilesFolder.exists()) {
            if (sourceFilesFolder.mkdir()) {
                System.out.println("Created folder: source_files");
            } else {
                System.err.println("Failed to create folder: source_files");
                return;
            }
        }

        for (File file : files) {
            try {
                System.out.println("Preprocessing file: " + file.getName());
                ExcelToCsvProcessor.processCsvFile(file); // Call the method from ExcelToCsvProcessor

                // Move the original source file to the source_files folder
                File movedFile = new File(sourceFilesFolder, file.getName());
                Files.move(file.toPath(), movedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Moved source file to: " + movedFile.getAbsolutePath());

            } catch (IOException e) {
                System.err.println("Error preprocessing file: " + file.getName() + ". " + e.getMessage());
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
                // Generate report using GPSRouteProcessor
                GPSRouteProcessor.processCSVFile(file);

                // Delete the processed file after successfully generating the report
                if (file.delete()) {
                    System.out.println("Deleted: " + file.getName());
                } else {
                    System.err.println("Failed to delete: " + file.getName());
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Error processing file: " + file.getName() + ". " + e.getMessage());
            }
        }
    }
}
