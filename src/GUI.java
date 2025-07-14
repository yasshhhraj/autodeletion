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
        JButton skipButton = new JButton("Skip");

        buttonPanel.add(deleteAllButton);
        buttonPanel.add(deleteSelectedButton);
        buttonPanel.add(skipButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---
        deleteAllButton.addActionListener(j -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete ALL files?", "Confirm Delete All",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                removeAllFileEntries();
            }
        });

        deleteSelectedButton.addActionListener(j -> deleteSelectedFiles());

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
     * Removes all file entry components from the UI.
     */
    public void removeAllFileEntries() {

        fileEntries.forEach(file ->{
            String path = file.getFilePath();
            File ftd = new File(path);
            if(ftd.exists()) {
                if (!ftd.delete()) {
                    System.out.println("Failed to delete file: " + ftd.getAbsolutePath());
                }
            }else {
                System.out.println(path + " file does not exist");
            }
        });
        Main.removeDbEntries(fileEntries.stream().map(FileEntryComponent::getFilePath).collect(Collectors.toList()), new File(Main.DB));


        entriesPanel.removeAll();
        fileEntries.clear();
        entriesPanel.revalidate();
        entriesPanel.repaint();
    }

    /**
     * Deletes (simulated) the selected file entries from the UI.
     */
    public void deleteSelectedFiles() {
        List<FileEntryComponent> toRemove = new ArrayList<>();
        for (FileEntryComponent entry : fileEntries) {
            if (entry.isSelected()) {
                System.out.println("Deleting (simulated): " + entry.getFilePath());
                toRemove.add(entry);
            }
        }

        if (toRemove.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files selected for deletion.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete " + toRemove.size() + " selected file(s)?", "Confirm Delete Selected",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            toRemove.forEach(file -> {
                String path = file.getFilePath();
                File ftd = new File(path);
                if (ftd.exists()) {
                    if (ftd.delete()) {
                        if (toRemove.contains(file)) {
                            entriesPanel.remove(file);
                        }
                        entriesPanel.revalidate();
                        entriesPanel.repaint();
                        Main.removeDbEntries(toRemove.stream().map(FileEntryComponent::getFilePath).collect(Collectors.toList()), new File(Main.DB));
                    } else {
                        System.out.println("Error while deleting: " + path);
                    }
                } else {
                    System.out.println(path + " file does not exist");
                }
            });
            JOptionPane.showMessageDialog(this, toRemove.size() + " file(s) deleted.", "Deletion Complete", JOptionPane.INFORMATION_MESSAGE);

        }
    }
}