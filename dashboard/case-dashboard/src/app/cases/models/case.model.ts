export interface ShapValue {
  feature: string;
  value: number;
  contribution: number;
}

export interface Case {
  id: string;
  payment: {
    id: string;
    amount: number;
    currency: string;
    status: string;
    createdAt: string;
  };
  fraudScore: number;
  shapValues: string; // stored as JSON string, parsed on use
  status: 'PENDING' | 'APPROVED' | 'BLOCKED';
  analystId: string | null;
  notes: string | null;
  decisionAt: string | null;
  createdAt: string;
}

export interface CasePage {
  content: Case[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface DecisionRequest {
  decision: 'APPROVE' | 'BLOCK';
  analystId: string;
  notes: string;
}
