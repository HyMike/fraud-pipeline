import { Component, OnInit, AfterViewInit, ElementRef, ViewChild, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Chart, BarController, BarElement, CategoryScale, LinearScale, Tooltip } from 'chart.js';
import { CaseService } from '../case';
import { Case, ShapValue, DecisionRequest } from '../models/case.model';

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip);

@Component({
  selector: 'app-case-detail',
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './case-detail.html',
  styleUrl: './case-detail.scss',
})
export class CaseDetail implements OnInit, AfterViewInit {

  @ViewChild('shapChart') chartCanvas!: ElementRef<HTMLCanvasElement>;

  fraudCase = signal<Case | null>(null);
  shapValues = signal<ShapValue[]>([]);
  loading = signal(false);
  submitting = signal(false);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  analystId = '';
  notes = '';
  private chart: Chart | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private caseService: CaseService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadCase(id);
  }

  ngAfterViewInit(): void {
    if (this.shapValues().length > 0) this.renderChart();
  }

  loadCase(id: string): void {
    this.loading.set(true);
    this.caseService.getCase(id).subscribe({
      next: (c) => {
        this.fraudCase.set(c);
        this.shapValues.set(JSON.parse(c.shapValues));
        this.loading.set(false);
        setTimeout(() => this.renderChart(), 0);
      },
      error: () => {
        this.error.set('Failed to load case.');
        this.loading.set(false);
      }
    });
  }

  renderChart(): void {
    if (!this.chartCanvas || this.shapValues().length === 0) return;

    if (this.chart) this.chart.destroy();

    const sorted = [...this.shapValues()].sort((a, b) => b.contribution - a.contribution);
    const labels = sorted.map(s => s.feature);
    const data = sorted.map(s => s.contribution);
    const colors = data.map(v => v >= 0 ? '#dc3545' : '#28a745');

    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          data,
          backgroundColor: colors,
          borderRadius: 4,
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        plugins: { legend: { display: false } },
        scales: {
          x: { title: { display: true, text: 'Contribution to Fraud Score' } }
        }
      }
    });
  }

  submit(decision: 'APPROVE' | 'BLOCK'): void {
    const fc = this.fraudCase();
    if (!fc || !this.analystId.trim()) return;

    this.submitting.set(true);
    const request: DecisionRequest = {
      decision,
      analystId: this.analystId,
      notes: this.notes
    };

    this.caseService.decide(fc.id, request).subscribe({
      next: () => {
        this.successMessage.set(`Payment ${decision === 'APPROVE' ? 'approved' : 'blocked'} successfully.`);
        this.submitting.set(false);
        setTimeout(() => this.router.navigate(['/cases']), 1500);
      },
      error: () => {
        this.error.set('Failed to submit decision.');
        this.submitting.set(false);
      }
    });
  }

  getRiskClass(score: number): string {
    if (score >= 0.7) return 'risk-high';
    if (score >= 0.4) return 'risk-medium';
    return 'risk-low';
  }
}
