import pandas as pd

# These are the exact columns the model is trained on.
# Never add/remove/rename a column here without retraining the model.
FEATURE_COLUMNS = [
    'TransactionAmt',
    'card1', 'card2', 'card3', 'card5',
    'addr1', 'addr2',
    'dist1', 'dist2',
    'C1', 'C2', 'C3', 'C4', 'C5', 'C6', 'C7', 'C8', 'C9', 'C10', 'C11', 'C12', 'C13', 'C14',
    'D1', 'D2', 'D3', 'D4', 'D5', 'D10', 'D11', 'D15',
    'V1', 'V2', 'V3', 'V4', 'V5', 'V6', 'V7', 'V8', 'V9', 'V10',
    'V11', 'V12', 'V13', 'V14', 'V15', 'V16', 'V17', 'V18', 'V19', 'V20',
]


def engineer_features(df: pd.DataFrame) -> pd.DataFrame:
    """
    Apply feature engineering to a DataFrame of transactions.
    Must be called identically during training (train.py) and inference (api.py).
    """
    df = df.copy()

    # Add any columns missing from the input so the model always sees the same shape
    for col in FEATURE_COLUMNS:
        if col not in df.columns:
            df[col] = -999

    # Fill missing values with -999 so XGBoost can distinguish "missing" from zero
    df[FEATURE_COLUMNS] = df[FEATURE_COLUMNS].fillna(-999)

    return df[FEATURE_COLUMNS]
