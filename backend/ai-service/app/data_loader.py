from io import BytesIO
from pathlib import Path
from typing import BinaryIO

import pandas as pd


class FinancialDataLoader:
    """
    Handles loading financial transaction datasets.

    The FinSight dataset may be supplied as:
    - CSV
    - Excel XLSX

    The loader also detects an Excel workbook even when
    the file has accidentally been given a .csv extension.
    """

    SUPPORTED_ENCODINGS = [
        "utf-8",
        "utf-8-sig",
        "cp1252",
        "latin1",
    ]

    REQUIRED_COLUMNS = [
        "Date",
        "Description",
        "Amount",
        "Transaction Type",
        "Category",
        "Account Name",
        "Month",
    ]

    def __init__(self) -> None:

        self.base_path = (
            Path(__file__)
            .resolve()
            .parent
            .parent
        )

        self.data_directory = (
            self.base_path
            / "data"
        )

        self.dataset_path = (
            self.data_directory
            / "personal_transactions_dataset.xlsx"
        )

        self.legacy_dataset_path = (
            self.data_directory
            / "personal_transactions_dataset.csv"
        )

    # ============================================================
    # LOAD DEFAULT DATASET
    # ============================================================

    def load_dataset(self) -> pd.DataFrame:
        """
        Load the FinSight training dataset.

        The preferred filename is:
        personal_transactions_dataset.xlsx

        The legacy .csv filename is also supported in case
        the user has not renamed the original file yet.
        """

        if self.dataset_path.exists():

            return self.load_file(
                self.dataset_path
            )

        if self.legacy_dataset_path.exists():

            return self.load_file(
                self.legacy_dataset_path
            )

        raise FileNotFoundError(
            "FinSight training dataset was not found. "
            "Expected file: "
            f"{self.dataset_path}"
        )

    # ============================================================
    # GENERIC FILE LOADER
    # ============================================================

    def load_file(
        self,
        file_path: Path
    ) -> pd.DataFrame:
        """
        Load a CSV or Excel file.

        The actual file content is inspected so that an
        Excel workbook incorrectly named .csv can still be
        loaded safely.
        """

        if file_path is None:

            raise ValueError(
                "Dataset file path cannot be None."
            )

        if not file_path.exists():

            raise FileNotFoundError(
                f"Dataset file not found: {file_path}"
            )

        if not file_path.is_file():

            raise ValueError(
                f"Dataset path is not a file: {file_path}"
            )

        try:

            with file_path.open(
                "rb"
            ) as file:

                file_bytes = file.read()

        except OSError as exception:

            raise ValueError(
                f"Unable to read dataset file: {file_path}"
            ) from exception

        if not file_bytes:

            raise ValueError(
                "The dataset file is empty."
            )

        return self.load_bytes(
            file_bytes,
            file_path.name
        )

    # ============================================================
    # LOAD BYTES
    # ============================================================

    def load_bytes(
        self,
        file_bytes: bytes,
        file_name: str = ""
    ) -> pd.DataFrame:
        """
        Detect and load CSV or Excel content from bytes.
        """

        if not file_bytes:

            raise ValueError(
                "The uploaded dataset is empty."
            )

        # XLSX files are ZIP containers and normally begin
        # with the PK ZIP signature.
        is_excel_workbook = (
            file_bytes.startswith(
                b"PK\x03\x04"
            )
        )

        if is_excel_workbook:

            try:

                dataframe = pd.read_excel(
                    BytesIO(file_bytes),
                    engine="openpyxl"
                )

                return self.validate_dataframe(
                    dataframe
                )

            except ImportError as exception:

                raise ValueError(
                    "The openpyxl package is required "
                    "to read Excel datasets. "
                    "Run: pip install openpyxl"
                ) from exception

            except Exception as exception:

                raise ValueError(
                    "The Excel dataset could not be read. "
                    "Make sure the workbook is valid."
                ) from exception

        return self.load_csv_bytes(
            file_bytes
        )

    # ============================================================
    # LOAD CSV
    # ============================================================

    @staticmethod
    def load_csv(
        file: BinaryIO
    ) -> pd.DataFrame:
        """
        Load a CSV file from a binary file object.

        Several common encodings are attempted.
        """

        if file is None:

            raise ValueError(
                "No file was provided."
            )

        try:

            file.seek(0)

        except Exception:

            pass

        file_bytes = file.read()

        return FinancialDataLoader.load_csv_bytes(
            file_bytes
        )

    # ============================================================
    # LOAD CSV BYTES
    # ============================================================

    @staticmethod
    def load_csv_bytes(
        file_bytes: bytes
    ) -> pd.DataFrame:
        """
        Load CSV data directly from bytes.
        """

        if not file_bytes:

            raise ValueError(
                "The uploaded CSV file is empty."
            )

        last_error = None

        for encoding in (
            FinancialDataLoader.SUPPORTED_ENCODINGS
        ):

            try:

                dataframe = pd.read_csv(
                    BytesIO(file_bytes),
                    encoding=encoding
                )

                if dataframe.empty:

                    raise ValueError(
                        "The CSV file does not contain "
                        "any records."
                    )

                return (
                    FinancialDataLoader
                    .validate_dataframe(
                        dataframe
                    )
                )

            except UnicodeDecodeError as exception:

                last_error = exception

                continue

            except pd.errors.EmptyDataError:

                raise ValueError(
                    "The uploaded CSV file is empty."
                )

            except pd.errors.ParserError as exception:

                raise ValueError(
                    "The uploaded file could not be "
                    "parsed as a valid CSV file."
                ) from exception

        raise ValueError(
            "The uploaded CSV file uses an unsupported "
            "text encoding. Please save the file as "
            "UTF-8 CSV and upload it again."
        ) from last_error

    # ============================================================
    # GENERIC DATAFRAME LOADER
    # ============================================================

    @staticmethod
    def load_dataframe(
        file: BinaryIO
    ) -> pd.DataFrame:
        """
        Backward-compatible dataframe loader.
        """

        return FinancialDataLoader.load_csv(
            file
        )

    # ============================================================
    # VALIDATE DATAFRAME
    # ============================================================

    @staticmethod
    def validate_dataframe(
        dataframe: pd.DataFrame
    ) -> pd.DataFrame:
        """
        Validate the basic structure of the dataset.
        """

        if dataframe is None:

            raise ValueError(
                "Loaded dataframe is None."
            )

        if dataframe.empty:

            raise ValueError(
                "The dataset does not contain any records."
            )

        dataframe = dataframe.copy()

        dataframe.columns = (
            dataframe.columns
            .astype(str)
            .str.strip()
        )

        missing_columns = [
            column
            for column
            in FinancialDataLoader.REQUIRED_COLUMNS
            if column not in dataframe.columns
        ]

        if missing_columns:

            raise ValueError(
                "The dataset is missing required "
                f"columns: {missing_columns}"
            )

        return dataframe

    # ============================================================
    # DATASET INFORMATION
    # ============================================================

    @staticmethod
    def get_dataset_information(
        dataframe: pd.DataFrame
    ) -> dict:
        """
        Return useful information about the dataset.
        """

        if dataframe is None:

            raise ValueError(
                "Dataframe cannot be None."
            )

        if dataframe.empty:

            return {
                "rows": 0,
                "columns": [],
                "categories": [],
                "category_count": 0,
            }

        information = {
            "rows": int(
                len(dataframe)
            ),
            "columns": (
                dataframe.columns
                .tolist()
            ),
            "categories": [],
            "category_count": 0,
        }

        if "Category" in dataframe.columns:

            categories = (
                dataframe["Category"]
                .dropna()
                .astype(str)
                .str.strip()
                .str.upper()
                .unique()
                .tolist()
            )

            categories.sort()

            information["categories"] = categories

            information["category_count"] = (
                len(categories)
            )

        return information