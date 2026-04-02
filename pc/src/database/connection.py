import sqlite3
from typing import Optional


class DatabaseManager:
    """SQLite connection manager with WAL mode and Row factory."""

    def __init__(self):
        self._connection: Optional[sqlite3.Connection] = None
        self._db_path: Optional[str] = None

    def load_database(self, db_path: str) -> None:
        """Open a SQLite database, store the path, and enable WAL mode."""
        if self._connection is not None:
            self.close()
        self._db_path = db_path
        self._connection = sqlite3.connect(db_path)
        self._connection.row_factory = sqlite3.Row
        self._connection.execute("PRAGMA journal_mode=WAL")

    def close(self) -> None:
        """Close the current database connection."""
        if self._connection is not None:
            self._connection.close()
            self._connection = None

    def get_connection(self) -> sqlite3.Connection:
        """Return the current connection. Raises RuntimeError if no database is loaded."""
        if self._connection is None:
            raise RuntimeError("No database loaded. Call load_database() first.")
        return self._connection

    def is_loaded(self) -> bool:
        """Check whether a database is currently loaded."""
        return self._connection is not None

    def get_path(self) -> str:
        """Return the path of the currently loaded database."""
        if self._db_path is None:
            raise RuntimeError("No database loaded. Call load_database() first.")
        return self._db_path

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()
        return False
