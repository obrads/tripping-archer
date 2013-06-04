package org.niri.pde;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.niri.util.files.TextFileManipulation;

public class ProjectExporter
{
	private static Pattern packageStatementPattern;
	private static Pattern[] unwantedImportPatterns;
	private static String mainFileName = "";
	private static HashSet<String> excludedFiles = new HashSet<String>();
	
	static
	{
		packageStatementPattern = Pattern.compile("(^|\\n)\\s*package\\s");
	}

	private static void exportFolder(File sourceFolder, String destinationFolder)
	{
		File[] children = sourceFolder.listFiles();
		for (int i = 0; i < children.length; i++) {
			File currentChild = children[i];
			if (currentChild.isDirectory()) {
				exportFolder(currentChild, destinationFolder);
			} else {
				String currentChildName = currentChild.getName();
				if (currentChildName.endsWith(".java") && (! excludedFiles.contains(currentChildName))) {
					try {
						StringBuilder refactoredFileBuilder = new StringBuilder();
						int currentlyCopied = 0;
						String currentChildContent = TextFileManipulation.readFile(currentChild.getAbsolutePath());
						Matcher packageStatementMatcher = packageStatementPattern.matcher(currentChildContent);
						while (packageStatementMatcher.find()) {
							int deleteStart = packageStatementMatcher.start();
							int deleteEnd = currentChildContent.indexOf("\n", deleteStart + 1);
							refactoredFileBuilder.append(currentChildContent.substring(currentlyCopied, deleteStart));
							currentlyCopied = deleteEnd;
						}
						for (int j = 0; j < unwantedImportPatterns.length; j++) {
							Matcher currentUnwantedImportMatcher = unwantedImportPatterns[j].matcher(currentChildContent);
							while (currentUnwantedImportMatcher.find()) {
								int deleteStart = currentUnwantedImportMatcher.start();
								int deleteEnd = currentChildContent.indexOf(";", deleteStart) + 1;
								refactoredFileBuilder.append(currentChildContent.substring(currentlyCopied, deleteStart));
								currentlyCopied = deleteEnd;
							}
						}
						refactoredFileBuilder.append(currentChildContent.substring(currentlyCopied));
						String destinationFilePath = destinationFolder + "/" + currentChildName;
						if (currentChildName.equals(mainFileName)) {
							destinationFilePath = destinationFilePath.substring(0, destinationFilePath.length() - 4) + "pde";
						}
						TextFileManipulation.writeFile(destinationFilePath, refactoredFileBuilder.toString());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private static void makeUnwantedImportPatterns(Collection<String> unwantedImports)
	{
		unwantedImportPatterns = new Pattern[unwantedImports.size()];
		int i = 0;
		for (String unwantedImport : unwantedImports) {
			unwantedImportPatterns[i++] = Pattern.compile("(^|\n)\\s*import\\s*" + unwantedImport);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String sourceFolderPath = "";
		String destinationFolderPath = "";
		int mode = 0;
		HashSet<String> unwantedImports = new HashSet<String>();
		for (int i = 0; i < args.length; i++) {
			
			if (args[i].startsWith("-")) {
				mode = 2;
			}
			switch (mode) {
			
			case 0:
				sourceFolderPath = args[i];
				mode = 1;
				break;
				
			case 1:
				destinationFolderPath = args[i];
				mode = 2;
				break;
				
			case 2:
				if (args[i].equalsIgnoreCase("-main")) {
					mode = 3;
				}
				if (args[i].equalsIgnoreCase("-unwantedImports")) {
					mode = 4;
				}
				if (args[i].equalsIgnoreCase("-excludedFiles")) {
					mode = 5;
				}
				break;
				
			case 3:
				mainFileName = args[i];
				break;
				
			case 4:
				unwantedImports.add(args[i]);
				break;
				
			case 5:
				excludedFiles.add(args[i]);
				break;

			default:
				break;
			}
		}
		makeUnwantedImportPatterns(unwantedImports);
		File destinationFolder = new File(destinationFolderPath);
		if (! mainFileName.equals("")) {
			String sketchName = mainFileName.substring(0,
					mainFileName.length() - 4);
			if (destinationFolder.getName() != sketchName) {
				destinationFolderPath += "/" + sketchName;
			}
		}
		exportFolder(new File(sourceFolderPath), destinationFolderPath);
	}

}
