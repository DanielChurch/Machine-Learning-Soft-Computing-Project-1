package arffConverter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArffConverter {

    public static final String attributeTag = "@attribute";
    public static final String relationTag = "@relation";
    public static final String dataTag = "@data";
    private File namesFile, dataFile;

    public ArffConverter(String[] args) {
        if (loadFiles(args)) {
            try {
                String namesPath = namesFile.getAbsolutePath();

                int pathLength = namesPath.split(Pattern.quote(File.separator)).length;

                String fileName = namesPath.split(Pattern.quote(File.separator))[pathLength - 1].split(
                        Pattern.quote("."))[0];

                File arffFile = new File(namesFile.getParentFile()
                        .getAbsolutePath()
                        + File.separator + fileName + ".arff");

                if (!arffFile.exists()) {
                    arffFile.getParentFile().mkdirs();
                    arffFile.createNewFile();
                }

                Scanner scanner = new Scanner(namesFile);

                String namesOut = "";
                int i = 1;
                String temp = "";

                List<String> attributeLabels = null;

                // Read through .names
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();

                    if (line.startsWith("" + i)) {
                        if (i == 1 + 1) { // Title
                            namesOut += temp;
                        } else if (i == 2 + 1) { // Sources
                            namesOut += temp;
                        } else if (i == 7 + 1) { // Attributes
                            attributeLabels = Stream.of(temp.split("\n"))
                                    .skip(1) // Ignore the Attribute Information definition
                                    .map(s -> s.substring(1, s.length())) // Remove the % we added at the start
                                    .map(String::trim)
                                    .filter(s -> s.matches("\\d+.+")) // Only care about lines that start with a number
                                    .map(s -> s.split("\\d+")[1].substring(1, s.split("\\d+")[1].length())) // Remove the numbering
                                    .map(String::trim)
                                    .map(s -> s.matches(".+\\s+.+") ? '"' + s + '"' : s) // Put quotes around the label if necessary
                                    .collect(Collectors.toList());
                        }
                        i++;
                        temp = "";
                    }
                    temp += "% " + line + "\n";
                }

                scanner.close();

                PrintWriter writer = new PrintWriter(arffFile);

                // Write Title & Sources
                writer.print(namesOut);

                // Write relation tag
                writer.println(relationTag + " " + fileName + "\n");

                scanner = new Scanner(dataFile);

                String data = "";

                // Use map for cheap single key storage
                Map<String, Void> dataTypes = new HashMap<String, Void>();

                // Read first line to determine attribute types
                List<String> types = null;
                if (scanner.hasNextLine()) {
                    data = scanner.nextLine() + "\n";
                    types = Stream.of(data.split(","))
                            .limit(data.split(",").length - 1)
                            .map(String::trim)
                            .map(Attribute::getType)
                            .collect(Collectors.toList());

                    // Also use the class for first line
                    dataTypes.put(data.split(",")[data.split(",").length - 1].trim(), null);
                }

                // Get all the data to copy over
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    data += line + "\n";

                    // Keep track of possible classes, the last value in the
                    // line
                    if (!line.trim().equals("")) {
                        dataTypes.put(line.split(",")[line.split(",").length - 1].trim(), null);
                    }
                }

                // Check if the attributes weren't defined
                if (attributeLabels.size() < types.size()) {
                    // If not, enumerate attributes
                    attributeLabels = Stream.iterate(0, k -> k + 1)
                            .limit(types.size())
                            .map(s -> "" + s)
                            .collect(Collectors.toList());
                }

                // Write the attribute definitions
                for (int j = 0; j < types.size(); j++) {
                    writer.println(attributeTag + " " + attributeLabels.get(j) + " " + types.get(j));
                }

                // Write the classes to file
                writer.println(attributeTag + " class {" +
                        dataTypes.keySet()
                                .stream()
                                .sorted()
                                .map(s -> s.matches(".+\\s+.+") ? '"' + s + '"' : s) // If the attribute contains a space, use quotes
                                .collect(Collectors.joining(","))
                        + "}\n");

                // Write the data to the file
                writer.println(dataTag);
                writer.print(data.trim());

                writer.close();
                scanner.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Please enter a .data file and a .names file");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new ArffConverter(args);
    }

    boolean loadFiles(String[] args) {
        String namesPath;
        String dataPath;

        if (args[0].contains(".names")) {
            namesPath = args[0];
            if (args[1].contains(".data")) {
                dataPath = args[1];
            } else return false;
        } else if (args[0].contains(".data")) {
            dataPath = args[0];
            if (args[1].contains(".names")) {
                namesPath = args[1];
            } else return false;
        } else return false;

        namesFile = new File(namesPath);
        dataFile = new File(dataPath);

        if (!namesFile.exists() || !dataFile.exists()) return false;

        return true;
    }

}
