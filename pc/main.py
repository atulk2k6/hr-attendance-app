#!/usr/bin/env python3
"""Attendance Manager PC Application"""

import sys
import os

# Add the project root to Python path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PyQt6.QtWidgets import QApplication
from PyQt6.QtGui import QIcon
from src.ui.main_window import MainWindow


def main():
    app = QApplication(sys.argv)
    app.setApplicationName("Attendance Manager")
    app.setOrganizationName("AttendanceApp")

    # Apply stylesheet
    style_path = os.path.join(os.path.dirname(__file__), "resources", "styles", "light.qss")
    if os.path.exists(style_path):
        with open(style_path, "r") as f:
            app.setStyleSheet(f.read())

    window = MainWindow()
    window.show()

    sys.exit(app.exec())


if __name__ == "__main__":
    main()
