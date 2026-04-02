import calendar
from datetime import datetime
from typing import Dict, List, Optional

from .connection import DatabaseManager
from .models import AttendanceRecord, Category, Department, Employee, Setting


def get_all_departments(db: DatabaseManager) -> List[Department]:
    """Return all departments ordered by name."""
    conn = db.get_connection()
    cursor = conn.execute("SELECT * FROM departments ORDER BY name")
    return [Department.from_row(row) for row in cursor.fetchall()]


def get_all_categories(db: DatabaseManager) -> List[Category]:
    """Return all categories ordered by name."""
    conn = db.get_connection()
    cursor = conn.execute("SELECT * FROM categories ORDER BY name")
    return [Category.from_row(row) for row in cursor.fetchall()]


def get_all_employees(db: DatabaseManager, active_only: bool = True) -> List[Employee]:
    """Return all employees with department and category names via JOIN."""
    conn = db.get_connection()
    sql = """
        SELECT e.*,
               d.name AS department_name,
               c.name AS category_name
        FROM employees e
        LEFT JOIN departments d ON e.department_id = d.id
        LEFT JOIN categories c ON e.category_id = c.id
    """
    if active_only:
        sql += " WHERE e.is_active = 1"
    sql += " ORDER BY e.code, e.name"
    cursor = conn.execute(sql)
    return [Employee.from_row(row) for row in cursor.fetchall()]


def get_employee_by_id(db: DatabaseManager, employee_id: int) -> Optional[Employee]:
    """Return a single employee by ID, or None if not found."""
    conn = db.get_connection()
    cursor = conn.execute(
        """
        SELECT e.*,
               d.name AS department_name,
               c.name AS category_name
        FROM employees e
        LEFT JOIN departments d ON e.department_id = d.id
        LEFT JOIN categories c ON e.category_id = c.id
        WHERE e.id = ?
        """,
        (employee_id,),
    )
    row = cursor.fetchone()
    return Employee.from_row(row) if row else None


def save_employee(db: DatabaseManager, employee: Employee) -> None:
    """Insert or update an employee record."""
    conn = db.get_connection()
    now = datetime.now().isoformat()
    if employee.id is not None:
        conn.execute(
            """
            UPDATE employees SET
                code = ?, emp_id = ?, name = ?, father_name = ?,
                department_id = ?, category_id = ?,
                dob = ?, doj = ?, weekly_off_day = ?, fd = ?,
                ot_rr_type = ?, ot_rr_value = ?, gross_salary = ?,
                is_active = ?, updated_at = ?
            WHERE id = ?
            """,
            (
                employee.code, employee.emp_id, employee.name, employee.father_name,
                employee.department_id, employee.category_id,
                employee.dob, employee.doj, employee.weekly_off_day, employee.fd,
                employee.ot_rr_type, employee.ot_rr_value, employee.gross_salary,
                int(employee.is_active), now,
                employee.id,
            ),
        )
    else:
        conn.execute(
            """
            INSERT INTO employees (
                code, emp_id, name, father_name,
                department_id, category_id,
                dob, doj, weekly_off_day, fd,
                ot_rr_type, ot_rr_value, gross_salary,
                is_active, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                employee.code, employee.emp_id, employee.name, employee.father_name,
                employee.department_id, employee.category_id,
                employee.dob, employee.doj, employee.weekly_off_day, employee.fd,
                employee.ot_rr_type, employee.ot_rr_value, employee.gross_salary,
                int(employee.is_active), now, now,
            ),
        )
    conn.commit()


def get_attendance_for_month(
    db: DatabaseManager, year: int, month: int
) -> List[AttendanceRecord]:
    """Return all attendance records for a given year/month."""
    conn = db.get_connection()
    start_date = f"{year:04d}-{month:02d}-01"
    days_in_month = calendar.monthrange(year, month)[1]
    end_date = f"{year:04d}-{month:02d}-{days_in_month:02d}"
    cursor = conn.execute(
        """
        SELECT * FROM attendance_records
        WHERE date BETWEEN ? AND ?
        ORDER BY employee_id, date
        """,
        (start_date, end_date),
    )
    return [AttendanceRecord.from_row(row) for row in cursor.fetchall()]


def get_attendance_for_date(
    db: DatabaseManager, date_str: str
) -> List[AttendanceRecord]:
    """Return all attendance records for a specific date (YYYY-MM-DD)."""
    conn = db.get_connection()
    cursor = conn.execute(
        "SELECT * FROM attendance_records WHERE date = ? ORDER BY employee_id",
        (date_str,),
    )
    return [AttendanceRecord.from_row(row) for row in cursor.fetchall()]


def save_attendance_record(db: DatabaseManager, record: AttendanceRecord) -> None:
    """Insert or replace an attendance record."""
    conn = db.get_connection()
    now = datetime.now().isoformat()
    conn.execute(
        """
        INSERT OR REPLACE INTO attendance_records (
            id, employee_id, date, in_time, out_time,
            status, total_hours, ot_hours, remarks,
            created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, COALESCE(?, ?), ?)
        """,
        (
            record.id, record.employee_id, record.date,
            record.in_time, record.out_time,
            record.status, record.total_hours, record.ot_hours,
            record.remarks, record.created_at, now, now,
        ),
    )
    conn.commit()


def get_settings(db: DatabaseManager) -> Dict[str, str]:
    """Return all settings as a key-value dict."""
    conn = db.get_connection()
    cursor = conn.execute("SELECT * FROM settings")
    return {row["key"]: row["value"] for row in cursor.fetchall()}


def save_setting(db: DatabaseManager, key: str, value: str) -> None:
    """Insert or update a setting."""
    conn = db.get_connection()
    now = datetime.now().isoformat()
    conn.execute(
        """
        INSERT OR REPLACE INTO settings (key, value, updated_at)
        VALUES (?, ?, ?)
        """,
        (key, value, now),
    )
    conn.commit()


def get_monthly_report_data(
    db: DatabaseManager, year: int, month: int
) -> List[Dict]:
    """
    Return employee details + daily attendance for the month.
    Each dict has employee fields plus a 'days' dict mapping
    day_number -> {'status': str, 'ot_hours': float}.
    """
    employees = get_all_employees(db, active_only=True)
    attendance_records = get_attendance_for_month(db, year, month)

    # Index attendance by (employee_id, day_number)
    att_map: Dict[tuple, AttendanceRecord] = {}
    for rec in attendance_records:
        # date is "YYYY-MM-DD"
        day_num = int(rec.date.split("-")[2])
        att_map[(rec.employee_id, day_num)] = rec

    days_in_month = calendar.monthrange(year, month)[1]
    result = []

    for emp in employees:
        days: Dict[int, Dict] = {}
        for day in range(1, days_in_month + 1):
            key = (emp.id, day)
            if key in att_map:
                rec = att_map[key]
                days[day] = {
                    "status": rec.status,
                    "ot_hours": rec.ot_hours,
                }
            else:
                days[day] = {
                    "status": "",
                    "ot_hours": 0.0,
                }

        result.append(
            {
                "id": emp.id,
                "code": emp.code,
                "emp_id": emp.emp_id,
                "name": emp.name,
                "father_name": emp.father_name,
                "department_name": emp.department_name or "",
                "category_name": emp.category_name or "",
                "dob": emp.dob,
                "doj": emp.doj,
                "weekly_off_day": emp.weekly_off_day,
                "fd": emp.fd,
                "ot_rr_type": emp.ot_rr_type,
                "ot_rr_value": emp.ot_rr_value,
                "gross_salary": emp.gross_salary,
                "days": days,
            }
        )

    return result
