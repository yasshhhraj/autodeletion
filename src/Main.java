import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Main {
    public static final String DB=System.getProperty("user.home")+"/.local/share/autodelete/db.json";
    public static final ObjectMapper mapper = new ObjectMapper();
    /**
     * reads db.json for scheduled deletions
     * @param db file db.json source
     * @return map of files
     */
    static HashMap<String, String> DbRead(File db) {
        try(BufferedReader br = new BufferedReader(new FileReader(db))) {
            if (db.length() == 0) return new HashMap<>(); // empty file check
            String line = br.readLine();
            return line == null || line.trim().isEmpty()
                    ? new HashMap<>()
                    : mapper.readValue(line, new TypeReference<>() {});
        } catch (IOException e) {
            System.err.println("Warning: Could not read database. Returning empty map.");
            return new HashMap<>();
        }
    }

    /**
     * writes back the map to db.json. to be used when actions are completed
     * @param map contains new data
     * @param db file db.json destination
     */
    static void DbWrite(HashMap<String, String> map, File db) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(db))) {
            String s = mapper.writeValueAsString(map);
            bw.write(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * add schedule new deletion
     * @param filepath absolute file path
     * @param deleteDate date if auto deletion
     * @param db destination file db.json
     */
    private static void addNewEntry(String filepath, LocalDate deleteDate, File db) {
        HashMap<String, String> map = Main.DbRead(db);
        map.put(filepath, deleteDate.toString());
        Main.DbWrite(map, db);
    }

    /**
     * cancelling scheduled auto deletion
     * @param filepath absolute path/to/file
     * @param db file db.json
     */
    private static void removeEntry(String filepath, File db) {
        HashMap<String, String> map = Main.DbRead(db);
        map.remove(filepath);
        Main.DbWrite(map, db);
    }

    /**
     * deletes files given using file paths as arguments
     * @param db file db.json
     * @param args command line arguments
     */
    private static void delete(File db, String[] args) {
            HashMap<String, String> map =Main.DbRead(db);
            for(int i=1; i<args.length; i++) {
                String filepath = args[i];
                if (map.containsKey(filepath)) {
                    File ftd = new File(filepath);
                    if (ftd.exists()) {
                        if(ftd.delete()) {
                            System.out.println("File "+filepath+" deleted successfully");
                        }else {
                            System.out.println("File "+filepath+" could not be deleted");
                        }
                    } else {
                        System.out.println(filepath + " does not exist!");
                    }
                    map.remove(filepath);
                }else {
                    System.out.println(filepath + " not scheduled for autodeletion!");
                }
            }
            Main.DbWrite(map, db);
    }

    /**
     * gives files that are due deletion and past deletion date
     * @param db file db.json
     * @return list of due delete files
     */
    private static List<String> dueDeleteFiles(File db) {
        List<String> list = new ArrayList<>();
        HashMap<String, String> map = Main.DbRead(db);
        map.forEach((filepath, date) ->{
            if(LocalDate.parse(date).isBefore(LocalDate.now())) list.add(filepath);
        });
        return list;
    }

    /**
     * gives all files that are scheduled for deletion
     * @param db file db.json
     * @return list of files scheduled for auto deletion
     */
    private static List<String> scheduled(File db) {
        List<String> list = new ArrayList<>();
        HashMap<String, String> map = Main.DbRead(db);
        map.forEach((filepath, _) -> list.add(filepath));
        return list;
    }

    /**
     * cleanup db after file deletion
     * @param filepaths absolute paths of files deleted
     * @param db file db.json
     */
    static void removeDbEntries(List<String> filepaths, File db) {
        HashMap<String, String> map = Main.DbRead(db);
        for (String filepath : filepaths) map.remove(filepath);
        Main.DbWrite(map, db);

    }

    /**
     * deletes all files that are due deletion
     * @param db file db.json
     */
    private static void deleteAll(File db) {
        try {
            HashMap<String, String> map = Main.DbRead(db);
            if(map.isEmpty()) {
                System.out.println("no files scheduled for autodeletion");
            }
            map.forEach((filePath, date) -> {
                if (!LocalDate.parse(date).isBefore(LocalDate.now())) return;
                File f = new File(filePath);
                if (f.exists()) {
                    if (f.delete()) {
                        System.out.println("file " + filePath + " deleted!");
                    }
                } else {
                    System.out.println(filePath + " does not exist!");
                }
            });

            FileWriter fw = new FileWriter(db);
            fw.write("{}");
            fw.close();



        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws IOException {
        File db =  new File(DB);
        if(!db.exists()){
            Files.createDirectories(db.toPath().getParent());
            boolean newFile = db.createNewFile();
            if(!newFile) {
                throw new RuntimeException("Unable to create file");
            }
        }

        if(args.length==0){
            System.out.println("this cli tool needs arguments to work. use help to see available options");
            return;
        }

        switch(args[0]) {
            case "add": {
                String filePath = args[1];
                LocalDate deleteDate = LocalDate.parse(args[2]);
                Main.addNewEntry(filePath, deleteDate, db);
                System.out.println(args[1] + " set for autodeletion on " + deleteDate);
                break;
            }
            case "remove": {
                String filePath = args[1];
                Main.removeEntry(filePath, db);
                System.out.println(args[1]+ " autodeletion cancelled");
                break;
            }
            case "deleteselect": {
                delete(db, args);
                System.out.println("deletion operation finished.");
                break;
            }
            case "deleteall": {
                Main.deleteAll(db);
                System.out.println("deleted all due deletion files");
                break;
            }
            case "duedelete": {
                List<String> list = Main.dueDeleteFiles(db);
                list.forEach(System.out::println);
                break;
            }
            case "scheduled": {
                List<String> list = Main.scheduled(db);
                list.forEach(System.out::println);
                break;
            }


            case "notify" : {
                List<String> list = Main.dueDeleteFiles(db);
                Process proc = new ProcessBuilder(
                        "zenity",
                        "--question",
                        "--title=AutoDelete Reminder",
                        "--text=You have "+list.size()+"  files awaiting for deletion",
                        "--ok-label=Delete All",
                        "--cancel-label=Skip",
                        "--extra-button=Review").start();
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    int ch = proc.waitFor();
                    String btn = br.readLine();
                    switch (ch) {
                        case 0: {
                            Main.deleteAll(db);
                            break;
                        }
                        case 1: {
                            if(btn!=null && btn.equals("Review")) {

                                GUI frame = new GUI();
                                list.forEach(frame::addFileEntry);
                                frame.setVisible(true);

                            }
                            break;
                        }

                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
            case "help": {
                System.out.println("Usage:");
                System.out.println("  add <filepath> <YYYY-MM-DD>       - Schedule file for deletion");
                System.out.println("  remove <filepath>                - Cancel scheduled deletion");
                System.out.println("  deleteselect <file1> <file2>...  - Delete selected files");
                System.out.println("  deleteall                        - Delete all due files");
                System.out.println("  duedelete                        - List files due for deletion");
                System.out.println("  scheduled                        - List all scheduled deletions");
                System.out.println("  notify                           - Show zenity reminder popup");
                break;
            }
            default:
                System.out.println("Invalid command. Run with `help` to see available options.");

        }
    }
}