import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toedter.calendar.JCalendar;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Main {
    public static final String LIN_DB=System.getProperty("user.home")+"/.local/share/autodelete/db.json";
    public static final ObjectMapper mapper = new ObjectMapper();

    /**
     * reads db.json for scheduled deletions
     * @param db file db.json source
     * @return map of files
     */
    static HashMap<String, String> DbRead(File db) {
        try {
            String json_string = Files.readString(db.toPath());
            if(json_string.trim().isEmpty()) return new HashMap<>();
            return mapper.readValue(json_string, new TypeReference<>() {});
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
        try {
            String s = mapper.writeValueAsString(map);
            Files.writeString(db.toPath(), s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private static List<String> get_due_delete_files(File db) {
        ArrayList<String> list_due_delete = new ArrayList<>();
        HashMap<String, String> map = Main.DbRead(db);
        map.forEach((filepath, date) ->{
            if(LocalDate.parse(date).isBefore(LocalDate.now())) list_due_delete.add(filepath);
        });
        return list_due_delete;
    }

    private static LocalDate pick_date() {
        JCalendar cal = new JCalendar();
        int result = JOptionPane.showConfirmDialog(
                null,
                cal,
                "Select Deletion Date",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            java.util.Date selected = cal.getDate();
            return selected.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }


    static void schedule(String[] args, File db) {
        String filePath = args[1];
        File f = new File(filePath);
        if (!f.exists()) {
            System.out.println("Error: File does not exist: " + filePath);
            return;
        }

        LocalDate deleteDate;
        if(args.length == 3){
            deleteDate = LocalDate.parse(args[2]);
        }else {
            deleteDate = pick_date();
            if(deleteDate == null) {
                System.out.println("Schedule cancelled.");
                return;
            }
        }

        HashMap<String, String> map = Main.DbRead(db);
        map.put(filePath, deleteDate.toString());
        Main.DbWrite(map, db);
        System.out.println(args[1] + " set for autodeletion on " + deleteDate);
    }

    static void cancel_schedule(String[] args, File db) {
        String filePath = args[1];
        HashMap<String, String> map = Main.DbRead(db);
        map.remove(filePath);
        Main.DbWrite(map, db);
        System.out.println(args[1]+ " autodeletion cancelled");
    }

    /**
     * cancels scheduled deletion for a list of filepaths
     * @param filepaths list of files to be removed from the schedule
     * @param db file db.json
     */
    static void cancel_schedule_for_multiple(List<String> filepaths, File db) {
        HashMap<String, String> map = Main.DbRead(db);
        filepaths.forEach(map::remove);
        Main.DbWrite(map, db);
    }

    static void delete_select(String[] args, File db) {
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
        System.out.println("deletion operation finished.");
    }

    static void delete_all_due(File db) {
        HashMap<String, String> map = Main.DbRead(db);
        if(map.isEmpty()) {
            System.out.println("no files scheduled for autodeletion");
        }
        List<String> filepaths = new ArrayList<>();
        map.forEach((filePath, date) -> {
            if (!LocalDate.parse(date).isBefore(LocalDate.now())) return;
            filepaths.add(filePath);
            File f = new File(filePath);
            if (f.exists()) {
                if (f.delete()) {
                    System.out.println("file " + filePath + " deleted!");
                }
            } else {
                System.out.println(filePath + " does not exist!");
            }
        });

        Main.removeDbEntries(filepaths, db);
        System.out.println("deleted all due deletion files");
    }

    static void list_due_delete(File db) {
        List<String> list = get_due_delete_files(db);
        list.forEach(System.out::println);
    }

    static void list_scheduled_files(File db) {
        HashMap<String, String> map = Main.DbRead(db);
        map.keySet().forEach(System.out::println);
    }

    static void review_deletion(File db) {
        GUI frame = new GUI();

        HashMap<String, String> map = Main.DbRead(db);
        map.forEach((filepath, date) -> {
            if(LocalDate.parse(date).isBefore(LocalDate.now())) frame.addFileEntry(filepath);
        });

        frame.setVisible(true);
    }

    static void notify(File db) {
        ArrayList<String> list_due_delete= (ArrayList<String>) get_due_delete_files(db);
        if (!list_due_delete.isEmpty()) {
            SwingUtilities.invokeLater(() -> new ReminderPopup(list_due_delete, db).setVisible(true));
        } else {
            System.out.println("No files pending deletion.");
        }
    }

    static void help() {
        System.out.println("Usage:");
        System.out.println("  schedule <filepath> <YYYY-MM-DD>          - Schedule file for deletion");
        System.out.println("  cancel-schedule <filepath>                - Cancel scheduled deletion");
        System.out.println("  delete-select <file1> <file2>...          - Delete selected files");
        System.out.println("  delete-all-due                            - Delete all due files");
        System.out.println("  list-due                                  - List files due for deletion");
        System.out.println("  list-scheduled                            - List all scheduled deletions");
        System.out.println("  review                                    - Show UI for reviewing before deletion ");
        System.out.println("  notify                                    - Show reminder popup");
        System.out.println("  help                                      - Show this menu");

    }

    public static void main(String[] args) throws IOException {
        if(args.length==0){
            System.out.println("this tool needs arguments to work. ");
            help();
            return;
        }

        File db =  new File(LIN_DB);
        if(!db.exists()){
            Files.createDirectories(db.toPath().getParent());
            boolean newFile = db.createNewFile();
            if(!newFile) {
                throw new RuntimeException("Unable to create file");
            }
        }


        switch(args[0]) {
            case "schedule": { schedule(args, db); break; }

            case "cancel-schedule": { cancel_schedule(args, db); break; }

            case "delete-select": { delete_select(args, db); break; }

            case "delete-all-due": { delete_all_due(db); break; }

            case "list-due": { list_due_delete(db); break; }

            case "list-scheduled": { list_scheduled_files(db); break; }

            case "review": { review_deletion(db); break; }

            case "notify" : { notify(db); break; }

            case "help": { help(); break; }

            default: System.out.println("Invalid command. Run with `help` to see available options.");

        }
    }
}