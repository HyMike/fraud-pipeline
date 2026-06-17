import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CaseService } from '../case';
import { Case, CasePage } from '../models/case.model';

@Component({
  selector: 'app-case-list',
  imports: [CommonModule, RouterModule],
  templateUrl: './case-list.html',
  styleUrl: './case-list.scss',
})
export class CaseList implements OnInit {

  casePage: CasePage | null = null;
  selectedStatus: string = 'PENDING';
  currentPage: number = 0;
  loading: boolean = false;
  error: string | null = null;

  statusOptions = ['PENDING', 'APPROVED', 'BLOCKED'];

  constructor(private caseService: CaseService) {}

  ngOnInit(): void {
    this.loadCases();
  }

  loadCases(): void {
    this.loading = true;
    this.error = null;

    this.caseService.getCases(this.selectedStatus, this.currentPage).subscribe({
      next: (page) => {
        this.casePage = page;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load cases. Is the Payment Service running?';
        this.loading = false;
      }
    });
  }

  onStatusChange(status: string): void {
    this.selectedStatus = status;
    this.currentPage = 0;
    this.loadCases();
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadCases();
  }

  getRiskClass(score: number): string {
    if (score >= 0.7) return 'risk-high';
    if (score >= 0.4) return 'risk-medium';
    return 'risk-low';
  }
}
