"""
One-time training script. Run this offline before starting the API.

Usage:
    cd services/fraud-scoring
    python train.py

Expects:
    data/train_transaction.csv
    data/train_identity.csv

Produces:
    model/xgb_model.pkl
"""

import pickle
import pandas as pd
import xgboost as xgb
from sklearn.model_selection import train_test_split
from sklearn.metrics import average_precision_score
from features import engineer_features

print("Loading dataset...")
transactions = pd.read_csv("data/train_transaction.csv")
identity = pd.read_csv("data/train_identity.csv")

df = transactions.merge(identity, on="TransactionID", how="left")
print(f"Dataset shape: {df.shape} | Fraud rate: {df['isFraud'].mean():.2%}")

X = engineer_features(df)
y = df["isFraud"]

X_train, X_val, y_train, y_val = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

# scale_pos_weight compensates for class imbalance (~28x more non-fraud than fraud)
scale = (y_train == 0).sum() / (y_train == 1).sum()
print(f"scale_pos_weight: {scale:.1f}")

print("Training XGBoost model...")
model = xgb.XGBClassifier(
    n_estimators=500,
    max_depth=6,
    learning_rate=0.05,
    subsample=0.8,
    colsample_bytree=0.8,
    scale_pos_weight=scale,
    eval_metric="aucpr",
    early_stopping_rounds=30,
    random_state=42,
    n_jobs=-1,
)

model.fit(
    X_train, y_train,
    eval_set=[(X_val, y_val)],
    verbose=50,
)

val_probs = model.predict_proba(X_val)[:, 1]
pr_auc = average_precision_score(y_val, val_probs)
print(f"\nValidation PR-AUC: {pr_auc:.4f}")

output_path = "model/xgb_model.pkl"
with open(output_path, "wb") as f:
    pickle.dump(model, f)
print(f"Model saved to {output_path}")
