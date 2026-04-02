# HR Attendance App

A complete attendance management system for manufacturing/factory settings — two apps that share a single SQLite database file.

## Apps

### Android App (`android/`)
Kotlin + Jetpack Compose mobile app for recording daily attendance.

**Features:**
- Quick Punch IN/OUT with employee search
- Kiosk self-service mode with PIN lock
- Daily attendance entry with time pickers
- Automatic OT calculation per employee
- Monthly view — scrollable grid with P/A/W/CO per day
- Employee monthly detail — stats + daily record list
- Department and category management
- Multi-unit/location support
- Daily auto-backup to `Documents/AttendanceApp/backups/` (synced by Google Drive app)
- Share daily attendance report via WhatsApp/Email

### PC App (`pc/`)
Python + PyQt6 desktop app for importing data and exporting Excel reports.

**Features:**
- Import `.db` file from Android backup
- View and edit employees, attendance, departments, categories
- Export customizable Excel attendance sheets
- Column selection, reordering, and computed columns
- Matches factory attendance sheet format (ATTENDANCE SHEET FOR THE MONTH OF...)

## Data Exchange

Both apps use the same SQLite `.db` file. Export from Android (Backup screen) → Import on PC.

## Getting Started

### Android
1. Open `android/` in Android Studio (Flamingo or newer)
2. Build and run on a device/emulator (minSdk 26)
3. Or download the latest APK from [GitHub Actions artifacts](../../actions)

### PC App
```bash
cd pc
pip install -r requirements.txt
python main.py
```

## Database Schema

See `android/app/src/main/java/com/attendance/app/data/local/AppDatabase.kt` for the full schema.
Key tables: `employees`, `attendance_records`, `punch_log`, `departments`, `categories`, `settings`.

## Contributing

Pull requests are welcome. For major changes, please open an issue first.

## License

[MIT](LICENSE)
