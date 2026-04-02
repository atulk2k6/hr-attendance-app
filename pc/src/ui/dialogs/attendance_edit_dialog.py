from PyQt6.QtCore import QTime, Qt
from PyQt6.QtWidgets import (
    QDialog,
    QDialogButtonBox,
    QDoubleSpinBox,
    QComboBox,
    QFormLayout,
    QLineEdit,
    QMessageBox,
    QTimeEdit,
    QVBoxLayout,
)

from src.database.connection import DatabaseManager
from src.database.models import AttendanceRecord
from src.database.queries import get_settings, save_attendance_record

STATUS_OPTIONS = ["P", "A", "W", "CO"]
DEFAULT_NORMAL_HOURS = 8.0


class AttendanceEditDialog(QDialog):
    """Dialog for editing a single attendance record."""

    def __init__(
        self,
        db_manager: DatabaseManager,
        employee_id: int,
        employee_name: str,
        date_str: str,
        existing_record: AttendanceRecord = None,
        parent=None,
    ):
        super().__init__(parent)
        self.db_manager = db_manager
        self.employee_id = employee_id
        self.employee_name = employee_name
        self.date_str = date_str
        self.existing_record = existing_record

        # Load normal work hours from settings
        settings = get_settings(db_manager)
        self.normal_hours = float(settings.get("normal_work_hours", DEFAULT_NORMAL_HOURS))

        self.setWindowTitle(f"Edit Attendance - {employee_name} - {date_str}")
        self.setMinimumWidth(360)

        self._build_ui()

        if existing_record:
            self._populate_from_record(existing_record)

    def _build_ui(self):
        layout = QVBoxLayout(self)
        form = QFormLayout()

        # In Time
        self.in_time_edit = QTimeEdit()
        self.in_time_edit.setDisplayFormat("HH:mm")
        self.in_time_edit.setTime(QTime(9, 0))
        self.in_time_edit.timeChanged.connect(self._recalculate_hours)
        form.addRow("In Time:", self.in_time_edit)

        # Out Time
        self.out_time_edit = QTimeEdit()
        self.out_time_edit.setDisplayFormat("HH:mm")
        self.out_time_edit.setTime(QTime(18, 0))
        self.out_time_edit.timeChanged.connect(self._recalculate_hours)
        form.addRow("Out Time:", self.out_time_edit)

        # Status
        self.status_combo = QComboBox()
        self.status_combo.addItems(STATUS_OPTIONS)
        form.addRow("Status:", self.status_combo)

        # Total Hours (read-only)
        self.total_hours_spin = QDoubleSpinBox()
        self.total_hours_spin.setRange(0, 24)
        self.total_hours_spin.setDecimals(1)
        self.total_hours_spin.setReadOnly(True)
        self.total_hours_spin.setButtonSymbols(QDoubleSpinBox.ButtonSymbols.NoButtons)
        form.addRow("Total Hours:", self.total_hours_spin)

        # OT Hours (read-only)
        self.ot_hours_spin = QDoubleSpinBox()
        self.ot_hours_spin.setRange(0, 24)
        self.ot_hours_spin.setDecimals(1)
        self.ot_hours_spin.setReadOnly(True)
        self.ot_hours_spin.setButtonSymbols(QDoubleSpinBox.ButtonSymbols.NoButtons)
        form.addRow("OT Hours:", self.ot_hours_spin)

        # Remarks
        self.remarks_edit = QLineEdit()
        form.addRow("Remarks:", self.remarks_edit)

        layout.addLayout(form)

        # OK / Cancel buttons
        button_box = QDialogButtonBox(QDialogButtonBox.StandardButton.Ok | QDialogButtonBox.StandardButton.Cancel)
        button_box.accepted.connect(self._on_accept)
        button_box.rejected.connect(self.reject)
        layout.addWidget(button_box)

        # Initial calculation
        self._recalculate_hours()

    def _populate_from_record(self, rec: AttendanceRecord):
        if rec.in_time:
            t = QTime.fromString(rec.in_time, "HH:mm")
            if t.isValid():
                self.in_time_edit.setTime(t)

        if rec.out_time:
            t = QTime.fromString(rec.out_time, "HH:mm")
            if t.isValid():
                self.out_time_edit.setTime(t)

        idx = self.status_combo.findText(rec.status or "P")
        if idx >= 0:
            self.status_combo.setCurrentIndex(idx)

        self.remarks_edit.setText(rec.remarks or "")
        self._recalculate_hours()

    def _recalculate_hours(self):
        in_time = self.in_time_edit.time()
        out_time = self.out_time_edit.time()

        in_secs = in_time.hour() * 3600 + in_time.minute() * 60
        out_secs = out_time.hour() * 3600 + out_time.minute() * 60

        if out_secs > in_secs:
            total = (out_secs - in_secs) / 3600.0
        else:
            total = 0.0

        ot = max(0.0, total - self.normal_hours)

        self.total_hours_spin.setValue(round(total, 1))
        self.ot_hours_spin.setValue(round(ot, 1))

    def _on_accept(self):
        record = AttendanceRecord(
            id=self.existing_record.id if self.existing_record else None,
            employee_id=self.employee_id,
            date=self.date_str,
            in_time=self.in_time_edit.time().toString("HH:mm"),
            out_time=self.out_time_edit.time().toString("HH:mm"),
            status=self.status_combo.currentText(),
            total_hours=self.total_hours_spin.value(),
            ot_hours=self.ot_hours_spin.value(),
            remarks=self.remarks_edit.text().strip(),
            created_at=self.existing_record.created_at if self.existing_record else None,
        )

        try:
            save_attendance_record(self.db_manager, record)
            self.accept()
        except Exception as e:
            QMessageBox.critical(self, "Error", f"Failed to save attendance record:\n{e}")
