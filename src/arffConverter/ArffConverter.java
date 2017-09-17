package arffConverter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArffConverter {

    public static final String attributeTag = "@attribute";
    public static final String relationTag = "@relation";
    public static final String dataTag = "@data";
    int dataIndex = 0;
    private File namesFile, dataFile;

    public ArffConverter(String[] args) {
        if (loadFiles(args)) {
            convert();
        } else {
            System.err.println("Please enter a .data file and a .names file");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new ArffConverter(args);
    }

    private boolean loadFiles(String[] args) {
        System.out.println("Loading Files...");
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

        System.out.println("Files Found!");

        return true;
    }

    void convert() {
        try {
            List<String> attributeLabels = new ArrayList<String>();
            String headerOut = getHeaderData(attributeLabels);

            // Use map for cheap single key storage
            Map<String, Void> dataTypes = new HashMap<String, Void>();
            List<String> types = new ArrayList<String>();

            String data = getDataTypes(types, dataTypes);

            // Check if the attributes weren't defined
            attributeLabels = checkAttributes(attributeLabels, types);

            writeData(createArffFile(), headerOut, dataTypes, types, attributeLabels, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getHeaderData(List<String> attributeLabels) throws IOException {
        System.out.println("Grabbing Header Data...");
        Scanner scanner = new Scanner(namesFile);

        int i = 1;
        String temp = "";

        String namesOut = "";

        // Read through .names
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (line.startsWith("" + i)) {
                if (i == 1 + 1) { // Title
                    namesOut += temp;
                } else if (i == 2 + 1) { // Sources
                    namesOut += temp;
                } else if (i == 7 + 1) { // Attributes
                    attributeLabels.addAll(Stream.of(temp.split("\n"))
                            .skip(1) // Ignore the Attribute Information definition
                            .map(s -> s.substring(1, s.length())) // Remove the % we added at the start
                            .map(String::trim)
                            .filter(s -> s.matches("\\d+.+")) // Only care about lines that start with a number
                            .map(s -> s.split("\\d+")[1].substring(1, s.split("\\d+")[1].length())) // Remove the numbering
                            .map(String::trim)
                            .map(s -> s.matches(".+\\s+.+") ? '"' + s + '"' : s) // Put quotes around the label if necessary
                            .collect(Collectors.toList()));
                }
                i++;
                temp = "";
            }
            temp += "% " + line + "\n";
        }

        scanner.close();

        System.out.println("Got Header Data!");
        return namesOut;
    }

    private String getDataTypes(List<String> types, Map<String, Void> dataTypes) throws IOException {
        System.out.println("Reading through the data...");
        Scanner scanner = new Scanner(dataFile);

        String data = "";

        // Read first line to determine attribute types
        if (scanner.hasNextLine()) {
            String line = scanner.nextLine() + "\n";

            // Prefer getting class from last line
            if (Attribute.getType(line.split(",")[line.split(",").length - 1].trim()) == Attribute.stringTag) {
                // Also use the class for first line
                dataTypes.put(line.split(",")[line.split(",").length - 1].trim(), null);

                types.addAll(Stream.of(line.split(","))
                        .limit(line.split(",").length - 1)
                        .map(String::trim)
                        .map(Attribute::getType)
                        .collect(Collectors.toList()));

                dataIndex = line.split(",").length - 1;
            } else if (Attribute.getType(line.split(",")[0].trim()) == Attribute.stringTag) {
                // Also use the class for first line
                dataTypes.put(line.split(",")[0].trim(), null);

                types.addAll(Stream.of(line.split(","))
                        .skip(1)
                        .limit(line.split(",").length)
                        .map(String::trim)
                        .map(Attribute::getType)
                        .collect(Collectors.toList()));

                dataIndex = 0;
            } else {
                System.err.println("We don't support your file, please select one that has the string class as the first or last parameter");
                System.exit(1);
            }

            if (dataIndex == 0) {
                System.out.println("Class attribute found at the start of the line.");
                System.out.println("Attribute types found: class, " + types.stream().map(String::trim).collect(Collectors.joining(", ")));
                String[] split = line.split(",");
                String out = "";
                for (int i = 0; i < split.length; i++) {
                    if (i == 0)
                        out += split[split.length - 1].trim() + ",";
                    else if (i == split.length - 1) {
                        out += split[0].trim() + ",";
                    } else {
                        out += split[i].trim() + ",";
                    }
                }
                data += out.substring(0, out.length() - 1) + "\n";
            } else {
                System.out.println("Class attribute found at the end of the line.");
                System.out.println("Attribute types found: " + types.stream().map(String::trim).collect(Collectors.joining(", ")) + ", class");
                data += line + "\n";
            }
        }

        // Get all the data to copy over
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (dataIndex == 0) {
                String[] split = line.split(",");
                String out = "";
                for (int i = 0; i < split.length; i++) {
                    if (i == 0)
                        out += split[split.length - 1] + ",";
                    else if (i == split.length - 1) {
                        out += split[0] + ",";
                    } else {
                        out += split[i] + ",";
                    }
                }
                data += out.substring(0, out.length() - 1) + "\n";
            } else {
                data += line + "\n";
            }

            if (!line.trim().equals("")) {
                dataTypes.put(line.split(",")[dataIndex].trim(), null);
            }
        }

        scanner.close();

        return data;
    }

    private List<String> checkAttributes(List<String> attributeLabels, List<String> types) {

        if (attributeLabels.size() < types.size()) {
            System.out.println("There seems to be a problem with the attributes... Defaulting to enumeration");
            // If not, enumerate attributes
            attributeLabels = Stream.iterate(0, k -> k + 1)
                    .limit(types.size())
                    .map(s -> "" + s)
                    .collect(Collectors.toList());
        } else {
            System.out.println("Checking for any pesky duplicate labels...");

            boolean changedLabel = false;
            List<String> labels = new ArrayList<String>();
            for (int i = 0; i < attributeLabels.size(); i++) {
                if (labels.contains(attributeLabels.get(i))) {
                    changedLabel = true;
                    labels.add(attributeLabels.get(i) + "-");
                } else {
                    labels.add(attributeLabels.get(i));
                }
            }

            if (changedLabel)
                System.out.println("Pesky duplicate labels exterminated!");
            else
                System.out.println("No duplicate labels were found!");
            attributeLabels = labels;
        }

        return attributeLabels;
    }

    private File createArffFile() throws IOException {
        String namesPath = namesFile.getAbsolutePath();

        int pathLength = namesPath.split(Pattern.quote(File.separator)).length;

        String fileName = namesPath.split(Pattern.quote(File.separator))[pathLength - 1].split(Pattern.quote("."))[0];

        File arffFile = new File(namesFile.getParentFile().getAbsolutePath() + File.separator + fileName + ".arff");

        if (!arffFile.exists()) {
            arffFile.getParentFile().mkdirs();
            arffFile.createNewFile();
        }

        return arffFile;
    }

    private void writeData(File arffFile, String namesOut, Map<String, Void> dataTypes, List<String> types, List<String> attributeLabels, String data) throws IOException {
        System.out.println("Writing all the data to the file..");
        int pathLength = arffFile.getAbsolutePath().split(Pattern.quote(File.separator)).length;
        String fileName = arffFile.getAbsolutePath().split(Pattern.quote(File.separator))[pathLength - 1].split(Pattern.quote("."))[0];

        PrintWriter writer = new PrintWriter(arffFile);

        // Write Title & Sources
        writer.print(namesOut);

        // Write relation tag
        writer.println(relationTag + " " + fileName + "\n");

        // Write the attribute definitions
        for (int i = 0; i < types.size(); i++) {
            writer.println(attributeTag + " " + attributeLabels.get(i) + " " + types.get(i));
        }

        // Write the classes to file
        writer.println(attributeTag + " class {" +
                dataTypes.keySet()
                        .stream()
                        .sorted()
                        .map(s -> s.matches(".+\\s+.+") ? '"' + s + '"' : s) // If the attribute contains a space, use quotes
                        .collect(Collectors.joining(","))
                + "}");

        writer.println();

        // Write the data to the file
        writer.println(dataTag);
        writer.print(data.trim());

        System.out.println("ARFF conversion done! File written to: " + arffFile.getAbsolutePath());

        writer.close();
    }
}
