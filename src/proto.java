import java.io.*;

public class proto {

    public static void main(String[] args) {
        String firstFlag = args[0];
        String inputOrProtoFile = args[1]; // input: ser | Json: deser

        if (firstFlag.equals("-s")) { // serialize
            serializeProtobuf(inputOrProtoFile, false);
        } else if (firstFlag.equals(("-d"))) { // deserialize
            deserializeProtobuf(inputOrProtoFile, false);
        } else if (firstFlag.equals("-t")) { // serialize, deserialize, and print metrics
            long[] serInfo = serializeProtobuf(inputOrProtoFile, true);
            long[] deserInfo = deserializeProtobuf("result_protobuf", true);

            long totalTime = serInfo[0] + deserInfo[0];
            long serKbps = (serInfo[1] / serInfo[0]) * 8;
            long deserKbps = (deserInfo[1] / serInfo[0]) * 8;

            System.out.println("Total time is "+totalTime+" ms");
            System.out.println("Speed of PROTOBUF Serialization "+serKbps+" kbps");
            System.out.println("Speed of PROTOBUF Deserialization "+deserKbps+" kbps");
        }
    }

    public static long[] serializeProtobuf(String inputFile, boolean doMetrics) {

        long[] returnVal = new long[2];
        try {
            OutputStream outputStream = new FileOutputStream("result_protobuf");

            // open file
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            long startTime = System.nanoTime(); // timing for metrics

            ResultProto.Result.Builder result = ResultProto.Result.newBuilder();

            // read file line by line
            String line;
            String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$"; // email regex

            while ((line = reader.readLine()) != null) {
                //System.out.println("line: " + line);
                ResultProto.Student.Builder student = ResultProto.Student.newBuilder();
                StringBuilder token = new StringBuilder();
                int counter = 1;
                for (int i = 0; i < line.length(); i++) {
                    while (i < line.length() && line.charAt(i) == ':') { // course/grade pair; comma separates the two
                        // add previous field
                        String field = token.toString();
                        if (field.matches(regex)) { // check for email
                            student.setEmail(token.toString());
                            token = new StringBuilder();
                        } else if (student.getFirstname().length() == 0) { // first name
                            student.setFirstname(field);
                            token = new StringBuilder();
                        }
                        // add course/grade pair to list in object
                        ResultProto.CourseMarks.Builder coursePair = ResultProto.CourseMarks.newBuilder();

                        i++;
                        while (line.charAt(i) != ',') {
                            token.append(line.charAt(i));
                            i++;
                        }
                        //System.out.println(token.toString());
                        coursePair.setName(token.toString());
                        token = new StringBuilder();
                        i++;
                        while (i < line.length() && line.charAt(i) != ':') {
                            token.append(line.charAt(i));
                            i++;
                        }
                        //.out.println(token.toString());
                        coursePair.setScore(Integer.parseInt(token.toString()));
                        token = new StringBuilder();
                        student.addMarks(coursePair);
                    }
                    if (i < line.length() && line.charAt(i) == ',') {
                        String field = token.toString();
                        //System.out.println(token.toString());
                        if (counter == 1) { // id
                            student.setId(field);
                            token = new StringBuilder();
                            //System.out.println("id set");
                        } else if (counter == 2) { // last name
                            student.setLastname(field);
                            token = new StringBuilder();
                            //System.out.println("last name set");
                        } else if (counter == 3) { // first name
                            student.setFirstname(field);
                            token = new StringBuilder();
                            //System.out.println("first name set");
                        }
                        counter++; // increment at the end of every column
                    } else if (i < line.length()) {
                        token.append(line.charAt(i));
                        //System.out.println(token.toString());
                    }
                }
                //System.out.println(student);
                result.addStudent(student);
            }

            long endTime = System.nanoTime(); // timing for metrics

            // serialize
            result.build().writeTo(outputStream);

            // end
            reader.close();
            outputStream.flush();
            outputStream.close();

            if (doMetrics) {
                // calculate serialization time
                long duration = (endTime - startTime) / 1000000; // time in ms

                // calculate size of result_protobuf
                File file = new File("result_protobuf");
                long size = file.length(); // fize size in bytes

                returnVal[0] = duration;
                returnVal[1] = size;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnVal;
    }

    public static long[] deserializeProtobuf(String protobuf_file, boolean doMetrics){

        long[] returnVal = new long[2];
        try {
            //OutputStream outputStream = new FileOutputStream("output_protobuf.txt");
            FileWriter writer = new FileWriter("output_protobuf.txt");
            InputStream inputStream = new FileInputStream(protobuf_file);

            long startTime = System.nanoTime();

            // load into memory
            ResultProto.Result result = ResultProto.Result.parseFrom(inputStream);

            StringBuilder line = new StringBuilder();
            // read through each student object and write to output stream in correct format
            for(ResultProto.Student student : result.getStudentList()){
                String id = student.getId();
                String lastName = student.getLastname();
                String firstName = student.getFirstname();
                String email = student.getEmail();
                line.append(id);
                line.append(',');
                line.append(lastName);
                line.append(',');
                line.append(firstName);
                if(email.length() > 0){
                    line.append(',');
                    line.append(email);
                }
                // go through coursemark list
                for(ResultProto.CourseMarks courseMarks : student.getMarksList()){
                    String courseName = courseMarks.getName();
                    int score = courseMarks.getScore();
                    line.append(':');
                    line.append(courseName);
                    line.append(',');
                    line.append(score);
                }
                line.append('\n');
                //line.setLength(0);
            }

            long endTime = System.nanoTime();

            writer.write(line.toString());
            //writer.write("\r\n"); // new line

            inputStream.close();
            writer.flush();
            writer.close();

            if (doMetrics) {
                // calculate serialization time
                long duration = (endTime - startTime) / 1000000; // time in ms

                // calculate size of output_protobuf.txt
                File file = new File("output_protobuf.txt");
                long size = file.length(); // fize size in bytes

                returnVal[0] = duration;
                returnVal[1] = size;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnVal;
    }
}
