# FinSight File Notes: Classifies transaction descriptions into FinSight categories using trusted rules and a TF-IDF/Logistic Regression model.

from __future__ import annotations

from pathlib import Path
from typing import Any

import joblib
import pandas as pd

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline


class TransactionClassifier:
    """
    FinSight transaction classifier.

    The classifier uses a hybrid strategy:

    1. High-confidence merchant/description rules are applied first for
       unambiguous transactions such as Spotify, Netflix, salary, fuel,
       groceries, pharmacy, etc.
    2. The trained TF-IDF + Logistic Regression model is used for unknown
       descriptions.

    This prevents obvious merchant names from being forced into the wrong
    dataset class when the training dataset does not contain the exact
    wording used by a user's transaction file.
    """

    MODEL_TO_FINSIGHT_CATEGORY = {
        "ALCOHOL & BARS": "ENTERTAINMENT",
        "AUTO INSURANCE": "FINANCIAL",
        "COFFEE SHOPS": "FOOD",
        "CREDIT CARD PAYMENT": "FINANCIAL",
        "ELECTRONICS & SOFTWARE": "SHOPPING",
        "FAST FOOD": "FOOD",
        "FOOD & DINING": "FOOD",
        "GAS & FUEL": "TRANSPORTATION",
        "GROCERIES": "GROCERIES",
        "HAIRCUT": "PERSONAL_CARE",
        "HOME IMPROVEMENT": "HOUSING",
        "INTERNET": "UTILITIES",
        "MOBILE PHONE": "UTILITIES",
        "MORTGAGE & RENT": "HOUSING",
        "MOVIES & DVDS": "ENTERTAINMENT",
        "MUSIC": "ENTERTAINMENT",
        "PAYCHECK": "SALARY",
        "RESTAURANTS": "FOOD",
        "SHOPPING": "SHOPPING",
        "TELEVISION": "SUBSCRIPTION",
        "UTILITIES": "UTILITIES",
    }

    # These rules are deliberately ordered from most specific to broadest.
    # Spotify and other subscriptions MUST be classified as SUBSCRIPTION.
    # Dataset-specific aliases are included in the main rules below.
    DESCRIPTION_RULES = (
        (
            "SUBSCRIPTION",
            0.99,
            (
                "SPOTIFY",
                "NETFLIX",
                "ADOBE CREATIVE CLOUD",
                "DISNEY PLUS",
                "DISNEY+",
                "YOUTUBE PREMIUM",
                "YOUTUBE MUSIC",
                "APPLE MUSIC",
                "APPLE TV",
                "AMAZON PRIME",
                "PRIME VIDEO",
                "HBO MAX",
                "PARAMOUNT PLUS",
                "PARAMOUNT+",
                "ADOBE",
                "CANVA",
                "MICROSOFT 365",
                "GOOGLE ONE",
                "DROPBOX",
                "CHATGPT PLUS",
                "GYM MEMBERSHIP",
                "MEMBERSHIP",
                "SUBSCRIPTION",
                "PREMIUM PLAN",
                "MONTHLY PLAN",
                "MONTHLY FEE",
                "ANNUAL PLAN",
                "STREAMING",
            ),
        ),
        (
            "SALARY",
            0.99,
            (
                "MONTHLY SALARY",
                "SALARY",
                "PAYCHECK",
                "BIWEEKLY PAYCHECK",
                "WAGES",
                "PAYROLL",
            ),
        ),
        (
            "GROCERIES",
            0.99,
            (
                "GROCERIES",
                "SUPERMARKET",
                "SUPERMARKET GROCERIES",
                "GROCERY",
                "GROCERIES",
                "GROCERY STORE",
            ),
        ),
        (
            "UTILITIES",
            0.99,
            (
                "ELECTRICITY",
                "ELECTRICITY BILL",
                "POWER COMPANY",
                "POWER BILL",
                "CITY WATER",
                "WATER BILL",
                "WATER CHARGES",
                "GAS COMPANY",
                "UTILITY",
                "UTILITIES",
                "INTERNET SERVICE",
                "INTERNET PROVIDER",
                "PHONE COMPANY",
                "MOBILE PHONE",
                "MOBILE BILL",
                "MOBILE PREPAID",
                "ELECTRICITY BILL",
                "INTERNET BILL",
            ),
        ),
        (
            "TRANSPORTATION",
            0.99,
            (
                "TRAIN FARE",
                "TRAIN",
                "BUS FARE",
                "BUS",
                "FUEL",
                "PETROL",
                "GAS STATION",
                "SHELL",
                "BP",
                "CHEVRON",
                "EXXON",
                "VALERO",
                "CONOCO",
                "QUIKTRIP",
                "TRANSPORT",
                "TRANSPORTATION",
                "GRAB RIDE",
                "PETROL",
            ),
        ),
        (
            "HEALTHCARE",
            0.99,
            (
                "PHARMACY",
                "HOSPITAL",
                "CLINIC",
                "DOCTOR",
                "MEDICINE",
                "HEALTHCARE",
                "MEDICAL CLINIC",
            ),
        ),
        (
            "SHOPPING",
            0.99,
            (
                "ONLINE SHOPPING",
                "SHOPPING",
                "AMAZON",
                "ONLINE STORE",
                "RETAIL",
            ),
        ),
        (
            "FOOD",
            0.99,
            (
                "COFFEE",
                "COFFEE SHOP",
                "STARBUCKS",
                "RESTAURANT",
                "RESTAURANTS",
                "LUNCH",
                "DINNER",
                "BREAKFAST",
                "PIZZA",
                "BURGER",
                "FAST FOOD",
                "FOOD TRUCK",
                "CAFE",
                "TAVERN",
                "DELI",
                "STEAKHOUSE",
                "SUSHI",
                "THAI RESTAURANT",
                "ITALIAN RESTAURANT",
                "GREEK RESTAURANT",
                "SEAFOOD",
                "CAMPUS CAFETERIA",
                "LUNCH DELIVERY",
                "FOOD DELIVERY",
                "LUNCH",
                "COFFEE SHOP",
            ),
        ),
        (
            "ENTERTAINMENT",
            0.99,
            (
                "MOVIE TICKET",
                "MOVIE TICKETS",
                "MOVIE THEATER",
                "CINEMA",
                "MOVIE",
                "ENTERTAINMENT",
                "GAMING PURCHASE",
                "PC GAME",
                "PC GAMES",
                "VIDEO GAME",
                "VIDEO GAMES",
                "STEAM",
                "EPIC GAMES",
                "PLAYSTATION",
                "XBOX",
                "NINTENDO",
                "GAMING",
                "GAME PURCHASE",
                "GAME BUNDLE",
                "GAMING EQUIPMENT",
                "GYM MEMBERSHIP",
            ),
        ),
        (
            "PERSONAL_CARE",
            0.99,
            (
                "BARBERSHOP",
                "BARBER",
                "HAIRCUT",
                "SALON",
                "PERSONAL CARE",
            ),
        ),
        (
            "HOUSING",
            0.99,
            (
                "MORTGAGE",
                "MORTGAGE PAYMENT",
                "RENT",
                "RENTAL",
                "HOSTEL FEE",
                "HOUSE RENT",
            ),
        ),
        (
            "FINANCIAL",
            0.99,
            (
                "CREDIT CARD PAYMENT",
                "BANK FEE",
                "BANK CHARGE",
                "FINANCIAL",
                "CAR LOAN",
                "INSURANCE PREMIUM",
            ),
        ),
        (
            "EDUCATION",
            0.99,
            (
                "TUITION",
                "SCHOOL FEE",
                "UNIVERSITY",
                "EDUCATION",
                "PRINTING AND PHOTOCOPY",
                "STUDY MATERIALS",
            ),
        ),
        (
            "TRAVEL",
            0.99,
            (
                "HOTEL",
                "FLIGHT",
                "AIRLINE",
                "TRAVEL",
                "WEEKEND TRIP",
            ),
        ),
        (
            "INVESTMENT",
            0.99,
            (
                "INVESTMENT",
                "STOCK",
                "MUTUAL FUND",
            ),
        ),
    )

    EXACT_DESCRIPTION_CATEGORIES = {
        "MONTHLY ALLOWANCE": "OTHER",
        "SCHOLARSHIP PAYMENT": "OTHER",
        "FREELANCE SIDE INCOME": "OTHER",
        "MONTHLY SALARY": "SALARY",
        "HOSTEL FEE": "HOUSING",
        "HOUSE RENT": "HOUSING",
        "CAR LOAN": "FINANCIAL",
        "INSURANCE PREMIUM": "FINANCIAL",
        "ELECTRICITY BILL": "UTILITIES",
        "INTERNET BILL": "UTILITIES",
        "MOBILE BILL": "UTILITIES",
        "MOBILE PREPAID": "UTILITIES",
        "GROCERIES": "GROCERIES",
        "PETROL": "TRANSPORTATION",
        "GRAB RIDE": "TRANSPORTATION",
        "NETFLIX": "SUBSCRIPTION",
        "SPOTIFY": "SUBSCRIPTION",
        "YOUTUBE PREMIUM": "SUBSCRIPTION",
        "ADOBE CREATIVE CLOUD": "SUBSCRIPTION",
        "CAMPUS CAFETERIA": "FOOD",
        "LUNCH DELIVERY": "FOOD",
        "FOOD DELIVERY": "FOOD",
        "LUNCH": "FOOD",
        "COFFEE SHOP": "FOOD",
        "PRINTING AND PHOTOCOPY": "EDUCATION",
        "STUDY MATERIALS": "EDUCATION",
        "ONLINE SHOPPING": "SHOPPING",
        "ENTERTAINMENT": "ENTERTAINMENT",
        "GAMING PURCHASE": "ENTERTAINMENT",
        "GYM MEMBERSHIP": "ENTERTAINMENT",
        "PERSONAL CARE": "PERSONAL_CARE",
        "WEEKEND TRIP": "TRAVEL",
        "MEDICAL CLINIC": "HEALTHCARE",
    }

    def __init__(self) -> None:
        self.base_path = Path(__file__).resolve().parent.parent.parent
        self.model_path = self.base_path / "models" / "transaction_classifier.joblib"
        self.model: Pipeline | None = None

    @staticmethod
    def create_model() -> Pipeline:
        return Pipeline(
            steps=[
                (
                    "tfidf",
                    TfidfVectorizer(
                        lowercase=True,
                        ngram_range=(1, 2),
                        min_df=1,
                        max_df=0.95,
                        sublinear_tf=True,
                    ),
                ),
                (
                    "classifier",
                    LogisticRegression(
                        max_iter=2000,
                        class_weight="balanced",
                    ),
                ),
            ]
        )

    def train(self, dataframe: pd.DataFrame) -> dict:
        if dataframe is None:
            raise ValueError("Training dataframe cannot be None.")
        if dataframe.empty:
            raise ValueError("Training dataframe cannot be empty.")

        required_columns = ["Description", "Category"]
        missing_columns = [
            column for column in required_columns if column not in dataframe.columns
        ]
        if missing_columns:
            raise ValueError(
                "Training dataframe is missing required columns: "
                f"{missing_columns}"
            )

        training_data = dataframe[required_columns].copy().dropna()
        training_data["Description"] = (
            training_data["Description"].astype(str).str.strip().str.upper()
        )
        training_data["Category"] = (
            training_data["Category"].astype(str).str.strip().str.upper()
        )
        training_data = training_data[
            (training_data["Description"].str.len() > 0)
            & (training_data["Category"].str.len() > 0)
        ]

        if training_data.empty:
            raise ValueError("No valid training records were found.")

        number_of_categories = int(training_data["Category"].nunique())
        if number_of_categories < 2:
            raise ValueError(
                "At least two transaction categories are required for classification."
            )

        X = training_data["Description"]
        y = training_data["Category"]
        class_counts = y.value_counts()
        can_stratify = len(class_counts) > 1 and class_counts.min() >= 2

        if len(training_data) >= 20:
            X_train, X_test, y_train, y_test = train_test_split(
                X,
                y,
                test_size=0.20,
                random_state=42,
                stratify=y if can_stratify else None,
            )
        else:
            X_train = X
            y_train = y
            X_test = pd.Series(dtype=str)
            y_test = pd.Series(dtype=str)

        model = self.create_model()
        model.fit(X_train, y_train)

        accuracy = None
        if not X_test.empty:
            predictions = model.predict(X_test)
            accuracy = accuracy_score(y_test, predictions)

        self.model_path.parent.mkdir(parents=True, exist_ok=True)
        joblib.dump(model, self.model_path)
        self.model = model

        return {
            "status": "TRAINED",
            "training_records": int(len(X_train)),
            "testing_records": int(len(X_test)),
            "categories": sorted(y.unique().tolist()),
            "category_count": number_of_categories,
            "accuracy": round(float(accuracy), 4) if accuracy is not None else None,
            "model_path": str(self.model_path),
        }

    def load_model(self) -> Pipeline:
        if not self.model_path.exists():
            raise FileNotFoundError(
                "Trained transaction classification model not found at: "
                f"{self.model_path}"
            )

        loaded_model = joblib.load(self.model_path)
        if not isinstance(loaded_model, Pipeline):
            raise ValueError(
                "The saved transaction classification model is not a valid sklearn Pipeline."
            )

        self.model = loaded_model
        return loaded_model

    def get_model(self) -> Pipeline:
        if self.model is None:
            self.load_model()
        if self.model is None:
            raise RuntimeError(
                "Transaction classification model could not be loaded."
            )
        return self.model

    @classmethod
    def _rule_classify(cls, description: str) -> tuple[str, float] | None:
        normalized = " ".join(str(description).strip().upper().split())
        if not normalized:
            return None

        for category, confidence, keywords in cls.DESCRIPTION_RULES:
            for keyword in keywords:
                if keyword in normalized:
                    return category, confidence

        return None

    @classmethod
    def map_model_category(cls, model_category: str) -> str:
        normalized = str(model_category).strip().upper()
        return cls.MODEL_TO_FINSIGHT_CATEGORY.get(normalized, "OTHER")

    def classify_one(self, description: str) -> dict[str, Any]:
        normalized = " ".join(str(description or "").strip().upper().split())
        if not normalized:
            raise ValueError("Transaction description cannot be empty.")

        exact_category = self.EXACT_DESCRIPTION_CATEGORIES.get(normalized)
        if exact_category is not None:
            return {
                "category": exact_category,
                "mappedCategory": exact_category,
                "confidence": 0.995,
                "source": "EXACT_DESCRIPTION_RULE",
            }

        rule_result = self._rule_classify(normalized)
        if rule_result is not None:
            category, confidence = rule_result
            return {
                "category": category,
                "mappedCategory": category,
                "confidence": confidence,
                "source": "RULE_BASED_HIGH_CONFIDENCE",
            }

        model = self.get_model()
        probabilities = model.predict_proba([normalized])[0]
        class_index = int(probabilities.argmax())
        model_category = str(model.classes_[class_index])
        confidence = float(probabilities[class_index])
        mapped_category = self.map_model_category(model_category)

        return {
            "category": mapped_category,
            "mappedCategory": mapped_category,
            "confidence": round(confidence, 4),
            "source": "TFIDF_LOGISTIC_REGRESSION",
            "modelCategory": model_category,
        }

    def classify(self, transactions: list[dict[str, Any]]) -> dict[str, Any]:
        if transactions is None:
            transactions = []

        if not isinstance(transactions, list):
            raise ValueError("Transactions must be a list.")

        classifications: list[dict[str, Any]] = []

        for transaction in transactions:
            if not isinstance(transaction, dict):
                continue

            description = str(transaction.get("description") or "").strip()
            if not description:
                raise ValueError("Every transaction must have a description.")

            result = self.classify_one(description)
            classifications.append(
                {
                    "id": transaction.get("id"),
                    "description": description,
                    "category": result["category"],
                    "confidence": result["confidence"],
                    "mappedCategory": result["mappedCategory"],
                    "source": result["source"],
                }
            )

        return {
            "status": "SUCCESS",
            "message": "Transaction classification completed.",
            "totalTransactions": len(transactions),
            "classifiedTransactions": len(classifications),
            "classifications": classifications,
        }


transaction_classifier = TransactionClassifier()
