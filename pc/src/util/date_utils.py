import calendar
from datetime import datetime


def get_days_in_month(year: int, month: int) -> int:
    """Return the number of days in the given month."""
    return calendar.monthrange(year, month)[1]


def get_day_of_week(year: int, month: int, day: int) -> str:
    """Return the abbreviated day name (SUN, MON, TUE, etc.)."""
    day_names = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]
    # weekday() returns 0=Monday..6=Sunday
    idx = datetime(year, month, day).weekday()
    return day_names[idx]


def get_month_name(month: int) -> str:
    """Return the abbreviated month name (JAN, FEB, etc.)."""
    return calendar.month_abbr[month].upper()


def format_date_display(iso_date: str) -> str:
    """Convert 'YYYY-MM-DD' to 'dd-MMM-yy' (e.g. '15-Sep-21')."""
    dt = datetime.strptime(iso_date, "%Y-%m-%d")
    return dt.strftime("%d-%b-%y")


def get_week_day_index(year: int, month: int, day: int) -> int:
    """Return the day-of-week index where 0=Sunday, 1=Monday, ..., 6=Saturday."""
    # isoweekday() returns 1=Monday..7=Sunday
    iso = datetime(year, month, day).isoweekday()
    # Convert: Sunday(7)->0, Monday(1)->1, ..., Saturday(6)->6
    return iso % 7
