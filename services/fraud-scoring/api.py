"""
Fraud Scoring Service — FastAPI application.

Loads the trained XGBoost model and SHAP explainer once at startup,
then scores incoming transactions on POST /score.
"""

import pickle
from contextlib import asynccontextmanager
from typing import Optional

import pandas as pd
import shap
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from features import engineer_features, FEATURE_COLUMNS

LOW_THRESHOLD = 0.30
REVIEW_THRESHOLD = 0.50

MODEL_PATH = "model/xgb_model.pkl"

# Loaded once at startup, shared across all requests
model = None
explainer = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global model, explainer
    try:
        with open(MODEL_PATH, "rb") as f:
            model = pickle.load(f)
        explainer = shap.TreeExplainer(model)
        print("Model and explainer loaded successfully.")
    except FileNotFoundError:
        raise RuntimeError(
            f"Model not found at {MODEL_PATH}. Run train.py first."
        )
    yield


app = FastAPI(title="Fraud Scoring Service", lifespan=lifespan)


class TransactionRequest(BaseModel):
    TransactionAmt: float
    card1: Optional[float] = None
    card2: Optional[float] = None
    card3: Optional[float] = None
    card5: Optional[float] = None
    addr1: Optional[float] = None
    addr2: Optional[float] = None
    dist1: Optional[float] = None
    dist2: Optional[float] = None
    C1: Optional[float] = None
    C2: Optional[float] = None
    C3: Optional[float] = None
    C4: Optional[float] = None
    C5: Optional[float] = None
    C6: Optional[float] = None
    C7: Optional[float] = None
    C8: Optional[float] = None
    C9: Optional[float] = None
    C10: Optional[float] = None
    C11: Optional[float] = None
    C12: Optional[float] = None
    C13: Optional[float] = None
    C14: Optional[float] = None
    D1: Optional[float] = None
    D2: Optional[float] = None
    D3: Optional[float] = None
    D4: Optional[float] = None
    D5: Optional[float] = None
    D10: Optional[float] = None
    D11: Optional[float] = None
    D15: Optional[float] = None
    V1: Optional[float] = None
    V2: Optional[float] = None
    V3: Optional[float] = None
    V4: Optional[float] = None
    V5: Optional[float] = None
    V6: Optional[float] = None
    V7: Optional[float] = None
    V8: Optional[float] = None
    V9: Optional[float] = None
    V10: Optional[float] = None
    V11: Optional[float] = None
    V12: Optional[float] = None
    V13: Optional[float] = None
    V14: Optional[float] = None
    V15: Optional[float] = None
    V16: Optional[float] = None
    V17: Optional[float] = None
    V18: Optional[float] = None
    V19: Optional[float] = None
    V20: Optional[float] = None


class ShapValue(BaseModel):
    feature: str
    value: float
    contribution: float


class ScoreResponse(BaseModel):
    score: float
    risk_level: str
    shap_values: list[ShapValue]


@app.post("/score", response_model=ScoreResponse)
def score(request: TransactionRequest):
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    df = pd.DataFrame([request.model_dump()])
    X = engineer_features(df)

    fraud_score = float(model.predict_proba(X)[0][1])

    if fraud_score < LOW_THRESHOLD:
        risk_level = "LOW"
    elif fraud_score < REVIEW_THRESHOLD:
        risk_level = "REVIEW"
    else:
        risk_level = "HIGH"

    shap_vals = explainer.shap_values(X)[0]
    top_features = sorted(
        zip(FEATURE_COLUMNS, X.values[0], shap_vals),
        key=lambda x: abs(x[2]),
        reverse=True,
    )[:10]

    return ScoreResponse(
        score=fraud_score,
        risk_level=risk_level,
        shap_values=[
            ShapValue(feature=f, value=float(v), contribution=float(s))
            for f, v, s in top_features
        ],
    )


@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": model is not None}
