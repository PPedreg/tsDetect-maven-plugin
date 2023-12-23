package es.upm.fi.pedro.tsDetect_maven_plugin;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.project.MavenProject;

import testsmell.AbstractSmell;
import testsmell.TestFile;
import testsmell.TestSmellDetector;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Mojo(name = "tsDetectUPM", defaultPhase = LifecyclePhase.COMPILE)
public class MyMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     * @parameter expression="${project.build.directory}"
     * @required
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;
    
    public void execute()
        throws MojoExecutionException
    {
    	File inputFile = new File("input.csv");

        if (!inputFile.exists() || inputFile.isDirectory()) {
            System.out.println("Please provide a valid CSV file containing the paths to the collection of test files");
            return;
        }

        TestSmellDetector testSmellDetector = TestSmellDetector.createTestSmellDetector();

        /*
          Read the input CSV file and build the TestFile objects
         */
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            TestFile testFile;
            List<TestFile> testFiles = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                String[] lineItem = line.split(",");

                // assuming the CSV has at least two columns
                if (lineItem.length >= 2) {
                    // check if the test file has an associated production file
                    if (lineItem.length == 2) {
                        testFile = new TestFile(lineItem[0], lineItem[1], "");
                    } else {
                        testFile = new TestFile(lineItem[0], lineItem[1], lineItem[2]);
                    }

                    testFiles.add(testFile);
                }
            }

            /*
              Iterate through all test files to detect smells and then write the output as HTML
            */
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();

            // Create a FileWriter to write to the index.html file
            PrintWriter writer = new PrintWriter(new FileWriter("index.html"));
                // Write HTML header
                writer.println("<html><head><title>Test Smell Report</title></head><body>");
                writer.println("<h1>Test Smell Report - " + dateFormat.format(date) + "</h1>");

                for (TestFile file : testFiles) {
                    date = new Date();
                    writer.println("<p>" + dateFormat.format(date) + " Processing: " + file.getTestFilePath() + "</p>");

                    // Detect smells
                    TestFile tempFile = testSmellDetector.detectSmells(file);

                    // Write results as HTML
                    printHtmlResult(writer, file, tempFile);
                }

                // Write HTML footer
                writer.println("</body></html>");
            } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    }
    private static void printHtmlResult(PrintWriter writer, TestFile file, TestFile tempFile) {
        // Write data outside the table
        writer.println("<p>App: " + file.getApp() + "</p>");
        writer.println("<p>Version: " + file.getTagName() + "</p>");
        writer.println("<p>TestFilePath: " + file.getTestFilePath() + "</p>");
        writer.println("<p>ProductionFilePath: " + file.getProductionFilePath() + "</p>");
        writer.println("<p>RelativeTestFilePath: " + file.getRelativeTestFilePath() + "</p>");
        writer.println("<p>RelativeProductionFilePath: " + file.getRelativeProductionFilePath() + "</p>");

        // Write results as HTML table
        writer.println("<table border=\"1\">");
        writer.println("<tr><th>Test Smell</th><th>Detected</th></tr>");

        for (AbstractSmell smell : tempFile.getTestSmells()) {
            writer.println("<tr><td>" + smell.getSmellName() + "</td><td>" + smell.getHasSmell() + "</td></tr>");
        }

        writer.println("</table>");
    }
}
