import calendar
from datetime import datetime
from typing import Dict, List, Optional

from PyQt6.QtCore import Qt
from PyQt6.QtGui import QBrush, QColor, QStandardItem, QStandardItemModel
from PyQt6.QtWidgets import (
    QComboBox,
    QHBoxLayout,
    QHeaderView,
    QLabel,
    QMessageBox,
    QSpinBox,
    QTableView,
    QVBoxLayout,
    QWidget,
)

from src.database.connection import DatabaseManager
from src.database.models import AttendanceRecord, Employee
from src.database.queries import (
    get_all_employees,
    get_attendance_for_month,
    save_attendance_record,
)
from src.util.date_utils import get_day_of_week, get_days_in_month

# Status code -> background colour mapping
STATUS_COLORS: Dict[str, QColor] = {
    "P": QColor(200, 255, 200),   # green
    "A": QColor(255, 200, 200),   # red
    "W": QColor(200, 200, 255),   # blue
    "CO": QColor(255, 220, 180),  # orange
}


class AttendanceTab(QWidget):
    """Tab displaying a month-view attendance grid."""

    def __init__(self, parent: Optional[QWidget] = None):
        super().__init__(parent)
        self._db: Optional[DatabaseManager] = None
        self._employees: List[Employee] = []
        self._init_ui()

    # ------------------------------------------------------------------
    # UI setup
    # ------------------------------------------------------------------

    def _init_ui(self) -> None:
        layout = QVBoxLayout(self)

        # Month/Year selector
        selector_row = QHBoxLayout()
        selector_row.addWidget(QLabel("Month:"))
        self._month_combo = QComboBox()
        month_names = [calendar.month_name[m] for m in range(1, 13)]
        self._month_combo.addItems(month_names)
        self._month_combo.setCurrentIndex(datetime.now().month - 1)
        self._month_combo.currentIndexChanged.connect(self._on_period_changed)
        selector_row.addWidget(self._month_combo)

        selector_row.addWidget(QLabel("Year:"))
        self._year_spin = QSpinBox()
        self._year_spin.setRange(2000, 2100)
        self._year_spin.setValue(datetime.now().year)
        self._year_spin.valueChanged.connect(self._on_period_changed)
        selector_row.addWidget(self._year_spin)

        selector_row.addStretch()
        layout.addLayout(selector_row)

        # Table model + view
        self._model = QStandardItemModel()
        self._table = QTableView()
        self._table.setModel(self._model)
        self._table.setEditTriggers(QTableView.EditTrigger.NoEditTriggers)
        self._table.setSelectionMode(QTableView.SelectionMode.SingleSelection)
        self._table.verticalHeader().setVisible(False)
        self._table.doubleClicked.connect(self._on_double_click)
        layout.addWidget(self._table)

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def refresh(self, db_manager: DatabaseManager) -> None:
        self._db = db_manager
        self._load_grid()

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _selected_month(self) -> int:
        return self._month_combo.currentIndex() + 1

    def _selected_year(self) -> int:
        return self._year_spin.value()

    def _on_period_changed(self) -> None:
        if self._db and self._db.is_loaded():
            self._load_grid()

    def _load_grid(self) -> None:
        if self._db is None or not self._db.is_loaded():
            return

        year = self._selected_year()
        month = self._selected_month()
        days = get_days_in_month(year, month)

        self._employees = get_all_employees(self._db, active_only=True)
        records = get_attendance_for_month(self._db, year, month)

        # Build lookup: (employee_id, day) -> AttendanceRecord
        att_map: Dict[tuple, AttendanceRecord] = {}
        for rec in records:
            day_num = int(rec.date.split("-")[2])
            att_map[(rec.employee_id, day_num)] = rec

        # Build header labels
        headers = ["Employee Name"]
        for d in range(1, days + 1):
            dow = get_day_of_week(year, month, d)
            headers.append(f"{d}\n{dow}")

        self._model.clear()
        self._model.setHorizontalHeaderLabels(headers)

        for emp in self._employees:
            row_items: list[QStandardItem] = []
            name_item = QStandardItem(emp.name)
            name_item.setData(emp.id, Qt.ItemDataRole.UserRole)
            row_items.append(name_item)

            for d in range(1, days + 1):
                rec = att_map.get((emp.id, d))
                status = rec.status if rec else ""
                item = QStandardItem(status)
                item.setTextAlignment(Qt.AlignmentFlag.AlignCenter)
                # Store the day number for double-click editing
                item.setData(d, Qt.ItemDataRole.UserRole)
                # Color coding
                if status in STATUS_COLORS:
                    item.setBackground(QBrush(STATUS_COLORS[status]))
                row_items.append(item)

            self._model.appendRow(row_items)

        self._table.resizeColumnsToContents()
        # Make the name column a bit wider
        self._table.setColumnWidth(0, 180)

    # ------------------------------------------------------------------
    # Editing
    # ------------------------------------------------------------------

    def _on_double_click(self, index) -> None:
        if self._db is None:
            return
        col = index.column()
        if col == 0:
            return  # clicked the name column

        row = index.row()
        name_item = self._model.item(row, 0)
        emp_id = name_item.data(Qt.ItemDataRole.UserRole)
        day_item = self._model.item(row, col)
        day_num = day_item.data(Qt.ItemDataRole.UserRole)

        if emp_id is None or day_num is None:
            return

        year = self._selected_year()
        month = self._selected_month()
        date_str = f"{year:04d}-{month:02d}-{day_num:02d}"

        from src.ui.dialogs.attendance_edit_dialog import AttendanceEditDialog

        dialog = AttendanceEditDialog(
            self._db, emp_id, date_str, parent=self
        )
        if dialog.exec():
            self._load_grid()
