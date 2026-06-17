import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Case, CasePage, DecisionRequest } from './models/case.model';

@Injectable({
  providedIn: 'root'
})
export class CaseService {

  private readonly apiUrl = '/api/cases';

  constructor(private http: HttpClient) {}

  getCases(status?: string, page: number = 0, size: number = 20): Observable<CasePage> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<CasePage>(this.apiUrl, { params });
  }

  getCase(id: string): Observable<Case> {
    return this.http.get<Case>(`${this.apiUrl}/${id}`);
  }

  decide(id: string, request: DecisionRequest): Observable<Case> {
    return this.http.post<Case>(`${this.apiUrl}/${id}/decision`, request);
  }
}
