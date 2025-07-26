# 🗑️ AutoDelete – Smart File Self-Destruct Scheduler

AutoDelete is a lightweight desktop tool that lets you **schedule automatic deletion of files** at a future date.  
Ideal for receipts, temporary files, screenshots, or anything you don’t want to linger forever.

## ✨ Features

- 🗓️ **Schedule deletions** using a calendar date picker (via right-click menu)
- 🔔 **Reminder notifications** for due deletions at login
- 🖥️ **GUI review panel** to choose which files to delete or skip
- 💣 **Permanent delete** (bypasses trash)
- 📁 **KDE Dolphin + GNOME Nautilus integration**
- 📦 Simple local installer (no sudo or system-wide installation required)

## 🛠️ Installation (Linux)

1. Download the latest release `.zip` or `.tar.gz` from the [Releases](https://github.com/yasshhhraj/autodeletion/releases) page.
2. Extract the archive and navigate into the folder.
3. Run the installer:

```bash
bash install.sh
```

---

## 🖱️ Right-Click Menu Options

### 🗓️ Schedule Auto Deletion

- Opens a calendar to select a deletion date.
- The file is scheduled and stored in `~/.local/share/autodelete/db.json`.

### 💣 Permanent Delete Now

- Immediately deletes selected files (skips trash).
- Useful for secure or clutter-free removal.

---

## 📋 CLI Usage

You can also run the JAR manually:

```bash
java -jar DeleteAutomation.jar schedule <file> <YYYY-MM-DD>
java -jar DeleteAutomation.jar cancel-schedule <file>
java -jar DeleteAutomation.jar delete-select <file1> <file2> ...
java -jar DeleteAutomation.jar delete-all-due
java -jar DeleteAutomation.jar list-due
java -jar DeleteAutomation.jar due-count
java -jar DeleteAutomation.jar list-scheduled
java -jar DeleteAutomation.jar review
java -jar DeleteAutomation.jar notify
java -jar DeleteAutomation.jar help
```

---

## 🔄 Uninstallation

```bash
bash install.sh uninstall
```

This removes all added context menus, scripts, and autostart reminders.
User data (`db.json`) will remain at `~/.local/share/autodelete/`.

---

## ⚙️ Requirements

- `java` (OpenJDK 11+ recommended)
- `dunst` (used for  notifications)

Install dependencies on Debian/Ubuntu:

```bash
sudo apt install default-jre dunst
```

---

## 🧩 Integration Details

| Environment | Method               | Path Installed To                              |
|-------------|----------------------|------------------------------------------------|
| KDE         | Dolphin service menu | `~/.local/share/kio/servicemenus/`             |
| GNOME       | Nautilus script      | `~/.local/share/nautilus/scripts/`             |

Wrapper scripts and the JAR are stored in:  
`~/.local/share/autodelete/`

---

## 🚀 Roadmap

- [x] KDE support
- [x] Reminder + GUI review panel
- [ ] Windows support (in progress)
- [ ] `.deb` packaging
- [ ] Optional rescheduling / snooze
- [ ] Secure shred/delete mode

---

## 👨‍💻 Author

**Yashraj Jangir**  
[GitHub](https://github.com/yasshhhraj) • [LinkedIn](https://www.linkedin.com/in/yashraj-jangir-a6111512b/)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

> 🧹 _Built to clean up your digital mess, one file at a time._
