/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class generates nice HTML graph annotations reports 
 * 
 * They are created with the help of getHTMLMessage function in {@link GraphBuilderAnnotation} derived classes.
 * @author mabu
 */
public class AnnotationsToHTML implements GraphBuilderModule {

    private static Logger LOG = LoggerFactory.getLogger(AnnotationsToHTML.class); 

    //Path to output folder
    private CompositeDataSource reportDirectory;

    //If there are more then this number annotations are split into multiple files
    //This is because browsers aren't made for giant HTML files which can be made with 500k annotations
    private int maxNumberOfAnnotationsPerFile;


    //This counts all occurrences of HTML annotations classes
    //If one annotation class is split into two files it has two entries in this Multiset
    //IT is used to show numbers in HTML files name and links
    private Multiset<String> annotationClassOccurences;

    //List of writers which are used for actual writing annotations to HTML
    private List<HTMLWriter> writers;

    //Key is classname, value is annotation message
    //Multimap because there are multiple annotations for each classname
    private Multimap<String, String> annotations;
  
    AnnotationsToHTML(CompositeDataSource reportDirectory, int maxNumberOfAnnotationsPerFile) {
        this.reportDirectory = reportDirectory;
        this.maxNumberOfAnnotationsPerFile = maxNumberOfAnnotationsPerFile;
        this.annotations = ArrayListMultimap.create();
        this.annotationClassOccurences = HashMultiset.create();
        this.writers = new ArrayList<>();
    }

    @Override
    public void checkInputs() { }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        try {
            // Delete all files in the report directory if it exist
            if (!deleteReportDirectoryAndContent()) { return; }

            //Groups annotations in multimap according to annotation class
            for (GraphBuilderAnnotation annotation : graph.getBuilderAnnotations()) {
                //writer.println("<p>" + annotation.getHTMLMessage() + "</p>");
                // writer.println("<small>" + annotation.getClass().getSimpleName()+"</small>");
                addAnnotation(annotation);

            }
            LOG.info("Creating Annotations log");

            //Creates list of HTML writers. Each writer has whole class of HTML annotations
            //Or multiple HTML writers can have parts of one class of HTML annotations if number
            // of annotations is larger than maxNumberOfAnnotationsPerFile
            for (Map.Entry<String, Collection<String>> entry : annotations.asMap().entrySet()) {
                List<String> annotationsList;
                if (entry.getValue() instanceof List) {
                    annotationsList = (List<String>) entry.getValue();
                }
                else {
                    annotationsList = new ArrayList<>(entry.getValue());
                }
                addAnnotations(entry.getKey(), annotationsList);
            }

            //Actual writing to the file is made here since
            // this is the first place where actual number of files is known (because it depends on annotations count)
            for (HTMLWriter writer : writers) {
                writer.writeFile(annotationClassOccurences, false);
            }

            HTMLWriter indexFileWriter = new HTMLWriter(
                    "index", (Multimap<String, String>) null
            );
            indexFileWriter.writeFile(annotationClassOccurences, true);

            LOG.info("Annotated logs are in {}", reportDirectory.path());
        }
        finally {
            try{
                reportDirectory.close();
            }
            catch (IOException e) {
                LOG.warn(
                        "Failed to close report directory: " + reportDirectory.path()
                        + ", details: " + e.getLocalizedMessage(),
                        e
                );
            }
        }
    }

    /**
     * Delete report if it exist, and return true if successful. Return {@code false} if the
     * {@code reportDirectory} is {@code null} or the directory can NOT be deleted.
     */
    private boolean deleteReportDirectoryAndContent() {
        if (reportDirectory == null) {
            LOG.error("Saving folder is empty!");
            return false;
        }

        if (reportDirectory.exists()) {
            //Removes all files from report directory
            try {
                reportDirectory.delete();
            } catch (IOException e) {
                LOG.error("Failed to clean HTML report directory: " + reportDirectory.path() + ". HTML report won't be generated!", e);
                return false;
            }
        }
        return true;
    }

    /**
     * Creates file with given class of annotations
     *
     * If number of annotations is larger then maxNumberOfAnnotationsPerFile multiple files are generated.
     * And named annotationClassName1,2,3 etc.
     *
     * @param annotationClassName name of annotation class and then also filename
     * @param annotations list of all annotations with that class
     */
    private void addAnnotations(String annotationClassName, List<String> annotations) {
        HTMLWriter file_writer;
        if (annotations.size() > 1.2 * maxNumberOfAnnotationsPerFile) {
            LOG.debug("Number of annotations is very large. Splitting: {}", annotationClassName);
            List<List<String>> partitions = Lists.partition(annotations, maxNumberOfAnnotationsPerFile);
            for (List<String> partition: partitions) {
                annotationClassOccurences.add(annotationClassName);
                int labelCount = annotationClassOccurences.count(annotationClassName);
                file_writer = new HTMLWriter(annotationClassName + labelCount, partition);
                writers.add(file_writer);
            }

        } else {
            annotationClassOccurences.add(annotationClassName);
            int labelCount = annotationClassOccurences.count(annotationClassName);
            file_writer = new HTMLWriter(annotationClassName + labelCount, annotations);
            writers.add(file_writer);
        }
    }

    /**
     * Groups annotations according to annotation class name
     *
     * All annotations are saved together in multimap where key is annotation classname
     * and values are list of annotations with that class
     */
    private void addAnnotation(GraphBuilderAnnotation annotation) {
        String className = annotation.getClass().getSimpleName();
        annotations.put(className, annotation.getHTMLMessage());
    }

    class HTMLWriter {
        private DataSource target;

        private Multimap<String, String> writerAnnotations;

        private String annotationClassName;

        HTMLWriter(String key, Collection<String> annotations) {
            LOG.debug("Making file: {}", key);
            this.target = reportDirectory.entry(key + ".html");
            this.writerAnnotations = ArrayListMultimap.create();
            this.writerAnnotations.putAll(key, annotations);
            this.annotationClassName = key;
        }

        HTMLWriter(String filename, Multimap<String, String> curMap) {
            LOG.debug("Making file: {}", filename);
            this.target = reportDirectory.entry(filename + ".html");
            this.writerAnnotations = curMap;
            this.annotationClassName = filename;
        }

        private void writeFile(Multiset<String> classes, boolean isIndexFile) {
            try(PrintWriter out = new PrintWriter(target.asOutputStream(), true, StandardCharsets.UTF_8)) {
                out.println("<html><head><title>Graph report for Graph.obj</title>");
                out.println("\t<meta charset=\"utf-8\">");
                out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
                out.println("<script src='http://code.jquery.com/jquery-1.11.1.js'></script>");
                out.println(
                    "<link rel='stylesheet' href='http://yui.yahooapis.com/pure/0.5.0/pure-min.css'>");
                String css = "\t\t<style>\n"
                    + "\n"
                    + "\t\t\tbutton.pure-button {\n"
                    + "\t\t\t\tmargin:5px;\n"
                    + "\t\t\t}\n"
                    + "\n"
                    + "\t\t\tspan.pure-button {\n"
                    + "\t\t\t\tcursor:default;\n"
                    + "\t\t\t}\n"
                    + "\n"
                    + "\t\t\t.button-graphwide,\n"
                    + "\t\t\t.button-parkandrideunlinked,\n"
                    + "\t\t\t.button-graphconnectivity,\n"
                    + "\t\t\t.button-turnrestrictionbad\t{\n"
                    + "\t\t\t\tcolor:white;\n"
                    + "\t\t\t\ttext-shadow: 0 1px 1px rgba(0, 0, 0, 0.2);\n"
                    + "\t\t\t}\n"
                    + "\n"
                    + "\t\t\t.button-graphwide {\n"
                    + "\t\t\t\tbackground: rgb(28, 184, 65); /* this is a green */\n"
                    + "\t\t\t}\n"
                    + "\n"
                    + "\t\t\t.button-parkandrideunlinked {\n"
                    + "\t\t\t\tbackground: rgb(202, 60, 60); /* this is a maroon */\n"
                    + "\t\t\t}\n"
                    + "\n"
                    + "\t\t\t.button-graphconnectivity{\n"
                    + "\t\t\t\tbackground: rgb(223, 117, 20); /* this is an orange */\n"
                    + "\t\t\t}\n"
                    + "\n"
                    + "\t\t\t.button-turnrestrictionbad {\n"
                    + "\t\t\t\tbackground: rgb(66, 184, 221); /* this is a light blue */\n"
                    + "\t\t\t}\n"
                    + "\n"
                    + "\t\t</style>\n"
                    + "";
                out.println(css);
                out.println("</head><body>");
                out.println(String.format("<h1>OpenTripPlanner annotations log for %s</h1>", annotationClassName));
                out.println("<h2>Graph report for Graph.obj</h2>");
                out.println("<p>");
                //adds links to the other HTML files
                for (Multiset.Entry<String> htmlAnnotationClass : classes.entrySet()) {
                    String label_name = htmlAnnotationClass.getElement();
                    String label;
                    int currentCount = 1;
                    //it needs to add link to every file even if they are split
                    while (currentCount <= htmlAnnotationClass.getCount()) {
                        label = label_name + currentCount;
                        if (label.equals(annotationClassName)) {
                            out.println(String.format(
                                    "<button class='pure-button pure-button-disabled button-%s'>%s</button>",
                                    label_name.toLowerCase(), label)
                            );
                        } else {
                            out.println(String.format(
                                    "<a class='pure-button button-%s' href=\"%s.html\">%s</a>",
                                    label_name.toLowerCase(), label, label)
                            );
                        }
                        currentCount++;
                    }
                }
                out.println("</p>");
                if (!isIndexFile) {
                    out.println("<ul id=\"log\">");
                    writeAnnotations(out);
                    out.println("</ul>");
                }

                out.println("</body></html>");
            }
        }

        /**
         * Writes annotations as LI html elements
         */
        private void writeAnnotations(PrintWriter out) {
            String annotationFMT = "<li>%s</li>";
            for (Map.Entry<String, String> annotation: writerAnnotations.entries()) {
                out.print(String.format(annotationFMT, annotation.getValue()));
            }
        }
    }
}
