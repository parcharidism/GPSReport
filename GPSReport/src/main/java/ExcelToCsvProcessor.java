import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ExcelToCsvProcessor {

    public static void processCsvFile(File inputFile) {
        String outputFilePath = inputFile.getParent() + File.separator + "processed_" + inputFile.getName();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             FileWriter writer = new FileWriter(outputFilePath)) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(";"); // Assuming semicolon-delimited input

                // Handle header row
                if (isFirstLine) {
                    writer.append("No.;Position time;Lat;Lon\n");
                    isFirstLine = false;
                    continue;
                }

                // Process only the first four columns (No., Position time, Lat, Lon)
                List<String> processedColumns = new ArrayList<>();
                for (int i = 0; i < columns.length && i <= 3; i++) {
                    String value = columns[i].trim();
                    if (i == 2 || i == 3) { // Lat or Lon column
                        value = insertDecimalAfterTwoDigits(value);
                    }
                    processedColumns.add(value);
                }

                // Write processed columns to the output CSV with ";" delimiter
                writer.append(String.join(";", processedColumns));
                writer.append("\n");
            }

            System.out.println("Processed: " + inputFile.getName() + " -> processed_" + inputFile.getName());

        } catch (Exception e) {
            System.err.println("Error processing file: " + inputFile.getName());
            e.printStackTrace();
        }
    }

    private static String insertDecimalAfterTwoDigits(String value) {
        // Remove all non-numeric characters except for - and .
        value = value.replaceAll("[^0-9.-]", "");

        if (value.length() > 2) {
            return value.substring(0, 2) + "." + value.substring(2);
        }
        return value; // Return as-is if length <= 2
    }
}
