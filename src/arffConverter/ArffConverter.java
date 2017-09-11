package arffConverter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ArffConverter {

	private File namesFile, dataFile;
	
	public ArffConverter(String[] args) {
		if(loadFiles(args)) {
			// TODO: This assumes .names and .data are in the same folder
			// and we are putting the .arff there also.  Is this okay?
			try {
				String namesPath = namesFile.getAbsolutePath();
				
				int pathLength = namesPath.split(Pattern.quote(File.separator)).length;

				File arffFile = new File(namesFile.getParentFile().getAbsolutePath() + File.separator + namesPath.split(Pattern.quote(File.separator))[pathLength-1].split(Pattern.quote("."))[0] + ".arff");
				
				if(!arffFile.exists()) {
					arffFile.getParentFile().mkdirs();
					arffFile.createNewFile();
				}

				Scanner scanner = new Scanner(namesFile);

				String namesOut = "";
				int i = 1;
				String temp = "";
				
				while(scanner.hasNextLine()) {
					String line = scanner.nextLine();

					if(line.startsWith("" + i)) {
						if(i == 1 + 1) { // Title
							namesOut += temp;
						} else if (i == 2 + 1) { // Sources
							namesOut += temp;
						} else if (i == 7 + 1) { // Attributes
							namesOut += temp;
						}
						i++;
						temp = "";
					}
					temp += "% " + line + "\n";
					
				}
				
				scanner.close();
				
				PrintWriter writer = new PrintWriter(arffFile);
				
				writer.println(namesOut);
				
				scanner = new Scanner(dataFile);
				
				writer.println("@Data");
				
				while(scanner.hasNextLine()) {
					writer.println(scanner.nextLine());
				}
				
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
	
	boolean loadFiles(String[] args) {
		String namesPath;
		String dataPath;
		
		if(args[0].contains(".names")) {
			namesPath = args[0];
			if(args[1].contains(".data")) {
				dataPath = args[1];
			} else return false;
		} else if(args[0].contains(".data")) {
			dataPath = args[0];
			if(args[1].contains(".names")) {
				namesPath = args[1];
			} else return false;
		} else return false;
		
		namesFile = new File(namesPath);
		dataFile = new File(dataPath);
		
		if(!namesFile.exists() || !dataFile.exists()) return false;
		
		return true;
	}
	
	public static void main(String[] args) {
		new ArffConverter(args);
	}
	
}
