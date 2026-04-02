import os
from datetime import datetime

from PyQt6.QtCore import Qt
from PyQt6.QtWidgets import (
    QComboBox,
    QDialog,
    QFileDialog,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QListWidget,
    QListWidgetItem,
    QMessageBox,
    QPushButton,
    QSpinBox,
    QVBoxLayout,
)

from src.database.connection import DatabaseManager
from src.database.queries import get_settings
from src.export.column_config import ColumnDefinition, get_default_columns
from src.export.excel_exporter import ExcelExporter


class ExportDialog(QDialog):
    """Dialog for configuring and executing an Excel attendance export."""

    def __init__(self, db_manager: DatabaseManager, parent=None):
        super().__init__(parent)
        self.db_manager = db_manager

        self.setWindowTitle("Export Attendance Report")
        self.setMinimumWidth(500)
        self.setMinimumHeight(520)

        self._build_ui()

    def _build_ui(self):
        layout = QVBoxLayout(self)

        # --- Month / Year selectors ---
        date_layout = QHBoxLayout()

        date_layout.addWidget(QLabel("Month:"))
        self.month_combo = QComboBox()
        months = [
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December",
        ]
        for i, name in enumerate(months, start=1):
            self.month_combo.addItem(name, i)
        now = datetime.now()
        self.month_combo.setCurrentIndex(now.month - 1)
        date_layout.addWidget(self.month_combo)

        date_layout.addWidget(QLabel("Year:"))
        self.year_spin = QSpinBox()
        self.year_spin.setRange(2000, 2100)
        self.year_spin.setValue(now.year)
        date_layout.addWidget(self.year_spin)

        layout.addLayout(date_layout)

        # --- Unit Number ---
        unit_layout = QHBoxLayout()
        unit_layout.addWidget(QLabel("Unit Number:"))
        self.unit_edit = QLineEdit()
        settings = get_settings(self.db_manager)
        self.unit_edit.setText(settings.get("unit_number", ""))
        unit_layout.addWidget(self.unit_edit)
        layout.addLayout(unit_layout)

        # --- Column configuration ---
        col_group = QGroupBox("Column Configuration")
        col_layout = QHBoxLayout()

        self.column_list = QListWidget()
        self._default_columns = get_default_columns()
        for col in self._default_columns:
            item = QListWidgetItem(col.label)
            item.setFlags(item.flags() | Qt.ItemFlag.ItemIsUserCheckable)
            item.setCheckState(Qt.CheckState.Checked)
            item.setData(Qt.ItemDataRole.UserRole, col.key)
            self.column_list.addItem(item)
        col_layout.addWidget(self.column_list)

        # Up / Down buttons
        btn_layout = QVBoxLayout()
        btn_layout.addStretch()
        self.up_btn = QPushButton("Up")
        self.up_btn.clicked.connect(self._move_up)
        btn_layout.addWidget(self.up_btn)
        self.down_btn = QPushButton("Down")
        self.down_btn.clicked.connect(self._move_down)
        btn_layout.addWidget(self.down_btn)
        btn_layout.addStretch()
        col_layout.addLayout(btn_layout)

        col_group.setLayout(col_layout)
        layout.addWidget(col_group)

        # --- Output file path ---
        path_layout = QHBoxLayout()
        path_layout.addWidget(QLabel("Output File:"))
        self.path_edit = QLineEdit()
        default_name = f"Attendance_{now.year}_{now.month:02d}.xlsx"
        self.path_edit.setText(os.path.join(os.path.expanduser("~"), "Desktop", default_name))
        path_layout.addWidget(self.path_edit)

        self.browse_btn = QPushButton("Browse...")
        self.browse_btn.clicked.connect(self._browse_output)
        path_layout.addWidget(self.browse_btn)
        layout.addLayout(path_layout)

        # --- Export button ---
        self.export_btn = QPushButton("Export")
        self.export_btn.setDefault(True)
        self.export_btn.clicked.connect(self._on_export)
        layout.addWidget(self.export_btn)

    # ---- Column reorder helpers ----

    def _move_up(self):
        row = self.column_list.currentRow()
        if row <= 0:
            return
        item = self.column_list.takeItem(row)
        self.column_list.insertItem(row - 1, item)
        self.column_list.setCurrentRow(row - 1)

    def _move_down(self):
        row = self.column_list.currentRow()
        if row < 0 or row >= self.column_list.count() - 1:
            return
        item = self.column_list.takeItem(row)
        self.column_list.insertItem(row + 1, item)
        self.column_list.setCurrentRow(row + 1)

    # ---- Browse ----

    def _browse_output(self):
        path, _ = QFileDialog.getSaveFileName(
            self, "Save As", self.path_edit.text(), "Excel Files (*.xlsx)"
        )
        if path:
            if not path.endswith(".xlsx"):
                path += ".xlsx"
            self.path_edit.setText(path)

    # ---- Gather visible columns in order ----

    def _get_selected_columns(self) -> list[ColumnDefinition]:
        col_map = {c.key: c for c in self._default_columns}
        selected = []
        for i in range(self.column_list.count()):
            item = self.column_list.item(i)
            if item.checkState() == Qt.CheckState.Checked:
                key = item.data(Qt.ItemDataRole.UserRole)
                if key in col_map:
                    col = col_map[key]
                    col.order = i
                    col.visible = True
                    selected.append(col)
        return selected

    # ---- Export ----

    def _on_export(self):
        output_path = self.path_edit.text().strip()
        if not output_path:
            QMessageBox.warning(self, "Validation Error", "Please specify an output file path.")
            return

        columns = self._get_selected_columns()
        if not columns:
            QMessageBox.warning(self, "Validation Error", "Please select at least one column.")
            return

        year = self.year_spin.value()
        month = self.month_combo.currentData()
        unit_number = self.unit_edit.text().strip()

        try:
            exporter = ExcelExporter(self.db_manager)
            exporter.export(year, month, unit_number, columns, output_path)
            QMessageBox.information(self, "Success", f"Report exported to:\n{output_path}")
            self.accept()
        except Exception as e:
            QMessageBox.critical(self, "Export Error", f"Failed to export report:\n{e}")
