
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GPSRouteProcessor {

    static class GPSRecord {

        int id;
        LocalDateTime timestamp;
        double latitude;
        double longitude;

        GPSRecord(int id, LocalDateTime timestamp, double latitude, double longitude) {
            this.id = id;
            this.timestamp = timestamp;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    static class Stop {

        int incrementalNumber;
        String startPosition;
        LocalDateTime startingTime;
        String stopPosition;
        LocalDateTime stoppingTime;
        long timeAtStop;

        Stop(int incrementalNumber, String startPosition, LocalDateTime startingTime, String stopPosition, LocalDateTime stoppingTime, long timeAtStop) {
            this.incrementalNumber = incrementalNumber;
            this.startPosition = startPosition;
            this.startingTime = startingTime;
            this.stopPosition = stopPosition;
            this.stoppingTime = stoppingTime;
            this.timeAtStop = timeAtStop;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.out.println("Usage: java GPSRouteProcessor <folder_path>");
            return;
        }

        String folderPath = args[0];
        File folder = new File(folderPath);

        if (!folder.isDirectory()) {
            System.err.println("The specified path is not a directory.");
            return;
        }

        // Process each CSV file in the folder
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (files == null || files.length == 0) {
            System.out.println("No CSV files found in the folder.");
            return;
        }

        for (File file : files) {
            System.out.println("Processing file: " + file.getName());
            processCSVFile(file);
        }
    }

    private static void processCSVFile(File csvFile) throws IOException, InterruptedException {
        String outputHtmlFile = csvFile.getParent() + "/" + csvFile.getName().replace(".csv", "_report.html");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy H:mm");

        List<GPSRecord> gpsRecords = new ArrayList<>();
        List<Stop> stops = new ArrayList<>();

        // Read GPS data
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) {
                    continue; // Skip header
                }
                String[] parts = line.split(";");
                try {
                    int id = Integer.parseInt(parts[0]);
                    LocalDateTime timestamp = LocalDateTime.parse(parts[1], formatter);
                    double latitude = Double.parseDouble(parts[2]);
                    double longitude = Double.parseDouble(parts[3]);
                    gpsRecords.add(new GPSRecord(id, timestamp, latitude, longitude));
                } catch (Exception e) {
                    System.err.println("Error processing line " + lineNumber + " in file " + csvFile.getName() + ": " + line);
                }
            }
        }

        gpsRecords.sort(Comparator.comparing(record -> record.timestamp));

        // Identify stops
        GPSRecord startPoint = null;
        GPSRecord previousRecord = null;
        int incrementalNumber = 1;

        for (GPSRecord currentRecord : gpsRecords) {
            if (previousRecord != null) {
                Duration timeDiff = Duration.between(previousRecord.timestamp, currentRecord.timestamp);
                if (timeDiff.toMinutes() > 10) {
                    if (startPoint != null) {
                        long timeAtStop = Duration.between(previousRecord.timestamp, startPoint.timestamp).toMinutes();
                        String startPosition = String.format(Locale.US, "%.6f, %.6f", startPoint.latitude, startPoint.longitude);
                        String stopPosition = String.format(Locale.US, "%.6f, %.6f", previousRecord.latitude, previousRecord.longitude);
                        stops.add(new Stop(incrementalNumber++, startPosition, startPoint.timestamp, stopPosition, previousRecord.timestamp, timeAtStop));
                    }
                    startPoint = currentRecord;
                }
            }
            if (startPoint == null) {
                startPoint = currentRecord;
            }
            previousRecord = currentRecord;
        }

        if (startPoint != null && previousRecord != null && !startPoint.equals(previousRecord)) {
            long timeAtStop = Duration.between(previousRecord.timestamp, startPoint.timestamp).toMinutes();
            String startPosition = String.format(Locale.US, "%.6f, %.6f", startPoint.latitude, startPoint.longitude);
            String stopPosition = String.format(Locale.US, "%.6f, %.6f", previousRecord.latitude, previousRecord.longitude);
            stops.add(new Stop(incrementalNumber++, startPosition, startPoint.timestamp, stopPosition, previousRecord.timestamp, timeAtStop));
        }

        // Adjust timeAtStop for stops
        for (int i = 0; i < stops.size() - 1; i++) {
            Stop currentStop = stops.get(i);
            Stop nextStop = stops.get(i + 1);

            long timeAtStop = Duration.between(currentStop.stoppingTime, nextStop.startingTime).toMinutes();
            currentStop.timeAtStop = timeAtStop;
        }

        if (!stops.isEmpty()) {
            stops.get(stops.size() - 1).timeAtStop = 0;
        }

        // Fetch addresses for stops
        Map<String, String> addressCache = new HashMap<>();
        for (Stop stop : stops) {
            String[] startCoords = stop.startPosition.split(", ");
            String[] stopCoords = stop.stopPosition.split(", ");

            if (!addressCache.containsKey(stop.startPosition)) {
                String address = getAddress(Double.parseDouble(startCoords[0]), Double.parseDouble(startCoords[1]));
                addressCache.put(stop.startPosition, address);
                System.out.println("Start Address: [" + stop.startPosition + "] -> " + address);
            }

            if (!addressCache.containsKey(stop.stopPosition)) {
                String address = getAddress(Double.parseDouble(stopCoords[0]), Double.parseDouble(stopCoords[1]));
                addressCache.put(stop.stopPosition, address);
                System.out.println("Stop Address: [" + stop.stopPosition + "] -> " + address);
            }
        }

        // Generate HTML report
        // Extract date range for the report title
String startDate = gpsRecords.get(0).timestamp.format(DateTimeFormatter.ofPattern("d/M/yyyy"));
String endDate = gpsRecords.get(gpsRecords.size() - 1).timestamp.format(DateTimeFormatter.ofPattern("d/M/yyyy"));

// Pass the date range to the HTML generation
generateHtmlReport(stops, addressCache, outputHtmlFile, startDate, endDate);

        System.out.println("Report generated: " + outputHtmlFile);
    }

    private static String getAddress(double latitude, double longitude) {
        String address = "Unknown Address";
        try {
            String urlString = String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?format=json&lat=%.6f&lon=%.6f", latitude, longitude);
            System.out.println("Requesting address for URL: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseString = response.toString();

            int roadStart = responseString.indexOf("\"road\":\"") + 8;
            int roadEnd = responseString.indexOf("\"", roadStart);
            String road = (roadStart > 7 && roadEnd > roadStart) ? responseString.substring(roadStart, roadEnd) : "";

            int houseNumberStart = responseString.indexOf("\"house_number\":\"") + 16;
            int houseNumberEnd = responseString.indexOf("\"", houseNumberStart);
            String houseNumber = (houseNumberStart > 15 && houseNumberEnd > houseNumberStart) ? responseString.substring(houseNumberStart, houseNumberEnd) : "";

            int cityStart = responseString.indexOf("\"city\":\"") + 8;
            int cityEnd = responseString.indexOf("\"", cityStart);
            String city = (cityStart > 7 && cityEnd > cityStart) ? responseString.substring(cityStart, cityEnd) : "";

            address = road + " " + houseNumber + ", " + city;

            Thread.sleep(1500);

        } catch (Exception e) {
            System.err.println("Error retrieving address: " + e.getMessage());
        }
        return address;
    }

    private static void generateHtmlReport(List<Stop> stops, Map<String, String> addressCache, String outputHtmlFile, String startDate, String endDate) {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputHtmlFile))) {
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Write the HTML report
        bw.write("<!DOCTYPE html>\n");
        bw.write("<html>\n<head>\n<title>Δρομολόγια από " + startDate + " έως " + endDate + "</title>\n");
        bw.write("<style>\n");
        bw.write("table { width: 100%; border-collapse: collapse; }\n");
        bw.write("th, td { border: 1px solid black; padding: 8px; text-align: left; }\n");
        bw.write("th { background-color: #f2f2f2; }\n");
        bw.write("</style>\n</head>\n<body>\n");

        bw.write("<h1>Δρομολόγια από " + startDate + " έως " + endDate + "</h1>\n");
        bw.write("<table>\n");
        bw.write("<tr><th>#</th><th>Start Address</th><th>Starting Time</th><th>Stop Address</th><th>Stopping Time</th><th>Time at Stop (minutes)</th></tr>\n");
        for (Stop stop : stops) {
            String startAddress = addressCache.getOrDefault(stop.startPosition, "Unknown Address");
            String stopAddress = addressCache.getOrDefault(stop.stopPosition, "Unknown Address");

            String startCoords = stop.startPosition.replace(", ", ",");
            String stopCoords = stop.stopPosition.replace(", ", ",");

            String startLink = "<a href='https://www.google.com/maps?q=" + startCoords + "' target='_blank'>" + startAddress + "</a>";
            String stopLink = "<a href='https://www.google.com/maps?q=" + stopCoords + "' target='_blank'>" + stopAddress + "</a>";

            bw.write("<tr>\n");
            bw.write("<td>" + stop.incrementalNumber + "</td>\n");
            bw.write("<td>" + startLink + "</td>\n");
            bw.write("<td>" + stop.startingTime.format(customFormatter) + "</td>\n");
            bw.write("<td>" + stopLink + "</td>\n");
            bw.write("<td>" + stop.stoppingTime.format(customFormatter) + "</td>\n");
            bw.write("<td>" + stop.timeAtStop + "</td>\n");
            bw.write("</tr>\n");
        }
        bw.write("</table>\n");

        bw.write("</body>\n</html>");
    } catch (IOException e) {
        System.err.println("Error writing the HTML report: " + e.getMessage());
    }
}

}
