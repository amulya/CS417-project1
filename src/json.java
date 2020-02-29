//import com.google.gson.Gson;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

public class json {

    public static void main(String args[]){
        // take in two params (first flag and input file)
        String firstFlag = args[0];
        //System.out.println(firstFlag);
        String inputOrJsonFile = args[1]; // input: ser | Json: deser
        //System.out.println(inputOrJsonFile);

        if(firstFlag.equals("-s")){ // serialize
            serializeJson(inputOrJsonFile, false);
        }else if(firstFlag.equals(("-d"))){ // deserialize
            deserializeJson(inputOrJsonFile, false);
        }else if(firstFlag.equals("-t")) { // serialize, deserialize, and print metrics
            long[] serInfo = serializeJson(inputOrJsonFile, true);
            long[] deserInfo = deserializeJson("result.json", true);

            long totalTime = serInfo[0] + deserInfo[0];
            long serKbps = (serInfo[1] / serInfo[0]) * 8;
            long deserKbps = (deserInfo[1] / serInfo[0]) * 8;

            System.out.println("Total time is "+totalTime+" ms");
            System.out.println("Speed of JSON Serialization "+serKbps+" kbps");
            System.out.println("Speed of JSON Deserialization "+deserKbps+" kbps");
        }
    }

    // returns serialization time (so deserialization can print total time)
    @SuppressWarnings("unchecked")
    public static long[] serializeJson(String inputFile, boolean doMetrics){

        long[] returnVal = new long[2];
        try {
            // write to result file
            FileWriter writer = new FileWriter("result.json");

            // open file
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            long startTime = System.nanoTime(); // timing for metrics

            // result object
            JSONArray result = new JSONArray();

            // read file line by line
            String line;
            String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$"; // email regex

            String lastname = "";
            String firstname = "";
            String id = "";
            String email = "";

            while ((line = reader.readLine()) != null) {
                Map<String, Object> studentMap = new LinkedHashMap<>();
                JSONArray courseMarksList = new JSONArray();

                StringBuilder token = new StringBuilder();
                int counter = 1;
                for (int i = 0; i < line.length(); i++) {
                    while (i < line.length() && line.charAt(i) == ':') { // course/grade pair; comma separates the two
                        // add previous field
                        String field = token.toString();
                        if (field.matches(regex)) { // check for email
                            //student.setEmail(token.toString());
                            email = field;
                            token = new StringBuilder();
                        } else if (firstname.length() == 0) { // first name
                            firstname = field;
                            token = new StringBuilder();
                        }
                        // add course/grade pair to list in object
                        Map<String, Object> coursePairMap = new LinkedHashMap<>();

                        i++;
                        while (line.charAt(i) != ',') {
                            token.append(line.charAt(i));
                            i++;
                        }
                        String courseName = token.toString();
                        token = new StringBuilder();
                        i++;
                        while (i < line.length() && line.charAt(i) != ':') {
                            token.append(line.charAt(i));
                            i++;
                        }
                        //.out.println(token.toString());
                        int score = Integer.parseInt(token.toString());
                        token = new StringBuilder();
                        coursePairMap.put("CourseName", courseName);
                        coursePairMap.put("CourseScore", score);

                        JSONObject courseMarksObj = new JSONObject(coursePairMap);
                        // add it to coursemarks list
                        courseMarksList.add(courseMarksObj);
                    }
                    if (i < line.length() && line.charAt(i) == ',') {
                        String field = token.toString();
                        if (counter == 1) { // id
                            id = field;
                            token = new StringBuilder();
                        } else if (counter == 2) { // last name
                            lastname = field;
                            token = new StringBuilder();
                            //System.out.println("last name set");
                        } else if (counter == 3) { // first name
                            firstname = field;
                            token = new StringBuilder();
                        }
                        counter++; // increment at the end of every column
                    } else if (i < line.length()) {
                        token.append(line.charAt(i));
                    }
                }
                // make JSON array of students; add student
                studentMap.put("id",id);
                studentMap.put("lastname",lastname);
                studentMap.put("firstname", firstname);
                studentMap.put("CourseMarks", courseMarksList);
                if(email.length() > 0){
                    studentMap.put("email", email);
                }

                JSONObject studentObj = new JSONObject(studentMap);
                result.add(studentObj);
            }

            long endTime = System.nanoTime();

            writer.write(result.toJSONString()); // serialize

            writer.flush();
            writer.close();

            if(doMetrics){
                // calculate serialization time
                long duration = (endTime - startTime)/1000000; // time in ms

                // calculate size of result.json
                File file = new File("result.json");
                long size = file.length(); // fize size in bytes

                returnVal[0] = duration;
                returnVal[1] = size;
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnVal;
    }

    @SuppressWarnings("unchecked")
    public static long[] deserializeJson(String json_file, boolean doMetrics){

        long[] returnVal = new long[2];
        try {
            FileWriter writer = new FileWriter("output_json.txt");
            BufferedReader reader = new BufferedReader(new FileReader(json_file));

            long startTime = System.nanoTime();

            //Gson gson = new Gson();
            //Result resultObj = gson.fromJson(jsonString, Result.class);
            FileReader fr = new FileReader(json_file);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(fr);
            JSONArray jsonArray = (JSONArray) obj;
            //System.out.println(jsonArray.toString());
            Iterator<JSONObject> iterator = jsonArray.iterator();

            StringBuilder line = new StringBuilder();
            while (iterator.hasNext()) {
                JSONObject currentStudent = iterator.next();
                //System.out.println(currentStudent.toString());
                String id = (String)currentStudent.get("id");
                String lastName = (String)currentStudent.get("lastname");
                String firstName = (String)currentStudent.get("firstname");
                String email = (String)currentStudent.get("email"); // might be null if no email
                JSONArray courseMarksList = (JSONArray) currentStudent.get("CourseMarks");

                // TODO put together string (need to iterate through courseMarksList)
                line.append(id);
                line.append(',');
                line.append(lastName);
                line.append(',');
                line.append(firstName);
                if(email != null){
                    line.append(',');
                    line.append(email);
                }

                Iterator<JSONObject> iterator2 = courseMarksList.iterator();
                while (iterator2.hasNext()) {
                    JSONObject courseMarks = iterator2.next();
                    String name = (String) courseMarks.get("CourseName");
                    long score = (long)courseMarks.get("CourseScore");
                    line.append(':');
                    line.append(name);
                    line.append(',');
                    line.append(score);
                }
                line.append('\n');
            }

            long endTime = System.nanoTime();

            // deserialize
            writer.write(line.toString());
            //writer.write("\r\n"); // new line

            reader.close();
            writer.flush();
            writer.close();

            if(doMetrics){
                // calculate serialization time
                long duration = (endTime - startTime)/1000000;

                // calculate size of output_json.txt
                File file = new File("output_json.txt");
                long size = file.length(); // fize size in bytes

                returnVal[0] = duration;
                returnVal[1] = size;
            }

            return returnVal;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnVal;
    }
}
