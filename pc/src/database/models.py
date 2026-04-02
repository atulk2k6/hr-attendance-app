import sqlite3
from dataclasses import dataclass, field
from typing import Optional


@dataclass
class Department:
    id: Optional[int] = None
    name: str = ""
    created_at: Optional[str] = None
    updated_at: Optional[str] = None

    @classmethod
    def from_row(cls, row: sqlite3.Row) -> "Department":
        return cls(
            id=row["id"],
            name=row["name"],
            created_at=row["created_at"],
            updated_at=row["updated_at"],
        )


@dataclass
class Category:
    id: Optional[int] = None
    name: str = ""
    created_at: Optional[str] = None
    updated_at: Optional[str] = None

    @classmethod
    def from_row(cls, row: sqlite3.Row) -> "Category":
        return cls(
            id=row["id"],
            name=row["name"],
            created_at=row["created_at"],
            updated_at=row["updated_at"],
        )


@dataclass
class Employee:
    id: Optional[int] = None
    code: str = ""
    emp_id: str = ""
    name: str = ""
    father_name: str = ""
    department_id: Optional[int] = None
    department_name: Optional[str] = None
    category_id: Optional[int] = None
    category_name: Optional[str] = None
    dob: Optional[str] = None
    doj: Optional[str] = None
    weekly_off_day: str = "SUN"
    fd: str = ""
    ot_rr_type: str = ""
    ot_rr_value: float = 0.0
    gross_salary: float = 0.0
    is_active: bool = True
    created_at: Optional[str] = None
    updated_at: Optional[str] = None

    @classmethod
    def from_row(cls, row: sqlite3.Row) -> "Employee":
        keys = row.keys()
        return cls(
            id=row["id"],
            code=row["code"],
            emp_id=row["emp_id"],
            name=row["name"],
            father_name=row["father_name"],
            department_id=row["department_id"],
            department_name=row["department_name"] if "department_name" in keys else None,
            category_id=row["category_id"],
            category_name=row["category_name"] if "category_name" in keys else None,
            dob=row["dob"],
            doj=row["doj"],
            weekly_off_day=row["weekly_off_day"],
            fd=row["fd"],
            ot_rr_type=row["ot_rr_type"],
            ot_rr_value=float(row["ot_rr_value"] or 0),
            gross_salary=float(row["gross_salary"] or 0),
            is_active=bool(row["is_active"]),
            created_at=row["created_at"],
            updated_at=row["updated_at"],
        )


@dataclass
class AttendanceRecord:
    id: Optional[int] = None
    employee_id: int = 0
    date: str = ""
    in_time: Optional[str] = None
    out_time: Optional[str] = None
    status: str = ""
    total_hours: float = 0.0
    ot_hours: float = 0.0
    remarks: Optional[str] = None
    created_at: Optional[str] = None
    updated_at: Optional[str] = None

    @classmethod
    def from_row(cls, row: sqlite3.Row) -> "AttendanceRecord":
        return cls(
            id=row["id"],
            employee_id=row["employee_id"],
            date=row["date"],
            in_time=row["in_time"],
            out_time=row["out_time"],
            status=row["status"],
            total_hours=float(row["total_hours"] or 0),
            ot_hours=float(row["ot_hours"] or 0),
            remarks=row["remarks"],
            created_at=row["created_at"],
            updated_at=row["updated_at"],
        )


@dataclass
class Setting:
    key: str = ""
    value: str = ""
    updated_at: Optional[str] = None

    @classmethod
    def from_row(cls, row: sqlite3.Row) -> "Setting":
        return cls(
            key=row["key"],
            value=row["value"],
            updated_at=row["updated_at"],
        )
