import com.toedter.calendar.JCalendar;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
public class DateSelector {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            JCalendar cal = new JCalendar();

            int result = JOptionPane.showConfirmDialog(
                    null,
                    cal,
                    "Select Deletion Date",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                java.util.Date selectedDate = cal.getDate();
                LocalDate localDate = new java.sql.Date(selectedDate.getTime()).toLocalDate();
                System.out.println(localDate.format(DateTimeFormatter.ISO_DATE));
                System.exit(0);
            } else {
                System.exit(1); // Cancelled
            }
        });
    }
}
