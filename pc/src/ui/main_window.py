import calendar
import os
from datetime import datetime
from typing import Optional

from PyQt6.QtCore import Qt
from PyQt6.QtGui import QAction
from PyQt6.QtWidgets import (
    QComboBox,
    QFileDialog,
    QLabel,
    QMainWindow,
    QMessageBox,
    QSpinBox,
    QTabWidget,
    QToolBar,
    QWidget,
)

from src.database.connection import DatabaseManager
from src.ui.tabs.employee_tab import EmployeeTab
from src.ui.tabs.attendance_tab import AttendanceTab
from src.ui.tabs.department_tab import DepartmentTab
from src.ui.tabs.category_tab import CategoryTab


class MainWindow(QMainWindow):
    """Primary application window for Attendance Manager."""

    def __init__(self) -> None:
        super().__init__()
        self._db = DatabaseManager()
        self._init_ui()

    # ------------------------------------------------------------------
    # UI construction
    # ------------------------------------------------------------------

    def _init_ui(self) -> None:
        self.setWindowTitle("Attendance Manager")
        self.resize(1200, 800)

        # --- Menu bar ---
        menubar = self.menuBar()

        file_menu = menubar.addMenu("File")
        import_action = QAction("Import DB", self)
        import_action.triggered.connect(self.import_database)
        file_menu.addAction(import_action)

        export_action = QAction("Export Excel", self)
        export_action.triggered.connect(self.export_excel)
        file_menu.addAction(export_action)

        file_menu.addSeparator()
        exit_action = QAction("Exit", self)
        exit_action.triggered.connect(self.close)
        file_menu.addAction(exit_action)

        edit_menu = menubar.addMenu("Edit")
        settings_action = QAction("Settings", self)
        settings_action.triggered.connect(self._open_settings)
        edit_menu.addAction(settings_action)

        help_menu = menubar.addMenu("Help")
        about_action = QAction("About", self)
        about_action.triggered.connect(self._show_about)
        help_menu.addAction(about_action)

        # --- Toolbar ---
        toolbar = QToolBar("Main Toolbar")
        toolbar.setMovable(False)
        self.addToolBar(toolbar)

        tb_import = QAction("Import DB", self)
        tb_import.triggered.connect(self.import_database)
        toolbar.addAction(tb_import)

        tb_export = QAction("Export Excel", self)
        tb_export.triggered.connect(self.export_excel)
        toolbar.addAction(tb_export)

        toolbar.addSeparator()

        toolbar.addWidget(QLabel(" Month: "))
        self._month_combo = QComboBox()
        month_names = [calendar.month_name[m] for m in range(1, 13)]
        self._month_combo.addItems(month_names)
        self._month_combo.setCurrentIndex(datetime.now().month - 1)
        toolbar.addWidget(self._month_combo)

        toolbar.addWidget(QLabel(" Year: "))
        self._year_spin = QSpinBox()
        self._year_spin.setRange(2000, 2100)
        self._year_spin.setValue(datetime.now().year)
        toolbar.addWidget(self._year_spin)

        # --- Central tab widget ---
        self._tabs = QTabWidget()
        self.setCentralWidget(self._tabs)

        self._employee_tab = EmployeeTab()
        self._attendance_tab = AttendanceTab()
        self._department_tab = DepartmentTab()
        self._category_tab = CategoryTab()

        self._tabs.addTab(self._employee_tab, "Employees")
        self._tabs.addTab(self._attendance_tab, "Attendance")
        self._tabs.addTab(self._department_tab, "Departments")
        self._tabs.addTab(self._category_tab, "Categories")

        # --- Status bar ---
        self._status_label = QLabel("No database loaded")
        self.statusBar().addPermanentWidget(self._status_label)

    # ------------------------------------------------------------------
    # Public methods
    # ------------------------------------------------------------------

    def import_database(self) -> None:
        """Open a file dialog, load the selected .db file, and refresh all tabs."""
        path, _ = QFileDialog.getOpenFileName(
            self,
            "Open Attendance Database",
            "",
            "SQLite Database (*.db *.sqlite *.sqlite3);;All Files (*)",
        )
        if not path:
            return
        try:
            self._db.load_database(path)
            self.refresh_all_tabs()
            self.update_status_bar()
        except Exception as exc:
            QMessageBox.critical(
                self, "Database Error", f"Failed to load database:\n{exc}"
            )

    def export_excel(self) -> None:
        """Open the ExportDialog for Excel export."""
        if not self._db.is_loaded():
            QMessageBox.warning(self, "No Database", "Please load a database first.")
            return
        from src.ui.dialogs.export_dialog import ExportDialog

        month = self._month_combo.currentIndex() + 1
        year = self._year_spin.value()
        dialog = ExportDialog(self._db, month, year, parent=self)
        dialog.exec()

    def refresh_all_tabs(self) -> None:
        """Reload data in every tab."""
        if not self._db.is_loaded():
            return
        self._employee_tab.refresh(self._db)
        self._attendance_tab.refresh(self._db)
        self._department_tab.refresh(self._db)
        self._category_tab.refresh(self._db)

    def update_status_bar(self) -> None:
        """Display the loaded DB path and record counts."""
        if not self._db.is_loaded():
            self._status_label.setText("No database loaded")
            return
        try:
            path = self._db.get_path()
            conn = self._db.get_connection()
            emp_count = conn.execute(
                "SELECT COUNT(*) FROM employees"
            ).fetchone()[0]
            att_count = conn.execute(
                "SELECT COUNT(*) FROM attendance"
            ).fetchone()[0]
            filename = os.path.basename(path)
            self._status_label.setText(
                f"DB: {filename}  |  Employees: {emp_count}  |  Attendance records: {att_count}"
            )
        except Exception:
            self._status_label.setText(f"DB: {self._db.get_path()}")

    # ------------------------------------------------------------------
    # Private helpers
    # ------------------------------------------------------------------

    def _open_settings(self) -> None:
        QMessageBox.information(self, "Settings", "Settings dialog not yet implemented.")

    def _show_about(self) -> None:
        QMessageBox.about(
            self,
            "About Attendance Manager",
            "Attendance Manager v1.0\n\n"
            "A desktop application for managing employee\n"
            "attendance records and generating reports.",
        )

    def closeEvent(self, event) -> None:
        self._db.close()
        super().closeEvent(event)
