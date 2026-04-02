import json
import os
from dataclasses import asdict, dataclass, field
from typing import List


@dataclass
class ColumnDefinition:
    key: str           # internal key
    label: str         # display header
    field: str         # DB field or employee attribute name
    width: int = 12    # Excel column width
    visible: bool = True
    order: int = 0


DEFAULT_COLUMNS: List[ColumnDefinition] = [
    ColumnDefinition(key="sr",           label="S.R.",           field="sr",            width=6,  order=0),
    ColumnDefinition(key="code",         label="Code",           field="code",          width=10, order=1),
    ColumnDefinition(key="emp_id",       label="EMP.ID",         field="emp_id",        width=10, order=2),
    ColumnDefinition(key="name",         label="EMP.NAME",       field="name",          width=20, order=3),
    ColumnDefinition(key="category",     label="Filter",         field="category_name", width=12, order=4),
    ColumnDefinition(key="father_name",  label="FATHERS NAME",   field="father_name",   width=18, order=5),
    ColumnDefinition(key="department",   label="DEPARTMENT",     field="department_name", width=15, order=6),
    ColumnDefinition(key="weekly_off",   label="W. OFF",         field="weekly_off_day", width=8,  order=7),
    ColumnDefinition(key="dob",          label="D.O.B",          field="dob",           width=12, order=8),
    ColumnDefinition(key="doj",          label="D.O.J",          field="doj",           width=12, order=9),
    ColumnDefinition(key="fd",           label="FD",             field="fd",            width=5,  order=10),
    ColumnDefinition(key="ot_rr",        label="OT/RR",          field="ot_rr",         width=10, order=11),
    ColumnDefinition(key="gross_salary", label="GROSS SALARY",   field="gross_salary",  width=14, order=12),
]


def get_default_columns() -> List[ColumnDefinition]:
    """Return a fresh copy of the default column definitions."""
    return [
        ColumnDefinition(
            key=c.key, label=c.label, field=c.field,
            width=c.width, visible=c.visible, order=c.order,
        )
        for c in DEFAULT_COLUMNS
    ]


def save_column_config(columns: List[ColumnDefinition], path: str) -> None:
    """Save column configuration to a JSON file."""
    data = [asdict(c) for c in columns]
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)


def load_column_config(path: str) -> List[ColumnDefinition]:
    """Load column configuration from a JSON file, falling back to defaults."""
    if not os.path.isfile(path):
        return get_default_columns()
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        columns = []
        for item in data:
            columns.append(ColumnDefinition(
                key=item["key"],
                label=item["label"],
                field=item["field"],
                width=item.get("width", 12),
                visible=item.get("visible", True),
                order=item.get("order", 0),
            ))
        return columns
    except Exception:
        return get_default_columns()
