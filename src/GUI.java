import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GUI extends JFrame {

    private final JPanel entriesPanel; // Panel to hold all individual file entry components
    private final List<FileEntryComponent> fileEntries; // To keep track of our custom components

    // Private constructor to enforce creation via static factory method
    public GUI() {
        setTitle("Review Files");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close operation for the frame
        setLayout(new BorderLayout()); // Use BorderLayout for the main frame

        fileEntries = new ArrayList<>();

        entriesPanel = new JPanel();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS)); // Stack entries vertically
        entriesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add some padding/border

        JScrollPane scrollPane = new JScrollPane(entriesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10)); // Right-aligned, some spacing
        JButton deleteAllButton = new JButton("Delete All");
        JButton deleteSelectedButton = new JButton("Delete Selected");
        JButton cancelSelectedButton = new JButton("Cancel Deletion"); // New button
        JButton skipButton = new JButton("Skip");

        buttonPanel.add(deleteAllButton);
        buttonPanel.add(deleteSelectedButton);
        buttonPanel.add(cancelSelectedButton); // Add the new button
        buttonPanel.add(skipButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---
        deleteAllButton.addActionListener(j -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete ALL files?", "Confirm Delete All",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                removeAllFileEntries();
                dispose(); // Close the GUI after action
            }
        });

        deleteSelectedButton.addActionListener(j -> deleteSelectedFiles());

        cancelSelectedButton.addActionListener(j -> cancelSelectedFiles()); // New action listener

        skipButton.addActionListener(j -> {
            JOptionPane.showMessageDialog(this, "Skipping deletion for now.");
            dispose();
        });

        pack(); // Pack the frame to its preferred size
        setSize(800, 600); // Set a default size
        setLocationRelativeTo(null); // Center the frame on screen
    }

    // Custom JPanel for each file entry
    private static class FileEntryComponent extends JPanel {
        private final JCheckBox selectCheckbox;
        private final String filePath;

        public FileEntryComponent(String filePath) {
            this.filePath = filePath;
            setLayout(new BorderLayout(10, 0));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            setBackground(Color.WHITE);

            // File icon (optional)
            JLabel iconLabel = new JLabel(UIManager.getIcon("FileView.fileIcon"));

            // File name (bold) and path (smaller)
            File f = new File(filePath);
            String name = f.getName();
            String parent = f.getParent();

            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));

            JLabel pathLabel = new JLabel(parent);
            pathLabel.setFont(pathLabel.getFont().deriveFont(Font.PLAIN, 11f));
            pathLabel.setForeground(Color.GRAY);

            // Vertical file info panel
            JPanel fileInfo = new JPanel();
            fileInfo.setLayout(new BoxLayout(fileInfo, BoxLayout.Y_AXIS));
            fileInfo.setOpaque(false);
            fileInfo.add(nameLabel);
            fileInfo.add(pathLabel);

            // Right checkbox
            selectCheckbox = new JCheckBox("Select");

            // Add to layout
            add(iconLabel, BorderLayout.WEST);
            add(fileInfo, BorderLayout.CENTER);
            add(selectCheckbox, BorderLayout.EAST);

            // Tooltip on hover
            setToolTipText(filePath);
        }

        public boolean isSelected() {
            return selectCheckbox.isSelected();
        }

        public String getFilePath() {
            return filePath;
        }
    }

    /**
     * Adds a new file entry component to the UI.
     * @param filePath The path of the file to display.
     */
    public void addFileEntry(String filePath) {
        FileEntryComponent entry = new FileEntryComponent(filePath);
        fileEntries.add(entry);
        entriesPanel.add(entry);
        entriesPanel.revalidate();
        entriesPanel.repaint();
    }

    /**
     * Removes all file entry components from the UI after permanently deleting them from filesystem.
     */
    public void removeAllFileEntries() {
        fileEntries.forEach(file -> {
            String path = file.getFilePath();
            File ftd = new File(path);
            if (ftd.exists()) {
                if (!ftd.delete()) {
                    System.out.println("Failed to delete file: " + ftd.getAbsolutePath());
                }
            } else {
                System.out.println(path + " file does not exist");
            }
        });
        Main.removeDbEntries(fileEntries.stream().map(FileEntryComponent::getFilePath).collect(Collectors.toList()), new File(Main.LIN_DB));

        entriesPanel.removeAll();
        fileEntries.clear();
        entriesPanel.revalidate();
        entriesPanel.repaint();
    }

    /**
     * Deletes the selected file entries from the UI and disk.
     */
    public void deleteSelectedFiles() {
        List<FileEntryComponent> toDelete = new ArrayList<>();
        for (FileEntryComponent entry : fileEntries) {
            if (entry.isSelected()) {
                toDelete.add(entry);
            }
        }

        if (toDelete.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files selected for deletion.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, toDelete.size() + " selected file(s) will be deleted permanently.", "Confirm Delete Selected", JOptionPane.YES_NO_OPTION);

        List<String> list = new ArrayList<>();
        if (confirm == JOptionPane.YES_OPTION) {
            toDelete.forEach(file -> {
                String path = file.getFilePath();
                list.add(path);
                File ftd = new File(path);
                if (ftd.exists()) {
                    if (ftd.delete()) entriesPanel.remove(file);
                    else System.out.println("Error while deleting: " + path);
                } else System.out.println(path + " file does not exist");
            });
            entriesPanel.revalidate();
            entriesPanel.repaint();
            Main.removeDbEntries(list, new File(Main.LIN_DB));
            JOptionPane.showMessageDialog(this, toDelete.size() + " file(s) deleted.", "Deletion Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Cancels scheduled deletion for selected files.
     */
    public void cancelSelectedFiles() {
        List<FileEntryComponent> toCancel = new ArrayList<>();
        for (FileEntryComponent entry : fileEntries) {
            if (entry.isSelected()) {
                toCancel.add(entry);
            }
        }

        if (toCancel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files selected for cancellation.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, toCancel.size() + " selected file(s) will be removed from the schedule.", "Confirm Cancellation", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            List<String> filepathsToCancel = new ArrayList<>();
            toCancel.forEach(file -> {
                String path = file.getFilePath();
                filepathsToCancel.add(path);
                entriesPanel.remove(file); // Remove from the GUI immediately
                fileEntries.remove(file); // Also remove from the list
            });

            // Call the new Main method to update the database
            Main.cancel_schedule_for_multiple(filepathsToCancel, new File(Main.LIN_DB));

            entriesPanel.revalidate();
            entriesPanel.repaint();
            JOptionPane.showMessageDialog(this, toCancel.size() + " file(s) removed from schedule.", "Cancellation Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}