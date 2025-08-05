#!/bin/bash
set -e

### CONFIGURABLE PATHS ###
INSTALL_DIR="$HOME/.local/share/autodelete"
JAR_NAME="DeleteAutomation.jar"
DESKTOP_MENU_DIR="$HOME/.local/share/kio/servicemenus"
NAUTILUS_SCRIPT_DIR="$HOME/.local/share/nautilus/scripts"
SCRIPT_DIR="$(dirname "$(realpath "$0")")"
JAR_SOURCE="$SCRIPT_DIR/$JAR_NAME"

# Check dependencies
check_dependencies() {
    local missing_deps=()

    command -v java >/dev/null 2>&1 || missing_deps+=("java")
    command -v dunstify >/dev/null 2>&1 || missing_deps+=("dunstify")

    if [ ${#missing_deps[@]} -gt 0 ]; then
        echo "Error: Missing dependencies: ${missing_deps[*]}" >&2
        exit 1
    fi
}

# Create wrapper scripts to avoid shell injection
create_wrapper_scripts() {
  # cancel scheduled deletion
  cat > "$INSTALL_DIR/cancel_schedule_wrapper.sh" <<EOF
#!/bin/bash
exec java -jar "$INSTALL_DIR/$JAR_NAME" cancel-schedule "\$@"
EOF
  chmod +x "$INSTALL_DIR/cancel_schedule_wrapper.sh"

  # Schedule delete wrapper
  cat > "$INSTALL_DIR/schedule_wrapper.sh" <<EOF
#!/bin/bash
exec java -jar "$INSTALL_DIR/$JAR_NAME" schedule "\$@"
EOF
  chmod +x "$INSTALL_DIR/schedule_wrapper.sh"
}

# Main installation function
install_autodelete() {
    echo "📦 Installing AutoDelete..."

    # Check dependencies first
    check_dependencies

    # Step 1: Validate and copy JAR
    if [ ! -f "$JAR_SOURCE" ]; then
        echo "Error: JAR file '$JAR_NAME' not found in the script directory." >&2
        exit 1
    fi

    # Test if JAR is executable
    if ! java -jar "$JAR_SOURCE" --help >/dev/null 2>&1; then
        echo "Warning: Could not verify JAR functionality"
    fi

    mkdir -p "$INSTALL_DIR"
    cp "$JAR_SOURCE" "$INSTALL_DIR/"

    # Step 2: Create wrapper scripts
    create_wrapper_scripts

    # Step 3: Add Dolphin context menu (KDE)
    mkdir -p "$DESKTOP_MENU_DIR"
    cat > "$DESKTOP_MENU_DIR/AutoDelete.desktop" <<EOF
[Desktop Entry]
Type=Service
ServiceTypes=KonqPopupMenu/Plugin
MimeType=all/all;
Actions=ScheduleAutoDelete;CancelAutoDelete;

[Desktop Action ScheduleAutoDelete]
Name=Schedule Auto Deletion
Exec=$INSTALL_DIR/schedule_wrapper.sh %F
Icon=appointment-new

[Desktop Action CancelAutoDelete]
Name=Cancel Scheduled Deletion
Exec=$INSTALL_DIR/cancel_schedule_wrapper.sh %F
Icon=edit-clear

EOF
    chmod +x "$DESKTOP_MENU_DIR/AutoDelete.desktop"
    echo "✅ Dolphin context menu added."

    # Step 4: Add Nautilus script (GNOME)
    mkdir -p "$NAUTILUS_SCRIPT_DIR"
    cat > "$NAUTILUS_SCRIPT_DIR/ScheduleAutoDelete" <<EOF
#!/bin/bash
exec "$INSTALL_DIR/schedule_wrapper.sh" "\$@"
EOF
    chmod +x "$NAUTILUS_SCRIPT_DIR/ScheduleAutoDelete"
    echo "✅ Nautilus script added."

    # Step 5: Create a dedicated GUI launcher script
    LAUNCHER_PATH="$INSTALL_DIR/autodelete_gui_launcher.sh"
    cat > "$LAUNCHER_PATH" <<EOF
#!/bin/bash

# If DISPLAY is not set, use :1 as fallback
# export DISPLAY=${DISPLAY:-:1}
# export XAUTHORITY="${XAUTHORITY:-$HOME/.Xauthority}"

# Allow access to X11 for this user
xhost +SI:localuser:$(whoami) >/dev/null 2>&1

INSTALL_DIR="$HOME/.local/share/autodelete"
JAR_NAME="DeleteAutomation.jar"

# Show notification and capture response
ACTION=\$(dunstify -a "AutoDelete Reminder" \
                  -A "review_action,Review Files" \
                  "AutoDelete Reminder" \
                  "You have \$(/usr/bin/java -jar "$INSTALL_DIR/$JAR_NAME" due-count) files scheduled for autodeletion today")

# If user clicked to review, launch GUI
if [ "\$ACTION" = "review_action" ]; then
    /usr/bin/java -jar "$INSTALL_DIR/$JAR_NAME" review
fi

EOF
    chmod +x "$LAUNCHER_PATH"
    echo "✅ GUI launcher script created."

    # Step 6: Setup systemd user timer to use the launcher script
    mkdir -p "$HOME/.config/systemd/user"

    cat > "$HOME/.config/systemd/user/autodelete.service" <<EOF
[Unit]
Description=AutoDelete Reminder Launcher

[Service]
Type=oneshot
#Environment=DISPLAY=\${DISPLAY}
Environment=XAUTHORITY=%h/.Xauthority
ExecStart=$LAUNCHER_PATH
EOF

    cat > "$HOME/.config/systemd/user/autodelete.timer" <<EOF
[Unit]
Description=Run AutoDelete Reminder every hour

[Timer]
OnBootSec=5min
OnUnitActiveSec=1h
Persistent=true
WakeSystem=true

[Install]
WantedBy=timers.target
EOF

    systemctl --user daemon-reload
    systemctl --user enable --now autodelete.timer

    systemctl --user restart autodelete

    echo "✅ systemd timer installed (AutoDelete will check every hour)"

    echo "🎉 AutoDelete installation complete!"
    echo ""
    read -n 1 -s -r -p "Press any key to exit..."
    echo ""
}

# Uninstall function
uninstall_autodelete() {
    echo "🗑️ Uninstalling AutoDelete..."
    echo "Stopping and disabling systemd timer..."
    systemctl --user stop autodelete.timer >/dev/null 2>&1 || true
    systemctl --user disable autodelete.timer >/dev/null 2>&1 || true
    systemctl --user daemon-reload

    rm -f "$INSTALL_DIR/$JAR_NAME"
    rm -f "$INSTALL_DIR/schedule_wrapper.sh"
    rm -f "$INSTALL_DIR/cancel_schedule_wrapper.sh"
    rm -f "$INSTALL_DIR/autodelete_gui_launcher.sh" # Remove the new launcher
    rm -f "$DESKTOP_MENU_DIR/AutoDelete.desktop"
    rm -f "$NAUTILUS_SCRIPT_DIR/ScheduleAutoDelete"
    rm -f "$HOME/.config/systemd/user/autodelete.timer"
    rm -f "$HOME/.config/systemd/user/autodelete.service"

    # Remove directory if empty
    rmdir "$INSTALL_DIR" 2>/dev/null || true

    echo "✅ AutoDelete uninstalled"
}

# Interactive menu function
show_menu() {
    # Check if whiptail is available for better UI
    if command -v whiptail >/dev/null 2>&1; then
        show_whiptail_menu
    else
        show_select_menu
    fi
}

# Whiptail-based menu (arrow key navigation)
show_whiptail_menu() {
    while true; do
        CHOICE=$(whiptail --title "AutoDelete Installation Script" \
            --menu "Choose an option:" 15 60 4 \
            "1" "Install AutoDelete" \
            "2" "Uninstall AutoDelete" \
            "3" "Exit" 3>&1 1>&2 2>&3)

        # Check if user pressed Cancel or ESC
        # shellcheck disable=SC2181
        if [ $? -ne 0 ]; then
            echo "Operation cancelled."
            exit 0
        fi

        case $CHOICE in
            1)
                install_autodelete
                break
                ;;
            2)
                uninstall_autodelete
                break
                ;;
            3)
                echo "Goodbye!"
                exit 0
                ;;
        esac
    done
}

# Fallback select-based menu
show_select_menu() {
    echo "AutoDelete Installation Script"
    echo "=============================="
    echo ""
    PS3="Please select an option (use numbers): "

    options=("Install AutoDelete" "Uninstall AutoDelete" "Exit")

    select opt in "${options[@]}"; do
        case $opt in
            "Install AutoDelete")
                install_autodelete
                break
                ;;
            "Uninstall AutoDelete")
                uninstall_autodelete
                break
                ;;
            "Exit")
                echo "Goodbye!"
                exit 0
                ;;
            *)
                echo "Invalid option. Please try again."
                ;;
        esac
    done
}

# Main script logic
if [ $# -eq 0 ]; then
    # No arguments - show interactive menu
    show_menu
else
    # Arguments provided - use command line mode
    case "$1" in
        install)
            install_autodelete
            ;;
        uninstall)
            uninstall_autodelete
            ;;
        *)
            echo "Usage: $0 [install|uninstall]"
            echo "Or run without arguments for interactive menu"
            exit 1
            ;;
    esac
fi