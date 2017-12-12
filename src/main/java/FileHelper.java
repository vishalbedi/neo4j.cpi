
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that handles File operations
 */
class FileHelper {

    /**
     * Reads the file which has the subgraph text
     * @return List<String[]> list that is equivalent of file with each item in list being a line in the file and
     * each item within the string array being each word in the file delimited by space.
     */
    List<String[]> readFile(File file, String delimiter){
        List<String[]> list;
        try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()))) {
            list = stream
                    .map(line->line.split(Pattern.quote(delimiter)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println("File Not Found at the specified Path");
            return null;
        }
        return list;
    }


    /**
     * Returns List of Files present at given path
     * @param path
     * @return
     */
    Stream<File> getAllFileNames(String path){
        File folder = new File(path);
        File[] files = folder.listFiles();
        return Stream.of(files != null ? files : new File[0]);
    }

    /**
     * Reads a Ground Truth File, searches for specific target and query pair and converts it into a Map
     * Where key is the search node within the query and its value is a list of nodes it matches within the target file.
     * @param file Ground Truth File
     * @param target name of the target file to find within ground truth file.
     * @param query name of the query file to find within the ground truth file.
     * @return Solution Map of specific query applied to given target.
     */
    Map<Integer, Set<Integer>> readGroundTruth(File file, String target, String query){
        List<String[]> groundTruthFile = readFile(file,":");
        boolean targetRead = false;
        boolean blockMatch = false;
        boolean solutionMode = false;
        Map<Integer,Set<Integer>> solutionMap = new HashMap<>();
        int currentSolutionIndex=0;
        int totalSolutions=0;
        if (groundTruthFile != null) {
            for (String [] line: groundTruthFile) {
                if(line.length>1){
                    if(targetRead && line[0].equals("P") && line[1].equals(query)){
                        blockMatch = true;
                        continue;
                    }
                    if(targetRead && !blockMatch)
                        targetRead = false;
                    if(line[0].equals("T") && line[1].equals(target+".grf")){
                        targetRead = true;
                        continue;
                    }
                    if(targetRead && blockMatch && line[0].equals("N")){
                        totalSolutions = Integer.parseInt(line[1]);
                        solutionMode = true;
                        continue;
                    }
                    if(solutionMode){
                        if(currentSolutionIndex<totalSolutions){
                            String[] pairs = line[2].split(Pattern.quote(";"));
                            for (String pair: pairs) {
                                String[] solution = pair.split(Pattern.quote(","));
                                int  key = Integer.parseInt(solution[0]);
                                int value = Integer.parseInt(solution[1]);
                                populateResultMap(solutionMap,key,value);
                            }
                            currentSolutionIndex++;
                        }else {
                            solutionMode = false;
                            targetRead=false;
                            blockMatch=false;
                        }
                    }
                }
            }
        }
        return solutionMap;
    }

    /**
     * Get all file names present in the folder and filter them based on number of nodes
     * @param path folder path
     * @param filterNodes number of nodes
     * @return list of files.
     */
    Stream<File> getAllFileNames(String path, int filterNodes){
        File folder = new File(path);
        File[] files = folder.listFiles();
        Stream<File> fileStream = Stream.of(files);
        fileStream = fileStream.filter(file -> file.getName().contains("."+filterNodes+"."));
        return fileStream;
    }

    void populateResultMap( Map<Integer, Set<Integer>>  solutionMap, int key, int value){
        if(solutionMap.containsKey(key)){
            solutionMap.get(key).add(value);
        }else {
            Set<Integer> list = new HashSet<>();
            list.add(value);
            solutionMap.put(key,list);
        }
    }
}

