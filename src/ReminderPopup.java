import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class ReminderPopup extends JDialog {
    public ReminderPopup(ArrayList<String> dueFiles, File db) {
        setTitle("AutoDelete Reminder");
        setModal(true);
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel label = new JLabel("You have " + dueFiles.size() + " file(s) awaiting deletion.");
        label.setBorder(BorderFactory.createEmptyBorder(10, 20, 0, 20));
        add(label, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteAllDue = new JButton("Delete All");
        JButton review = new JButton("Review");
        JButton skip = new JButton("Skip");

        buttonPanel.add(deleteAllDue);
        buttonPanel.add(review);
        buttonPanel.add(skip);
        add(buttonPanel, BorderLayout.SOUTH);

        deleteAllDue.addActionListener(e -> {
            Main.delete_all_due(db);
            dispose();
        });

        review.addActionListener(e -> {
            Main.review_deletion(db);
            dispose();
        });


        skip.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(null); // center on screen
    }
}
