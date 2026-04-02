import os
import sys
from typing import Optional

from PyQt6.QtCore import Qt, QSortFilterProxyModel
from PyQt6.QtGui import QAction, QStandardItem, QStandardItemModel
from PyQt6.QtWidgets import (
    QHBoxLayout,
    QHeaderView,
    QLineEdit,
    QMenu,
    QMessageBox,
    QPushButton,
    QTableView,
    QVBoxLayout,
    QWidget,
)

from src.database.connection import DatabaseManager
from src.database.models import Employee
from src.database.queries import get_all_employees, save_employee


EMPLOYEE_COLUMNS = [
    "S.R.", "Code", "Emp ID", "Name", "Father Name", "Department",
    "Category", "DOB", "DOJ", "Weekly Off", "FD", "OT/RR", "OT Value",
    "Gross Salary",
]


class EmployeeTab(QWidget):
    """Tab displaying a searchable, editable employee table."""

    def __init__(self, parent: Optional[QWidget] = None):
        super().__init__(parent)
        self._db: Optional[DatabaseManager] = None
        self._employees: list[Employee] = []
        self._init_ui()

    # ------------------------------------------------------------------
    # UI setup
    # ------------------------------------------------------------------

    def _init_ui(self) -> None:
        layout = QVBoxLayout(self)

        # Top toolbar row
        toolbar = QHBoxLayout()
        self._search_input = QLineEdit()
        self._search_input.setPlaceholderText("Search by name, code, or emp ID...")
        self._search_input.textChanged.connect(self._apply_filter)
        toolbar.addWidget(self._search_input, 1)

        self._btn_add = QPushButton("Add Employee")
        self._btn_add.clicked.connect(self._add_employee)
        toolbar.addWidget(self._btn_add)

        layout.addLayout(toolbar)

        # Table model
        self._model = QStandardItemModel()
        self._model.setHorizontalHeaderLabels(EMPLOYEE_COLUMNS)

        # Proxy for filtering
        self._proxy = QSortFilterProxyModel()
        self._proxy.setSourceModel(self._model)
        self._proxy.setFilterCaseSensitivity(Qt.CaseSensitivity.CaseInsensitive)
        self._proxy.setFilterKeyColumn(-1)  # search all columns

        # Table view
        self._table = QTableView()
        self._table.setModel(self._proxy)
        self._table.setSelectionBehavior(QTableView.SelectionBehavior.SelectRows)
        self._table.setEditTriggers(QTableView.EditTrigger.NoEditTriggers)
        self._table.setSortingEnabled(True)
        self._table.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        self._table.customContextMenuRequested.connect(self._context_menu)
        self._table.doubleClicked.connect(self._on_double_click)
        self._table.horizontalHeader().setStretchLastSection(True)
        self._table.verticalHeader().setVisible(False)
        layout.addWidget(self._table)

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def refresh(self, db_manager: DatabaseManager) -> None:
        """Reload employee data from the database."""
        self._db = db_manager
        self._employees = get_all_employees(db_manager, active_only=False)
        self._populate_table()

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _populate_table(self) -> None:
        self._model.removeRows(0, self._model.rowCount())
        for idx, emp in enumerate(self._employees, start=1):
            row = [
                QStandardItem(str(idx)),
                QStandardItem(emp.code),
                QStandardItem(emp.emp_id),
                QStandardItem(emp.name),
                QStandardItem(emp.father_name),
                QStandardItem(emp.department_name or ""),
                QStandardItem(emp.category_name or ""),
                QStandardItem(emp.dob or ""),
                QStandardItem(emp.doj or ""),
                QStandardItem(emp.weekly_off_day),
                QStandardItem(emp.fd),
                QStandardItem(emp.ot_rr_type),
                QStandardItem(str(emp.ot_rr_value)),
                QStandardItem(str(emp.gross_salary)),
            ]
            # Store the employee id in the first column's data
            row[0].setData(emp.id, Qt.ItemDataRole.UserRole)
            # Grey out inactive employees
            if not emp.is_active:
                for item in row:
                    item.setForeground(Qt.GlobalColor.gray)
            self._model.appendRow(row)
        self._table.resizeColumnsToContents()

    def _apply_filter(self, text: str) -> None:
        self._proxy.setFilterFixedString(text)

    def _selected_employee(self) -> Optional[Employee]:
        """Return the Employee object for the currently selected row."""
        indexes = self._table.selectionModel().selectedRows()
        if not indexes:
            return None
        source_index = self._proxy.mapToSource(indexes[0])
        row_num = source_index.row()
        item = self._model.item(row_num, 0)
        emp_id = item.data(Qt.ItemDataRole.UserRole)
        for emp in self._employees:
            if emp.id == emp_id:
                return emp
        return None

    # ------------------------------------------------------------------
    # Actions
    # ------------------------------------------------------------------

    def _add_employee(self) -> None:
        if self._db is None:
            QMessageBox.warning(self, "No Database", "Please load a database first.")
            return
        # Import here to avoid circular imports at module level
        from src.ui.dialogs.employee_edit_dialog import EmployeeEditDialog

        dialog = EmployeeEditDialog(self._db, parent=self)
        if dialog.exec():
            self.refresh(self._db)

    def _edit_employee(self, employee: Employee) -> None:
        if self._db is None:
            return
        from src.ui.dialogs.employee_edit_dialog import EmployeeEditDialog

        dialog = EmployeeEditDialog(self._db, employee=employee, parent=self)
        if dialog.exec():
            self.refresh(self._db)

    def _deactivate_employee(self, employee: Employee) -> None:
        if self._db is None:
            return
        action = "Deactivate" if employee.is_active else "Reactivate"
        reply = QMessageBox.question(
            self,
            f"{action} Employee",
            f"Are you sure you want to {action.lower()} '{employee.name}'?",
            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
        )
        if reply == QMessageBox.StandardButton.Yes:
            employee.is_active = not employee.is_active
            save_employee(self._db, employee)
            self.refresh(self._db)

    # ------------------------------------------------------------------
    # Event handlers
    # ------------------------------------------------------------------

    def _on_double_click(self, index) -> None:
        emp = self._selected_employee()
        if emp:
            self._edit_employee(emp)

    def _context_menu(self, position) -> None:
        emp = self._selected_employee()
        if emp is None:
            return
        menu = QMenu(self)
        edit_action = QAction("Edit", self)
        edit_action.triggered.connect(lambda: self._edit_employee(emp))
        menu.addAction(edit_action)

        toggle_text = "Deactivate" if emp.is_active else "Reactivate"
        deactivate_action = QAction(toggle_text, self)
        deactivate_action.triggered.connect(lambda: self._deactivate_employee(emp))
        menu.addAction(deactivate_action)

        menu.exec(self._table.viewport().mapToGlobal(position))
