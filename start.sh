#!/bin/bash

# fraud-pipeline startup script
# Run from the project root: ./start.sh

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

start_fraud_scoring() {
  echo "Starting Fraud Scoring Service..."
  cd "$ROOT_DIR/services/fraud-scoring"

  if [ ! -f "model/xgb_model.pkl" ]; then
    echo "ERROR: model/xgb_model.pkl not found. Run 'python train.py' first."
    exit 1
  fi

  conda run -n fraud-pipeline uvicorn api:app --host 0.0.0.0 --port 8001 &
  FRAUD_PID=$!
  echo "Fraud Scoring Service started (PID $FRAUD_PID) → http://localhost:8001"
}

stop_all() {
  echo "Stopping all services..."
  lsof -ti :8001 | xargs kill -9 2>/dev/null && echo "Fraud Scoring Service stopped"
  lsof -ti :8080 | xargs kill -9 2>/dev/null && echo "Payment Service stopped"
}

case "${1:-start}" in
  start)
    start_fraud_scoring
    echo ""
    echo "Services running:"
    echo "  Fraud Scoring  → http://localhost:8001/docs"
    echo ""
    echo "Run './start.sh stop' to shut everything down."
    ;;
  stop)
    stop_all
    ;;
  *)
    echo "Usage: ./start.sh [start|stop]"
    exit 1
    ;;
esac
