import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CaseService } from '../case';
import { CasePage } from '../models/case.model';

@Component({
  selector: 'app-case-list',
  imports: [CommonModule, RouterModule],
  templateUrl: './case-list.html',
  styleUrl: './case-list.scss',
})
export class CaseList implements OnInit {

  casePage = signal<CasePage | null>(null);
  selectedStatus = signal<string>('PENDING');
  currentPage = signal<number>(0);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  statusOptions = ['PENDING', 'APPROVED', 'BLOCKED'];

  constructor(private caseService: CaseService) {}

  ngOnInit(): void {
    this.loadCases();
  }

  loadCases(): void {
    this.loading.set(true);
    this.error.set(null);

    this.caseService.getCases(this.selectedStatus(), this.currentPage()).subscribe({
      next: (page) => {
        this.casePage.set(page);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load cases. Is the Payment Service running?');
        this.loading.set(false);
      }
    });
  }

  onStatusChange(status: string): void {
    this.selectedStatus.set(status);
    this.currentPage.set(0);
    this.loadCases();
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadCases();
  }

  getRiskClass(score: number): string {
    if (score >= 0.7) return 'risk-high';
    if (score >= 0.4) return 'risk-medium';
    return 'risk-low';
  }
}
