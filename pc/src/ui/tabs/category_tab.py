from datetime import datetime
from typing import Optional

from PyQt6.QtCore import Qt
from PyQt6.QtGui import QStandardItem, QStandardItemModel
from PyQt6.QtWidgets import (
    QHBoxLayout,
    QHeaderView,
    QInputDialog,
    QMessageBox,
    QPushButton,
    QTableView,
    QVBoxLayout,
    QWidget,
)

from src.database.connection import DatabaseManager
from src.database.queries import get_all_categories


class CategoryTab(QWidget):
    """Tab for managing categories."""

    def __init__(self, parent: Optional[QWidget] = None):
        super().__init__(parent)
        self._db: Optional[DatabaseManager] = None
        self._init_ui()

    # ------------------------------------------------------------------
    # UI setup
    # ------------------------------------------------------------------

    def _init_ui(self) -> None:
        layout = QVBoxLayout(self)

        # Buttons
        btn_row = QHBoxLayout()
        self._btn_add = QPushButton("Add Category")
        self._btn_add.clicked.connect(self._add_category)
        btn_row.addWidget(self._btn_add)

        self._btn_edit = QPushButton("Edit")
        self._btn_edit.clicked.connect(self._edit_category)
        btn_row.addWidget(self._btn_edit)

        self._btn_delete = QPushButton("Delete")
        self._btn_delete.clicked.connect(self._delete_category)
        btn_row.addWidget(self._btn_delete)

        btn_row.addStretch()
        layout.addLayout(btn_row)

        # Table
        self._model = QStandardItemModel()
        self._model.setHorizontalHeaderLabels(["ID", "Name", "Employee Count"])

        self._table = QTableView()
        self._table.setModel(self._model)
        self._table.setSelectionBehavior(QTableView.SelectionBehavior.SelectRows)
        self._table.setEditTriggers(QTableView.EditTrigger.NoEditTriggers)
        self._table.horizontalHeader().setStretchLastSection(True)
        self._table.verticalHeader().setVisible(False)
        layout.addWidget(self._table)

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def refresh(self, db_manager: DatabaseManager) -> None:
        self._db = db_manager
        self._load_data()

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _load_data(self) -> None:
        if self._db is None or not self._db.is_loaded():
            return
        categories = get_all_categories(self._db)
        conn = self._db.get_connection()

        self._model.removeRows(0, self._model.rowCount())
        for cat in categories:
            cursor = conn.execute(
                "SELECT COUNT(*) FROM employees WHERE category_id = ?",
                (cat.id,),
            )
            count = cursor.fetchone()[0]

            id_item = QStandardItem(str(cat.id))
            id_item.setData(cat.id, Qt.ItemDataRole.UserRole)
            name_item = QStandardItem(cat.name)
            count_item = QStandardItem(str(count))

            self._model.appendRow([id_item, name_item, count_item])
        self._table.resizeColumnsToContents()

    def _selected_category_id(self) -> Optional[int]:
        indexes = self._table.selectionModel().selectedRows()
        if not indexes:
            return None
        row = indexes[0].row()
        item = self._model.item(row, 0)
        return item.data(Qt.ItemDataRole.UserRole)

    def _selected_category_name(self) -> Optional[str]:
        indexes = self._table.selectionModel().selectedRows()
        if not indexes:
            return None
        row = indexes[0].row()
        return self._model.item(row, 1).text()

    # ------------------------------------------------------------------
    # Actions
    # ------------------------------------------------------------------

    def _add_category(self) -> None:
        if self._db is None:
            QMessageBox.warning(self, "No Database", "Please load a database first.")
            return
        name, ok = QInputDialog.getText(self, "Add Category", "Category name:")
        if ok and name.strip():
            conn = self._db.get_connection()
            now = datetime.now().isoformat()
            conn.execute(
                "INSERT INTO categories (name, created_at, updated_at) VALUES (?, ?, ?)",
                (name.strip(), now, now),
            )
            conn.commit()
            self._load_data()

    def _edit_category(self) -> None:
        if self._db is None:
            return
        cat_id = self._selected_category_id()
        current_name = self._selected_category_name()
        if cat_id is None:
            QMessageBox.information(self, "No Selection", "Please select a category to edit.")
            return
        name, ok = QInputDialog.getText(
            self, "Edit Category", "Category name:", text=current_name or ""
        )
        if ok and name.strip():
            conn = self._db.get_connection()
            now = datetime.now().isoformat()
            conn.execute(
                "UPDATE categories SET name = ?, updated_at = ? WHERE id = ?",
                (name.strip(), now, cat_id),
            )
            conn.commit()
            self._load_data()

    def _delete_category(self) -> None:
        if self._db is None:
            return
        cat_id = self._selected_category_id()
        cat_name = self._selected_category_name()
        if cat_id is None:
            QMessageBox.information(self, "No Selection", "Please select a category to delete.")
            return

        conn = self._db.get_connection()
        cursor = conn.execute(
            "SELECT COUNT(*) FROM employees WHERE category_id = ?", (cat_id,)
        )
        count = cursor.fetchone()[0]
        if count > 0:
            QMessageBox.warning(
                self,
                "Cannot Delete",
                f"Category '{cat_name}' has {count} employee(s) assigned. "
                "Remove or reassign them first.",
            )
            return

        reply = QMessageBox.question(
            self,
            "Confirm Delete",
            f"Are you sure you want to delete category '{cat_name}'?",
            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
        )
        if reply == QMessageBox.StandardButton.Yes:
            conn.execute("DELETE FROM categories WHERE id = ?", (cat_id,))
            conn.commit()
            self._load_data()
