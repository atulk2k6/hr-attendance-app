from PyQt6.QtCore import QDate, Qt
from PyQt6.QtWidgets import (
    QComboBox,
    QDateEdit,
    QDialog,
    QDialogButtonBox,
    QDoubleSpinBox,
    QFormLayout,
    QLineEdit,
    QMessageBox,
    QVBoxLayout,
)

from src.database.connection import DatabaseManager
from src.database.models import Employee
from src.database.queries import get_all_categories, get_all_departments, save_employee

WEEKLY_OFF_DAYS = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
WEEKLY_OFF_CODES = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"]


class EmployeeEditDialog(QDialog):
    """Dialog for adding or editing an employee."""

    def __init__(self, db_manager: DatabaseManager, employee: Employee = None, parent=None):
        super().__init__(parent)
        self.db_manager = db_manager
        self.employee = employee
        self._departments = []
        self._categories = []

        self.setWindowTitle("Edit Employee" if employee else "Add Employee")
        self.setMinimumWidth(420)

        self._build_ui()

        if employee:
            self._populate_from_employee(employee)

    def _build_ui(self):
        layout = QVBoxLayout(self)
        form = QFormLayout()

        # Code
        self.code_edit = QLineEdit()
        form.addRow("Code:", self.code_edit)

        # Employee ID
        self.emp_id_edit = QLineEdit()
        form.addRow("Employee ID:", self.emp_id_edit)

        # Name
        self.name_edit = QLineEdit()
        form.addRow("Name:", self.name_edit)

        # Father's Name
        self.father_name_edit = QLineEdit()
        form.addRow("Father's Name:", self.father_name_edit)

        # Department
        self.department_combo = QComboBox()
        self._load_departments()
        form.addRow("Department:", self.department_combo)

        # Category
        self.category_combo = QComboBox()
        self._load_categories()
        form.addRow("Category:", self.category_combo)

        # DOB
        self.dob_edit = QDateEdit()
        self.dob_edit.setCalendarPopup(True)
        self.dob_edit.setDisplayFormat("dd-MM-yyyy")
        self.dob_edit.setDate(QDate(2000, 1, 1))
        form.addRow("DOB:", self.dob_edit)

        # DOJ
        self.doj_edit = QDateEdit()
        self.doj_edit.setCalendarPopup(True)
        self.doj_edit.setDisplayFormat("dd-MM-yyyy")
        self.doj_edit.setDate(QDate.currentDate())
        form.addRow("DOJ:", self.doj_edit)

        # Weekly Off Day
        self.weekly_off_combo = QComboBox()
        self.weekly_off_combo.addItems(WEEKLY_OFF_DAYS)
        form.addRow("Weekly Off Day:", self.weekly_off_combo)

        # FD
        self.fd_combo = QComboBox()
        self.fd_combo.addItems(["", "D"])
        form.addRow("FD:", self.fd_combo)

        # OT/RR Type
        self.ot_rr_type_combo = QComboBox()
        self.ot_rr_type_combo.addItems(["COMP", "RR"])
        form.addRow("OT/RR Type:", self.ot_rr_type_combo)

        # OT/RR Value
        self.ot_rr_value_spin = QDoubleSpinBox()
        self.ot_rr_value_spin.setRange(0, 999999)
        self.ot_rr_value_spin.setDecimals(2)
        form.addRow("OT/RR Value:", self.ot_rr_value_spin)

        # Gross Salary
        self.gross_salary_spin = QDoubleSpinBox()
        self.gross_salary_spin.setRange(0, 9999999)
        self.gross_salary_spin.setDecimals(2)
        form.addRow("Gross Salary:", self.gross_salary_spin)

        layout.addLayout(form)

        # OK / Cancel buttons
        button_box = QDialogButtonBox(QDialogButtonBox.StandardButton.Ok | QDialogButtonBox.StandardButton.Cancel)
        button_box.accepted.connect(self._on_accept)
        button_box.rejected.connect(self.reject)
        layout.addWidget(button_box)

    def _load_departments(self):
        self._departments = get_all_departments(self.db_manager)
        self.department_combo.clear()
        self.department_combo.addItem("-- Select --", None)
        for dept in self._departments:
            self.department_combo.addItem(dept.name, dept.id)

    def _load_categories(self):
        self._categories = get_all_categories(self.db_manager)
        self.category_combo.clear()
        self.category_combo.addItem("-- Select --", None)
        for cat in self._categories:
            self.category_combo.addItem(cat.name, cat.id)

    def _populate_from_employee(self, emp: Employee):
        self.code_edit.setText(emp.code or "")
        self.emp_id_edit.setText(emp.emp_id or "")
        self.name_edit.setText(emp.name or "")
        self.father_name_edit.setText(emp.father_name or "")

        # Department
        if emp.department_id is not None:
            idx = self.department_combo.findData(emp.department_id)
            if idx >= 0:
                self.department_combo.setCurrentIndex(idx)

        # Category
        if emp.category_id is not None:
            idx = self.category_combo.findData(emp.category_id)
            if idx >= 0:
                self.category_combo.setCurrentIndex(idx)

        # DOB
        if emp.dob:
            qdate = QDate.fromString(emp.dob, "yyyy-MM-dd")
            if qdate.isValid():
                self.dob_edit.setDate(qdate)

        # DOJ
        if emp.doj:
            qdate = QDate.fromString(emp.doj, "yyyy-MM-dd")
            if qdate.isValid():
                self.doj_edit.setDate(qdate)

        # Weekly off day
        off_code = (emp.weekly_off_day or "SUN").upper()
        if off_code in WEEKLY_OFF_CODES:
            self.weekly_off_combo.setCurrentIndex(WEEKLY_OFF_CODES.index(off_code))

        # FD
        idx = self.fd_combo.findText(emp.fd or "")
        if idx >= 0:
            self.fd_combo.setCurrentIndex(idx)

        # OT/RR
        idx = self.ot_rr_type_combo.findText(emp.ot_rr_type or "COMP")
        if idx >= 0:
            self.ot_rr_type_combo.setCurrentIndex(idx)

        self.ot_rr_value_spin.setValue(emp.ot_rr_value or 0.0)
        self.gross_salary_spin.setValue(emp.gross_salary or 0.0)

    def get_employee_data(self) -> dict:
        """Return all field values as a dictionary."""
        dept_id = self.department_combo.currentData()
        cat_id = self.category_combo.currentData()
        off_index = self.weekly_off_combo.currentIndex()

        return {
            "code": self.code_edit.text().strip(),
            "emp_id": self.emp_id_edit.text().strip(),
            "name": self.name_edit.text().strip(),
            "father_name": self.father_name_edit.text().strip(),
            "department_id": dept_id,
            "category_id": cat_id,
            "dob": self.dob_edit.date().toString("yyyy-MM-dd"),
            "doj": self.doj_edit.date().toString("yyyy-MM-dd"),
            "weekly_off_day": WEEKLY_OFF_CODES[off_index],
            "fd": self.fd_combo.currentText(),
            "ot_rr_type": self.ot_rr_type_combo.currentText(),
            "ot_rr_value": self.ot_rr_value_spin.value(),
            "gross_salary": self.gross_salary_spin.value(),
        }

    def _on_accept(self):
        data = self.get_employee_data()

        if not data["name"]:
            QMessageBox.warning(self, "Validation Error", "Employee name is required.")
            return

        emp = Employee(
            id=self.employee.id if self.employee else None,
            code=data["code"],
            emp_id=data["emp_id"],
            name=data["name"],
            father_name=data["father_name"],
            department_id=data["department_id"],
            category_id=data["category_id"],
            dob=data["dob"],
            doj=data["doj"],
            weekly_off_day=data["weekly_off_day"],
            fd=data["fd"],
            ot_rr_type=data["ot_rr_type"],
            ot_rr_value=data["ot_rr_value"],
            gross_salary=data["gross_salary"],
            is_active=self.employee.is_active if self.employee else True,
        )

        try:
            save_employee(self.db_manager, emp)
            self.accept()
        except Exception as e:
            QMessageBox.critical(self, "Error", f"Failed to save employee:\n{e}")
