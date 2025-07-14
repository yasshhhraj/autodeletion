import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FileToDelete {
    @JsonProperty("filename")
    public String fileName;
    @JsonProperty("filepath")
    public String filePath;
    @JsonProperty("deleteAt")
    public String deleteAt;

    public FileToDelete() {}

    public FileToDelete(String fileName, String filePath,  String dateTime) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.deleteAt = dateTime;
    }

    @JsonIgnore
    public String getDeleteAt() {
        return deleteAt;
    }
    @JsonIgnore
    public boolean isDueDelete() {
        return LocalDate.parse(deleteAt).isBefore(LocalDate.now());
    }

    @Override
    public String toString() {
        return filePath +" deleteOn "+ deleteAt;
    }

}
